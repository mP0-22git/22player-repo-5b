package com.kabouzeid.trebl.ui.fragments.mainactivity.library.pager;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.dialogs.ScanMediaFolderChooserDialog;
import com.kabouzeid.trebl.ui.activities.MainActivity;
import com.kabouzeid.trebl.ui.activities.PurchaseActivity;
import com.kabouzeid.trebl.ui.activities.SettingsActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.folders.FoldersFragment;


public class MoreFragment extends Fragment {
    private ConstraintLayout foldersButton,settingsButton,scanButton, proButton;

    @Nullable
    MainActivity.MainActivityFragmentCallbacks currentFragment;

    public MoreFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static MoreFragment newInstance(String param1, String param2) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        foldersButton = view.findViewById(R.id.foldersButton);
        settingsButton = view.findViewById(R.id.settingsButton);
        scanButton = view.findViewById(R.id.scanButton);
        proButton = view.findViewById(R.id.pro_layout);

        settingsButton.setOnClickListener(view1 -> new Handler().postDelayed(() -> startActivity(new Intent(getActivity(), SettingsActivity.class)), 200));

        scanButton.setOnClickListener(view13 -> new Handler().postDelayed(() -> {
            ScanMediaFolderChooserDialog dialog = ScanMediaFolderChooserDialog.create();
            dialog.show(getActivity().getSupportFragmentManager(), "SCAN_MEDIA_FOLDER_CHOOSER");
        }, 200));

        foldersButton.setOnClickListener(v -> {
            if (!App.isProVersion()) {
                Toast.makeText(getActivity(), R.string.folder_view_is_a_pro_feature, Toast.LENGTH_LONG).show();
                startActivity(new Intent(getActivity(), PurchaseActivity.class));
            }else{
                setCurrentFragment(FoldersFragment.newInstance(getActivity()));
            }
        });

        proButton.setOnClickListener(v -> startActivity((new Intent(getActivity(), PurchaseActivity.class))));

        Intent webIntent = new Intent();
        webIntent.setAction(Intent.ACTION_VIEW);
        webIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        if (!App.isProVersion()){
            proButton.setVisibility(View.GONE);
        }else{
            proButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void setCurrentFragment(@SuppressWarnings("NullableProblems") Fragment fragment) {
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivity.MainActivityFragmentCallbacks) fragment;
    }
}