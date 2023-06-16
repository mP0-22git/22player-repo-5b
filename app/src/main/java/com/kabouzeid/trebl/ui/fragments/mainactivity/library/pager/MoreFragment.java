package com.kabouzeid.trebl.ui.fragments.mainactivity.library.pager;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.dialogs.PlayerDialog;
import com.kabouzeid.trebl.dialogs.ThemeDialog;
import com.kabouzeid.trebl.dialogs.WallpaperDialog;
import com.kabouzeid.trebl.ui.activities.MainActivity;
import com.kabouzeid.trebl.ui.activities.PurchaseActivity;
import com.kabouzeid.trebl.ui.activities.SettingsActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.folders.FoldersFragment;

public class MoreFragment extends Fragment {
    private Dialog playerDialog;
    private Dialog themeDialog;
    private WallpaperDialog wallpaperDialog;

    @Nullable
    MainActivity.MainActivityFragmentCallbacks currentFragment;

    public MoreFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConstraintLayout proButton = view.findViewById(R.id.pro_layout);
        ListView listView = view.findViewById(R.id.list_more);

        proButton.setVisibility(App.isProVersion() ? View.GONE : View.VISIBLE);
        proButton.setOnClickListener(v -> startActivity((new Intent(getActivity(), PurchaseActivity.class))));

        String [] more_items = getResources().getStringArray(R.array.more_array);
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), R.layout.item_more, R.id.text_view, more_items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view2, i, l) -> performAction(i));
    }

    private void performAction(int action) {
        switch (action) {
            case 0:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            case 1:
                if (!App.isProVersion()) {
                    Toast.makeText(getActivity(), R.string.folder_view_is_a_pro_feature, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(getActivity(), PurchaseActivity.class));
                }else{
                    setCurrentFragment(FoldersFragment.newInstance(getActivity()));
                }
                break;
            case 2:
                playerDialog = PlayerDialog.createPlayerDialog(getActivity());
                playerDialog.show();
                break;
            case 3:
                themeDialog = ThemeDialog.createThemeDialog(getActivity());
                themeDialog.show();
                break;
            case 4:
                wallpaperDialog = new WallpaperDialog();
                wallpaperDialog.show(getChildFragmentManager(), "WallpaperDialog");
                break;
        }
    }

    private void setCurrentFragment(@SuppressWarnings("NullableProblems") Fragment fragment) {
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivity.MainActivityFragmentCallbacks) fragment;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (playerDialog != null && playerDialog.isShowing()) {
            playerDialog.dismiss();
        }

        if (themeDialog != null && themeDialog.isShowing()) {
            themeDialog.dismiss();
        }
    }
}