package com.kabouzeid.trebl.ui.rating;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.preference.PreferenceManager;
import com.kabouzeid.trebl.R;

public class FiveStarsDialog {

    private final static String SP_NUM_OF_ACCESS = "numOfAccess";
    private static final String SP_DISABLED = "disabled";

    private final static String EMOJI_VERY_NEGATIVE = "\uD83D\uDE2D";
    private final static String EMOJI_NEGATIVE = "\uD83D\uDE22";
    private final static String EMOJI_NEUTRAL = "\uD83D\uDE41";
    private final static String EMOJI_POSITIVE = "\uD83D\uDE42";
    private final static String EMOJI_VERY_POSITIVE = "\uD83D\uDCAF\uD83E\uDD70\uD83D\uDD25";
    private final Context context;

    private final SharedPreferences sharedPrefs;

    private RatingBar ratingBar;

    private AlertDialog alertDialog;
    private View dialogView;
    private int upperBound = 5;
    private int starColor = Color.parseColor("#FFB347");

    public FiveStarsDialog(Context context) {
        this.context = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void build() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.dialog_rating, null);
        Button positiveButton = dialogView.findViewById(R.id.button_positive);
        TextView negativeButton = dialogView.findViewById(R.id.button_negative);
        TextView icon = dialogView.findViewById(R.id.icon);

        ratingBar = dialogView.findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            switch ((int) v) {
                case 1:
                    icon.setText(EMOJI_VERY_NEGATIVE);
                    break;
                case 2:
                    icon.setText(EMOJI_NEGATIVE);
                    break;
                case 3:
                    icon.setText(EMOJI_NEUTRAL);
                    break;
                case 4:
                    icon.setText(EMOJI_POSITIVE);
                    break;
                case 5:
                    icon.setText(EMOJI_VERY_POSITIVE);
                    break;
            }

            positiveButton.setText(v >= upperBound ? "RATE ON GOOGLE PLAY" : "RATE");
        });

        positiveButton.setOnClickListener(view -> {
            if (ratingBar.getRating() >= upperBound) {
                openMarket();
            }
            disable();
            alertDialog.hide();
        });

        negativeButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt(SP_NUM_OF_ACCESS, 0);
            editor.apply();
            alertDialog.hide();
        });

        if (starColor != -1) {
            LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
            stars.getDrawable(1).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(2).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
        }

        dialogView.setBackgroundColor(Color.TRANSPARENT);
        dialogView.setBackgroundResource(R.drawable.rating_dialog_background);

        builder.setView(dialogView);

        alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void disable() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(SP_DISABLED, true);
        editor.apply();
    }

    private void openMarket() {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void show() {
        boolean disabled = sharedPrefs.getBoolean(SP_DISABLED, false);
        if (!disabled) {
            build();
            alertDialog.show();
        }
    }

    public void showAfter(int numberOfAccess) {
        build();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int numOfAccess = sharedPrefs.getInt(SP_NUM_OF_ACCESS, 0);
        editor.putInt(SP_NUM_OF_ACCESS, numOfAccess + 1);
        editor.apply();
        if (numOfAccess + 1 >= numberOfAccess) {
            show();
        }
    }

    public FiveStarsDialog setUpperBound(int bound) {
        this.upperBound = bound;
        return this;
    }

}

