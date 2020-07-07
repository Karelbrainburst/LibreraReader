package org.ebookdroid.droids.mupdf.codec;

import android.util.Log;

import org.ebookdroid.core.codec.CodecDocument;

public class PdfContext extends MuPdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, final String password) {
        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        Log.v("PdfContext","PdfContext 111");
        return muPdfDocument;
    }


}
