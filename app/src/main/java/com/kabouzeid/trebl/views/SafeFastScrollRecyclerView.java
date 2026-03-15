package com.kabouzeid.trebl.views;

import android.content.Context;
import android.util.AttributeSet;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

/**
 * Workaround for a divide-by-zero crash in {@link FastScrollRecyclerView#scrollToPositionAtProgress}
 * when the first visible child has zero height (e.g. during layout or with empty adapters).
 */
public class SafeFastScrollRecyclerView extends FastScrollRecyclerView {

    public SafeFastScrollRecyclerView(Context context) {
        super(context);
    }

    public SafeFastScrollRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeFastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        try {
            return super.scrollToPositionAtProgress(touchFraction);
        } catch (ArithmeticException e) {
            return "";
        }
    }
}
