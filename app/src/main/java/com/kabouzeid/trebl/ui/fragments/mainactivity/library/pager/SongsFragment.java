package com.kabouzeid.trebl.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.adapter.song.NativeAdSongAdapter;
import com.kabouzeid.trebl.adapter.song.SongAdapter;
import com.kabouzeid.trebl.ads.NativeAdManager;
import com.kabouzeid.trebl.interfaces.LoaderIds;
import com.kabouzeid.trebl.loader.SongLoader;
import com.kabouzeid.trebl.misc.WrappedAsyncTaskLoader;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int LOADER_ID = LoaderIds.SONGS_FRAGMENT;

    @Nullable
    private NativeAdManager nativeAdManager;
    @Nullable
    private NativeAdSongAdapter nativeAdSongAdapter;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // Initialize native ad manager BEFORE super.onViewCreated (which calls createAdapter)
        if (!App.isProVersion() && getActivity() != null) {
            nativeAdManager = new NativeAdManager(getActivity());
        }

        super.onViewCreated(view, savedInstanceState);

        // If we have a wrapper adapter, set it on the RecyclerView
        if (nativeAdSongAdapter != null) {
            getRecyclerView().setAdapter(nativeAdSongAdapter);

            // Refresh adapter when ads become available - use targeted updates to avoid rebinding all items
            nativeAdManager.setAdLoadCallback(() -> {
                if (nativeAdSongAdapter != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> nativeAdSongAdapter.notifyAdPositionsChanged());
                }
            });
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected GridLayoutManager createLayoutManager() {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), getGridSize());

        // Make ads span full width in grid layouts
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (nativeAdSongAdapter != null && nativeAdSongAdapter.isAdPosition(position)) {
                    return layoutManager.getSpanCount(); // Full width for ads
                }
                return 1;
            }
        });

        return layoutManager;
    }

    @NonNull
    @Override
    protected SongAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        boolean usePalette = loadUsePalette();
        List<Song> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();

        //note: shuffle button above song list removed to try having it on the toolbar instead
        /*if (getGridSize() <= getMaxGridSizeForList()) {
            return new ShuffleButtonSongAdapter(
                    getLibraryFragment().getMainActivity(),
                    dataSet,
                    itemLayoutRes,
                    usePalette,
                    getLibraryFragment());
        }*/
        SongAdapter songAdapter = new SongAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                usePalette,
                getLibraryFragment());

        // Wrap with native ad adapter if ads are enabled
        if (nativeAdManager != null && nativeAdManager.shouldShowAds()) {
            nativeAdSongAdapter = new NativeAdSongAdapter(songAdapter, nativeAdManager);
        }

        return songAdapter;
    }

    /**
     * Get the adapter to set on the RecyclerView.
     * Returns the wrapper adapter if ads are enabled, otherwise the song adapter.
     */
    @NonNull
    protected RecyclerView.Adapter<?> getRecyclerViewAdapter() {
        if (nativeAdSongAdapter != null) {
            return nativeAdSongAdapter;
        }
        return getAdapter();
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_songs;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance(getActivity()).getSongSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance(getActivity()).setSongSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance(getActivity()).getSongGridSize(getActivity());
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setSongGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance(getActivity()).getSongGridSizeLand(getActivity());
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance(getActivity()).setSongGridSizeLand(gridSize);
    }

    @Override
    public void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance(getActivity()).setSongColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance(getActivity()).songColoredFooters();
    }

    @Override
    public void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }

    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Song>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    @Override
    public void onDestroyView() {
        // Cleanup native ad resources
        if (nativeAdSongAdapter != null) {
            nativeAdSongAdapter.cleanup();
            nativeAdSongAdapter = null;
        }
        if (nativeAdManager != null) {
            nativeAdManager.destroy();
            nativeAdManager = null;
        }
        super.onDestroyView();
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }
}
