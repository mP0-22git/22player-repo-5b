package com.kabouzeid.trebl.ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.BuildConfig;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages native ad loading and caching for song list integration.
 */
public class NativeAdManager {
    private static final String TAG = "NativeAdManager";
    // Keep only one ad staged. We refill it lazily the first time an ad slot is
    // actually bound, so screens that never render a slot never issue a request.
    private static final int CACHE_SIZE = 1;

    private final WeakReference<Activity> activityRef;
    private final Queue<NativeAd> adCache = new LinkedList<>();
    private final ConsentManager consentManager;
    private boolean isLoading = false;
    private boolean canShowAds = false;

    public interface AdLoadCallback {
        void onAdLoaded();
    }

    private AdLoadCallback adLoadCallback;

    public NativeAdManager(@NonNull Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.consentManager = new ConsentManager(activity);

        // Check if we can show ads (not pro, has consent)
        if (!App.isProVersion()) {
            canShowAds = consentManager.canRequestAds();
            // Deliberately no preload here. Loading is lazy: the first time an ad
            // slot is bound, getAd() triggers a load. This avoids requesting ads on
            // screens (short lists, quick open-and-back-out) that show no ad.
        }
    }

    /**
     * Returns true if ads should be displayed.
     */
    public boolean shouldShowAds() {
        return canShowAds && !App.isProVersion();
    }

    /**
     * Set a callback to be notified when ads are loaded.
     */
    public void setAdLoadCallback(AdLoadCallback callback) {
        this.adLoadCallback = callback;
    }

    /**
     * Preload ads to fill the cache.
     */
    public void preloadAds() {
        if (!shouldShowAds() || isLoading) {
            return;
        }

        int adsNeeded = CACHE_SIZE - adCache.size();
        if (adsNeeded > 0) {
            loadAds(adsNeeded, null);
        }
    }

    /**
     * Get a native ad from the cache if available.
     * Returns null if no ad is available.
     */
    @Nullable
    public NativeAd getAd() {
        if (!shouldShowAds()) {
            return null;
        }

        NativeAd ad = adCache.poll();

        // Refill the pool for the next slot. preloadAds() is a no-op if the pool is
        // already full or a load is in flight, so it is safe to call every time.
        preloadAds();

        return ad;
    }

    /**
     * Return an ad to the cache (e.g., when view is recycled but ad is still valid).
     */
    public void returnAd(@NonNull NativeAd ad) {
        if (adCache.size() < CACHE_SIZE) {
            adCache.offer(ad);
        } else {
            ad.destroy();
        }
    }

    private void loadAds(int count, @Nullable AdLoadCallback callback) {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        isLoading = true;
        final int targetCount = count;

        AdInitializer.runWhenReady(activity, () -> {
            Activity act = activityRef.get();
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                isLoading = false;
                return;
            }

            final int[] loaded = {0};

            for (int i = 0; i < targetCount; i++) {
                AdLoader adLoader = new AdLoader.Builder(act, BuildConfig.ADMOB_NATIVE_ID)
                        .forNativeAd(nativeAd -> {
                            if (adCache.size() < CACHE_SIZE) {
                                adCache.offer(nativeAd);
                                if (adLoadCallback != null) {
                                    adLoadCallback.onAdLoaded();
                                }
                            } else {
                                nativeAd.destroy();
                            }

                            loaded[0]++;
                            if (loaded[0] >= targetCount) {
                                isLoading = false;
                                if (callback != null) {
                                    callback.onAdLoaded();
                                }
                            }
                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                Log.e(TAG, "Native ad failed to load: " + loadAdError.getMessage());
                                loaded[0]++;
                                if (loaded[0] >= targetCount) {
                                    isLoading = false;
                                    if (callback != null) {
                                        callback.onAdLoaded();
                                    }
                                }
                            }
                        })
                        .build();

                adLoader.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    /**
     * Clean up all cached ads.
     */
    public void destroy() {
        while (!adCache.isEmpty()) {
            NativeAd ad = adCache.poll();
            if (ad != null) {
                ad.destroy();
            }
        }
    }

    /**
     * Refresh consent status and preload ads if consent was granted.
     */
    public void refreshConsent(@NonNull Activity activity, @Nullable Runnable onComplete) {
        if (App.isProVersion()) {
            canShowAds = false;
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        consentManager.requestConsentIfRequired(activity, canRequest -> {
            canShowAds = canRequest;
            // Loading is lazy (see getAd), so there is nothing to preload here.
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
