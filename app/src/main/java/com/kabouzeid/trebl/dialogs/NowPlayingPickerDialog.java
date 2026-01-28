package com.kabouzeid.trebl.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.heinrichreimersoftware.materialintro.view.InkPageIndicator;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ui.fragments.player.NowPlayingScreen;
import com.superwall.sdk.Superwall;
import com.superwall.sdk.paywall.presentation.PublicPresentationKt;
import com.kabouzeid.trebl.util.PreferenceUtil;
import com.kabouzeid.trebl.util.ViewUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class NowPlayingPickerDialog extends DialogFragment implements MaterialDialog.SingleButtonCallback, ViewPager.OnPageChangeListener {

    private DialogAction whichButtonClicked;
    private int viewPagerPosition;

    public static NowPlayingPickerDialog newInstance() {
        return new NowPlayingPickerDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_now_playing_picker, null);
        ViewPager viewPager = view.findViewById(R.id.now_playing_screen_view_pager);
        viewPager.setAdapter(new NowPlayingScreenAdapter(getContext()));
        viewPager.addOnPageChangeListener(this);
        viewPager.setPageMargin((int) ViewUtil.convertDpToPixel(32, getResources()));
        viewPager.setCurrentItem(PreferenceUtil.getInstance(getContext()).getNowPlayingScreen().ordinal());

        InkPageIndicator pageIndicator = view.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(viewPager);
        pageIndicator.onPageSelected(viewPager.getCurrentItem());

        return new MaterialDialog.Builder(getContext())
                .title(R.string.pref_title_now_playing_screen_appearance)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onAny(this)
                .customView(view, false)
                .build();
    }

    @Override
    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
        whichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (whichButtonClicked == DialogAction.POSITIVE) {
            if(viewPagerPosition!=2){
                PreferenceUtil.getInstance(getContext()).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                getActivity().recreate();
            }else if(App.isProVersion() && viewPagerPosition==2){
                PreferenceUtil.getInstance(getContext()).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                getActivity().recreate();
            }else{
                PublicPresentationKt.register(Superwall.Companion.getInstance(), "feature_now_playing");
            }

        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        this.viewPagerPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

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
