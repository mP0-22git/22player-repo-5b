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
        // Only return to pool — never call bitmap.recycle() manually.
        // A recycled bitmap can still be referenced by an ImageView in a
        // RecyclerView, and drawing it causes RuntimeException in
        // RecordingCanvas.throwIfCannotDraw(). Let the GC collect
        // unreferenced bitmaps naturally if the pool rejects them.
        bitmapPool.put(bitmapPaletteWrapper.getBitmap());
    }
}
