package com.kabouzeid.trebl.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.PlaylistsColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.provider.InternalPlaylistStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader for playlists from internal database.
 */
public class PlaylistLoader {

    /**
     * Get all playlists from internal database.
     */
    @NonNull
    public static List<Playlist> getAllPlaylists(@NonNull final Context context) {
        List<Playlist> playlists = new ArrayList<>();
        List<InternalPlaylistStore.PlaylistEntity> entities =
                InternalPlaylistStore.getInstance(context).getAllPlaylists();

        for (InternalPlaylistStore.PlaylistEntity entity : entities) {
            playlists.add(new Playlist(entity.id, entity.name));
        }

        return playlists;
    }

    /**
     * Get a playlist by ID from internal database.
     */
    @NonNull
    public static Playlist getPlaylist(@NonNull final Context context, final long playlistId) {
        InternalPlaylistStore.PlaylistEntity entity =
                InternalPlaylistStore.getInstance(context).getPlaylist(playlistId);

        if (entity != null) {
            return new Playlist(entity.id, entity.name);
        }
        return new Playlist();
    }

    /**
     * Get a playlist by name from internal database.
     */
    @NonNull
    public static Playlist getPlaylist(@NonNull final Context context, final String playlistName) {
        InternalPlaylistStore.PlaylistEntity entity =
                InternalPlaylistStore.getInstance(context).getPlaylistByName(playlistName);

        if (entity != null) {
            return new Playlist(entity.id, entity.name);
        }
        return new Playlist();
    }

    // ==================== MediaStore Methods (for migration) ====================

    /**
     * Get all playlists from MediaStore (used for migration from old system).
     */
    @NonNull
    public static List<Playlist> getAllPlaylistsFromMediaStore(@NonNull final Context context) {
        return getAllPlaylistsFromCursor(makePlaylistCursor(context, null, null));
    }

    /**
     * Get a playlist by ID from MediaStore (used for migration).
     */
    @NonNull
    public static Playlist getPlaylistFromMediaStore(@NonNull final Context context, final long playlistId) {
        return getPlaylistFromCursor(makePlaylistCursor(
                context,
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(playlistId)}
        ));
    }

    @NonNull
    private static Playlist getPlaylistFromCursor(@Nullable final Cursor cursor) {
        Playlist playlist = new Playlist();

        if (cursor != null && cursor.moveToFirst()) {
            playlist = getPlaylistFromCursorImpl(cursor);
        }
        if (cursor != null)
            cursor.close();
        return playlist;
    }

    @NonNull
    private static List<Playlist> getAllPlaylistsFromCursor(@Nullable final Cursor cursor) {
        List<Playlist> playlists = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                playlists.add(getPlaylistFromCursorImpl(cursor));
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
        return playlists;
    }

    @NonNull
    private static Playlist getPlaylistFromCursorImpl(@NonNull final Cursor cursor) {
        final long id = cursor.getLong(0);
        final String name = cursor.getString(1);
        return new Playlist(id, name);
    }

    @Nullable
    private static Cursor makePlaylistCursor(@NonNull final Context context, final String selection, final String[] values) {
        try {
            return context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{
                            /* 0 */ BaseColumns._ID,
                            /* 1 */ PlaylistsColumns.NAME
                    }, selection, values, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
        } catch (SecurityException e) {
            return null;
        }
    }
}
