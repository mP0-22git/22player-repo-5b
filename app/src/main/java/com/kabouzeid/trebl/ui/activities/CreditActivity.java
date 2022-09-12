package com.kabouzeid.trebl.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.internal.ThemeSingleton;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;

import de.psdev.licensesdialog.LicensesDialog;

public class CreditActivity extends AppCompatActivity {

    LinearLayout licenses;
    TextView appVersion;
    LinearLayout openGithub;
    LinearLayout PrivacyPolicy;

    private static String GITHUB = "";
    private static String PRIVACYPOLICY = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit);

        licenses = findViewById(R.id.licenses);
        openGithub = findViewById(R.id.trebl_on_github);
        PrivacyPolicy = findViewById(R.id.privacy_policy);
        appVersion = findViewById(R.id.app_version);

        licenses.setOnClickListener(v -> showLicenseDialog());
        openGithub.setOnClickListener(v -> openUrl(GITHUB));
        PrivacyPolicy.setOnClickListener(v -> openUrl(PRIVACYPOLICY));
        appVersion.setText(getCurrentVersionName(this));
    }

    private void showLicenseDialog() {
        new LicensesDialog.Builder(this)
                .setNotices(R.raw.notices)
                .setTitle(R.string.licenses)
                .setNoticesCssStyle(getString(R.string.license_dialog_style)
                        .replace("{bg-color}", ThemeSingleton.get().darkTheme ? "424242" : "ffffff")
                        .replace("{text-color}", ThemeSingleton.get().darkTheme ? "ffffff" : "000000")
                        .replace("{license-bg-color}", ThemeSingleton.get().darkTheme ? "535353" : "eeeeee")
                )
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }

    private void openUrl(String url) {
        this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    private static String getCurrentVersionName(@NonNull final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName + (App.isProVersion() ? " Pro" : "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "Unkown";
    }
}