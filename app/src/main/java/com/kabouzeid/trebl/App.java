package com.kabouzeid.trebl;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.trebl.appshortcuts.DynamicShortcutManager;
import com.kabouzeid.trebl.billing.BillingManager;
import com.superwall.sdk.Superwall;


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

    public static final String PRO_VERSION_PRODUCT_ID = BuildConfig.PRO_PRODUCT_ID;
    public static final String PRO_SUBSCRIPTION_PRODUCT_ID = BuildConfig.PRO_SUBSCRIPTION_ID;
    private static final String PREF_PRO_STATUS_CACHED = "pro_status_cached";

    private static App app;

    private BillingManager billingManager;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        // Configure Superwall
        Superwall.configure(this, BuildConfig.SUPERWALL_API_KEY);

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
                updateCachedProStatus();
                notifyProVersionChanged();
            }

            @Override
            public void onPurchaseRestored(boolean hasPurchase) {
                if (updateCachedProStatus()) {
                    notifyProVersionChanged();
                }
            }

            @Override
            public void onBillingError(int errorCode, String message) {
                // Errors are logged in BillingManager
            }

            @Override
            public void onBillingReady() {
                if (updateCachedProStatus()) {
                    notifyProVersionChanged();
                }
            }
        });
    }

    public static boolean isProVersion() {
        boolean billingPro = app.billingManager.isPurchased(PRO_VERSION_PRODUCT_ID) ||
                app.billingManager.isPurchased(PRO_SUBSCRIPTION_PRODUCT_ID);
        if (billingPro) return true;
        // Fall back to cached status while billing is still loading
        return PreferenceManager.getDefaultSharedPreferences(app)
                .getBoolean(PREF_PRO_STATUS_CACHED, false);
    }

    /**
     * Updates cached pro status. Returns true if the status changed.
     */
    private static boolean updateCachedProStatus() {
        boolean isPro = app.billingManager.isPurchased(PRO_VERSION_PRODUCT_ID) ||
                app.billingManager.isPurchased(PRO_SUBSCRIPTION_PRODUCT_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        boolean wasPro = prefs.getBoolean(PREF_PRO_STATUS_CACHED, false);
        prefs.edit().putBoolean(PREF_PRO_STATUS_CACHED, isPro).apply();
        return isPro != wasPro;
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
