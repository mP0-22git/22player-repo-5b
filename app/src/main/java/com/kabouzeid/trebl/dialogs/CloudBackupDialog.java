package com.kabouzeid.trebl.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.backup.DriveBackupManager;
import com.kabouzeid.trebl.service.MusicService;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for managing Google Drive backup and restore of playlists.
 */
public class CloudBackupDialog extends DialogFragment {

    private DriveBackupManager backupManager;
    private ActivityResultLauncher<Intent> signInLauncher;

    // Views
    private LinearLayout signedOutLayout;
    private LinearLayout signedInLayout;
    private TextView signedInAsText;
    private Button backupButton;
    private Button signOutButton;
    private RecyclerView backupListRecycler;
    private TextView noBackupsText;
    private ProgressBar progressBar;

    private BackupListAdapter adapter;

    @NonNull
    public static CloudBackupDialog create() {
        return new CloudBackupDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backupManager = DriveBackupManager.getInstance(requireContext());

        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        backupManager.handleSignInResult(result.getData(), new DriveBackupManager.SignInCallback() {
                            @Override
                            public void onSuccess(String email) {
                                updateUI();
                                loadBackupList();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), getString(R.string.sign_in_failed, error), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_cloud_backup, null);

        // Initialize views
        signedOutLayout = view.findViewById(R.id.signed_out_layout);
        signedInLayout = view.findViewById(R.id.signed_in_layout);
        signedInAsText = view.findViewById(R.id.signed_in_as_text);
        backupButton = view.findViewById(R.id.backup_button);
        signOutButton = view.findViewById(R.id.sign_out_button);
        backupListRecycler = view.findViewById(R.id.backup_list_recycler);
        noBackupsText = view.findViewById(R.id.no_backups_text);
        progressBar = view.findViewById(R.id.progress_bar);

        // Setup RecyclerView
        adapter = new BackupListAdapter();
        backupListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        backupListRecycler.setAdapter(adapter);

        // Setup click listeners
        backupButton.setOnClickListener(v -> performBackup());
        signOutButton.setOnClickListener(v -> signOut());

        updateUI();

        if (backupManager.isSignedIn()) {
            loadBackupList();
        }

        // Build dialog - show "Continue" button only when not signed in
        MaterialDialog.Builder builder = new MaterialDialog.Builder(requireContext())
                .title(R.string.cloud_backup)
                .customView(view, false)
                .negativeText(android.R.string.cancel);

        if (!backupManager.isSignedIn()) {
            builder.positiveText(R.string.continue_action)
                    .onPositive((dialog, which) -> signIn());
        }

        return builder.build();
    }

    private void updateUI() {
        if (backupManager.isSignedIn()) {
            signedOutLayout.setVisibility(View.GONE);
            signedInLayout.setVisibility(View.VISIBLE);
            String email = backupManager.getSignedInEmail();
            signedInAsText.setText(getString(R.string.signed_in_as, email != null ? email : ""));
        } else {
            signedOutLayout.setVisibility(View.VISIBLE);
            signedInLayout.setVisibility(View.GONE);
        }
    }

    private void signIn() {
        signInLauncher.launch(backupManager.getSignInIntent());
    }

    private void signOut() {
        backupManager.signOut(() -> {
            updateUI();
            adapter.setBackups(new ArrayList<>());
            noBackupsText.setVisibility(View.VISIBLE);
            backupListRecycler.setVisibility(View.GONE);
        });
    }

    private void performBackup() {
        progressBar.setVisibility(View.VISIBLE);
        backupButton.setEnabled(false);

        backupManager.backupPlaylists(new DriveBackupManager.BackupCallback() {
            @Override
            public void onSuccess(int playlistCount) {
                progressBar.setVisibility(View.GONE);
                backupButton.setEnabled(true);
                Toast.makeText(getContext(), getString(R.string.backup_successful, playlistCount), Toast.LENGTH_SHORT).show();
                loadBackupList();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                backupButton.setEnabled(true);
                Toast.makeText(getContext(), getString(R.string.backup_failed, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadBackupList() {
        progressBar.setVisibility(View.VISIBLE);

        backupManager.getBackupList(new DriveBackupManager.BackupListCallback() {
            @Override
            public void onSuccess(List<DriveBackupManager.BackupInfo> backups) {
                progressBar.setVisibility(View.GONE);
                adapter.setBackups(backups);

                if (backups.isEmpty()) {
                    noBackupsText.setVisibility(View.VISIBLE);
                    backupListRecycler.setVisibility(View.GONE);
                } else {
                    noBackupsText.setVisibility(View.GONE);
                    backupListRecycler.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.failed_to_load_backups, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void restoreBackup(DriveBackupManager.BackupInfo backup) {
        new MaterialDialog.Builder(requireContext())
                .title(R.string.restore_backup)
                .content(getString(R.string.restore_backup_confirm, backup.formattedDate))
                .positiveText(R.string.restore_action)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    backupManager.restoreBackup(backup.id, new DriveBackupManager.RestoreCallback() {
                        @Override
                        public void onSuccess(int restoredCount, int skippedCount) {
                            progressBar.setVisibility(View.GONE);
                            String message;
                            if (skippedCount > 0) {
                                message = getString(R.string.restore_successful_with_skipped, restoredCount, skippedCount);
                            } else {
                                message = getString(R.string.restore_successful, restoredCount);
                            }
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                            // Notify UI to refresh
                            if (getActivity() != null) {
                                getActivity().sendBroadcast(new Intent(MusicService.MEDIA_STORE_CHANGED)
                                        .setPackage(getActivity().getPackageName()));
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), getString(R.string.restore_failed, error), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void deleteBackup(DriveBackupManager.BackupInfo backup) {
        new MaterialDialog.Builder(requireContext())
                .title(R.string.delete_backup)
                .content(getString(R.string.delete_backup_confirm, backup.formattedDate))
                .positiveText(R.string.delete_action)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    backupManager.deleteBackup(backup.id, new DriveBackupManager.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), R.string.backup_deleted, Toast.LENGTH_SHORT).show();
                            loadBackupList();
                        }

                        @Override
                        public void onFailure(String error) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), getString(R.string.delete_failed, error), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    // RecyclerView Adapter
    private class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
        private List<DriveBackupManager.BackupInfo> backups = new ArrayList<>();

        void setBackups(List<DriveBackupManager.BackupInfo> backups) {
            this.backups = backups;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_backup, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DriveBackupManager.BackupInfo backup = backups.get(position);
            holder.dateText.setText(backup.formattedDate);
            holder.restoreButton.setOnClickListener(v -> restoreBackup(backup));
            holder.deleteButton.setOnClickListener(v -> deleteBackup(backup));
        }

        @Override
        public int getItemCount() {
            return backups.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;
            Button restoreButton;
            Button deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.backup_date_text);
                restoreButton = itemView.findViewById(R.id.restore_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
}
