package com.kabouzeid.trebl.adapter;

import android.content.Context;
import android.os.Build;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.adapter.base.AbsMultiSelectAdapter;
import com.kabouzeid.trebl.adapter.base.MediaEntryViewHolder;
import com.kabouzeid.trebl.dialogs.ClearSmartPlaylistDialog;
import com.kabouzeid.trebl.dialogs.DeletePlaylistDialog;
import com.kabouzeid.trebl.dialogs.ManagePlaylistsDialog;
import com.kabouzeid.trebl.helper.menu.PlaylistMenuHelper;
import com.kabouzeid.trebl.helper.menu.SongsMenuHelper;
import com.kabouzeid.trebl.interfaces.CabHolder;
import com.kabouzeid.trebl.loader.PlaylistSongLoader;
import com.kabouzeid.trebl.misc.WeakContextAsyncTask;
import com.kabouzeid.trebl.model.AbsCustomPlaylist;
import com.kabouzeid.trebl.model.Playlist;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.model.smartplaylist.AbsSmartPlaylist;
import com.kabouzeid.trebl.model.smartplaylist.LastAddedPlaylist;
import com.kabouzeid.trebl.util.MusicUtil;
import com.kabouzeid.trebl.util.NavigationUtil;
import com.kabouzeid.trebl.util.PlaylistsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistAdapter extends AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, Playlist> {

    private static final int SMART_PLAYLIST = 0;
    private static final int DEFAULT_PLAYLIST = 1;
    private static final int MANAGE_HEADER = 2;

    protected final AppCompatActivity activity;
    protected List<Playlist> dataSet;
    protected int itemLayoutRes;

    public PlaylistAdapter(AppCompatActivity activity, List<Playlist> dataSet, @LayoutRes int itemLayoutRes, @Nullable CabHolder cabHolder) {
        super(activity, cabHolder, R.menu.menu_playlists_selection);
        this.activity = activity;
        this.dataSet = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        setHasStableIds(true);
    }

    public List<Playlist> getDataSet() {
        return dataSet;
    }

    public void swapDataSet(List<Playlist> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) return -200;
        return dataSet.get(position - 1).id;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false);
        return createViewHolder(view, viewType);
    }

    protected ViewHolder createViewHolder(View view, int viewType) {
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            holder.itemView.setActivated(false);
            if (holder.title != null) {
                holder.title.setText(R.string.manage_playlists);
            }
            if (holder.image != null) {
                holder.image.setImageResource(R.drawable.ic_settings_white_24dp);
            }
            return;
        }

        final Playlist playlist = dataSet.get(position - 1);

        holder.itemView.setActivated(isChecked(playlist));

        if (holder.title != null) {
            holder.title.setText(playlist.name);
        }

        if (holder.getAdapterPosition() == getItemCount() - 1) {
            if (holder.shortSeparator != null) {
                holder.shortSeparator.setVisibility(View.GONE);
            }
        } else {
            if (holder.shortSeparator != null && !(dataSet.get(position - 1) instanceof AbsSmartPlaylist)) {
                holder.shortSeparator.setVisibility(View.GONE);
            }
        }

        if (holder.image != null) {
            holder.image.setImageResource(getIconRes(playlist));
        }
    }

    private int getIconRes(Playlist playlist) {
        if (playlist instanceof AbsSmartPlaylist) {
            return ((AbsSmartPlaylist) playlist).iconRes;
        }
        return MusicUtil.isFavoritePlaylist(activity, playlist) ? R.drawable.ic_playlist_favorite : R.drawable.ic_playlist_queue_music;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return MANAGE_HEADER;
        return dataSet.get(position - 1) instanceof AbsSmartPlaylist ? SMART_PLAYLIST : DEFAULT_PLAYLIST;
    }

    @Override
    public int getItemCount() {
        return dataSet.size() + 1;
    }

    @Override
    protected Playlist getIdentifier(int position) {
        if (position == 0) return null;
        return dataSet.get(position - 1);
    }

    @Override
    protected String getName(Playlist playlist) {
        return playlist.name;
    }

    @Override
    protected void onMultipleItemAction(@NonNull MenuItem menuItem, @NonNull List<Playlist> selection) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete_playlist:
                for (int i = 0; i < selection.size(); i++) {
                    Playlist playlist = selection.get(i);
                    if (playlist instanceof AbsSmartPlaylist) {
                        AbsSmartPlaylist absSmartPlaylist = (AbsSmartPlaylist) playlist;
                        ClearSmartPlaylistDialog.create(absSmartPlaylist).show(activity.getSupportFragmentManager(), "CLEAR_PLAYLIST_" + absSmartPlaylist.name);
                        selection.remove(playlist);
                        i--;
                    }
                }
                if (selection.size() > 0) {
                    DeletePlaylistDialog.create(selection).show(activity.getSupportFragmentManager(), "DELETE_PLAYLIST");
                }
                break;
            case R.id.action_save_playlist:
                if (selection.size() == 1) {
                    PlaylistMenuHelper.handleMenuClick(activity, selection.get(0), menuItem);
                } else {
                    new SavePlaylistsAsyncTask(activity).execute(selection);
                }
                break;
            default:
                SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.getItemId());
                break;
        }
    }

    private static class SavePlaylistsAsyncTask extends WeakContextAsyncTask<List<Playlist>, String, String> {
        public SavePlaylistsAsyncTask(Context context) {
            super(context);
        }

        @Override
        protected String doInBackground(List<Playlist>... params) {
            int successes = 0;
            int failures = 0;

            String dir = "";

            for (Playlist playlist : params[0]) {
                try {
                    dir = PlaylistsUtil.savePlaylist(App.getInstance().getApplicationContext(), playlist).getParent();
                    successes++;
                } catch (IOException e) {
                    failures++;
                    e.printStackTrace();
                }
            }

            return failures == 0
                    ? String.format(App.getInstance().getApplicationContext().getString(R.string.saved_x_playlists_to_x), successes, dir)
                    : String.format(App.getInstance().getApplicationContext().getString(R.string.saved_x_playlists_to_x_failed_to_save_x), successes, dir, failures);
        }

        @Override
        protected void onPostExecute(String string) {
            super.onPostExecute(string);
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        }
    }

    @NonNull
    private List<Song> getSongList(@NonNull List<Playlist> playlists) {
        final List<Song> songs = new ArrayList<>();
        for (Playlist playlist : playlists) {
            if (playlist instanceof AbsCustomPlaylist) {
                songs.addAll(((AbsCustomPlaylist) playlist).getSongs(activity));
            } else {
                songs.addAll(PlaylistSongLoader.getPlaylistSongList(activity, playlist.id));
            }
        }
        return songs;
    }

    public class ViewHolder extends MediaEntryViewHolder {

        public ViewHolder(@NonNull View itemView, int itemViewType) {
            super(itemView);

            if (itemViewType == MANAGE_HEADER) {
                if (shortSeparator != null) {
                    shortSeparator.setVisibility(View.GONE);
                }
                if (menu != null) {
                    menu.setVisibility(View.GONE);
                }
                itemView.setBackgroundColor(ATHUtil.resolveColor(activity, R.attr.playlistCardBackground));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    itemView.setElevation(activity.getResources().getDimensionPixelSize(R.dimen.card_elevation));
                }
            } else {
                if (itemViewType == SMART_PLAYLIST) {
                    if (shortSeparator != null) {
                        shortSeparator.setVisibility(View.GONE);
                    }
                    itemView.setBackgroundColor(ATHUtil.resolveColor(activity, R.attr.playlistCardBackground));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        itemView.setElevation(activity.getResources().getDimensionPixelSize(R.dimen.card_elevation));
                    }
                }

                if (menu != null) {
                    menu.setOnClickListener(view -> {
                        final Playlist playlist = dataSet.get(getAdapterPosition() - 1);
                        final PopupMenu popupMenu = new PopupMenu(activity, view);
                        popupMenu.inflate(getItemViewType() == SMART_PLAYLIST ? R.menu.menu_item_smart_playlist : R.menu.menu_item_playlist);
                        if (playlist instanceof LastAddedPlaylist) {
                            popupMenu.getMenu().findItem(R.id.action_clear_playlist).setVisible(false);
                        }
                        popupMenu.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.action_clear_playlist) {
                                if (playlist instanceof AbsSmartPlaylist) {
                                    ClearSmartPlaylistDialog.create((AbsSmartPlaylist) playlist).show(activity.getSupportFragmentManager(), "CLEAR_SMART_PLAYLIST_" + playlist.name);
                                    return true;
                                }
                            }
                            return PlaylistMenuHelper.handleMenuClick(
                                    activity, dataSet.get(getAdapterPosition() - 1), item);
                        });
                        popupMenu.show();
                    });
                }
            }

            if (image != null) {
                int iconPadding = activity.getResources().getDimensionPixelSize(R.dimen.list_item_image_icon_padding);
                image.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
            }
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == 0) {
                ManagePlaylistsDialog.create().show(activity.getSupportFragmentManager(), "MANAGE_PLAYLISTS");
                return;
            }
            if (isInQuickSelectMode()) {
                toggleChecked(getAdapterPosition());
            } else {
                Playlist playlist = dataSet.get(getAdapterPosition() - 1);
                NavigationUtil.goToPlaylist(activity, playlist);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (getAdapterPosition() == 0) return false;
            toggleChecked(getAdapterPosition());
            return true;
        }
    }
}
