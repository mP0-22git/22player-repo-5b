package com.kabouzeid.trebl.ui.fragments.player;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.glide.PhonographColoredTarget;
import com.kabouzeid.trebl.glide.SongGlideRequest;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;
import com.kabouzeid.trebl.helper.MusicProgressViewUpdateHelper;
import com.kabouzeid.trebl.helper.PlayPauseButtonOnClickHandler;
import com.kabouzeid.trebl.ui.fragments.AbsMusicServiceFragment;
import com.kabouzeid.trebl.views.PlayPauseDrawable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MiniPlayerFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    private Unbinder unbinder;

    @BindView(R.id.mini_player_title)
    TextView miniPlayerTitle;

    @BindView(R.id.mini_player_artist)
    TextView miniPlayerArtist;

    @BindView(R.id.mini_player_image)
    ImageView miniPlayerImage;

    @BindView(R.id.mini_player_play_pause_button)
    ImageView miniPlayerPlayPauseButton;

    @BindView(R.id.progress_indicator)
    CircularProgressIndicator progressIndicator;

    @BindView(R.id.progress_indicator_dummy)
    CircularProgressIndicator dummyIndicator;

    @BindView(R.id.mini_player_bg_tint)
    ImageView miniPlayerBgTint;

    int lastColor = Color.BLACK;

    private PlayPauseDrawable miniPlayerPlayPauseDrawable;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_player, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);

        view.setOnTouchListener(new FlingPlayBackController(getActivity()));
        setUpMiniPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void setUpMiniPlayer() {
        setUpPlayPauseButton();
    }

    private void setUpPlayPauseButton() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(R.attr.textColor, typedValue, true);
        @ColorInt int color = typedValue.data;
        miniPlayerPlayPauseDrawable = new PlayPauseDrawable(getActivity());
        miniPlayerPlayPauseButton.setScaleX(0.8f);
        miniPlayerPlayPauseButton.setScaleY(0.8f);
        miniPlayerPlayPauseButton.setImageDrawable(miniPlayerPlayPauseDrawable);
        miniPlayerPlayPauseButton.setColorFilter(color,PorterDuff.Mode.SRC_IN);
        miniPlayerPlayPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
    }

    private void updateSongTitle() {
        miniPlayerTitle.setText(MusicPlayerRemote.getCurrentSong().title);
        miniPlayerArtist.setText(MusicPlayerRemote.getCurrentSong().artistName);

        SongGlideRequest.Builder.from(Glide.with(getActivity()), MusicPlayerRemote.getCurrentSong())
                .checkIgnoreMediaStore(getActivity())
                .generatePalette(getActivity()).build()
                .placeholder(R.drawable.default_album_art)
                .error(R.drawable.default_album_art)
                .into(new PhonographColoredTarget(miniPlayerImage) {
                    @Override
                    public void onColorReady(int color) {
                        ValueAnimator anim = new ValueAnimator();
                        anim.setIntValues(lastColor, color);
                        anim.setEvaluator(new ArgbEvaluator());
                        anim.addUpdateListener(valueAnimator -> {
                            if(miniPlayerBgTint!=null) {
                                miniPlayerBgTint.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
                                progressIndicator.setIndicatorColor(color);
                                dummyIndicator.setTrackColor((Integer)valueAnimator.getAnimatedValue());
                            }
                        });
                        anim.setDuration(1000);
                        anim.setInterpolator(new FastOutSlowInInterpolator());
                        anim.start();
                        lastColor = color;
                    }
                });

    }

    @Override
    public void onServiceConnected() {
        updateSongTitle();
        updatePlayPauseDrawableState(false);
    }

    @Override
    public void onPlayingMetaChanged() {
        updateSongTitle();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        progressIndicator.setMax(total);
        progressIndicator.setProgress(progress);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    private static class FlingPlayBackController implements View.OnTouchListener {

        GestureDetector flingPlayBackController;

        public FlingPlayBackController(Context context) {
            flingPlayBackController = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong();
                            return true;
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return flingPlayBackController.onTouchEvent(event);
        }
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            miniPlayerPlayPauseDrawable.setPause(animate);
        } else {
            miniPlayerPlayPauseDrawable.setPlay(animate);
        }
    }
}
