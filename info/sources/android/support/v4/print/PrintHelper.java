package android.support.v4.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument.Page;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Builder;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PrintHelper {
    public static final int COLOR_MODE_COLOR = 2;
    public static final int COLOR_MODE_MONOCHROME = 1;
    public static final int ORIENTATION_LANDSCAPE = 1;
    public static final int ORIENTATION_PORTRAIT = 2;
    public static final int SCALE_MODE_FILL = 2;
    public static final int SCALE_MODE_FIT = 1;
    private final PrintHelperVersionImpl mImpl;

    @Retention(RetentionPolicy.SOURCE)
    private @interface ColorMode {
    }

    public interface OnPrintFinishCallback {
        void onFinish();
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface Orientation {
    }

    private static class PrintHelperApi19 implements PrintHelperVersionImpl {
        private static final String LOG_TAG = "PrintHelperApi19";
        private static final int MAX_PRINT_SIZE = 3500;
        int mColorMode = 2;
        final Context mContext;
        Options mDecodeOptions = null;
        protected boolean mIsMinMarginsHandlingCorrect = true;
        /* access modifiers changed from: private */
        public final Object mLock = new Object();
        int mOrientation;
        protected boolean mPrintActivityRespectsOrientation = true;
        int mScaleMode = 2;

        PrintHelperApi19(Context context) {
            this.mContext = context;
        }

        public void setScaleMode(int scaleMode) {
            this.mScaleMode = scaleMode;
        }

        public int getScaleMode() {
            return this.mScaleMode;
        }

        public void setColorMode(int colorMode) {
            this.mColorMode = colorMode;
        }

        public void setOrientation(int orientation) {
            this.mOrientation = orientation;
        }

        public int getOrientation() {
            int i = this.mOrientation;
            if (i == 0) {
                return 1;
            }
            return i;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        /* access modifiers changed from: private */
        public static boolean isPortrait(Bitmap bitmap) {
            return bitmap.getWidth() <= bitmap.getHeight();
        }

        /* access modifiers changed from: protected */
        public Builder copyAttributes(PrintAttributes other) {
            Builder b = new Builder().setMediaSize(other.getMediaSize()).setResolution(other.getResolution()).setMinMargins(other.getMinMargins());
            if (other.getColorMode() != 0) {
                b.setColorMode(other.getColorMode());
            }
            return b;
        }

        public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback) {
            MediaSize mediaSize;
            if (bitmap != null) {
                int fittingMode = this.mScaleMode;
                PrintManager printManager = (PrintManager) this.mContext.getSystemService("print");
                if (isPortrait(bitmap)) {
                    mediaSize = MediaSize.UNKNOWN_PORTRAIT;
                } else {
                    mediaSize = MediaSize.UNKNOWN_LANDSCAPE;
                }
                PrintAttributes attr = new Builder().setMediaSize(mediaSize).setColorMode(this.mColorMode).build();
                final String str = jobName;
                final int i = fittingMode;
                final Bitmap bitmap2 = bitmap;
                final OnPrintFinishCallback onPrintFinishCallback = callback;
                AnonymousClass1 r0 = new PrintDocumentAdapter() {
                    private PrintAttributes mAttributes;

                    public void onLayout(PrintAttributes oldPrintAttributes, PrintAttributes newPrintAttributes, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
                        this.mAttributes = newPrintAttributes;
                        layoutResultCallback.onLayoutFinished(new PrintDocumentInfo.Builder(str).setContentType(1).setPageCount(1).build(), true ^ newPrintAttributes.equals(oldPrintAttributes));
                    }

                    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor fileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
                        PrintHelperApi19.this.writeBitmap(this.mAttributes, i, bitmap2, fileDescriptor, cancellationSignal, writeResultCallback);
                    }

                    public void onFinish() {
                        OnPrintFinishCallback onPrintFinishCallback = onPrintFinishCallback;
                        if (onPrintFinishCallback != null) {
                            onPrintFinishCallback.onFinish();
                        }
                    }
                };
                printManager.print(jobName, r0, attr);
            }
        }

        /* access modifiers changed from: private */
        public Matrix getMatrix(int imageWidth, int imageHeight, RectF content, int fittingMode) {
            float scale;
            Matrix matrix = new Matrix();
            float scale2 = content.width() / ((float) imageWidth);
            if (fittingMode == 2) {
                scale = Math.max(scale2, content.height() / ((float) imageHeight));
            } else {
                scale = Math.min(scale2, content.height() / ((float) imageHeight));
            }
            matrix.postScale(scale, scale);
            matrix.postTranslate((content.width() - (((float) imageWidth) * scale)) / 2.0f, (content.height() - (((float) imageHeight) * scale)) / 2.0f);
            return matrix;
        }

        /* access modifiers changed from: private */
        public void writeBitmap(PrintAttributes attributes, int fittingMode, Bitmap bitmap, ParcelFileDescriptor fileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
            PrintAttributes pdfAttributes;
            if (this.mIsMinMarginsHandlingCorrect) {
                pdfAttributes = attributes;
            } else {
                pdfAttributes = copyAttributes(attributes).setMinMargins(new Margins(0, 0, 0, 0)).build();
            }
            final CancellationSignal cancellationSignal2 = cancellationSignal;
            final PrintAttributes printAttributes = pdfAttributes;
            final Bitmap bitmap2 = bitmap;
            final PrintAttributes printAttributes2 = attributes;
            final int i = fittingMode;
            final ParcelFileDescriptor parcelFileDescriptor = fileDescriptor;
            final WriteResultCallback writeResultCallback2 = writeResultCallback;
            AnonymousClass2 r0 = new AsyncTask<Void, Void, Throwable>() {
                /* access modifiers changed from: protected */
                public Throwable doInBackground(Void... params) {
                    PrintedPdfDocument pdfDocument;
                    Bitmap maybeGrayscale;
                    RectF contentRect;
                    try {
                        if (cancellationSignal2.isCanceled()) {
                            return null;
                        }
                        pdfDocument = new PrintedPdfDocument(PrintHelperApi19.this.mContext, printAttributes);
                        maybeGrayscale = PrintHelperApi19.this.convertBitmapForColorMode(bitmap2, printAttributes.getColorMode());
                        if (cancellationSignal2.isCanceled()) {
                            return null;
                        }
                        Page page = pdfDocument.startPage(1);
                        if (PrintHelperApi19.this.mIsMinMarginsHandlingCorrect) {
                            contentRect = new RectF(page.getInfo().getContentRect());
                        } else {
                            PrintedPdfDocument dummyDocument = new PrintedPdfDocument(PrintHelperApi19.this.mContext, printAttributes2);
                            Page dummyPage = dummyDocument.startPage(1);
                            RectF contentRect2 = new RectF(dummyPage.getInfo().getContentRect());
                            dummyDocument.finishPage(dummyPage);
                            dummyDocument.close();
                            contentRect = contentRect2;
                        }
                        Matrix matrix = PrintHelperApi19.this.getMatrix(maybeGrayscale.getWidth(), maybeGrayscale.getHeight(), contentRect, i);
                        if (!PrintHelperApi19.this.mIsMinMarginsHandlingCorrect) {
                            matrix.postTranslate(contentRect.left, contentRect.top);
                            page.getCanvas().clipRect(contentRect);
                        }
                        page.getCanvas().drawBitmap(maybeGrayscale, matrix, null);
                        pdfDocument.finishPage(page);
                        if (cancellationSignal2.isCanceled()) {
                            pdfDocument.close();
                            if (parcelFileDescriptor != null) {
                                try {
                                    parcelFileDescriptor.close();
                                } catch (IOException e) {
                                }
                            }
                            if (maybeGrayscale != bitmap2) {
                                maybeGrayscale.recycle();
                            }
                            return null;
                        }
                        pdfDocument.writeTo(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
                        pdfDocument.close();
                        if (parcelFileDescriptor != null) {
                            try {
                                parcelFileDescriptor.close();
                            } catch (IOException e2) {
                            }
                        }
                        if (maybeGrayscale != bitmap2) {
                            maybeGrayscale.recycle();
                        }
                        return null;
                    } catch (Throwable t) {
                        return t;
                    }
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Throwable throwable) {
                    if (cancellationSignal2.isCanceled()) {
                        writeResultCallback2.onWriteCancelled();
                    } else if (throwable == null) {
                        writeResultCallback2.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                    } else {
                        Log.e(PrintHelperApi19.LOG_TAG, "Error writing printed content", throwable);
                        writeResultCallback2.onWriteFailed(null);
                    }
                }
            };
            r0.execute(new Void[0]);
        }

        public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback) throws FileNotFoundException {
            final String str = jobName;
            final Uri uri = imageFile;
            final OnPrintFinishCallback onPrintFinishCallback = callback;
            final int i = this.mScaleMode;
            AnonymousClass3 r0 = new PrintDocumentAdapter() {
                /* access modifiers changed from: private */
                public PrintAttributes mAttributes;
                Bitmap mBitmap = null;
                AsyncTask<Uri, Boolean, Bitmap> mLoadBitmap;

                public void onLayout(PrintAttributes oldPrintAttributes, PrintAttributes newPrintAttributes, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
                    synchronized (this) {
                        this.mAttributes = newPrintAttributes;
                    }
                    if (cancellationSignal.isCanceled()) {
                        layoutResultCallback.onLayoutCancelled();
                    } else if (this.mBitmap != null) {
                        layoutResultCallback.onLayoutFinished(new PrintDocumentInfo.Builder(str).setContentType(1).setPageCount(1).build(), true ^ newPrintAttributes.equals(oldPrintAttributes));
                    } else {
                        final CancellationSignal cancellationSignal2 = cancellationSignal;
                        final PrintAttributes printAttributes = newPrintAttributes;
                        final PrintAttributes printAttributes2 = oldPrintAttributes;
                        final LayoutResultCallback layoutResultCallback2 = layoutResultCallback;
                        AnonymousClass1 r2 = new AsyncTask<Uri, Boolean, Bitmap>() {
                            /* access modifiers changed from: protected */
                            public void onPreExecute() {
                                cancellationSignal2.setOnCancelListener(new OnCancelListener() {
                                    public void onCancel() {
                                        AnonymousClass3.this.cancelLoad();
                                        AnonymousClass1.this.cancel(false);
                                    }
                                });
                            }

                            /* access modifiers changed from: protected */
                            public Bitmap doInBackground(Uri... uris) {
                                try {
                                    return PrintHelperApi19.this.loadConstrainedBitmap(uri);
                                } catch (FileNotFoundException e) {
                                    return null;
                                }
                            }

                            /* access modifiers changed from: protected */
                            /* JADX WARNING: Code restructure failed: missing block: B:11:0x0022, code lost:
                                if (r1 == null) goto L_0x0054;
                             */
                            /* JADX WARNING: Code restructure failed: missing block: B:13:0x002c, code lost:
                                if (r1.isPortrait() == android.support.v4.print.PrintHelper.PrintHelperApi19.access$600(r12)) goto L_0x0054;
                             */
                            /* JADX WARNING: Code restructure failed: missing block: B:14:0x002e, code lost:
                                r2 = new android.graphics.Matrix();
                                r2.postRotate(90.0f);
                                r12 = android.graphics.Bitmap.createBitmap(r12, 0, 0, r12.getWidth(), r12.getHeight(), r2, true);
                             */
                            public void onPostExecute(Bitmap bitmap) {
                                super.onPostExecute(bitmap);
                                if (bitmap != null && (!PrintHelperApi19.this.mPrintActivityRespectsOrientation || PrintHelperApi19.this.mOrientation == 0)) {
                                    synchronized (this) {
                                        try {
                                            MediaSize mediaSize = AnonymousClass3.this.mAttributes.getMediaSize();
                                            try {
                                            } catch (Throwable th) {
                                                MediaSize mediaSize2 = mediaSize;
                                                th = th;
                                                MediaSize mediaSize3 = mediaSize2;
                                                while (true) {
                                                    try {
                                                        break;
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                    }
                                                }
                                                throw th;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    }
                                }
                                AnonymousClass3 r1 = AnonymousClass3.this;
                                r1.mBitmap = bitmap;
                                if (bitmap != null) {
                                    layoutResultCallback2.onLayoutFinished(new PrintDocumentInfo.Builder(str).setContentType(1).setPageCount(1).build(), true ^ printAttributes.equals(printAttributes2));
                                } else {
                                    layoutResultCallback2.onLayoutFailed(null);
                                }
                                AnonymousClass3.this.mLoadBitmap = null;
                            }

                            /* access modifiers changed from: protected */
                            public void onCancelled(Bitmap result) {
                                layoutResultCallback2.onLayoutCancelled();
                                AnonymousClass3.this.mLoadBitmap = null;
                            }
                        };
                        this.mLoadBitmap = r2.execute(new Uri[0]);
                    }
                }

                /* access modifiers changed from: private */
                public void cancelLoad() {
                    synchronized (PrintHelperApi19.this.mLock) {
                        if (PrintHelperApi19.this.mDecodeOptions != null) {
                            PrintHelperApi19.this.mDecodeOptions.requestCancelDecode();
                            PrintHelperApi19.this.mDecodeOptions = null;
                        }
                    }
                }

                public void onFinish() {
                    super.onFinish();
                    cancelLoad();
                    AsyncTask<Uri, Boolean, Bitmap> asyncTask = this.mLoadBitmap;
                    if (asyncTask != null) {
                        asyncTask.cancel(true);
                    }
                    OnPrintFinishCallback onPrintFinishCallback = onPrintFinishCallback;
                    if (onPrintFinishCallback != null) {
                        onPrintFinishCallback.onFinish();
                    }
                    Bitmap bitmap = this.mBitmap;
                    if (bitmap != null) {
                        bitmap.recycle();
                        this.mBitmap = null;
                    }
                }

                public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor fileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
                    PrintHelperApi19.this.writeBitmap(this.mAttributes, i, this.mBitmap, fileDescriptor, cancellationSignal, writeResultCallback);
                }
            };
            PrintManager printManager = (PrintManager) this.mContext.getSystemService("print");
            Builder builder = new Builder();
            builder.setColorMode(this.mColorMode);
            int i2 = this.mOrientation;
            if (i2 == 1 || i2 == 0) {
                builder.setMediaSize(MediaSize.UNKNOWN_LANDSCAPE);
            } else if (i2 == 2) {
                builder.setMediaSize(MediaSize.UNKNOWN_PORTRAIT);
            }
            printManager.print(jobName, r0, builder.build());
        }

        /* access modifiers changed from: private */
        public Bitmap loadConstrainedBitmap(Uri uri) throws FileNotFoundException {
            if (uri == null || this.mContext == null) {
                throw new IllegalArgumentException("bad argument to getScaledBitmap");
            }
            Options opt = new Options();
            opt.inJustDecodeBounds = true;
            loadBitmap(uri, opt);
            int w = opt.outWidth;
            int h = opt.outHeight;
            if (w <= 0 || h <= 0) {
                return null;
            }
            int imageSide = Math.max(w, h);
            int sampleSize = 1;
            while (imageSide > MAX_PRINT_SIZE) {
                imageSide >>>= 1;
                sampleSize <<= 1;
            }
            if (sampleSize <= 0 || Math.min(w, h) / sampleSize <= 0) {
                return null;
            }
            synchronized (this.mLock) {
                try {
                    this.mDecodeOptions = new Options();
                    this.mDecodeOptions.inMutable = true;
                    this.mDecodeOptions.inSampleSize = sampleSize;
                    Options decodeOptions = this.mDecodeOptions;
                    try {
                        try {
                            Bitmap loadBitmap = loadBitmap(uri, decodeOptions);
                            synchronized (this.mLock) {
                                this.mDecodeOptions = null;
                            }
                            return loadBitmap;
                        } catch (Throwable th) {
                            synchronized (this.mLock) {
                                this.mDecodeOptions = null;
                                throw th;
                            }
                        }
                    } catch (Throwable th2) {
                        Throwable th3 = th2;
                        Options options = decodeOptions;
                        th = th3;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }

        private Bitmap loadBitmap(Uri uri, Options o) throws FileNotFoundException {
            String str = "close fail ";
            String str2 = LOG_TAG;
            if (uri != null) {
                Context context = this.mContext;
                if (context != null) {
                    InputStream is = null;
                    try {
                        is = context.getContentResolver().openInputStream(uri);
                        Bitmap decodeStream = BitmapFactory.decodeStream(is, null, o);
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException t) {
                                Log.w(str2, str, t);
                            }
                        }
                        return decodeStream;
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException t2) {
                                Log.w(str2, str, t2);
                            }
                        }
                    }
                }
            }
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }

        /* access modifiers changed from: private */
        public Bitmap convertBitmapForColorMode(Bitmap original, int colorMode) {
            if (colorMode != 1) {
                return original;
            }
            Bitmap grayscale = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Config.ARGB_8888);
            Canvas c = new Canvas(grayscale);
            Paint p = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0.0f);
            p.setColorFilter(new ColorMatrixColorFilter(cm));
            c.drawBitmap(original, 0.0f, 0.0f, p);
            c.setBitmap(null);
            return grayscale;
        }
    }

    private static class PrintHelperApi20 extends PrintHelperApi19 {
        PrintHelperApi20(Context context) {
            super(context);
            this.mPrintActivityRespectsOrientation = false;
        }
    }

    private static class PrintHelperApi23 extends PrintHelperApi20 {
        /* access modifiers changed from: protected */
        public Builder copyAttributes(PrintAttributes other) {
            Builder b = super.copyAttributes(other);
            if (other.getDuplexMode() != 0) {
                b.setDuplexMode(other.getDuplexMode());
            }
            return b;
        }

        PrintHelperApi23(Context context) {
            super(context);
            this.mIsMinMarginsHandlingCorrect = false;
        }
    }

    private static class PrintHelperApi24 extends PrintHelperApi23 {
        PrintHelperApi24(Context context) {
            super(context);
            this.mIsMinMarginsHandlingCorrect = true;
            this.mPrintActivityRespectsOrientation = true;
        }
    }

    private static final class PrintHelperStub implements PrintHelperVersionImpl {
        int mColorMode;
        int mOrientation;
        int mScaleMode;

        private PrintHelperStub() {
            this.mScaleMode = 2;
            this.mColorMode = 2;
            this.mOrientation = 1;
        }

        public void setScaleMode(int scaleMode) {
            this.mScaleMode = scaleMode;
        }

        public int getScaleMode() {
            return this.mScaleMode;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        public void setColorMode(int colorMode) {
            this.mColorMode = colorMode;
        }

        public void setOrientation(int orientation) {
            this.mOrientation = orientation;
        }

        public int getOrientation() {
            return this.mOrientation;
        }

        public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback) {
        }

        public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback) {
        }
    }

    interface PrintHelperVersionImpl {
        int getColorMode();

        int getOrientation();

        int getScaleMode();

        void printBitmap(String str, Bitmap bitmap, OnPrintFinishCallback onPrintFinishCallback);

        void printBitmap(String str, Uri uri, OnPrintFinishCallback onPrintFinishCallback) throws FileNotFoundException;

        void setColorMode(int i);

        void setOrientation(int i);

        void setScaleMode(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ScaleMode {
    }

    public static boolean systemSupportsPrint() {
        return VERSION.SDK_INT >= 19;
    }

    public PrintHelper(Context context) {
        if (VERSION.SDK_INT >= 24) {
            this.mImpl = new PrintHelperApi24(context);
        } else if (VERSION.SDK_INT >= 23) {
            this.mImpl = new PrintHelperApi23(context);
        } else if (VERSION.SDK_INT >= 20) {
            this.mImpl = new PrintHelperApi20(context);
        } else if (VERSION.SDK_INT >= 19) {
            this.mImpl = new PrintHelperApi19(context);
        } else {
            this.mImpl = new PrintHelperStub();
        }
    }

    public void setScaleMode(int scaleMode) {
        this.mImpl.setScaleMode(scaleMode);
    }

    public int getScaleMode() {
        return this.mImpl.getScaleMode();
    }

    public void setColorMode(int colorMode) {
        this.mImpl.setColorMode(colorMode);
    }

    public int getColorMode() {
        return this.mImpl.getColorMode();
    }

    public void setOrientation(int orientation) {
        this.mImpl.setOrientation(orientation);
    }

    public int getOrientation() {
        return this.mImpl.getOrientation();
    }

    public void printBitmap(String jobName, Bitmap bitmap) {
        this.mImpl.printBitmap(jobName, bitmap, (OnPrintFinishCallback) null);
    }

    public void printBitmap(String jobName, Bitmap bitmap, OnPrintFinishCallback callback) {
        this.mImpl.printBitmap(jobName, bitmap, callback);
    }

    public void printBitmap(String jobName, Uri imageFile) throws FileNotFoundException {
        this.mImpl.printBitmap(jobName, imageFile, (OnPrintFinishCallback) null);
    }

    public void printBitmap(String jobName, Uri imageFile, OnPrintFinishCallback callback) throws FileNotFoundException {
        this.mImpl.printBitmap(jobName, imageFile, callback);
    }
}
