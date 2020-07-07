package org.ebookdroid.droids.mupdf.codec;

import android.graphics.Bitmap;

import org.ebookdroid.core.codec.AbstractCodecContext;

/**
 * MuPdf解码器，读取小说
 */
public abstract class MuPdfContext extends AbstractCodecContext {

    public static final Bitmap.Config NATIVE_BITMAP_CFG = Bitmap.Config.ARGB_8888;

    public MuPdfContext() {
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
    return NATIVE_BITMAP_CFG;
    }


    @Override
    public boolean isParallelPageAccessAvailable() {
        return false;
    }



}
