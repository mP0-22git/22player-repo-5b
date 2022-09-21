package com.kabouzeid.trebl.ui.fragments.player;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.kabouzeid.trebl.R;

public enum NowPlayingScreen {
    CARD(R.string.card, R.drawable.nowplaying_1, 0),
    FLAT(R.string.flat, R.drawable.nowplaying_2, 1),
    BLUR(R.string.blur, R.drawable.nowplaying_3, 2);

    @StringRes
    public final int titleRes;
    @DrawableRes
    public final int drawableResId;
    public final int id;

    NowPlayingScreen(@StringRes int titleRes, @DrawableRes int drawableResId, int id) {
        this.titleRes = titleRes;
        this.drawableResId = drawableResId;
        this.id = id;
    }
}
