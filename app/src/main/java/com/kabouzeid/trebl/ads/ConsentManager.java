package com.kabouzeid.trebl.ads;

import android.app.Activity;
import android.util.Log;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentManager {
    private static final String TAG = "ConsentManager";
    private final ConsentInformation consentInformation;

    public interface ConsentCallback {
        void onConsentResult(boolean canRequestAds);
    }

    public ConsentManager(Activity activity) {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    }

    public void requestConsentIfRequired(Activity activity, ConsentCallback callback) {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            () -> {
                // Consent info updated successfully
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity,
                    formError -> {
                        // Form dismissed or not needed
                        if (formError != null) {
                            Log.e(TAG, "Consent form error: " + formError.getMessage());
                        }
                        callback.onConsentResult(consentInformation.canRequestAds());
                    }
                );
            },
            requestConsentError -> {
                // Error updating consent info - still allow ads (fail open)
                Log.e(TAG, "Consent update error: " + requestConsentError.getMessage());
                callback.onConsentResult(true);
            }
        );
    }

    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    /**
     * Reset consent state for testing. Call this to see the consent dialog again.
     */
    public void reset() {
        consentInformation.reset();
    }
}
