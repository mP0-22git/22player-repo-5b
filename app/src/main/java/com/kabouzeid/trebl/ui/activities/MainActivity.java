package com.kabouzeid.trebl.ui.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.navigation.NavigationView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import android.util.Log;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.glide.BlurTransformation;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;
import com.kabouzeid.trebl.helper.SearchQueryHelper;
import com.kabouzeid.trebl.loader.AlbumLoader;
import com.kabouzeid.trebl.loader.ArtistLoader;
import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.service.MusicService;
import com.kabouzeid.trebl.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.folders.FoldersFragment;
import com.kabouzeid.trebl.ui.fragments.mainactivity.library.LibraryFragment;
import com.kabouzeid.trebl.util.MusicUtil;
import com.kabouzeid.trebl.util.PreferenceUtil;

import com.kabouzeid.trebl.util.Util;
import com.sofakingforever.stars.AnimatedStarsView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.kabouzeid.trebl.ui.rating.FiveStarsDialog;

public class MainActivity extends AbsSlidingMusicPanelActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int APP_INTRO_REQUEST = 100;

    private static final int LIBRARY = 0;
    private static final int FOLDERS = 1;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView( R.id.stars)
    AnimatedStarsView starsView;

    @BindView(R.id.starry_bg)
    ImageView starryBg;

    @BindView(R.id.blurry_bg)
    ImageView blurryBg;

    @BindView(R.id.custom_bg)
    ImageView customBg;

    SharedPreferences mPreferences;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    @Nullable
    private View navigationDrawerHeader;

    private boolean blockRequestPermissions;

    int launchCount;

    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            setMusicChooser(PreferenceUtil.getInstance(this).getLastMusicChooser());
        } else {
            restoreCurrentFragment();
        }

        App.setOnProVersionChangedListener(() -> {
            // called if the cached value was outdated (should be a rare event)
            checkSetUpPro();
            if (!App.isProVersion() && PreferenceUtil.getInstance(MainActivity.this).getLastMusicChooser() == FOLDERS) {
                setMusicChooser(FOLDERS); // shows the purchase activity and switches to LIBRARY
            }
        });

        if(PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("starry")){
            setStarBg();
        }else if(PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("midnight")){
            starryBg.setVisibility(View.GONE);
        }

        blurryBg.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (App.isProVersion()) {
            setCustomBg();
        }

        checkFirstRun();

        launchCount = mPreferences.getInt("launchTimes",0);
        if(launchCount % 5 == 0 && launchCount != 0 && !App.isProVersion()){
            startActivity(new Intent(this, PurchaseActivity.class));
        }
        mPreferences.edit().putInt("launchTimes",launchCount+1).apply();

        FiveStarsDialog fiveStarsDialog = new FiveStarsDialog(this);
        fiveStarsDialog.setUpperBound(5).showAfter(5);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.setOnProVersionChangedListener(null);
    }

    private void showDialog(){
        ImageView darkButton, lightButton;
        ConstraintLayout proButton;

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.intro_layout);

        darkButton = dialog.findViewById(R.id.dark_button);
        lightButton = dialog.findViewById(R.id.light_button);
        proButton = dialog.findViewById(R.id.cl_pro);

        darkButton.setOnClickListener(v -> {
            PreferenceUtil.getInstance(MainActivity.this).setGeneralTheme("dark");
            dialog.dismiss();
        });

        lightButton.setOnClickListener(v -> {
            PreferenceUtil.getInstance(MainActivity.this).setGeneralTheme("light");
            dialog.dismiss();
        });

        proButton.setOnClickListener(v -> startActivity((new Intent(MainActivity.this, PurchaseActivity.class))));

        dialog.setOnDismissListener(dialog1 -> {
            recreate(); //note: recreate here to show permission dialog on first launch
        });

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.show();
    }

    private void setMusicChooser(int key) {
        if (!App.isProVersion() && key == FOLDERS) {
            Toast.makeText(this, R.string.folder_view_is_a_pro_feature, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, PurchaseActivity.class));
            key = LIBRARY;
        }

        PreferenceUtil.getInstance(this).setLastMusicChooser(key);
        switch (key) {
            case LIBRARY:
                navigationView.setCheckedItem(R.id.nav_library);
                setCurrentFragment(LibraryFragment.newInstance());
                break;
            case FOLDERS:
                navigationView.setCheckedItem(R.id.nav_folders);
                setCurrentFragment(FoldersFragment.newInstance(this));
                break;
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
        View contentView = getLayoutInflater().inflate(R.layout.activity_main_drawer_layout, null);
        ViewGroup drawerContent = contentView.findViewById(R.id.drawer_content_container);
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    private void checkSetUpPro() {
        navigationView.getMenu().setGroupVisible(R.id.navigation_drawer_menu_category_buy_pro, !App.isProVersion());
    }

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun){
            showDialog();
            mPreferences.edit().putInt("launchTimes",0).apply();
            mPreferences.edit().putInt("numOfAccess",0).apply();
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();
        }
    }

    private void updateNavigationDrawerHeader() {
        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            Song song = MusicPlayerRemote.getCurrentSong();
            if (navigationDrawerHeader == null) {
                navigationDrawerHeader = navigationView.inflateHeaderView(R.layout.navigation_drawer_header);
                //noinspection ConstantConditions
                navigationDrawerHeader.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        expandPanel();
                    }
                });
            }
            ((TextView) navigationDrawerHeader.findViewById(R.id.title)).setText(song.title);
            ((TextView) navigationDrawerHeader.findViewById(R.id.text)).setText(MusicUtil.getSongInfoString(song));

        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader);
                navigationDrawerHeader = null;
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateNavigationDrawerHeader();
        if(PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("blurry")) {
            updateBlurAlbumBackground();
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        updateNavigationDrawerHeader();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleBackPress() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return true;
        }
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

    private long parseIdFromIntent(@NonNull Intent intent, String longKey,
                                   String stringKey) {
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
    public void onPanelExpanded(View view) {
        super.onPanelExpanded(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onPanelCollapsed(View view) {
        super.onPanelCollapsed(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("starry")||PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("midnight")){
            starsView.onStart();
        }
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("starry")||PreferenceUtil.getInstance(this).getGeneralTheme()==PreferenceUtil.getThemeResFromPrefValue("midnight")) {
            starsView.onStop();
        }
        if(dialog!=null){
            dialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void setStarBg(){
        int[] backgrounds = {
                R.drawable.gradient_stars_1,
                R.drawable.gradient_stars_2,
                R.drawable.gradient_stars_3,
                R.drawable.gradient_stars_4
        };
        starryBg.setImageResource(backgrounds[new Random().nextInt(4)]);
    }

    public void setCustomBg(){
        SharedPreferences pref = this.getPreferences(this.MODE_PRIVATE);
        String imageUriString = pref.getString("imageUri", "");
        if(!imageUriString.isEmpty()) {
            Uri imageUri = Uri.parse(imageUriString);
            Glide.with(this).load(imageUri).into(customBg);
        }
    }

    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();
    }

    public void updateBlurAlbumBackground(){
        final Handler handler = new Handler();
        blurryBg.setAlpha(0.0f);
        blurryBg.setScaleX(1);
        blurryBg.setScaleY(1);

        handler.postDelayed(() -> {
            Glide.with(getApplicationContext()).load(Util.getAlbumArtUri(MusicPlayerRemote.getCurrentSong().albumId))
                    .transform(new BlurTransformation.Builder(MainActivity.this).build())
                    .placeholder(R.drawable.default_blur)
                    .error(R.drawable.default_blur)
                    .override(30,30)
                    .into(blurryBg);
            blurryBg.animate().scaleX(1.3f).scaleY(1.3f).alpha(1.0f).setDuration(1000).setInterpolator(new DecelerateInterpolator());
        }, 500);
    }
}
