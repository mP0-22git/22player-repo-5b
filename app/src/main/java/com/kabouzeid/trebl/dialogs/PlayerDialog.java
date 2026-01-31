package com.kabouzeid.trebl.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ui.fragments.player.NowPlayingScreen;
import com.superwall.sdk.Superwall;
import com.superwall.sdk.paywall.presentation.PublicPresentationKt;
import com.kabouzeid.trebl.util.PreferenceUtil;
import com.kabouzeid.trebl.util.ViewUtil;

public class PlayerDialog {
    private static int viewPagerPosition;

    public static Dialog createPlayerDialog(Activity activity){
        Dialog playerDialog = new Dialog(activity);
        playerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        playerDialog.setContentView(R.layout.dialog_player_picker);
        ViewPager viewPager = playerDialog.findViewById(R.id.now_playing_screen_view_pager);
        viewPager.setAdapter(new NowPlayingScreenAdapter(activity));
        viewPager.setPageMargin((int) ViewUtil.convertDpToPixel(32, activity.getResources()));
        viewPager.setCurrentItem(PreferenceUtil.getInstance(activity).getNowPlayingScreen().ordinal());

        Button selectButton = playerDialog.findViewById(R.id.button_select_player);
        selectButton.setTextColor(ThemeStore.accentColor(activity));

        DotsIndicator pageIndicator = playerDialog.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewPagerPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        selectButton.setOnClickListener(view -> {
            if(viewPagerPosition!=2){
                PreferenceUtil.getInstance(activity).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                playerDialog.dismiss();
                activity.recreate();
            }else if(App.isProVersion() && viewPagerPosition==2){
                PreferenceUtil.getInstance(activity).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                playerDialog.dismiss();
                activity.recreate();
            }else{
                PublicPresentationKt.register(Superwall.Companion.getInstance(), "feature_now_playing");
            }
        });

        playerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        playerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        playerDialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        playerDialog.getWindow().setGravity(Gravity.BOTTOM);
        return playerDialog;
    }


    private static class NowPlayingScreenAdapter extends PagerAdapter {

        private Context context;

        public NowPlayingScreenAdapter(Context context) {
            this.context = context;
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            NowPlayingScreen nowPlayingScreen = NowPlayingScreen.values()[position];

            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.preference_now_playing_screen_item, collection, false);
            collection.addView(layout);

            ImageView image = layout.findViewById(R.id.image);
            TextView title = layout.findViewById(R.id.title);
            image.setImageResource(nowPlayingScreen.drawableResId);
            title.setText(nowPlayingScreen.titleRes);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return NowPlayingScreen.values().length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return context.getString(NowPlayingScreen.values()[position].titleRes);
        }
    }
}
