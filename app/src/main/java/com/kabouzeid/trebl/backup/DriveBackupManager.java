package com.kabouzeid.trebl.backup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kabouzeid.trebl.loader.PlaylistLoader;
import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.loader.SongLoader;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.PlaylistSong;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.util.PlaylistsUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages Google Drive backup and restore operations for playlists.
 * Uses the drive.appdata scope which doesn't require Google verification.
 */
public class DriveBackupManager {
    private static final String TAG = "DriveBackupManager";
    private static final String BACKUP_FILE_PREFIX = "trebl_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final String BACKUP_MIME_TYPE = "application/json";

    private static DriveBackupManager instance;
    private final Context applicationContext;
    private final ExecutorService executor;
    private final Gson gson;

    private GoogleSignInClient signInClient;
    private Drive driveService;
    private GoogleSignInAccount currentAccount;

    public static synchronized DriveBackupManager getInstance(Context context) {
        if (instance == null) {
            instance = new DriveBackupManager(context.getApplicationContext());
        }
        return instance;
    }

    private DriveBackupManager(Context context) {
        this.applicationContext = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initSignInClient();

        // Check if already signed in
        currentAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (currentAccount != null && hasRequiredScopes(currentAccount)) {
            initDriveService(currentAccount);
        }
    }

    private void initSignInClient() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
        signInClient = GoogleSignIn.getClient(applicationContext, signInOptions);
    }

    private boolean hasRequiredScopes(GoogleSignInAccount account) {
        return GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_APPDATA));
    }

    private void initDriveService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Trebl Music Player")
                .build();
    }

    public boolean isSignedIn() {
        return currentAccount != null && hasRequiredScopes(currentAccount) && driveService != null;
    }

    @Nullable
    public String getSignedInEmail() {
        return currentAccount != null ? currentAccount.getEmail() : null;
    }

    public Intent getSignInIntent() {
        return signInClient.getSignInIntent();
    }

    public void handleSignInResult(Intent data, SignInCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        task.addOnSuccessListener(account -> {
            currentAccount = account;
            initDriveService(account);
            callback.onSuccess(account.getEmail());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Sign-in failed", e);
            callback.onFailure(e.getMessage());
        });
    }

    public void signOut(SignOutCallback callback) {
        signInClient.signOut().addOnCompleteListener(task -> {
            currentAccount = null;
            driveService = null;
            callback.onComplete();
        });
    }

    public void backupPlaylists(BackupCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure("Not signed in");
            return;
        }

        executor.execute(() -> {
            try {
                // Load all playlists and their songs
                List<Playlist> playlists = PlaylistLoader.getAllPlaylists(applicationContext);
                List<BackupPlaylist> backupPlaylists = new ArrayList<>();

                for (Playlist playlist : playlists) {
                    // Skip smart playlists (negative IDs)
                    if (playlist.id < 0) continue;

                    List<PlaylistSong> songs = PlaylistSongLoader.getPlaylistSongList(applicationContext, playlist.id);
                    List<BackupSong> backupSongs = new ArrayList<>();

                    for (PlaylistSong song : songs) {
                        backupSongs.add(new BackupSong(
                                song.title,
                                song.artistName,
                                song.albumName,
                                song.data,
                                song.duration
                        ));
                    }

                    backupPlaylists.add(new BackupPlaylist(playlist.name, backupSongs));
                }

                // Create backup data
                BackupData backupData = new BackupData(
                        1, // version
                        System.currentTimeMillis(),
                        backupPlaylists
                );

                String jsonContent = gson.toJson(backupData);

                // Generate filename with timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String fileName = BACKUP_FILE_PREFIX + sdf.format(new Date()) + BACKUP_FILE_EXTENSION;

                // Upload to Drive appdata folder
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList("appDataFolder"));

                ByteArrayContent content = new ByteArrayContent(BACKUP_MIME_TYPE, jsonContent.getBytes());

                File uploadedFile = driveService.files().create(fileMetadata, content)
                        .setFields("id, name, createdTime")
                        .execute();

                Log.d(TAG, "Backup created: " + uploadedFile.getName());

                // Run callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(backupPlaylists.size()));

            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public void getBackupList(BackupListCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure("Not signed in");
            return;
        }

        executor.execute(() -> {
            try {
                FileList fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("files(id, name, createdTime)")
                        .setOrderBy("createdTime desc")
                        .execute();

                List<BackupInfo> backups = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US);

                for (File file : fileList.getFiles()) {
                    if (file.getName().startsWith(BACKUP_FILE_PREFIX)) {
                        long timestamp = file.getCreatedTime() != null ?
                                file.getCreatedTime().getValue() : 0;
                        backups.add(new BackupInfo(
                                file.getId(),
                                file.getName(),
                                timestamp,
                                sdf.format(new Date(timestamp))
                        ));
                    }
                }

                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(backups));

            } catch (Exception e) {
                Log.e(TAG, "Failed to list backups", e);
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public void restoreBackup(String backupId, RestoreCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure("Not signed in");
            return;
        }

        executor.execute(() -> {
            try {
                // Download the backup file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                driveService.files().get(backupId).executeMediaAndDownloadTo(outputStream);
                String jsonContent = outputStream.toString();

                // Parse the backup data
                BackupData backupData = gson.fromJson(jsonContent, BackupData.class);

                // Get all songs on the device for matching
                List<Song> deviceSongs = SongLoader.getAllSongs(applicationContext);

                int restoredCount = 0;
                int skippedCount = 0;

                for (BackupPlaylist backupPlaylist : backupData.playlists) {
                    // Check if playlist already exists
                    if (PlaylistsUtil.doesPlaylistExist(applicationContext, backupPlaylist.name)) {
                        skippedCount++;
                        continue;
                    }

                    // Create the playlist
                    long playlistId = PlaylistsUtil.createPlaylist(applicationContext, backupPlaylist.name);
                    if (playlistId == -1) {
                        skippedCount++;
                        continue;
                    }

                    // Match and add songs
                    List<Song> matchedSongs = new ArrayList<>();
                    for (BackupSong backupSong : backupPlaylist.songs) {
                        Song matched = findMatchingSong(backupSong, deviceSongs);
                        if (matched != null) {
                            matchedSongs.add(matched);
                        }
                    }

                    if (!matchedSongs.isEmpty()) {
                        PlaylistsUtil.addToPlaylist(applicationContext, matchedSongs, playlistId, false);
                    }

                    restoredCount++;
                }

                final int finalRestored = restoredCount;
                final int finalSkipped = skippedCount;

                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(finalRestored, finalSkipped));

            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    public void deleteBackup(String backupId, DeleteCallback callback) {
        if (!isSignedIn()) {
            callback.onFailure("Not signed in");
            return;
        }

        executor.execute(() -> {
            try {
                driveService.files().delete(backupId).execute();

                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e(TAG, "Delete failed", e);
                android.os.Handler mainHandler = new android.os.Handler(applicationContext.getMainLooper());
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    @Nullable
    private Song findMatchingSong(BackupSong backupSong, List<Song> deviceSongs) {
        // First try exact path match
        for (Song song : deviceSongs) {
            if (song.data != null && song.data.equals(backupSong.path)) {
                return song;
            }
        }

        // Fallback: match by title and artist
        for (Song song : deviceSongs) {
            if (matchesTitleAndArtist(song, backupSong)) {
                return song;
            }
        }

        return null;
    }

    private boolean matchesTitleAndArtist(Song song, BackupSong backupSong) {
        boolean titleMatches = song.title != null && backupSong.title != null &&
                song.title.equalsIgnoreCase(backupSong.title);
        boolean artistMatches = song.artistName != null && backupSong.artist != null &&
                song.artistName.equalsIgnoreCase(backupSong.artist);
        return titleMatches && artistMatches;
    }

    // Callback interfaces
    public interface SignInCallback {
        void onSuccess(String email);
        void onFailure(String error);
    }

    public interface SignOutCallback {
        void onComplete();
    }

    public interface BackupCallback {
        void onSuccess(int playlistCount);
        void onFailure(String error);
    }

    public interface BackupListCallback {
        void onSuccess(List<BackupInfo> backups);
        void onFailure(String error);
    }

    public interface RestoreCallback {
        void onSuccess(int restoredCount, int skippedCount);
        void onFailure(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // Data classes for backup format
    public static class BackupData {
        public int version;
        public long timestamp;
        public List<BackupPlaylist> playlists;

        public BackupData(int version, long timestamp, List<BackupPlaylist> playlists) {
            this.version = version;
            this.timestamp = timestamp;
            this.playlists = playlists;
        }
    }

    public static class BackupPlaylist {
        public String name;
        public List<BackupSong> songs;

        public BackupPlaylist(String name, List<BackupSong> songs) {
            this.name = name;
            this.songs = songs;
        }
    }

    public static class BackupSong {
        public String title;
        public String artist;
        public String album;
        public String path;
        public long duration;

        public BackupSong(String title, String artist, String album, String path, long duration) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.path = path;
            this.duration = duration;
        }
    }

    public static class BackupInfo {
        public String id;
        public String name;
        public long timestamp;
        public String formattedDate;

        public BackupInfo(String id, String name, long timestamp, String formattedDate) {
            this.id = id;
            this.name = name;
            this.timestamp = timestamp;
            this.formattedDate = formattedDate;
        }
    }
}
