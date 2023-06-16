package com.kabouzeid.trebl.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.glide.BlurTransformation;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;

import java.util.Random;

public class BackgroundUtil {

    public static void setStarBg(ImageView starryBg) {
        int[] backgrounds = {
                R.drawable.gradient_stars_1,
                R.drawable.gradient_stars_2,
                R.drawable.gradient_stars_3,
                R.drawable.gradient_stars_4
        };
        starryBg.setImageResource(backgrounds[new Random().nextInt(4)]);
    }

    public static void setBlurBg(Context context, ImageView imageView) {
        Glide.with(context)
                .load(Util.getAlbumArtUri(MusicPlayerRemote.getCurrentSong().albumId))
                .transform(new BlurTransformation.Builder(context).build())
                .error(R.drawable.default_blur)
                .override(30, 30)
                .into(imageView);
    }

    public static void checkStarBg(Context context, ImageView imageView) {
        int generalTheme = PreferenceUtil.getInstance(context).getGeneralTheme();
        if (generalTheme == R.style.Theme_Phonograph_Stars) {
            BackgroundUtil.setStarBg(imageView);
        }
    }

    public static void checkWallpaperBg(Activity activity, ImageView customBg) {
        SharedPreferences pref = activity.getPreferences(Context.MODE_PRIVATE);
        String imageUriString = pref.getString("imageUri", "");
        if (!imageUriString.isEmpty()) {
            Uri imageUri = Uri.parse(imageUriString);
            Glide.with(activity).load(imageUri).into(customBg);
        }
    }

    public static void updateBlurBg(Context context, ImageView imageView) {
        // TODO: Migrate to Glide V4 and enable seamless transitions
        imageView.animate()
                .alpha(0.0f)
                .scaleX(2.0f)
                .scaleY(2.0f)
                .withEndAction(() -> {
                    setBlurBg(context, imageView);
                    imageView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(500).setInterpolator(new DecelerateInterpolator());
                })
                .start();
    }
}
