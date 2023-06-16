package com.kabouzeid.trebl.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ui.activities.PurchaseActivity;
import com.kabouzeid.trebl.util.PreferenceUtil;

public class ThemeDialog {
    public static Dialog createThemeDialog(Activity activity){
        Dialog themeDialog = new Dialog(activity);
        themeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        themeDialog.setContentView(R.layout.dialog_theme_picker);

        ListView themeList = themeDialog.findViewById(R.id.list_theme);

        String[] themeValues = activity.getResources().getStringArray(R.array.pref_general_theme_list_values);
        String[] themeNames = activity.getResources().getStringArray(R.array.pref_general_theme_list_titles);

        ArrayAdapter mAdapter = new ArrayAdapter(activity, R.layout.list_item, R.id.text_view, themeNames);
        themeList.setAdapter(mAdapter);

        themeList.setOnItemClickListener((adapterView, view, i, l) -> {
            boolean isProTheme = PreferenceUtil.getInstance(activity).checkProTheme(themeValues[i], activity);
            if (!isProTheme || App.isProVersion()) {
                PreferenceUtil.getInstance(activity).setGeneralTheme((themeValues[i]));
                themeDialog.dismiss();
                activity.recreate();
            } else {
                themeDialog.dismiss();
                activity.startActivity(new Intent(activity, PurchaseActivity.class));
            }
        });

        themeDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        themeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        themeDialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        themeDialog.getWindow().setGravity(Gravity.BOTTOM);
        return themeDialog;
    }
}
