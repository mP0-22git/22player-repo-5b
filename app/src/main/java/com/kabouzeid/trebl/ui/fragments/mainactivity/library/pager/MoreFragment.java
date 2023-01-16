package com.kabouzeid.trebl.ui.fragments.mainactivity.library.pager;

import static android.app.Activity.RESULT_OK;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.heinrichreimersoftware.materialintro.view.InkPageIndicator;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.dialogs.ScanMediaFolderChooserDialog;
import com.kabouzeid.trebl.ui.activities.MainActivity;
import com.kabouzeid.trebl.ui.activities.PurchaseActivity;
import com.kabouzeid.trebl.ui.activities.SettingsActivity;
import com.kabouzeid.trebl.ui.fragments.mainactivity.folders.FoldersFragment;
import com.kabouzeid.trebl.ui.fragments.player.NowPlayingScreen;
import com.kabouzeid.trebl.util.PreferenceUtil;
import com.kabouzeid.trebl.util.ViewUtil;

import java.net.URI;


public class MoreFragment extends Fragment {
    private ConstraintLayout foldersButton,settingsButton,scanButton, proButton, playerButton, themeButton, bgButton;
    Dialog themeDialog, playerDialog, bgDialog;
    ListView themeList;
    private int viewPagerPosition;
    private static final int PICK_IMAGE = 1;
    private static final int READ_REQUEST_CODE = 2;
    private String currentUri;
    SharedPreferences pref;

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
        playerButton = view.findViewById(R.id.playerButton);
        themeButton = view.findViewById(R.id.themeButton);
        bgButton = view.findViewById(R.id.bgButton);
        pref = getActivity().getPreferences(Context.MODE_PRIVATE);

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

        playerButton.setOnClickListener(view12 -> {
            showPlayerDialog();
        });

        themeButton.setOnClickListener(view14 -> showThemeDialog());

        Intent webIntent = new Intent();
        webIntent.setAction(Intent.ACTION_VIEW);
        webIntent.addCategory(Intent.CATEGORY_BROWSABLE);

        if (!App.isProVersion()){
            proButton.setVisibility(View.GONE);
        }else{
            proButton.setVisibility(View.VISIBLE);
        }

        bgButton.setOnClickListener(view15 -> showBgDialog());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(playerDialog!=null){
            playerDialog.dismiss();
        }
        if(themeDialog!=null){
            themeDialog.dismiss();
        }
        if(bgDialog!=null){
            bgDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String imageUriString = pref.getString("imageUri", "");
        Boolean appliedNewBg = false;

        if(imageUriString.isEmpty()) {
            if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("imageUri", imageUri.toString());
                editor.apply();
                appliedNewBg = true;
            }
            if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
                Uri uri = data.getData();
                getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("imageUri", uri.toString());
                editor.apply();
                getActivity().getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                appliedNewBg = true;
            }
            if (!appliedNewBg){
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("imageUri", currentUri);
                editor.apply();
            }
            getActivity().recreate();
        }
    }

    private void setCurrentFragment(@SuppressWarnings("NullableProblems") Fragment fragment) {
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivity.MainActivityFragmentCallbacks) fragment;
    }

    private void showThemeDialog(){
        themeDialog = new Dialog(getActivity());
        themeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        themeDialog.setContentView(R.layout.dialog_theme_picker);

        themeList = themeDialog.findViewById(R.id.list_theme);

        String[] themeValues = getResources().getStringArray(R.array.pref_general_theme_list_values);

        String[] themeNames = getResources().getStringArray(R.array.pref_general_theme_list_titles);

        ArrayAdapter mAdapter = new ArrayAdapter(getActivity(), R.layout.list_item, R.id.text_view, themeNames);

        themeList.setAdapter(mAdapter);

        themeList.setOnItemClickListener((adapterView, view, i, l) -> {
            if(i==0||i==1){
                PreferenceUtil.getInstance(getActivity()).setGeneralTheme(themeValues[i]);
                themeDialog.dismiss();
                getActivity().recreate();
            }else if(App.isProVersion()){
                PreferenceUtil.getInstance(getActivity()).setGeneralTheme(themeValues[i]);
                themeDialog.dismiss();
                getActivity().recreate();
            }else{
                themeDialog.dismiss();
                startActivity(new Intent(getActivity(), PurchaseActivity.class));
            }
        });
        themeDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        themeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        themeDialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        themeDialog.getWindow().setGravity(Gravity.BOTTOM);
        themeDialog.show();
    }

    private void showPlayerDialog(){
        playerDialog = new Dialog(getActivity());
        playerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        playerDialog.setContentView(R.layout.dialog_player_picker);
        ViewPager viewPager = playerDialog.findViewById(R.id.now_playing_screen_view_pager);
        viewPager.setAdapter(new NowPlayingScreenAdapter(getContext()));
        viewPager.setPageMargin((int) ViewUtil.convertDpToPixel(32, getResources()));
        viewPager.setCurrentItem(PreferenceUtil.getInstance(getContext()).getNowPlayingScreen().ordinal());

        Button selectButton = playerDialog.findViewById(R.id.button_select_player);
        selectButton.setTextColor(ThemeStore.accentColor(getActivity()));

        InkPageIndicator pageIndicator = playerDialog.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(viewPager);
        pageIndicator.onPageSelected(viewPager.getCurrentItem());

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewPagerPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        selectButton.setOnClickListener(view -> {
            if(viewPagerPosition!=2){
                PreferenceUtil.getInstance(getContext()).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                playerDialog.dismiss();
                getActivity().recreate();
            }else if(App.isProVersion() && viewPagerPosition==2){
                PreferenceUtil.getInstance(getContext()).setNowPlayingScreen(NowPlayingScreen.values()[viewPagerPosition]);
                playerDialog.dismiss();
                getActivity().recreate();
            }else{
                Toast.makeText(getActivity(), "This is a pro feature", Toast.LENGTH_LONG).show();
                startActivity(new Intent(getContext(), PurchaseActivity.class));
            }
        });

        playerDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        playerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        playerDialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        playerDialog.getWindow().setGravity(Gravity.BOTTOM);
        playerDialog.show();
    }

    private void showBgDialog(){
        ImageView resetButton, pickButton;
        bgDialog = new Dialog(getActivity());
        bgDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        bgDialog.setContentView(R.layout.dialog_change_background);

        resetButton = bgDialog.findViewById(R.id.reset_button);
        pickButton = bgDialog.findViewById(R.id.pick_button);

        currentUri = pref.getString("imageUri", "");

        resetButton.setOnClickListener(view -> {

            SharedPreferences.Editor editor = pref.edit();
            editor.remove("imageUri");
            editor.apply();
            getActivity().recreate();
        });

        pickButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = pref.edit();
            editor.remove("imageUri");
            editor.apply();
            pickFromGallery();
        });

        bgDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        bgDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bgDialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        bgDialog.getWindow().setGravity(Gravity.BOTTOM);
        bgDialog.show();
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

    private static class NowPlayingScreenAdapter extends PagerAdapter {

        private Context context;

        public NowPlayingScreenAdapter(Context context) {
            this.context = context;
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            NowPlayingScreen nowPlayingScreen = NowPlayingScreen.values()[position];

            LayoutInflater inflater = LayoutInflater.from(context);
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.preference_now_playing_screen_item, collection, false);
            collection.addView(layout);

            ImageView image = layout.findViewById(R.id.image);
            TextView title = layout.findViewById(R.id.title);
            image.setImageResource(nowPlayingScreen.drawableResId);
            title.setText(nowPlayingScreen.titleRes);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return NowPlayingScreen.values().length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return context.getString(NowPlayingScreen.values()[position].titleRes);
        }
    }
}