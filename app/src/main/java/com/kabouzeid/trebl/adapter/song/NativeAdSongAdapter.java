package com.kabouzeid.trebl.adapter.song;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.kabouzeid.trebl.App;
import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.ads.NativeAdManager;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

/**
 * Wrapper adapter that inserts native ads between songs in the list.
 * Uses the decorator pattern to wrap a SongAdapter without modifying its logic.
 */
public class NativeAdSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter, SongAdapter.PositionMapper {



    // Use a high value for ad view type to avoid conflicts with wrapped adapter's view types
    private static final int VIEW_TYPE_AD = Integer.MAX_VALUE - 1;

    private static final int FIRST_AD_OFFSET = 4;  // First ad after the first 4 songs
    private static final int AD_INTERVAL = 15;     // Then one ad every 15 songs

    // Any non-empty list shorter than FIRST_AD_OFFSET still gets a single ad, placed
    // at the end. (A list needs at least this many songs to show an ad.)
    private static final int MIN_ITEMS_FOR_AD = 1;

    private final SongAdapter wrappedAdapter;
    private final NativeAdManager adManager;
    private final boolean showAds;

    // Cache ads by position so they remain consistent when scrolling
    private final SparseArray<NativeAd> adCache = new SparseArray<>();

    private final RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyDataSetChanged(); // Simplified - positions shift due to ads
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyDataSetChanged();
        }
    };

    public NativeAdSongAdapter(@NonNull SongAdapter wrappedAdapter, @Nullable NativeAdManager adManager) {
        this.wrappedAdapter = wrappedAdapter;
        this.adManager = adManager;
        this.showAds = adManager != null && adManager.shouldShowAds() && !App.isProVersion();

        // Set up position mapping
        wrappedAdapter.setPositionMapper(this);

        // Forward data changes
        wrappedAdapter.registerAdapterDataObserver(dataObserver);

        setHasStableIds(true);
    }

    @Override
    public int map(int adapterPosition) {
        return getSongPosition(adapterPosition);
    }

    /**
     * Check if the item at the given position is an ad.
     * Uses O(1) math instead of iteration.
     *
     * Long lists: first ad at index 4, then every (AD_INTERVAL + 1): 4, 20, 36, ...
     * Short lists (fewer than FIRST_AD_OFFSET songs): a single ad at the end.
     */
    private boolean isAdAtPosition(int position) {
        if (!showAds) {
            return false;
        }
        int songCount = wrappedAdapter.getItemCount();
        if (songCount < MIN_ITEMS_FOR_AD) {
            return false;
        }
        // The first ad sits after min(FIRST_AD_OFFSET, songCount) songs: for a short
        // list that places a single ad at the very end; for a long list the first ad is
        // at index 4 and then repeats every (AD_INTERVAL + 1): 4, 20, 36, ...
        int firstAd = Math.min(FIRST_AD_OFFSET, songCount);
        if (position < firstAd) {
            return false;
        }
        return (position - firstAd) % (AD_INTERVAL + 1) == 0;
    }

    /**
     * Convert wrapper adapter position to actual song index in the wrapped adapter.
     * Uses O(1) math instead of iteration.
     */
    public int getSongPosition(int adapterPosition) {
        if (!showAds) {
            return adapterPosition;
        }
        int songCount = wrappedAdapter.getItemCount();
        if (songCount < MIN_ITEMS_FOR_AD) {
            return adapterPosition;
        }
        int firstAd = Math.min(FIRST_AD_OFFSET, songCount);
        if (adapterPosition < firstAd) {
            return adapterPosition;
        }
        // Subtract the number of ads that appear at or before adapterPosition.
        int adCount = (adapterPosition - firstAd) / (AD_INTERVAL + 1) + 1;
        return adapterPosition - adCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (isAdAtPosition(position)) {
            return VIEW_TYPE_AD;
        }
        // Delegate to wrapped adapter's view type for proper handling of offset items etc.
        int songPosition = getSongPosition(position);
        return wrappedAdapter.getItemViewType(songPosition);
    }

    @Override
    public int getItemCount() {
        int songCount = wrappedAdapter.getItemCount();
        if (!showAds || songCount < MIN_ITEMS_FOR_AD) {
            return songCount;
        }
        int adCount;
        if (songCount <= FIRST_AD_OFFSET) {
            adCount = 1; // single ad placed at the end of a short list
        } else {
            adCount = (songCount - FIRST_AD_OFFSET) / AD_INTERVAL + 1;
        }
        return songCount + adCount;
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            // Use negative IDs for ads to avoid conflicts with song IDs
            return -(position + 1);
        }
        int songPosition = getSongPosition(position);
        return wrappedAdapter.getItemId(songPosition);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_native_ad, parent, false);
            return new NativeAdViewHolder(view);
        }
        return wrappedAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NativeAdViewHolder) {
            // Get cached ad for this position, or fetch a new one
            NativeAd ad = adCache.get(position);
            if (ad == null && adManager != null) {
                ad = adManager.getAd();
                if (ad != null) {
                    adCache.put(position, ad);
                }
            }
            ((NativeAdViewHolder) holder).bind(ad, position);
        } else {
            int songPosition = getSongPosition(position);
            //noinspection unchecked
            wrappedAdapter.onBindViewHolder((SongAdapter.ViewHolder) holder, songPosition);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof NativeAdViewHolder) {
            // Don't return ads to pool - keep them in our position cache
            ((NativeAdViewHolder) holder).unbind();
        } else {
            //noinspection unchecked
            wrappedAdapter.onViewRecycled((SongAdapter.ViewHolder) holder);
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (getItemViewType(position) == VIEW_TYPE_AD) {
            // Return empty string for ads - they don't belong to any section
            return "";
        }
        int songPosition = getSongPosition(position);
        return wrappedAdapter.getSectionName(songPosition);
    }

    public void cleanup() {
        wrappedAdapter.unregisterAdapterDataObserver(dataObserver);
        wrappedAdapter.setPositionMapper(null);

        // Destroy all cached ads
        for (int i = 0; i < adCache.size(); i++) {
            NativeAd ad = adCache.valueAt(i);
            if (ad != null) {
                ad.destroy();
            }
        }
        adCache.clear();
    }

    /**
     * Get the wrapped SongAdapter.
     */
    public SongAdapter getWrappedAdapter() {
        return wrappedAdapter;
    }

    /**
     * Check if the item at the given position is an ad (public API).
     */
    public boolean isAdPosition(int position) {
        return isAdAtPosition(position);
    }

    /**
     * Notify only ad positions that data has changed.
     * This is more efficient than notifyDataSetChanged() and avoids rebinding all items.
     */
    public void notifyAdPositionsChanged() {
        int itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (isAdAtPosition(i)) {
                notifyItemChanged(i);
            }
        }
    }

    /**
     * ViewHolder for native ads.
     */
    static class NativeAdViewHolder extends RecyclerView.ViewHolder {
        private final NativeAdView adView;
        private final ImageView iconView;
        private final TextView headlineView;
        private final TextView bodyView;
        private final TextView ctaView;

        // Track the identity of the bound ad using System.identityHashCode
        private int boundAdIdentity = 0;

        NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = (NativeAdView) itemView;
            iconView = itemView.findViewById(R.id.ad_app_icon);
            headlineView = itemView.findViewById(R.id.ad_headline);
            bodyView = itemView.findViewById(R.id.ad_body);
            ctaView = itemView.findViewById(R.id.ad_call_to_action);

            // Register views with NativeAdView
            adView.setIconView(iconView);
            adView.setHeadlineView(headlineView);
            adView.setBodyView(bodyView);
            adView.setCallToActionView(ctaView);
        }

        void bind(@Nullable NativeAd ad, int position) {
            if (ad == null) {
                collapseView();
                boundAdIdentity = 0;
                return;
            }

            int adIdentity = System.identityHashCode(ad);
            expandView();

            // Always populate the views - ViewHolders get recycled so views may be stale
            headlineView.setText(ad.getHeadline());

            if (ad.getBody() != null) {
                bodyView.setText(ad.getBody());
                bodyView.setVisibility(View.VISIBLE);
            } else {
                bodyView.setVisibility(View.GONE);
            }

            if (ad.getCallToAction() != null) {
                ctaView.setText(ad.getCallToAction());
                ctaView.setVisibility(View.VISIBLE);
            } else {
                ctaView.setVisibility(View.GONE);
            }

            // Register the ad with the SDK first (this populates the asset views).
            // Only skip setNativeAd() if this exact ad is already bound to this exact
            // ViewHolder — position-level tracking doesn't work because ViewHolders
            // get recycled across positions.
            if (boundAdIdentity != adIdentity) {
                boundAdIdentity = adIdentity;
                adView.setNativeAd(ad);
            }

            // Decouple the icon from the ad SDK's bitmap. setNativeAd() (and some
            // mediation adapters) hand us an icon Drawable backed by a bitmap the SDK
            // can recycle out from under us; if this RecyclerView ImageView still
            // holds it, the next draw crashes in BaseCanvas.throwIfCannotDraw().
            // Snapshot it into an app-owned bitmap that only we control. Runs on every
            // bind (after any setNativeAd) so the ImageView never keeps the SDK's copy.
            Bitmap iconBitmap = ad.getIcon() != null ? toOwnedBitmap(ad.getIcon().getDrawable()) : null;
            if (iconBitmap != null) {
                iconView.setImageBitmap(iconBitmap);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setImageDrawable(null);
                iconView.setVisibility(View.INVISIBLE);
            }
        }

        /**
         * Render a native-ad icon Drawable into a fresh, app-owned Bitmap so the
         * RecyclerView never draws a bitmap the ad SDK / mediation can recycle.
         * Returns null (caller hides the icon) if anything goes wrong — never crashes.
         */
        @Nullable
        private static Bitmap toOwnedBitmap(@Nullable Drawable d) {
            if (d == null) return null;
            try {
                if (d instanceof BitmapDrawable) {
                    Bitmap src = ((BitmapDrawable) d).getBitmap();
                    if (src != null && !src.isRecycled()) {
                        Bitmap.Config cfg = src.getConfig() != null ? src.getConfig() : Bitmap.Config.ARGB_8888;
                        return src.copy(cfg, false);
                    }
                }
                int w = Math.max(1, d.getIntrinsicWidth());
                int h = Math.max(1, d.getIntrinsicHeight());
                Rect savedBounds = new Rect(d.getBounds());
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                d.setBounds(0, 0, w, h);
                d.draw(canvas);
                d.setBounds(savedBounds);
                return bmp;
            } catch (Exception | OutOfMemoryError e) {
                return null;
            }
        }

        void unbind() {
            // DON'T clear boundAdIdentity here!
            // The ViewHolder should remember what ad is bound to it.
            // When bind() is called again with the same ad, we'll skip setNativeAd().
            // Only clear when binding a null ad or a different ad.
        }

        private void collapseView() {
            itemView.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            if (params != null) {
                params.height = 0;
                itemView.setLayoutParams(params);
            }
        }

        private void expandView() {
            itemView.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            if (params != null) {
                // 72dp converted to pixels
                int heightPx = (int) (72 * itemView.getContext().getResources().getDisplayMetrics().density);
                params.height = heightPx;
                itemView.setLayoutParams(params);
            }
        }
    }
}
