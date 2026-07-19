package com.kabouzeid.trebl.glide.palette;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BitmapPaletteResource implements Resource<BitmapPaletteWrapper> {

    private final BitmapPaletteWrapper bitmapPaletteWrapper;
    private final BitmapPool bitmapPool;

    public BitmapPaletteResource(BitmapPaletteWrapper bitmapPaletteWrapper, BitmapPool bitmapPool) {
        this.bitmapPaletteWrapper = bitmapPaletteWrapper;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public BitmapPaletteWrapper get() {
        return bitmapPaletteWrapper;
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(bitmapPaletteWrapper.getBitmap());
    }

    @Override
    public void recycle() {
        // Intentionally a no-op. BitmapPaletteTarget displays this exact Bitmap via
        // ImageView.setImageBitmap(), so returning it to Glide's BitmapPool (as this
        // used to do) let Glide reuse/recycle it for another decode while a
        // RecyclerView row was still drawing it — the "trying to use a recycled
        // bitmap" crash in BaseCanvas.throwIfCannotDraw(). We now leave the bitmap
        // for the GC to reclaim once no view references it. This gives up some
        // bitmap-pool reuse in exchange for not recycling on-screen thumbnails.
    }
}
