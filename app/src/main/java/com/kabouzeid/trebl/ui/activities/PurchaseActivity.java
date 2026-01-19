package com.kabouzeid.trebl.ui.activities;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.PurchaseInfo;
import com.anjlab.android.iab.v3.SkuDetails;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.BuildConfig;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ui.activities.base.AbsBaseActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PurchaseActivity extends AbsBaseActivity implements BillingProcessor.IBillingHandler {

    public static final String TAG = PurchaseActivity.class.getSimpleName();

    @BindView(R.id.restore_button)
    TextView restoreButton;

    @BindView(R.id.buy_button)
    Button purchaseButton;

    @BindView(R.id.close_button)
    ImageView closeButton;

    private BillingProcessor billingProcessor;

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

        restoreButton.setOnClickListener(v -> billingProcessor.loadOwnedPurchasesFromGoogleAsync(new BillingProcessor.IPurchasesResponseListener() {
            @Override
            public void onPurchasesSuccess() {

            }

            @Override
            public void onPurchasesError() {

            }
        }));

        purchaseButton.setOnClickListener(v -> billingProcessor.purchase(PurchaseActivity.this, App.PRO_VERSION_PRODUCT_ID));

        closeButton.setOnClickListener(v -> onBackPressed());

        billingProcessor = new BillingProcessor(this, BuildConfig.GOOGLE_PLAY_LICENSE_KEY, this);

    }

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable PurchaseInfo details) {
        Toast.makeText(this, R.string.thank_you, Toast.LENGTH_SHORT).show();
        App.notifyProVersionChanged();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        if (App.isProVersion()) {
            Toast.makeText(this, R.string.restored_previous_purchase_please_restart, Toast.LENGTH_LONG).show();
            App.notifyProVersionChanged();
        } else {
            Toast.makeText(this, R.string.no_purchase_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
        Log.e(TAG, "Billing error: code = " + errorCode, error);
    }

    @Override
    public void onBillingInitialized() {
        restoreButton.setEnabled(true);
        purchaseButton.setEnabled(true);
        billingProcessor.getPurchaseListingDetailsAsync(App.PRO_VERSION_PRODUCT_ID, new BillingProcessor.ISkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@Nullable List<SkuDetails> products) {
                if (products != null && !products.isEmpty()) {
                    purchaseButton.setText(products.get(0).priceText);
                }
            }

            @Override
            public void onSkuDetailsError(String error) {

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
        if (billingProcessor != null) {
            billingProcessor.release();
        }
        super.onDestroy();
    }
}
