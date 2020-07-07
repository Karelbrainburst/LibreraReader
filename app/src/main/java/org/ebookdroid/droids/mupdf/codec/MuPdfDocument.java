package org.ebookdroid.droids.mupdf.codec;

import android.graphics.RectF;
import android.util.Log;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MuPdfDocument extends AbstractCodecDocument {

    public static final int FORMAT_PDF = 0;
    private static long cacheHandle;
    private static int cacheWH;
    private static long cacheSize;
    private static int cacheCount;
    int w, h;
    BookType bookType;
    private boolean isEpub = false;
    private volatile Map<String, String> footNotes;
    private volatile List<String> mediaAttachment;
    private int pagesCount = -1;
    private String fname;



    public MuPdfDocument(final MuPdfContext context, final int format, final String fname, final String pwd) {
        super(context, openFile(format, fname, pwd, BookCSS.get().toCssString(fname)));
        this.fname = fname;
        isEpub = ExtUtils.isTextFomat(fname);
        bookType = BookType.getByUri(fname);
        Log.v("MuPdfDocument","MuPdfDocument 111");
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect, final int flags) {

        if ((flags & 0x0F) == 0) {
            targetRect.right = targetRect.left = 0;
            targetRect.bottom = targetRect.top = 0;
            return;
        }

        final CodecPageInfo cpi = new CodecPageInfo();
        TempHolder.lock.lock();
        try {
            MuPdfDocument.getPageInfo(docHandle, targetPage, cpi);
        } finally {
            TempHolder.lock.unlock();
        }

        final float left = targetRect.left;
        final float top = targetRect.top;

        if (((cpi.rotation / 90) % 2) != 0) {
            targetRect.right = targetRect.left = left / cpi.height;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.width;
        } else {
            targetRect.right = targetRect.left = left / cpi.width;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.height;
        }
    }

    /**
     * 获取页面信息
     * @param docHandle
     * @param pageNumber
     * @param cpi
     * @return
     */
    native static int getPageInfo(long docHandle, int pageNumber, CodecPageInfo cpi);

    /**
     * 获取书元信息
     * @param docHandle
     * @param option
     * @return
     */
    // 'info:Title'
    // 'info:Author'
    // 'info:Subject'
    // 'info:Keywords'
    // 'info:Creator'
    // 'info:Producer'
    // 'info:CreationDate'
    // 'info:ModDate'
    private native static String getMeta(long docHandle, final String option);

    /**
     * 打开小说
     * @param format
     * @param fname
     * @param pwd
     * @param css
     * @return
     */
    private static long openFile(final int format, String fname, final String pwd, String css) {
        TempHolder.lock.lock();
        try {
            int allocatedMemory = AppState.get().allocatedMemorySize * 1024 * 1024;
            // int allocatedMemory = CoreSettings.get().pdfStorageSize;
            LOG.d("allocatedMemory", AppState.get().allocatedMemorySize, " MB " + allocatedMemory);
            final long open = open(allocatedMemory, format, fname, pwd, css, BookCSS.get().documentStyle == BookCSS.STYLES_ONLY_USER ? 0 : 1);
            LOG.d("TEST", "Open document " + fname + " " + open);
            LOG.d("TEST", "Open document css ", css);
            LOG.d("MUPDF! >>> open [document]", open, ExtUtils.getFileName(fname));
            //css: documentStyle0{}isAutoHypens1truezh{}b>span,strong>span{font-weight:normal}svg {display:block}math, m, svg>text {display:none}sup>* {font-size:0.83em;vertical-align:super; font-weigh:bold}@page{margin-top:0.9em !important;margin-right:1.0em !important;margin-bottom:0.5em !important;margin-left:1.0em !important;}section>title{page-break-before:avoide;}section>title>p{text-align:center !important; text-indent:0px !important;}title>p{text-align:center !important; text-indent:0px !important;}subtitle{text-align:center !important; text-indent:0px !important;}image{text-align:center; text-indent:0px;}section+section>title{page-break-before:always;}empty-line{display:block; padding:0.1em;}epigraph{text-align:right; margin-left:2em;font-style: italic;}text-author{font-style: italic;font-weight: bold;}p>image{display:block;}del,ins,u,strikethrough{font-family:monospace;}body {background-color:#FFFFFF;color:#000000;line-height:1.3em !important;}body{padding:0 !important; margin:0 !important;}t{color:#0066cc !important; font-style: italic;}a{color:#0066cc !important;}h1{font-size:1.50em; text-align: center; font-weight: bold; font-family: Times New Roman;}h2{font-size:1.30em; text-align: center; font-weight: bold; font-family: Times New Roman;}h3{font-size:1.15em; text-align: center; font-weight: bold; font-family: Times New Roman;}h4{font-size:1.00em; text-align: center; font-weight: bold; font-family: Times New Roman;}h5{font-size:0.80em; text-align: center; font-weight: bold; font-family: Times New Roman;}h6{font-size:0.60em; text-align: center; font-weight: bold; font-family: Times New Roman;}title, title>p{font-size:1.2em; font-weight: bold; font-family: Times New Roman;}subtitle, subtitle>p{font-size:1.0em; font-weight: bold; font-family: Times New Roman;}h1,h2,h3,h4,h5,h6,img {text-indent:0px !important; text-align: center;}body,p{text-align:justify;}p,span{text-indent:1.0em;}i{font-family:Times New Roman; font-style: italic, oblique;}code,pre,pre>* {white-space: pre-line;}|


            if (open == -1) {
                throw new RuntimeException("Document is corrupted");
            }

            // final int n = getPageCountWithException(open);
            return open;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    /**
     * 获取Mupdf版本号
     * @return
     */
    public static native int getMupdfVersion();


    /**
     * 打开小说方法的最终入口
     * @param storememory
     * @param format
     * @param fname
     * @param pwd
     * @param css
     * @param useDocStyle
     * @return
     */
    private static native long open(int storememory, int format, String fname, String pwd, String css, int useDocStyle);

    /**
     * 释放
     * @param handle
     */
    private static native void free(long handle);

    private static int getPageCountWithException(final long handle, int w, int h, int size) {
        final int count = getPageCountSafe(handle, w, h, Dips.spToPx(size));
//        if (count == 0) {
//            throw new RuntimeException("Document is corrupted");
//        }
        return count;
    }

    private static int getPageCountSafe(long handle, int w, int h, int size) {
        LOG.d("getPageCountSafe w h size", w, h, size);

        if (handle == cacheHandle && size == cacheSize && w + h == cacheWH) {
            LOG.d("getPageCount from cache", cacheCount);
            return cacheCount;
        }
        TempHolder.lock.lock();
        try {
            cacheHandle = handle;
            cacheSize = size;
            cacheWH = w + h;
            cacheCount = getPageCount(handle, w, h, size);
            LOG.d("getPageCount put to  cache", cacheCount);
            return cacheCount;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    /**
     * 获取页数
     * @param handle
     * @param w
     * @param h
     * @param size
     * @return
     */
    private static native int getPageCount(long handle, int w, int h, int size);

    @Override
    public BookType getBookType() {
        return bookType;
    }

    @Override
    public String documentToHtml() {
        StringBuilder out = new StringBuilder();
        int pages = getPageCount();
        for (int i = 0; i < pages; i++) {
            CodecPage pageCodec = getPage(i);
            String pageHTML = pageCodec.getPageHTML();
            out.append(pageHTML);
        }
        Log.v("MuPdfDocument","documentToHtml: "+ out.toString());
        return out.toString();
    }

    @Override
    public Map<String, String> getFootNotes() {
        return footNotes;
    }

    public void setFootNotes(Map<String, String> footNotes) {
        this.footNotes = footNotes;
    }

    @Override
    public List<OutlineLink> getOutline() {
        final MuPdfOutline ou = new MuPdfOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        MuPdfPage createPage = MuPdfPage.createPage(this, pageNumber + 1);
        return createPage;
    }

    @Override
    public int getPageCount() {
        LOG.d("MuPdfDocument,getPageCount", getW(), getH(), BookCSS.get().fontSizeSp);
        return getPageCountWithException(documentHandle, getW(), getH(), BookCSS.get().fontSizeSp);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        if (isEpub) {
            LOG.d("MuPdfDocument, getUnifiedPageInfo");
            return new CodecPageInfo(getW(), getH());
        } else {
            return null;
        }
    }

    @Override
    public int getPageCount(int w, int h, int size) {
        this.w = w;
        this.h = h;
        int pageCountWithException = getPageCountWithException(documentHandle, w, h, size);
        LOG.d("MuPdfDocument,, getPageCount", w, h, size, "count", pageCountWithException);
        return pageCountWithException;
    }

    public int getW() {
        return w > 0 ? w : Dips.screenWidth();
    }

    public int getH() {
        return h > 0 ? h : Dips.screenHeight();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        TempHolder.lock.lock();
        try {
            final int res = getPageInfo(documentHandle, pageNumber + 1, info);
            if (res == -1) {
                return null;
            } else {
                // Check rotation
                info.rotation = (360 + info.rotation) % 360;
                return info;
            }
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    protected void freeDocument() {
        TempHolder.lock.lock();
        try {
            cacheHandle = -1;
            free(documentHandle);
        }finally {
            TempHolder.lock.unlock();
        }

        LOG.d("MUPDF! <<< recycle [document]", documentHandle, ExtUtils.getFileName(fname));
    }

    @Override
    public String getMeta(final String option) {
        TempHolder.lock.lock();
        try {

            if (true) {
                return getMeta(documentHandle, option);
            }

            final AtomicBoolean ready = new AtomicBoolean(false);
            final StringBuilder info = new StringBuilder();

            new Thread() {
                @Override
                public void run() {

                    try {
                        LOG.d("getMeta", option);
                        String key = getMeta(documentHandle, option);
                        info.append(key);
                    } catch (Throwable e) {
                        LOG.e(e);
                    } finally {
                        ready.set(true);
                    }

                }

                ;
            }.start();

            while (!ready.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }

            return info.toString();
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getBookTitle() {
        return getMeta("info:Title");
    }

    @Override
    public String getBookAuthor() {
        return getMeta("info:Author");
    }

    /**
     * 保存内部
     * @param handle
     * @param path
     */
    private native void saveInternal(long handle, String path);

    /**
     * 内部有变更
     * @param handle
     * @return
     */
    private native boolean hasChangesInternal(long handle);

    @Override
    public boolean hasChanges() {
        TempHolder.lock.lock();
        try {
            return hasChangesInternal(documentHandle);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public void saveAnnotations(String path) {
        LOG.d("Save Annotations saveInternal 1");
        TempHolder.lock.lock();
        try {
            saveInternal(documentHandle, path);
            LOG.d("Save Annotations saveInternal 2");
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public List<RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        throw new DocSearchNotSupported();
    }

    @Override
    public void deleteAnnotation(long pageHandle, int index) {
        TempHolder.lock.lock();
        try {
            deleteAnnotationInternal(documentHandle, pageHandle, index);
        } finally {
            TempHolder.lock.unlock();
        }

    }

    /**
     * 删除内部注释
     * @param docHandle
     * @param pageHandle
     * @param annot_index
     */
    private native void deleteAnnotationInternal(long docHandle, long pageHandle, int annot_index);

    public void setMediaAttachment(List<String> mediaAttachment) {
        this.mediaAttachment = mediaAttachment;
    }

    @Override
    public List<String> getMediaAttachments() {
        return mediaAttachment;
    }

}
