package com.kabouzeid.trebl;

import android.app.Application;
import android.os.Build;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.trebl.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.trebl.billing.BillingManager;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

    public static final String PRO_VERSION_PRODUCT_ID = BuildConfig.PRO_PRODUCT_ID;
    public static final String PRO_SUBSCRIPTION_PRODUCT_ID = BuildConfig.PRO_SUBSCRIPTION_ID;

    private static App app;

    private BillingManager billingManager;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
                    .primaryColorRes(R.color.md_indigo_500)
                    .accentColorRes(R.color.md_red_900)
                    .commit();
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }

        // Initialize billing - automatically restores purchases
        billingManager = BillingManager.getInstance(this);
        billingManager.addCallback(new BillingManager.BillingCallback() {
            @Override
            public void onPurchaseComplete(String productId) {
                notifyProVersionChanged();
            }

            @Override
            public void onPurchaseRestored(boolean hasPurchase) {
                if (hasPurchase) {
                    notifyProVersionChanged();
                }
            }

            @Override
            public void onBillingError(int errorCode, String message) {
                // Errors are logged in BillingManager
            }

            @Override
            public void onBillingReady() {
                // Billing is ready, purchases have been queried
            }
        });
    }

    public static boolean isProVersion() {
        return BuildConfig.DEBUG ||
                app.billingManager.isPurchased(PRO_VERSION_PRODUCT_ID) ||
                app.billingManager.isPurchased(PRO_SUBSCRIPTION_PRODUCT_ID);
    }

    private static OnProVersionChangedListener onProVersionChangedListener;
    public static void setOnProVersionChangedListener(OnProVersionChangedListener listener) {
        onProVersionChangedListener = listener;
    }
    public static void notifyProVersionChanged() {
        if (onProVersionChangedListener != null) {
            onProVersionChangedListener.onProVersionChanged();
        }
    }
    public interface OnProVersionChangedListener {
        void onProVersionChanged();
    }

    public static App getInstance() {
        return app;
    }

    public static BillingManager getBillingManager() {
        return app.billingManager;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (billingManager != null) {
            billingManager.destroy();
        }
    }
}
