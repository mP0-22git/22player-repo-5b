package com.kabouzeid.trebl.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatusBarMarginFrameLayout extends FrameLayout {

    public StatusBarMarginFrameLayout(Context context) {
        super(context);
        init();
    }

    public StatusBarMarginFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusBarMarginFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            if (lp != null) {
                lp.topMargin = statusBarInsets.top;
                v.setLayoutParams(lp);
            }
            return insets;
        });
    }
}
