package com.kabouzeid.trebl.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.dialogs.RatingDialog;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;
import com.kabouzeid.trebl.helper.SearchQueryHelper;
import com.kabouzeid.trebl.loader.AlbumLoader;
import com.kabouzeid.trebl.loader.ArtistLoader;
import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.service.MusicService;
import com.kabouzeid.trebl.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.library.LibraryFragment;
import com.kabouzeid.trebl.provider.InternalPlaylistStore;
import com.kabouzeid.trebl.util.BackgroundUtil;
import com.kabouzeid.trebl.util.PreferenceUtil;
import com.superwall.sdk.Superwall;
import com.superwall.sdk.paywall.presentation.PublicPresentationKt;

import com.sofakingforever.stars.AnimatedStarsView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AbsSlidingMusicPanelActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int APP_INTRO_REQUEST = 100;

    @BindView( R.id.stars)
    AnimatedStarsView starsView;

    @BindView(R.id.starry_bg)
    ImageView starryBg;

    @BindView(R.id.blurry_bg)
    ImageView blurryBg;

    @BindView(R.id.custom_bg)
    ImageView customBg;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    private boolean blockRequestPermissions;

    private int generalTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            setCurrentFragment(LibraryFragment.newInstance());
        } else {
            restoreCurrentFragment();
        }

        generalTheme = PreferenceUtil.getInstance(this).getGeneralTheme();

        App.setOnProVersionChangedListener(() -> runOnUiThread(this::recreate));

        runChecks();
    }

    private void runChecks() {
        checkFirstRun();
        checkProBg();
        checkShowPro();
        checkRating();
        checkPlaylistMigration();
        PreferenceUtil.getInstance(this).incrementLaunchCount();
    }

    private void checkPlaylistMigration() {
        PreferenceUtil prefs = PreferenceUtil.getInstance(this);

        // Skip if already completed
        if (prefs.isPlaylistMigrationCompleted()) return;

        // Silent auto-import in background
        new Thread(() -> {
            InternalPlaylistStore store = InternalPlaylistStore.getInstance(this);

            // Only import if internal DB is empty and MediaStore has playlists
            if (store.isEmpty() && store.hasMediaStorePlaylists(this)) {
                store.importFromMediaStore(this);
            }

            runOnUiThread(() -> prefs.setPlaylistMigrationCompleted());
        }).start();
    }

    public void checkFirstRun() {
        boolean isFirstRun = PreferenceUtil.getInstance(this).isFirstRun();
        if (isFirstRun) {
            PreferenceUtil.getInstance(this).disableFirstRun();
        }
    }

    private void checkRating() {
        RatingDialog ratingDialog = new RatingDialog(this);
        ratingDialog.setUpperBound(4).showAfter(4);
    }

    private void checkShowPro() {
        int launchCount = PreferenceUtil.getInstance(this).getLaunchCount();
        if(launchCount % 5 == 0 && launchCount != 0 && !App.isProVersion()) {
            PublicPresentationKt.register(Superwall.Companion.getInstance(), "campaign_periodic");
        }
    }

    private void checkProBg() {
        if (App.isProVersion()) {
            BackgroundUtil.checkStarBg(this, starryBg);
            BackgroundUtil.checkWallpaperBg(this, customBg);
        }
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivityFragmentCallbacks) fragment;
    }

    private void restoreCurrentFragment() {
        currentFragment = (MainActivityFragmentCallbacks) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false;
            if (!hasPermissions()) {
                requestPermissions();
            }
        }
    }

    @Override
    protected void requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions();
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams")
        View contentView = getLayoutInflater().inflate(R.layout.activity_main_layout, null);
        ViewGroup mainContent = contentView.findViewById(R.id.main_content_container);
        mainContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        if (generalTheme == R.style.Theme_Phonograph_Blur) {
            BackgroundUtil.updateBlurBg(getApplicationContext(), blurryBg);
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean handleBackPress() {
        return super.handleBackPress() || (currentFragment != null && currentFragment.handleBackPress());
    }

    private void handlePlaybackIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            final List<Song> songs = SearchQueryHelper.getSongs(this, intent.getExtras());
            if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
                MusicPlayerRemote.openAndShuffleQueue(songs, true);
            } else {
                MusicPlayerRemote.openQueue(songs, 0, true);
            }
            handled = true;
        }

        if (uri != null && uri.toString().length() > 0) {
            MusicPlayerRemote.playFromUri(uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                List<Song> songs = new ArrayList<>(PlaylistSongLoader.getPlaylistSongList(this, id));
                MusicPlayerRemote.openQueue(songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "albumId", "album");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            final long id = parseIdFromIntent(intent, "artistId", "artist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).getSongs(), position, true);
                handled = true;
            }
        }
        if (handled) {
            setIntent(new Intent());
        }
    }

    private long parseIdFromIntent(@NonNull Intent intent, String longKey, String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return id;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (starsView.getVisibility() == View.VISIBLE) {
            starsView.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (starsView.getVisibility() == View.VISIBLE) {
            starsView.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.setOnProVersionChangedListener(null);
    }

    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();
    }
}
