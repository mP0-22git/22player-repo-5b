package com.kabouzeid.trebl.adapter.song;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.dialogs.RemoveFromPlaylistDialog;
import com.kabouzeid.trebl.interfaces.CabHolder;
import com.kabouzeid.trebl.model.PlaylistSong;
import com.kabouzeid.trebl.model.Song;
import com.kabouzeid.trebl.util.ViewUtil;

import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@SuppressWarnings("unchecked")
public class OrderablePlaylistSongAdapter extends PlaylistSongAdapter implements DraggableItemAdapter<OrderablePlaylistSongAdapter.ViewHolder> {

    private static final String TAG = "OrderablePlaylistAdapter";
    private OnMoveItemListener onMoveItemListener;

    public OrderablePlaylistSongAdapter(@NonNull AppCompatActivity activity, @NonNull List<PlaylistSong> dataSet, @LayoutRes int itemLayoutRes, boolean usePalette, @Nullable CabHolder cabHolder, @Nullable OnMoveItemListener onMoveItemListener) {
        super(activity, (List<Song>) (List) dataSet, itemLayoutRes, usePalette, cabHolder);
        setMultiSelectMenuRes(R.menu.menu_playlists_songs_selection);
        this.onMoveItemListener = onMoveItemListener;
    }

    @Override
    protected SongAdapter.ViewHolder createViewHolder(View view) {
        return new OrderablePlaylistSongAdapter.ViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        position--;
        if (position < 0) return -2;
        return ((List<PlaylistSong>) (List) dataSet).get(position).idInPlayList; // important!
    }

    @Override
    protected void onMultipleItemAction(@NonNull MenuItem menuItem, @NonNull List<Song> selection) {
        switch (menuItem.getItemId()) {
            case R.id.action_remove_from_playlist:
                RemoveFromPlaylistDialog.create((List<PlaylistSong>) (List) selection).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return;
        }
        super.onMultipleItemAction(menuItem, selection);
    }

    @Override
    public boolean onCheckCanStartDrag(ViewHolder holder, int position, int x, int y) {
        return onMoveItemListener != null && position > 0 &&
                (ViewUtil.hitTest(holder.dragView, x, y) || ViewUtil.hitTest(holder.image, x, y));
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(ViewHolder holder, int position) {
        return new ItemDraggableRange(1, dataSet.size());
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        Log.d(TAG, "onMoveItem called: fromPosition=" + fromPosition + ", toPosition=" + toPosition +
              ", listener=" + (onMoveItemListener != null));
        if (onMoveItemListener != null && fromPosition != toPosition) {
            Log.d(TAG, "onMoveItem: calling listener.onMoveItem(" + (fromPosition - 1) + ", " + (toPosition - 1) + ")");
            onMoveItemListener.onMoveItem(fromPosition - 1, toPosition - 1);
        } else {
            Log.d(TAG, "onMoveItem: NOT calling listener - listener null? " + (onMoveItemListener == null) +
                  ", same position? " + (fromPosition == toPosition));
        }
    }

    @Override
    public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
        boolean canDrop = dropPosition > 0;
        Log.d(TAG, "onCheckCanDrop: draggingPosition=" + draggingPosition + ", dropPosition=" + dropPosition + ", canDrop=" + canDrop);
        return canDrop;
    }

    @Override
    public void onItemDragStarted(int position) {
        Log.d(TAG, "onItemDragStarted: position=" + position);
        notifyDataSetChanged();
    }

    @Override
    public void onItemDragFinished(int fromPosition, int toPosition, boolean result) {
        Log.d(TAG, "onItemDragFinished: fromPosition=" + fromPosition + ", toPosition=" + toPosition + ", result=" + result);
        notifyDataSetChanged();
    }

    public interface OnMoveItemListener {
        void onMoveItem(int fromPosition, int toPosition);
    }

    public class ViewHolder extends PlaylistSongAdapter.ViewHolder implements DraggableItemViewHolder {
        @DraggableItemStateFlags
        private int mDragStateFlags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (dragView != null) {
                if (onMoveItemListener != null) {
                    dragView.setVisibility(View.VISIBLE);
                } else {
                    dragView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        protected int getSongMenuRes() {
            return R.menu.menu_item_playlist_song;
        }

        @Override
        protected boolean onSongMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_remove_from_playlist:
                    RemoveFromPlaylistDialog.create((PlaylistSong) getSong()).show(activity.getSupportFragmentManager(), "REMOVE_FROM_PLAYLIST");
                    return true;
            }
            return super.onSongMenuItemClick(item);
        }

        @Override
        public void setDragStateFlags(@DraggableItemStateFlags int flags) {
            mDragStateFlags = flags;
        }

        @Override
        @DraggableItemStateFlags
        public int getDragStateFlags() {
            return mDragStateFlags;
        }

        @NonNull
        @Override
        public DraggableItemState getDragState() {
            return null;
        }
    }
}
