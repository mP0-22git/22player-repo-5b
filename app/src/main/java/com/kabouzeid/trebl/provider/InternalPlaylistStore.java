package com.kabouzeid.trebl.provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.Update;

import com.kabouzeid.trebl.loader.PlaylistLoader;
import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.PlaylistSong;

import java.util.List;

/**
 * Internal playlist database using Room.
 * This replaces MediaStore playlists which are broken on Android 11+.
 */
public class InternalPlaylistStore {

    public static final String DATABASE_NAME = "internal_playlists.db";

    @Nullable
    private static InternalPlaylistStore sInstance = null;

    private final PlaylistDatabase database;

    private InternalPlaylistStore(@NonNull Context context) {
        database = Room.databaseBuilder(
                context.getApplicationContext(),
                PlaylistDatabase.class,
                DATABASE_NAME
        )
        // Allow main thread queries for compatibility with existing code
        // The old MediaStore-based code also ran on the main thread
        .allowMainThreadQueries()
        .build();
    }

    @NonNull
    public static synchronized InternalPlaylistStore getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new InternalPlaylistStore(context.getApplicationContext());
        }
        return sInstance;
    }

    // ==================== Playlist Operations ====================

    /**
     * Create a new playlist.
     * @return the ID of the newly created playlist, or -1 if failed
     */
    public long createPlaylist(@NonNull String name) {
        PlaylistEntity playlist = new PlaylistEntity();
        playlist.name = name;
        playlist.createdAt = System.currentTimeMillis();
        playlist.modifiedAt = System.currentTimeMillis();
        return database.playlistDao().insertPlaylist(playlist);
    }

    /**
     * Get all playlists.
     */
    @NonNull
    public List<PlaylistEntity> getAllPlaylists() {
        return database.playlistDao().getAllPlaylists();
    }

    /**
     * Get a playlist by ID.
     */
    @Nullable
    public PlaylistEntity getPlaylist(long id) {
        return database.playlistDao().getPlaylistById(id);
    }

    /**
     * Get a playlist by name.
     */
    @Nullable
    public PlaylistEntity getPlaylistByName(@NonNull String name) {
        return database.playlistDao().getPlaylistByName(name);
    }

    /**
     * Check if a playlist exists by ID.
     */
    public boolean doesPlaylistExist(long id) {
        return database.playlistDao().getPlaylistById(id) != null;
    }

    /**
     * Check if a playlist exists by name.
     */
    public boolean doesPlaylistExist(@NonNull String name) {
        return database.playlistDao().getPlaylistByName(name) != null;
    }

    /**
     * Rename a playlist.
     */
    public void renamePlaylist(long id, @NonNull String newName) {
        database.playlistDao().renamePlaylist(id, newName, System.currentTimeMillis());
    }

    /**
     * Delete a playlist.
     */
    public void deletePlaylist(long id) {
        database.playlistDao().deletePlaylistById(id);
    }

    /**
     * Delete multiple playlists.
     */
    public void deletePlaylists(@NonNull List<Long> ids) {
        for (Long id : ids) {
            database.playlistDao().deletePlaylistById(id);
        }
    }

    /**
     * Check if the database is empty (no playlists).
     */
    public boolean isEmpty() {
        return database.playlistDao().getPlaylistCount() == 0;
    }

    // ==================== Playlist Song Operations ====================

    /**
     * Add a song to a playlist.
     * @param playlistId the playlist ID
     * @param audioId the MediaStore audio ID
     * @return true if added successfully
     */
    public boolean addSongToPlaylist(long playlistId, long audioId) {
        // Check if song already exists in playlist
        if (doesPlaylistContainSong(playlistId, audioId)) {
            return false;
        }

        // Get the next order position
        int nextOrder = database.playlistDao().getMaxSongOrder(playlistId) + 1;

        PlaylistSongEntity song = new PlaylistSongEntity();
        song.playlistId = playlistId;
        song.audioId = audioId;
        song.songOrder = nextOrder;
        song.addedAt = System.currentTimeMillis();

        long result = database.playlistDao().insertPlaylistSong(song);

        // Update playlist modified time
        if (result != -1) {
            database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
        }

        return result != -1;
    }

    /**
     * Add multiple songs to a playlist.
     * @return number of songs added
     */
    public int addSongsToPlaylist(long playlistId, @NonNull List<Long> audioIds) {
        int added = 0;
        int nextOrder = database.playlistDao().getMaxSongOrder(playlistId) + 1;

        for (Long audioId : audioIds) {
            // Skip if song already exists
            if (doesPlaylistContainSong(playlistId, audioId)) {
                continue;
            }

            PlaylistSongEntity song = new PlaylistSongEntity();
            song.playlistId = playlistId;
            song.audioId = audioId;
            song.songOrder = nextOrder++;
            song.addedAt = System.currentTimeMillis();

            long result = database.playlistDao().insertPlaylistSong(song);
            if (result != -1) {
                added++;
            }
        }

        if (added > 0) {
            database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
        }

        return added;
    }

    /**
     * Get all songs in a playlist ordered by songOrder.
     */
    @NonNull
    public List<PlaylistSongEntity> getPlaylistSongs(long playlistId) {
        return database.playlistDao().getPlaylistSongs(playlistId);
    }

    /**
     * Remove a song from a playlist by audio ID.
     */
    public void removeSongFromPlaylist(long playlistId, long audioId) {
        database.playlistDao().removeSongByAudioId(playlistId, audioId);
        database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
    }

    /**
     * Remove a song from a playlist by its ID in the playlist.
     */
    public void removeSongFromPlaylistById(long playlistId, long idInPlaylist) {
        database.playlistDao().removeSongById(idInPlaylist);
        database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
    }

    /**
     * Remove multiple songs from a playlist by their IDs.
     */
    public void removeSongsFromPlaylist(long playlistId, @NonNull List<Long> idsInPlaylist) {
        for (Long id : idsInPlaylist) {
            database.playlistDao().removeSongById(id);
        }
        database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
    }

    /**
     * Check if a playlist contains a specific song.
     */
    public boolean doesPlaylistContainSong(long playlistId, long audioId) {
        return database.playlistDao().getSongByAudioId(playlistId, audioId) != null;
    }

    /**
     * Move a song within a playlist (reorder).
     * @param playlistId the playlist ID
     * @param fromPosition current position (0-based index)
     * @param toPosition target position (0-based index)
     * @return true if move was successful
     */
    public boolean moveSong(long playlistId, int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return true;
        }

        List<PlaylistSongEntity> songs = database.playlistDao().getPlaylistSongs(playlistId);
        if (fromPosition < 0 || fromPosition >= songs.size() ||
            toPosition < 0 || toPosition >= songs.size()) {
            return false;
        }

        // Reorder in memory
        PlaylistSongEntity movedSong = songs.remove(fromPosition);
        songs.add(toPosition, movedSong);

        // Update all order values
        for (int i = 0; i < songs.size(); i++) {
            PlaylistSongEntity song = songs.get(i);
            if (song.songOrder != i) {
                database.playlistDao().updateSongOrder(song.id, i);
            }
        }

        database.playlistDao().updatePlaylistModifiedTime(playlistId, System.currentTimeMillis());
        return true;
    }

    /**
     * Get the number of songs in a playlist.
     */
    public int getPlaylistSongCount(long playlistId) {
        return database.playlistDao().getPlaylistSongCount(playlistId);
    }

    // ==================== Migration ====================

    /**
     * Import all playlists from MediaStore into the internal database.
     * This is used for migrating existing users to the new internal playlist system.
     *
     * @param context the application context
     * @return the number of playlists imported
     */
    public int importFromMediaStore(@NonNull Context context) {
        int imported = 0;

        // Get all playlists from MediaStore
        List<Playlist> mediaStorePlaylists = PlaylistLoader.getAllPlaylistsFromMediaStore(context);

        for (Playlist playlist : mediaStorePlaylists) {
            // Create playlist in internal database
            long newPlaylistId = createPlaylist(playlist.name);

            if (newPlaylistId != -1) {
                // Get songs from MediaStore playlist
                List<PlaylistSong> songs = PlaylistSongLoader.getPlaylistSongListFromMediaStore(context, playlist.id);

                // Add songs to internal playlist
                for (int i = 0; i < songs.size(); i++) {
                    PlaylistSong song = songs.get(i);

                    PlaylistSongEntity entity = new PlaylistSongEntity();
                    entity.playlistId = newPlaylistId;
                    entity.audioId = song.id;
                    entity.songOrder = i;
                    entity.addedAt = System.currentTimeMillis();

                    database.playlistDao().insertPlaylistSong(entity);
                }

                imported++;
            }
        }

        return imported;
    }

    /**
     * Check if there are any playlists in MediaStore that can be migrated.
     */
    public boolean hasMediaStorePlaylists(@NonNull Context context) {
        List<Playlist> playlists = PlaylistLoader.getAllPlaylistsFromMediaStore(context);
        return !playlists.isEmpty();
    }

    // ==================== Room Database ====================

    @Database(entities = {PlaylistEntity.class, PlaylistSongEntity.class}, version = 1, exportSchema = false)
    public abstract static class PlaylistDatabase extends RoomDatabase {
        public abstract PlaylistDao playlistDao();
    }

    // ==================== Entities ====================

    @Entity(tableName = "playlists")
    public static class PlaylistEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;

        @NonNull
        @ColumnInfo(name = "name")
        public String name = "";

        @ColumnInfo(name = "created_at")
        public long createdAt;

        @ColumnInfo(name = "modified_at")
        public long modifiedAt;
    }

    @Entity(tableName = "playlist_songs",
            foreignKeys = @ForeignKey(
                    entity = PlaylistEntity.class,
                    parentColumns = "id",
                    childColumns = "playlist_id",
                    onDelete = ForeignKey.CASCADE
            ),
            indices = {
                    @Index(value = "playlist_id"),
                    @Index(value = {"playlist_id", "audio_id"}, unique = true)
            })
    public static class PlaylistSongEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;

        @ColumnInfo(name = "playlist_id")
        public long playlistId;

        @ColumnInfo(name = "audio_id")
        public long audioId;

        @ColumnInfo(name = "song_order")
        public int songOrder;

        @ColumnInfo(name = "added_at")
        public long addedAt;
    }

    // ==================== DAO ====================

    @Dao
    public interface PlaylistDao {
        // Playlist queries
        @Query("SELECT * FROM playlists ORDER BY name ASC")
        List<PlaylistEntity> getAllPlaylists();

        @Query("SELECT * FROM playlists WHERE id = :id")
        PlaylistEntity getPlaylistById(long id);

        @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
        PlaylistEntity getPlaylistByName(String name);

        @Query("SELECT COUNT(*) FROM playlists")
        int getPlaylistCount();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insertPlaylist(PlaylistEntity playlist);

        @Query("UPDATE playlists SET name = :newName, modified_at = :modifiedAt WHERE id = :id")
        void renamePlaylist(long id, String newName, long modifiedAt);

        @Query("UPDATE playlists SET modified_at = :modifiedAt WHERE id = :id")
        void updatePlaylistModifiedTime(long id, long modifiedAt);

        @Query("DELETE FROM playlists WHERE id = :id")
        void deletePlaylistById(long id);

        // Playlist song queries
        @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY song_order ASC")
        List<PlaylistSongEntity> getPlaylistSongs(long playlistId);

        @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId AND audio_id = :audioId LIMIT 1")
        PlaylistSongEntity getSongByAudioId(long playlistId, long audioId);

        @Query("SELECT COALESCE(MAX(song_order), -1) FROM playlist_songs WHERE playlist_id = :playlistId")
        int getMaxSongOrder(long playlistId);

        @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
        int getPlaylistSongCount(long playlistId);

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insertPlaylistSong(PlaylistSongEntity song);

        @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND audio_id = :audioId")
        void removeSongByAudioId(long playlistId, long audioId);

        @Query("DELETE FROM playlist_songs WHERE id = :id")
        void removeSongById(long id);

        @Query("UPDATE playlist_songs SET song_order = :newOrder WHERE id = :id")
        void updateSongOrder(long id, int newOrder);
    }
}
