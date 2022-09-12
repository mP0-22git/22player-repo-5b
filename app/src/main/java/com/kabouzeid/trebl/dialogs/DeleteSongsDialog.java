package com.kabouzeid.trebl.dialogs;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.util.MusicUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class DeleteSongsDialog extends DialogFragment {

    @NonNull
    public static DeleteSongsDialog create(Song song) {
        List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    @NonNull
    public static DeleteSongsDialog create(List<Song> songs) {
        DeleteSongsDialog dialog = new DeleteSongsDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //noinspection unchecked
        final List<Song> songs = getArguments().getParcelableArrayList("songs");
        int title;
        CharSequence content;
        if (songs.size() > 1) {
            title = R.string.delete_songs_title;
            content = Html.fromHtml(getString(R.string.delete_x_songs, songs.size()));
        } else {
            title = R.string.delete_song_title;
            content = Html.fromHtml(getString(R.string.delete_song_x, songs.get(0).title));
        }
        return new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(content)
                .positiveText(R.string.delete_action)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    if (getActivity() == null)
                        return;

                    //Important Note: a11 onwards we can't just delete user files and need user permission every single time
                    // So we use createDeleteRequest instead of f.delete()
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            final String[] projection = new String[]{
                                    BaseColumns._ID, MediaStore.MediaColumns.DATA
                            };
                            final StringBuilder selection = new StringBuilder();
                            selection.append(BaseColumns._ID + " IN (");
                            for (int i = 0; i < songs.size(); i++) {
                                selection.append(songs.get(i).id);
                                if (i < songs.size() - 1) {
                                    selection.append(",");
                                }
                            }
                            selection.append(")");

                            try {
                                final Cursor cursor = getActivity().getContentResolver().query(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                                        null, null);
                                if (cursor != null) {
                                    ArrayList<Uri> extractedUris = new ArrayList<>();
                                    cursor.moveToFirst();
                                    //get uri of every song the user wants to delete and add it to the list of uris
                                    while (!cursor.isAfterLast()) {
                                        final long id = cursor.getLong(0);
                                        final String name = cursor.getString(1);
                                        try {
                                            Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                                            extractedUris.add(contentUri);
                                            cursor.moveToNext();
                                        } catch (@NonNull SecurityException e) {
                                            try {
                                                cursor.moveToNext();
                                            } catch (NoSuchElementException f) {
                                                Log.e("DeleteSongsDialog", "Security exception + Element not found");
                                            }
                                        } catch (NullPointerException e) {
                                            Log.e("MusicUtils", "Failed to find file " + name);
                                        } catch (NoSuchElementException e) {
                                            Log.e("DeleteSongsDialog", "Element not found");
                                        }

                                    }
                                    try {
                                        //pass uris to createdeleterequest (necessary from a11 onwards)
                                        PendingIntent pi = MediaStore.createDeleteRequest(getActivity().getContentResolver(), extractedUris);
                                        startIntentSenderForResult(pi.getIntentSender(),
                                                1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                                                Integer.valueOf(0), null);
                                    } catch (IntentSender.SendIntentException e) {

                                    }
                                    cursor.close();
                                }
                            } catch (SecurityException ignored) {

                            }
                        } else {
                            MusicUtil.deleteTracks(getActivity(), songs);
                        }
                    }catch (NoSuchElementException e){
                        Toast.makeText(getContext(), "NoSuchElementException", Toast.LENGTH_SHORT).show();
                        Log.e("DeleteSongsDialog", "Element not found");
                    }
                })
                .build();
    }
}
