package com.kabouzeid.trebl.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.trebl.model.PlaylistSong;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.provider.InternalPlaylistStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader for playlist songs.
 *
 * <p>
 * Gets song references (audio IDs and order) from internal Room database
 * ({@link InternalPlaylistStore}), then fetches song metadata from MediaStore.
 * This hybrid approach ensures playlist data is stored internally (fixing Android 11+ issues)
 * while song metadata stays in sync with the device's media library.
 * </p>
 *
 * <p>
 * MediaStore methods are retained with "FromMediaStore" suffix for migration purposes only.
 * </p>
 *
 * @see InternalPlaylistStore
 * @see PlaylistsUtil
 */
public class PlaylistSongLoader {

    /**
     * Get all songs in a playlist from internal database.
     * Song metadata is fetched from MediaStore.
     */
    @NonNull
    public static List<PlaylistSong> getPlaylistSongList(@NonNull final Context context, final long playlistId) {
        List<PlaylistSong> songs = new ArrayList<>();

        // Get song entries from internal database (ordered)
        List<InternalPlaylistStore.PlaylistSongEntity> playlistSongs =
                InternalPlaylistStore.getInstance(context).getPlaylistSongs(playlistId);

        if (playlistSongs.isEmpty()) {
            return songs;
        }

        // Build list of audio IDs to query
        List<Long> audioIds = new ArrayList<>();
        for (InternalPlaylistStore.PlaylistSongEntity entry : playlistSongs) {
            audioIds.add(entry.audioId);
        }

        // Query MediaStore for song metadata
        Map<Long, Song> songMap = getSongsById(context, audioIds);

        // Build PlaylistSong list in the correct order
        for (InternalPlaylistStore.PlaylistSongEntity entry : playlistSongs) {
            Song song = songMap.get(entry.audioId);
            if (song != null) {
                PlaylistSong playlistSong = new PlaylistSong(
                        song.id,
                        song.title,
                        song.trackNumber,
                        song.year,
                        song.duration,
                        song.data,
                        (int) song.dateModified,
                        song.albumId,
                        song.albumName,
                        song.artistId,
                        song.artistName,
                        playlistId,
                        entry.id  // idInPlaylist - use the internal DB entry ID
                );
                songs.add(playlistSong);
            }
            // Note: If song is not found in MediaStore (deleted file), we skip it
        }

        return songs;
    }

    /**
     * Query MediaStore for multiple songs by their IDs.
     * Returns a map of audioId -> Song for efficient lookup.
     */
    @NonNull
    private static Map<Long, Song> getSongsById(@NonNull Context context, @NonNull List<Long> audioIds) {
        Map<Long, Song> songMap = new HashMap<>();

        if (audioIds.isEmpty()) {
            return songMap;
        }

        // Build selection for multiple IDs: _ID IN (?, ?, ?, ...)
        StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns._ID).append(" IN (");
        String[] selectionArgs = new String[audioIds.size()];
        for (int i = 0; i < audioIds.size(); i++) {
            if (i > 0) selection.append(",");
            selection.append("?");
            selectionArgs[i] = String.valueOf(audioIds.get(i));
        }
        selection.append(")");

        // Add base selection for valid music files
        String fullSelection = SongLoader.BASE_SELECTION + " AND " + selection.toString();

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            AudioColumns._ID,           // 0
                            AudioColumns.TITLE,         // 1
                            AudioColumns.TRACK,         // 2
                            AudioColumns.YEAR,          // 3
                            AudioColumns.DURATION,      // 4
                            AudioColumns.DATA,          // 5
                            AudioColumns.DATE_MODIFIED, // 6
                            AudioColumns.ALBUM_ID,      // 7
                            AudioColumns.ALBUM,         // 8
                            AudioColumns.ARTIST_ID,     // 9
                            AudioColumns.ARTIST         // 10
                    },
                    fullSelection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    int trackNumber = cursor.getInt(2);
                    int year = cursor.getInt(3);
                    long duration = cursor.getLong(4);
                    String data = cursor.getString(5);
                    long dateModified = cursor.getLong(6);
                    long albumId = cursor.getLong(7);
                    String albumName = cursor.getString(8);
                    long artistId = cursor.getLong(9);
                    String artistName = cursor.getString(10);

                    Song song = new Song(id, title, trackNumber, year, duration, data,
                            dateModified, albumId, albumName, artistId, artistName);
                    songMap.put(id, song);
                } while (cursor.moveToNext());
            }
        } catch (SecurityException e) {
            // Permission denied
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return songMap;
    }

    // ==================== MediaStore Methods (for migration) ====================

    /**
     * Get playlist songs directly from MediaStore (used for migration from old system).
     */
    @NonNull
    public static List<PlaylistSong> getPlaylistSongListFromMediaStore(@NonNull final Context context, final long playlistId) {
        List<PlaylistSong> songs = new ArrayList<>();
        Cursor cursor = makePlaylistSongCursor(context, playlistId);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getPlaylistSongFromCursorImpl(cursor, playlistId));
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        return songs;
    }

    @NonNull
    private static PlaylistSong getPlaylistSongFromCursorImpl(@NonNull Cursor cursor, long playlistId) {
        final long id = cursor.getLong(0);
        final String title = cursor.getString(1);
        final int trackNumber = cursor.getInt(2);
        final int year = cursor.getInt(3);
        final long duration = cursor.getLong(4);
        final String data = cursor.getString(5);
        final int dateModified = cursor.getInt(6);
        final long albumId = cursor.getLong(7);
        final String albumName = cursor.getString(8);
        final long artistId = cursor.getLong(9);
        final String artistName = cursor.getString(10);
        final long idInPlaylist = cursor.getLong(11);

        return new PlaylistSong(id, title, trackNumber, year, duration, data, dateModified, albumId, albumName, artistId, artistName, playlistId, idInPlaylist);
    }

    @Nullable
    private static Cursor makePlaylistSongCursor(@NonNull final Context context, final long playlistId) {
        try {
            return context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                    new String[]{
                            MediaStore.Audio.Playlists.Members.AUDIO_ID,// 0
                            AudioColumns.TITLE,// 1
                            AudioColumns.TRACK,// 2
                            AudioColumns.YEAR,// 3
                            AudioColumns.DURATION,// 4
                            AudioColumns.DATA,// 5
                            AudioColumns.DATE_MODIFIED,// 6
                            AudioColumns.ALBUM_ID,// 7
                            AudioColumns.ALBUM,// 8
                            AudioColumns.ARTIST_ID,// 9
                            AudioColumns.ARTIST,// 10
                            MediaStore.Audio.Playlists.Members._ID // 11
                    }, SongLoader.BASE_SELECTION, null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
        } catch (SecurityException e) {
            return null;
        }
    }
}
