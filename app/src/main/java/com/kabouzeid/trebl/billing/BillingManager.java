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
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper class for Google Play Billing Library 7.
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

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                ProductDetails productDetails = productDetailsList.get(0);
                launchBillingFlow(activity, productDetails);
            } else {
                Log.e(TAG, "Failed to get product details: " + billingResult.getResponseCode());
                notifyBillingError(billingResult.getResponseCode(), "Failed to get product details");
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
     * Restore purchases by querying Google Play.
     */
    public void restorePurchases() {
        if (!isConnected) {
            Log.e(TAG, "Cannot restore - not connected");
            // Try to reconnect first
            startConnection();
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                purchasedProducts.clear();
                boolean hasPurchase = false;

                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        purchasedProducts.addAll(purchase.getProducts());
                        acknowledgePurchaseIfNeeded(purchase);
                        hasPurchase = true;
                    }
                }

                notifyPurchaseRestored(hasPurchase);
            } else {
                Log.e(TAG, "Failed to restore purchases: " + billingResult.getResponseCode());
                notifyBillingError(billingResult.getResponseCode(), "Failed to restore purchases");
            }
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

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
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
        });
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
