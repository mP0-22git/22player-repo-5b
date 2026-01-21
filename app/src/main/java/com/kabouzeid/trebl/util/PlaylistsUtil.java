package com.kabouzeid.trebl.util;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.helper.M3UWriter;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.PlaylistSong;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.provider.InternalPlaylistStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for playlist operations.
 * Uses internal Room database for reliable playlist management on all Android versions.
 *
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistsUtil {

    private static final String TAG = "PlaylistsUtil";

    public static boolean doesPlaylistExist(@NonNull final Context context, final long playlistId) {
        if (playlistId == -1) return false;
        return InternalPlaylistStore.getInstance(context).doesPlaylistExist(playlistId);
    }

    public static boolean doesPlaylistExist(@NonNull final Context context, final String name) {
        if (name == null || name.isEmpty()) return false;
        return InternalPlaylistStore.getInstance(context).doesPlaylistExist(name);
    }

    public static long createPlaylist(@NonNull final Context context, @Nullable final String name) {
        long id = -1;
        if (name != null && name.length() > 0) {
            // Check if playlist already exists
            InternalPlaylistStore store = InternalPlaylistStore.getInstance(context);
            InternalPlaylistStore.PlaylistEntity existing = store.getPlaylistByName(name);

            if (existing != null) {
                // Playlist already exists, return its ID
                id = existing.id;
            } else {
                // Create new playlist
                id = store.createPlaylist(name);
                if (id != -1) {
                    Toast.makeText(context, context.getResources().getString(
                            R.string.created_playlist_x, name), Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (id == -1) {
            Toast.makeText(context, context.getResources().getString(
                    R.string.could_not_create_playlist), Toast.LENGTH_SHORT).show();
        }
        return id;
    }

    public static void deletePlaylists(@NonNull final Context context, @NonNull final List<Playlist> playlists) {
        InternalPlaylistStore store = InternalPlaylistStore.getInstance(context);
        List<Long> ids = new ArrayList<>();
        for (Playlist playlist : playlists) {
            ids.add(playlist.id);
        }
        store.deletePlaylists(ids);
    }

    public static void addToPlaylist(@NonNull final Context context, final Song song, final long playlistId, final boolean showToastOnFinish) {
        List<Song> helperList = new ArrayList<>();
        helperList.add(song);
        addToPlaylist(context, helperList, playlistId, showToastOnFinish);
    }

    public static void addToPlaylist(@NonNull final Context context, @NonNull final List<Song> songs, final long playlistId, final boolean showToastOnFinish) {
        InternalPlaylistStore store = InternalPlaylistStore.getInstance(context);

        List<Long> audioIds = new ArrayList<>();
        for (Song song : songs) {
            audioIds.add(song.id);
        }

        int numInserted = store.addSongsToPlaylist(playlistId, audioIds);

        if (showToastOnFinish) {
            Toast.makeText(context, context.getResources().getString(
                    R.string.inserted_x_songs_into_playlist_x, numInserted, getNameForPlaylist(context, playlistId)), Toast.LENGTH_SHORT).show();
        }
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final Song song, long playlistId) {
        InternalPlaylistStore store = InternalPlaylistStore.getInstance(context);
        store.removeSongFromPlaylist(playlistId, song.id);
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final List<PlaylistSong> songs) {
        if (songs.isEmpty()) return;

        final long playlistId = songs.get(0).playlistId;
        InternalPlaylistStore store = InternalPlaylistStore.getInstance(context);

        List<Long> idsInPlaylist = new ArrayList<>();
        for (PlaylistSong song : songs) {
            idsInPlaylist.add(song.idInPlayList);
        }

        store.removeSongsFromPlaylist(playlistId, idsInPlaylist);
    }

    public static boolean doesPlaylistContain(@NonNull final Context context, final long playlistId, final long songId) {
        if (playlistId == -1) return false;
        return InternalPlaylistStore.getInstance(context).doesPlaylistContainSong(playlistId, songId);
    }

    public static boolean moveItem(@NonNull final Context context, long playlistId, int from, int to) {
        if (from == to) {
            return true;
        }
        return InternalPlaylistStore.getInstance(context).moveSong(playlistId, from, to);
    }

    public static void renamePlaylist(@NonNull final Context context, final long id, final String newName) {
        InternalPlaylistStore.getInstance(context).renamePlaylist(id, newName);
    }

    public static String getNameForPlaylist(@NonNull final Context context, final long id) {
        InternalPlaylistStore.PlaylistEntity playlist = InternalPlaylistStore.getInstance(context).getPlaylist(id);
        return playlist != null ? playlist.name : "";
    }

    public static File savePlaylist(Context context, Playlist playlist) throws IOException {
        return M3UWriter.write(context, new File(Environment.getExternalStorageDirectory(), "Playlists"), playlist);
    }
}
