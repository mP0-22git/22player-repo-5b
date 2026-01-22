package com.kabouzeid.trebl.ui.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.BuildConfig;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.billing.BillingManager;
import com.kabouzeid.trebl.ui.activities.base.AbsBaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PurchaseActivity extends AbsBaseActivity implements BillingManager.BillingCallback {

    public static final String TAG = PurchaseActivity.class.getSimpleName();

    @BindView(R.id.restore_button)
    TextView restoreButton;

    @BindView(R.id.buy_button)
    Button purchaseButton;

    @BindView(R.id.close_button)
    ImageView closeButton;

    @BindView(R.id.subscription_terms)
    TextView subscriptionTerms;

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        restoreButton.setEnabled(false);
        purchaseButton.setEnabled(false);

        // Get billing manager instance
        billingManager = BillingManager.getInstance(this);
        billingManager.addCallback(this);

        // Set up button click listeners
        restoreButton.setOnClickListener(v -> billingManager.restorePurchases());

        purchaseButton.setOnClickListener(v -> billingManager.purchaseSubscription(PurchaseActivity.this, App.PRO_SUBSCRIPTION_PRODUCT_ID, BuildConfig.SUBSCRIPTION_OFFER_ID));

        closeButton.setOnClickListener(v -> onBackPressed());

        // If billing is already ready, enable buttons and load price
        if (billingManager.isReady()) {
            onBillingReady();
        }
    }

    @Override
    public void onPurchaseComplete(String productId) {
        Toast.makeText(this, R.string.thank_you, Toast.LENGTH_SHORT).show();
        App.notifyProVersionChanged();
    }

    @Override
    public void onPurchaseRestored(boolean hasPurchase) {
        if (hasPurchase) {
            Toast.makeText(this, R.string.restored_previous_purchase_please_restart, Toast.LENGTH_LONG).show();
            App.notifyProVersionChanged();
        } else {
            Toast.makeText(this, R.string.no_purchase_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBillingError(int errorCode, String message) {
        Log.e(TAG, "Billing error: code = " + errorCode + ", message = " + message);
    }

    @Override
    public void onBillingReady() {
        restoreButton.setEnabled(true);
        purchaseButton.setEnabled(true);

        // Load subscription details
        billingManager.querySubscriptionDetails(App.PRO_SUBSCRIPTION_PRODUCT_ID, new BillingManager.SubscriptionDetailsCallback() {
            @Override
            public void onSubscriptionDetails(String weeklyPrice, String trialPeriod) {
                purchaseButton.setText(R.string.start_free_trial);
                subscriptionTerms.setText(getString(R.string.subscription_terms, weeklyPrice));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get subscription details: " + error);
                purchaseButton.setText(R.string.start_free_trial);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (billingManager != null) {
            billingManager.removeCallback(this);
        }
        super.onDestroy();
    }
}
