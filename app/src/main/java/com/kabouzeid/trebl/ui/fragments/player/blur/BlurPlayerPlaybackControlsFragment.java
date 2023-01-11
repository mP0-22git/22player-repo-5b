package com.kabouzeid.trebl.ui.fragments.player.blur;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.kabouzeid.appthemehelper.util.TintHelper;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;
import com.kabouzeid.trebl.helper.MusicProgressViewUpdateHelper;
import com.kabouzeid.trebl.helper.PlayPauseButtonOnClickHandler;
import com.kabouzeid.trebl.misc.SimpleOnSeekbarChangeListener;
import com.kabouzeid.trebl.service.MusicService;
import com.kabouzeid.trebl.ui.fragments.AbsMusicServiceFragment;
import com.kabouzeid.trebl.util.MusicUtil;
import com.kabouzeid.trebl.views.PlayPauseDrawable;

import java.util.Collection;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BlurPlayerPlaybackControlsFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    private Unbinder unbinder;

    @BindView(R.id.player_play_pause_button)
    ImageButton playPauseButton;
    @BindView(R.id.player_play_pause_fab)
    FloatingActionButton playPauseFab;
    @BindView(R.id.player_prev_button)
    ImageButton prevButton;
    @BindView(R.id.player_next_button)
    ImageButton nextButton;
    @BindView(R.id.player_replay10_button)
    ImageButton replayTenButton;
    @BindView(R.id.player_forward10_button)
    ImageButton forwardTenButton;
    @BindView(R.id.player_repeat_button)
    ImageButton repeatButton;
    @BindView(R.id.player_shuffle_button)
    ImageButton shuffleButton;
    @BindView(R.id.player_progress_slider)
    SeekBar progressSlider;
    @BindView(R.id.player_song_total_time)
    TextView songTotalTime;
    @BindView(R.id.player_song_current_progress)
    TextView songCurrentProgress;

    private PlayPauseDrawable playerFabPlayPauseDrawable;
    private PlayPauseDrawable playPauseDrawable;

    private int lastPlaybackControlsColor;
    private int lastDisabledPlaybackControlsColor;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    private AnimatorSet musicControllerAnimationSet;

    private boolean hidden = false;

    private Animation rotate1;
    private Animation rotate2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
        rotate1 = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate1);
        rotate1.setFillAfter(true);
        rotate2 = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate2);
        rotate2.setFillAfter(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blur_player_playback_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        setUpMusicControllers();
        updateProgressTextColor();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
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

    @Override
    public void onServiceConnected() {
        updatePlayPauseDrawableState(false);
        updateRepeatState();
        updateShuffleState();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
        if(MusicPlayerRemote.getCurrentSong().duration>600000){
            nextButton.setVisibility(View.GONE);
            prevButton.setVisibility(View.GONE);
            forwardTenButton.setVisibility(View.VISIBLE);
            replayTenButton.setVisibility(View.VISIBLE);
        }else if (MusicPlayerRemote.getCurrentSong().duration<600000){
            nextButton.setVisibility(View.VISIBLE);
            prevButton.setVisibility(View.VISIBLE);
            forwardTenButton.setVisibility(View.GONE);
            replayTenButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRepeatModeChanged() {
        updateRepeatState();
    }

    @Override
    public void onShuffleModeChanged() {
        updateShuffleState();
    }

    public void setDark(boolean dark) {
        if (dark) {
            lastPlaybackControlsColor = MaterialValueHelper.getSecondaryTextColor(getActivity(), true);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getSecondaryDisabledTextColor(getActivity(), true);
        } else {
            lastPlaybackControlsColor = MaterialValueHelper.getPrimaryTextColor(getActivity(), false);
            lastDisabledPlaybackControlsColor = MaterialValueHelper.getPrimaryDisabledTextColor(getActivity(), false);
        }

        updateRepeatState();
        updateShuffleState();
        updatePrevNextColor();
        updateProgressTextColor();
    }

    private void setUpPlayPauseFab() {
        //setting play pause background color
        TintHelper.setTintAuto(playPauseFab, Color.WHITE, true);

        //setting play pause button color
        playPauseFab.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        playerFabPlayPauseDrawable = new PlayPauseDrawable(getActivity());
        playPauseFab.setImageDrawable(playerFabPlayPauseDrawable); // Note: set the drawable AFTER TintHelper.setTintAuto() was called

        playPauseFab.setOnClickListener(new PlayPauseButtonOnClickHandler());
        playPauseFab.post(() -> {
            if (playPauseFab != null) {
                playPauseFab.setPivotX(playPauseFab.getWidth() / 2);
                playPauseFab.setPivotY(playPauseFab.getHeight() / 2);
            }
        });
    }

    private void setUpPlayPauseButton() {
        playPauseDrawable = new PlayPauseDrawable(getActivity());
        playPauseButton.setImageDrawable(playPauseDrawable);
        updatePlayPauseColor();
        playPauseButton.setColorFilter(Color.WHITE);
        playPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
        playPauseButton.post(() -> {
            if (playPauseButton != null) {
                playPauseButton.setPivotX(playPauseButton.getWidth() / 2);
                playPauseButton.setPivotY(playPauseButton.getHeight() / 2);
            }
        });
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            playerFabPlayPauseDrawable.setPause(animate);
            playPauseDrawable.setPause(animate);
        } else {
            playerFabPlayPauseDrawable.setPlay(animate);
            playPauseDrawable.setPlay(animate);
        }
    }

    private void setUpMusicControllers() {
        setUpPlayPauseButton();
        setUpPlayPauseFab();
        setUpPrevNext();
        setUpRepeatButton();
        setUpShuffleButton();
        setUpProgressSlider();
    }

    private void setUpPrevNext() {
        updatePrevNextColor();
        if(MusicPlayerRemote.getCurrentSong().duration>600000){
            nextButton.setVisibility(View.GONE);
            prevButton.setVisibility(View.GONE);
            forwardTenButton.setVisibility(View.VISIBLE);
            replayTenButton.setVisibility(View.VISIBLE);
        }else if (MusicPlayerRemote.getCurrentSong().duration<600000){
            nextButton.setVisibility(View.VISIBLE);
            prevButton.setVisibility(View.VISIBLE);
            forwardTenButton.setVisibility(View.GONE);
            replayTenButton.setVisibility(View.GONE);
        }
        nextButton.setOnClickListener(v -> MusicPlayerRemote.playNextSong());
        prevButton.setOnClickListener(v -> MusicPlayerRemote.back());
        forwardTenButton.setOnClickListener(v -> MusicPlayerRemote.seekTo(MusicPlayerRemote.goForwardTenSeconds()));
        replayTenButton.setOnClickListener(v -> MusicPlayerRemote.seekTo(MusicPlayerRemote.goBackTenSeconds()));
        forwardTenButton.setOnLongClickListener(v -> {
            MusicPlayerRemote.playNextSong();
            return false;
        });
        replayTenButton.setOnLongClickListener(v -> {
            MusicPlayerRemote.back();
            return false;
        });
    }

    private void updatePlayPauseColor() {
        playPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void updateProgressTextColor() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        //songTotalTime.setTextColor(color);
        //songCurrentProgress.setTextColor(color);
    }

    private void updatePrevNextColor() {
        //nextButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        //prevButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        //forwardTenButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
        //replayTenButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
    }

    private void setUpShuffleButton() {
        shuffleButton.setOnClickListener(v -> MusicPlayerRemote.toggleShuffleMode());
    }

    private void updateShuffleState() {
        switch (MusicPlayerRemote.getShuffleMode()) {
            case MusicService.SHUFFLE_MODE_SHUFFLE:
                //shuffleButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                shuffleButton.setAlpha(1.0f);
                break;
            default:
                //shuffleButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                shuffleButton.setAlpha(0.5f);
                break;
        }
    }

    private void setUpRepeatButton() {
        repeatButton.setOnClickListener(v -> MusicPlayerRemote.cycleRepeatMode());
    }

    private void updateRepeatState() {
        switch (MusicPlayerRemote.getRepeatMode()) {
            case MusicService.REPEAT_MODE_NONE:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                //repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                repeatButton.setAlpha(0.5f);
                break;
            case MusicService.REPEAT_MODE_ALL:
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp);
                //repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                repeatButton.setAlpha(1.0f);
                break;
            case MusicService.REPEAT_MODE_THIS:
                repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                //repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN);
                repeatButton.setAlpha(1.0f);
                break;
        }
    }

   /* public void show() {
        playPauseFab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(360f)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void hide() {
        if (playPauseFab != null) {
            playPauseFab.setScaleX(0f);
            playPauseFab.setScaleY(0f);
            playPauseFab.setRotation(0f);
        }
    }*/

    public void show() {
        if (hidden) {
            if (musicControllerAnimationSet == null) {
                TimeInterpolator interpolator = new FastOutSlowInInterpolator();
                final int duration = 300;

                LinkedList<Animator> animators = new LinkedList<>();

                addAnimation(animators, playPauseButton, interpolator, duration, 0);
                addAnimation(animators, nextButton, interpolator, duration, 100);
                addAnimation(animators, prevButton, interpolator, duration, 100);
                addAnimation(animators, replayTenButton, interpolator, duration, 100);
                addAnimation(animators, forwardTenButton, interpolator, duration, 100);
                addAnimation(animators, shuffleButton, interpolator, duration, 200);
                addAnimation(animators, repeatButton, interpolator, duration, 200);

                musicControllerAnimationSet = new AnimatorSet();
                musicControllerAnimationSet.playTogether(animators);
            } else {
                musicControllerAnimationSet.cancel();
            }
            musicControllerAnimationSet.start();
        }

        hidden = false;
    }

    public void hide() {
        if (musicControllerAnimationSet != null) {
            musicControllerAnimationSet.cancel();
        }
        prepareForAnimation(playPauseButton);
        prepareForAnimation(nextButton);
        prepareForAnimation(prevButton);
        prepareForAnimation(shuffleButton);
        prepareForAnimation(repeatButton);
        prepareForAnimation(replayTenButton);
        prepareForAnimation(forwardTenButton);

        hidden = true;
    }

    private static void addAnimation(Collection<Animator> animators, View view, TimeInterpolator interpolator, int duration, int delay) {
        Animator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f);
        scaleX.setInterpolator(interpolator);
        scaleX.setDuration(duration);
        scaleX.setStartDelay(delay);
        animators.add(scaleX);

        Animator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f);
        scaleY.setInterpolator(interpolator);
        scaleY.setDuration(duration);
        scaleY.setStartDelay(delay);
        animators.add(scaleY);
    }

    private static void prepareForAnimation(View view) {
        if (view != null) {
            view.setScaleX(0f);
            view.setScaleY(0f);
        }
    }

    private void setUpProgressSlider() {
        int color = MaterialValueHelper.getPrimaryTextColor(getContext(), false);
        progressSlider.setOnSeekBarChangeListener(new SimpleOnSeekbarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress);
                    onUpdateProgressViews(MusicPlayerRemote.getSongProgressMillis(), MusicPlayerRemote.getSongDurationMillis());
                }
            }
        });
    }


    @Override
    public void onUpdateProgressViews(int progress, int total) {
        progressSlider.setMax(total);
        progressSlider.setProgress(progress);
        songTotalTime.setText(MusicUtil.getReadableDurationString(total));
        songCurrentProgress.setText(MusicUtil.getReadableDurationString(progress));
    }
}
