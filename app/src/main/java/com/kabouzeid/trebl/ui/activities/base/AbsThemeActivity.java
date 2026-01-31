package com.kabouzeid.trebl.ui.activities.base;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.util.TypedValue;
import android.view.View;

import com.kabouzeid.appthemehelper.ATH;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.ATHToolbarActivity;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialDialogsUtil;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.util.PreferenceUtil;
import com.kabouzeid.trebl.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public abstract class AbsThemeActivity extends ATHToolbarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceUtil.getInstance(this).getGeneralTheme());
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        MaterialDialogsUtil.updateMaterialDialogsThemeSingleton(this);
    }

    protected void setDrawUnderStatusbar() {
            Util.setAllowDrawUnderStatusBar(getWindow());
    }

    public void setStatusbarColor(int color) {
        final View statusBar = getWindow().getDecorView().getRootView().findViewById(R.id.status_bar);
        if (statusBar != null) {
            statusBar.setBackgroundColor(color);
        }
        setLightStatusbarAuto(color);
    }

    public void setStatusbarColorAuto() {
        int generalTheme = PreferenceUtil.getInstance(this).getGeneralTheme();
        if (generalTheme == R.style.Theme_Phonograph_ClassicLight || generalTheme == R.style.Theme_Phonograph_ClassicDark) {
            setStatusbarColor(ThemeStore.primaryColor((this)));
        } else {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.statusBarColor, typedValue, true);
            int statusBarColor = typedValue.data;
            setLightStatusbarAuto(statusBarColor);
        }
    }

    public void setTaskDescriptionColor(@ColorInt int color) {
        ATH.setTaskDescriptionColor(this, color);
    }

    public void setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeStore.primaryColor(this));
    }

    public void setNavigationbarColor(int color) {
        int targetColor;
        if (ThemeStore.coloredNavigationBar(this)) {
            targetColor = color;
        } else {
            targetColor = Color.BLACK;
        }
        View navBarView = getWindow().getDecorView().getRootView().findViewById(R.id.navigation_bar);
        if (navBarView != null) {
            navBarView.setBackgroundColor(targetColor);
        }
    }

    public void setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeStore.navigationBarColor(this));
    }

    public void setLightStatusbar(boolean enabled) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(enabled);
    }

    public void setLightStatusbarAuto(int bgColor) {
        setLightStatusbar(ColorUtil.isColorLight(bgColor));
    }
}
