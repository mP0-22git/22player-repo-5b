package com.kabouzeid.trebl.helper;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.model.AbsCustomPlaylist;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.Song;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class M3UWriter implements M3UConstants {

    /**
     * Write playlist to a file.
     * On Android 10+, uses MediaStore to save to Music/Playlists (persists after uninstall).
     * On older versions, writes directly to the specified directory.
     */
    public static File write(Context context, File dir, Playlist playlist) throws IOException {
        List<? extends Song> songs;
        if (playlist instanceof AbsCustomPlaylist) {
            songs = ((AbsCustomPlaylist) playlist).getSongs(context);
        } else {
            songs = PlaylistSongLoader.getPlaylistSongList(context, playlist.id);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use MediaStore to write to public Music/Playlists folder
            return writeViaMediaStore(context, playlist, songs);
        } else {
            // Legacy: Write directly to file system
            return writeLegacy(dir, playlist, songs);
        }
    }

    /**
     * Write playlist via MediaStore API (Android 10+).
     * Saves to Music/Playlists/ which persists after app uninstall.
     */
    private static File writeViaMediaStore(Context context, Playlist playlist, List<? extends Song> songs) throws IOException {
        String fileName = playlist.name + "." + EXTENSION;
        String relativePath = Environment.DIRECTORY_MUSIC + "/Playlists";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-mpegurl");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

        Uri collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        // Delete existing file with same name if it exists
        context.getContentResolver().delete(
                collection,
                MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                new String[]{fileName, relativePath + "/"}
        );

        Uri uri = context.getContentResolver().insert(collection, values);
        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(uri);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os))) {

            bw.write(HEADER);
            for (Song song : songs) {
                bw.newLine();
                bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artistName + " - " + song.title);
                bw.newLine();
                bw.write(song.data);
            }
        }

        // Return a File object representing the path (for display purposes)
        return new File(Environment.getExternalStorageDirectory(), relativePath + "/" + fileName);
    }

    /**
     * Write playlist directly to file system (legacy, pre-Android 10).
     */
    private static File writeLegacy(File dir, Playlist playlist, List<? extends Song> songs) throws IOException {
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        File file = new File(dir, playlist.name + "." + EXTENSION);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(HEADER);
            for (Song song : songs) {
                bw.newLine();
                bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artistName + " - " + song.title);
                bw.newLine();
                bw.write(song.data);
            }
        }

        return file;
    }
}
