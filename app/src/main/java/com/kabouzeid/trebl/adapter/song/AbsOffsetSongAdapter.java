package com.kabouzeid.trebl.adapter.song;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.helper.MusicPlayerRemote;
import com.kabouzeid.trebl.interfaces.CabHolder;
import com.kabouzeid.trebl.model.Song;

import java.util.List;

/**
 * @author Eugene Cheung (arkon)
 */
public abstract class AbsOffsetSongAdapter extends SongAdapter {

    protected static final int OFFSET_ITEM = 0;
    protected static final int SONG = 1;

    public AbsOffsetSongAdapter(AppCompatActivity activity, List<Song> dataSet, @LayoutRes int itemLayoutRes, boolean usePalette, @Nullable CabHolder cabHolder) {
        super(activity, dataSet, itemLayoutRes, usePalette, cabHolder);
    }

    public AbsOffsetSongAdapter(AppCompatActivity activity, List<Song> dataSet, @LayoutRes int itemLayoutRes, boolean usePalette, @Nullable CabHolder cabHolder, boolean showSectionName) {
        super(activity, dataSet, itemLayoutRes, usePalette, cabHolder, showSectionName);
    }

    @NonNull
    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == OFFSET_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_list_single_row, parent, false);
            return createViewHolder(view);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    protected SongAdapter.ViewHolder createViewHolder(View view) {
        return new AbsOffsetSongAdapter.ViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        position--;
        if (position < 0) return -2;
        return super.getItemId(position);
    }

    @Nullable
    @Override
    protected Song getIdentifier(int position) {
        position--;
        if (position < 0) return null;
        return super.getIdentifier(position);
    }

    @Override
    public int getItemCount() {
        int superItemCount = super.getItemCount();
        return superItemCount == 0 ? 0 : superItemCount + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? OFFSET_ITEM : SONG;
    }

    @Override
    protected int getMappedPosition(int adapterPosition) {
        return adapterPosition - 1;
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        position--;
        if (position < 0) return "";
        return super.getSectionName(position);
    }

    public class ViewHolder extends SongAdapter.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            int songPosition = position - 1;
            if (isInQuickSelectMode() && getItemViewType() != OFFSET_ITEM) {
                toggleChecked(position);
            } else if (songPosition >= 0 && songPosition < dataSet.size()) {
                MusicPlayerRemote.openQueue(dataSet, songPosition, true);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            if (getItemViewType() == OFFSET_ITEM) return false;
            toggleChecked(position);
            return true;
        }
    }
}
