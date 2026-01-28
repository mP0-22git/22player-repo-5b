package com.kabouzeid.trebl.billing;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper class for Google Play Billing Library 8.
 * Provides a simple interface for in-app purchases.
 */
public class BillingManager {

    private static final String TAG = "BillingManager";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static BillingManager instance;
    private final Context context;
    private BillingClient billingClient;
    private final Set<String> purchasedProducts = new HashSet<>();
    private final List<BillingCallback> callbacks = new ArrayList<>();
    private boolean isConnected = false;
    private int retryAttempts = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Callback interfaces
    public interface BillingCallback {
        void onPurchaseComplete(String productId);
        void onPurchaseRestored(boolean hasPurchase);
        void onBillingError(int errorCode, String message);
        void onBillingReady();
    }

    public interface ProductDetailsCallback {
        void onProductDetails(String price);
        void onError(String error);
    }

    // Purchase update listener
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled the purchase");
        } else {
            Log.e(TAG, "Purchase error: " + billingResult.getResponseCode() + " - " + billingResult.getDebugMessage());
            notifyBillingError(billingResult.getResponseCode(), billingResult.getDebugMessage());
        }
    };

    private BillingManager(Context context) {
        this.context = context.getApplicationContext();
        initializeBillingClient();
    }

    /**
     * Get singleton instance of BillingManager.
     */
    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    /**
     * Initialize the BillingClient and connect to Google Play.
     */
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .enablePrepaidPlans()
                                .build()
                )
                .build();

        startConnection();
    }

    /**
     * Start connection to Google Play Billing service.
     */
    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected");
                    isConnected = true;
                    retryAttempts = 0;
                    queryExistingPurchases();
                    queryExistingSubscriptions();
                    notifyBillingReady();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getResponseCode());
                    isConnected = false;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected");
                isConnected = false;
                retryConnection();
            }
        });
    }

    /**
     * Retry connection with exponential backoff.
     */
    private void retryConnection() {
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts++;
            long delay = (long) Math.pow(2, retryAttempts) * 1000; // Exponential backoff
            Log.d(TAG, "Retrying connection in " + delay + "ms (attempt " + retryAttempts + ")");
            mainHandler.postDelayed(this::startConnection, delay);
        } else {
            Log.e(TAG, "Max retry attempts reached");
        }
    }

    /**
     * Query existing purchases from Google Play.
     */
    private void queryExistingPurchases() {
        if (!isConnected) {
            Log.w(TAG, "Cannot query purchases - not connected");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                boolean hadPurchases = !purchasedProducts.isEmpty();
                purchasedProducts.clear();

                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        purchasedProducts.addAll(purchase.getProducts());
                        acknowledgePurchaseIfNeeded(purchase);
                    }
                }

                Log.d(TAG, "Purchased products: " + purchasedProducts);

                // Notify if this was a restore operation and we found purchases
                if (!hadPurchases && !purchasedProducts.isEmpty()) {
                    notifyPurchaseRestored(true);
                }
            } else {
                Log.e(TAG, "Failed to query purchases: " + billingResult.getResponseCode());
            }
        });
    }

    /**
     * Query existing subscriptions from Google Play.
     */
    private void queryExistingSubscriptions() {
        if (!isConnected) {
            Log.w(TAG, "Cannot query subscriptions - not connected");
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        purchasedProducts.addAll(purchase.getProducts());
                        acknowledgePurchaseIfNeeded(purchase);
                    }
                }

                Log.d(TAG, "Active subscriptions queried. Purchased products: " + purchasedProducts);

                if (!purchasedProducts.isEmpty()) {
                    notifyPurchaseRestored(true);
                }
            } else {
                Log.e(TAG, "Failed to query subscriptions: " + billingResult.getResponseCode());
            }
        });
    }

    /**
     * Check if a product has been purchased.
     */
    public boolean isPurchased(String productId) {
        return purchasedProducts.contains(productId);
    }

    /**
     * Launch the purchase flow for a product.
     */
    public void purchase(Activity activity, String productId) {
        if (!isConnected) {
            Log.e(TAG, "Cannot purchase - not connected");
            notifyBillingError(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, "Billing service not connected");
            return;
        }

        // First query product details
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult result) {
                List<ProductDetails> productDetailsList = result.getProductDetailsList();
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                    ProductDetails productDetails = productDetailsList.get(0);
                    launchBillingFlow(activity, productDetails);
                } else {
                    Log.e(TAG, "Failed to get product details: " + billingResult.getResponseCode());
                    notifyBillingError(billingResult.getResponseCode(), "Failed to get product details");
                }
            }
        });
    }

    /**
     * Launch the actual billing flow.
     */
    private void launchBillingFlow(Activity activity, ProductDetails productDetails) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: " + result.getResponseCode());
            notifyBillingError(result.getResponseCode(), result.getDebugMessage());
        }
    }

    /**
     * Restore purchases by querying Google Play (both INAPP and SUBS).
     */
    public void restorePurchases() {
        if (!isConnected) {
            Log.e(TAG, "Cannot restore - not connected");
            startConnection();
            return;
        }

        purchasedProducts.clear();

        // Query one-time purchases
        QueryPurchasesParams inappParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(inappParams, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        purchasedProducts.addAll(purchase.getProducts());
                        acknowledgePurchaseIfNeeded(purchase);
                    }
                }
            }

            // Then query subscriptions
            QueryPurchasesParams subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();

            billingClient.queryPurchasesAsync(subsParams, (subsResult, subsPurchases) -> {
                if (subsResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : subsPurchases) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            purchasedProducts.addAll(purchase.getProducts());
                            acknowledgePurchaseIfNeeded(purchase);
                        }
                    }
                }

                notifyPurchaseRestored(!purchasedProducts.isEmpty());
            });
        });
    }

    /**
     * Query product details to get price.
     */
    public void queryProductDetails(String productId, ProductDetailsCallback callback) {
        if (!isConnected) {
            callback.onError("Billing service not connected");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult result) {
                List<ProductDetails> productDetailsList = result.getProductDetailsList();
                mainHandler.post(() -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                        ProductDetails productDetails = productDetailsList.get(0);
                        ProductDetails.OneTimePurchaseOfferDetails offerDetails = productDetails.getOneTimePurchaseOfferDetails();
                        if (offerDetails != null) {
                            callback.onProductDetails(offerDetails.getFormattedPrice());
                        } else {
                            callback.onError("No price available");
                        }
                    } else {
                        callback.onError("Failed to get product details");
                    }
                });
            }
        });
    }

    /**
     * Launch the purchase flow for a subscription.
     */
    public void purchaseSubscription(Activity activity, String productId, @Nullable String offerId) {
        if (!isConnected) {
            Log.e(TAG, "Cannot purchase subscription - not connected");
            notifyBillingError(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, "Billing service not connected");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult result) {
                List<ProductDetails> productDetailsList = result.getProductDetailsList();
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                    ProductDetails productDetails = productDetailsList.get(0);
                    launchSubscriptionBillingFlow(activity, productDetails, offerId);
                } else {
                    Log.e(TAG, "Failed to get subscription details: " + billingResult.getResponseCode());
                    notifyBillingError(billingResult.getResponseCode(), "Failed to get subscription details");
                }
            }
        });
    }

    /**
     * Launch the subscription billing flow with the correct offer token.
     */
    private void launchSubscriptionBillingFlow(Activity activity, ProductDetails productDetails, @Nullable String offerId) {
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            Log.e(TAG, "No subscription offers found");
            notifyBillingError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, "No subscription offers available");
            return;
        }

        // Find the offer with the matching ID (e.g., free trial), fall back to first offer
        ProductDetails.SubscriptionOfferDetails selectedOffer = null;
        if (offerId != null) {
            for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                if (offerId.equals(offer.getOfferId())) {
                    selectedOffer = offer;
                    break;
                }
            }
        }
        if (selectedOffer == null) {
            selectedOffer = offers.get(0);
        }

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(selectedOffer.getOfferToken())
                        .build()
        );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch subscription billing flow: " + result.getResponseCode());
            notifyBillingError(result.getResponseCode(), result.getDebugMessage());
        }
    }

    /**
     * Query subscription product details to get pricing and trial info.
     */
    public void querySubscriptionDetails(String productId, SubscriptionDetailsCallback callback) {
        if (!isConnected) {
            callback.onError("Billing service not connected");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult result) {
                List<ProductDetails> productDetailsList = result.getProductDetailsList();
                mainHandler.post(() -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                        ProductDetails productDetails = productDetailsList.get(0);
                        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();

                        if (offers != null && !offers.isEmpty()) {
                            // Get the base plan price (last pricing phase of the base offer)
                            String weeklyPrice = "";
                            String trialPeriod = "";

                            for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                                List<ProductDetails.PricingPhase> pricingPhases = offer.getPricingPhases().getPricingPhaseList();
                                for (ProductDetails.PricingPhase phase : pricingPhases) {
                                    if (phase.getPriceAmountMicros() == 0) {
                                        // This is the free trial phase
                                        trialPeriod = phase.getBillingPeriod();
                                    } else {
                                        // This is the paid phase
                                        weeklyPrice = phase.getFormattedPrice();
                                    }
                                }
                                // Use the first offer that has pricing info
                                if (!weeklyPrice.isEmpty()) break;
                            }

                            callback.onSubscriptionDetails(weeklyPrice, trialPeriod);
                        } else {
                            callback.onError("No subscription offers available");
                        }
                    } else {
                        callback.onError("Failed to get subscription details");
                    }
                });
            }
        });
    }

    /**
     * Callback for subscription product details.
     */
    public interface SubscriptionDetailsCallback {
        void onSubscriptionDetails(String weeklyPrice, String trialPeriod);
        void onError(String error);
    }

    /**
     * Handle a purchase (acknowledge and update state).
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            purchasedProducts.addAll(purchase.getProducts());
            acknowledgePurchaseIfNeeded(purchase);

            // Notify callbacks
            for (String productId : purchase.getProducts()) {
                notifyPurchaseComplete(productId);
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase pending: " + purchase.getProducts());
        }
    }

    /**
     * Acknowledge a purchase if not already acknowledged.
     * Purchases must be acknowledged within 3 days or they auto-refund.
     */
    private void acknowledgePurchaseIfNeeded(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(params, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully");
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getResponseCode());
                }
            });
        }
    }

    // Callback management

    public void addCallback(BillingCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void removeCallback(BillingCallback callback) {
        callbacks.remove(callback);
    }

    private void notifyPurchaseComplete(String productId) {
        mainHandler.post(() -> {
            for (BillingCallback callback : callbacks) {
                callback.onPurchaseComplete(productId);
            }
        });
    }

    private void notifyPurchaseRestored(boolean hasPurchase) {
        mainHandler.post(() -> {
            for (BillingCallback callback : callbacks) {
                callback.onPurchaseRestored(hasPurchase);
            }
        });
    }

    private void notifyBillingError(int errorCode, String message) {
        mainHandler.post(() -> {
            for (BillingCallback callback : callbacks) {
                callback.onBillingError(errorCode, message);
            }
        });
    }

    private void notifyBillingReady() {
        mainHandler.post(() -> {
            for (BillingCallback callback : callbacks) {
                callback.onBillingReady();
            }
        });
    }

    /**
     * Check if billing is ready.
     */
    public boolean isReady() {
        return isConnected;
    }

    /**
     * End connection and release resources.
     */
    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
        callbacks.clear();
        isConnected = false;
    }
}
