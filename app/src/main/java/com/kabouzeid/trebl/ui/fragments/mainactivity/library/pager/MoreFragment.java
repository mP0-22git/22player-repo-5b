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
import android.widget.TextView;
import android.widget.Toast;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.billing.BillingManager;
import com.kabouzeid.trebl.dialogs.PlayerDialog;
import com.kabouzeid.trebl.dialogs.ThemeDialog;
import com.kabouzeid.trebl.dialogs.WallpaperDialog;
import com.kabouzeid.trebl.ui.activities.MainActivity;
import com.kabouzeid.trebl.ui.activities.SettingsActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.folders.FoldersFragment;
import com.superwall.sdk.Superwall;
import com.superwall.sdk.paywall.presentation.PublicPresentationKt;

public class MoreFragment extends Fragment {
    private Dialog playerDialog;
    private Dialog themeDialog;
    private WallpaperDialog wallpaperDialog;
    private BillingManager.BillingCallback billingCallback;

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
        proButton.setOnClickListener(v -> PublicPresentationKt.register(Superwall.Companion.getInstance(), "campaign_pro_button"));

        TextView restoreButton = view.findViewById(R.id.restore_purchase);
        restoreButton.setVisibility(App.isProVersion() ? View.GONE : View.VISIBLE);
        restoreButton.setOnClickListener(v -> restorePurchase());

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
                setCurrentFragment(FoldersFragment.newInstance(getActivity()));
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

    private void restorePurchase() {
        BillingManager billingManager = App.getBillingManager();
        if (billingManager == null) return;

        Toast.makeText(getActivity(), R.string.restoring, Toast.LENGTH_SHORT).show();

        if (billingCallback != null) billingManager.removeCallback(billingCallback);
        billingCallback = new BillingManager.BillingCallback() {
            @Override
            public void onPurchaseComplete(String productId) {}

            @Override
            public void onPurchaseRestored(boolean hasPurchase) {
                billingManager.removeCallback(this);
                if (!isAdded()) return;
                if (hasPurchase) {
                    Toast.makeText(getActivity(), R.string.restored_previous_purchase_please_restart, Toast.LENGTH_LONG).show();
                    View v = getView();
                    if (v != null) {
                        v.findViewById(R.id.pro_layout).setVisibility(View.GONE);
                        v.findViewById(R.id.restore_purchase).setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.no_purchase_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBillingError(int errorCode, String message) {
                billingManager.removeCallback(this);
                if (!isAdded()) return;
                Toast.makeText(getActivity(), R.string.could_not_restore, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBillingReady() {}
        };
        billingManager.addCallback(billingCallback);
        billingManager.restorePurchases();
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

        if (billingCallback != null && App.getBillingManager() != null) {
            App.getBillingManager().removeCallback(billingCallback);
            billingCallback = null;
        }
    }
}