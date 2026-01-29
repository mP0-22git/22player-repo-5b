package com.kabouzeid.trebl.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ui.fragments.mainactivity.library.LibraryFragment;

public class ManagePlaylistsDialog extends DialogFragment {

    public static ManagePlaylistsDialog create() {
        return new ManagePlaylistsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.manage_playlists)
                .items(new CharSequence[]{
                        getString(R.string.import_playlists),
                        getString(R.string.export_all_playlists)
                })
                .itemsCallback((dialog, itemView, position, text) -> {
                    LibraryFragment libraryFragment = findLibraryFragment();
                    if (libraryFragment == null) return;

                    switch (position) {
                        case 0:
                            libraryFragment.importPlaylistsFromMediaStore();
                            break;
                        case 1:
                            libraryFragment.exportAllPlaylists();
                            break;
                    }
                })
                .build();
    }

    private LibraryFragment findLibraryFragment() {
        if (getActivity() == null) return null;
        Fragment fragment = getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (fragment instanceof LibraryFragment) {
            return (LibraryFragment) fragment;
        }
        return null;
    }
}
