package com.kabouzeid.trebl.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.loader.PlaylistLoader;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.util.PlaylistsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class AddToPlaylistDialog extends DialogFragment {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @NonNull
    public static AddToPlaylistDialog create(Song song) {
        List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    @NonNull
    public static AddToPlaylistDialog create(List<Song> songs) {
        AddToPlaylistDialog dialog = new AddToPlaylistDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = requireContext();
        final Activity activity = requireActivity();

        // Show loading dialog first
        MaterialDialog loadingDialog = new MaterialDialog.Builder(context)
                .title(R.string.add_playlist_title)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(true)
                .build();

        // Load playlists in background
        executor.execute(() -> {
            final List<Playlist> playlists = PlaylistLoader.getAllPlaylists(context);

            mainHandler.post(() -> {
                if (!isAdded() || activity.isFinishing()) return;

                loadingDialog.dismiss();

                CharSequence[] playlistNames = new CharSequence[playlists.size() + 1];
                playlistNames[0] = context.getResources().getString(R.string.action_new_playlist);
                for (int i = 1; i < playlistNames.length; i++) {
                    playlistNames[i] = playlists.get(i - 1).name;
                }

                new MaterialDialog.Builder(context)
                        .title(R.string.add_playlist_title)
                        .items(playlistNames)
                        .itemsCallback((materialDialog, view, i, charSequence) -> {
                            //noinspection unchecked
                            final List<Song> songs = getArguments().getParcelableArrayList("songs");
                            if (songs == null) return;
                            if (i == 0) {
                                materialDialog.dismiss();
                                if (activity instanceof AppCompatActivity && !activity.isFinishing()) {
                                    CreatePlaylistDialog.create(songs).show(
                                            ((AppCompatActivity) activity).getSupportFragmentManager(),
                                            "ADD_TO_PLAYLIST");
                                }
                            } else {
                                materialDialog.dismiss();
                                PlaylistsUtil.addToPlaylist(context, songs, playlists.get(i - 1).id, true);
                            }
                        })
                        .show();
            });
        });

        return loadingDialog;
    }
}
