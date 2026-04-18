package com.kabouzeid.trebl.ads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

public final class AdInitializer {
    private enum State { NOT_STARTED, IN_PROGRESS, READY }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static State state = State.NOT_STARTED;
    private static final List<Runnable> pending = new ArrayList<>();

    private AdInitializer() {}

    public static void runWhenReady(@NonNull Context context, @NonNull Runnable onReady) {
        final Context appContext = context.getApplicationContext();
        boolean shouldStartInit = false;

        synchronized (LOCK) {
            if (state == State.READY) {
                MAIN.post(onReady);
                return;
            }
            pending.add(onReady);
            if (state == State.NOT_STARTED) {
                state = State.IN_PROGRESS;
                shouldStartInit = true;
            }
        }

        if (shouldStartInit) {
            MobileAds.initialize(appContext, initializationStatus -> {
                List<Runnable> toRun;
                synchronized (LOCK) {
                    state = State.READY;
                    toRun = new ArrayList<>(pending);
                    pending.clear();
                }
                for (Runnable r : toRun) {
                    MAIN.post(r);
                }
            });
        }
    }
}
