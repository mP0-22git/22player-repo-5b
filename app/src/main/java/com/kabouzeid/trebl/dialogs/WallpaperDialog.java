package com.kabouzeid.trebl.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import static android.app.Activity.RESULT_OK;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.superwall.sdk.Superwall;
import com.superwall.sdk.paywall.presentation.PublicPresentationKt;

public class WallpaperDialog extends DialogFragment {
    private SharedPreferences pref;
    private Context context;
    private static final int PICK_IMAGE = 1;
    private static final int READ_REQUEST_CODE = 2;

    public WallpaperDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        pref = getActivity().getPreferences(Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_change_background, container, false);

        Button resetButton = view.findViewById(R.id.reset_button);
        Button pickButton = view.findViewById(R.id.pick_button);

        resetButton.setOnClickListener(view1 -> {
            SharedPreferences.Editor editor = pref.edit();
            editor.remove("imageUri");
            editor.apply();
            dismiss();
            getActivity().recreate();
        });

        pickButton.setOnClickListener(view12 -> {
            if (App.isProVersion()) {
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("imageUri");
                editor.apply();
                pickFromGallery();
            } else {
                PublicPresentationKt.register(Superwall.Companion.getInstance(), "feature_wallpaper");
            }
        });

        return view;
    }

    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("imageUri", imageUri.toString());
            editor.apply();
            getActivity().recreate();
        } else if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("imageUri", uri.toString());
            editor.apply();
            context.getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().recreate();
        }
        dismiss();
    }
}
