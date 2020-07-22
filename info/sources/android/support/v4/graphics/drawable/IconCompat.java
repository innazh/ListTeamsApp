package android.support.v4.graphics.drawable;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build.VERSION;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;

public class IconCompat {
    private static final float ADAPTIVE_ICON_INSET_FACTOR = 0.25f;
    private static final int AMBIENT_SHADOW_ALPHA = 30;
    private static final float BLUR_FACTOR = 0.010416667f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 0.6666667f;
    private static final float ICON_DIAMETER_FACTOR = 0.9166667f;
    private static final int KEY_SHADOW_ALPHA = 61;
    private static final float KEY_SHADOW_OFFSET_FACTOR = 0.020833334f;
    private static final int TYPE_ADAPTIVE_BITMAP = 5;
    private static final int TYPE_BITMAP = 1;
    private static final int TYPE_DATA = 3;
    private static final int TYPE_RESOURCE = 2;
    private static final int TYPE_URI = 4;
    private int mInt1;
    private int mInt2;
    private Object mObj1;
    private final int mType;

    public static IconCompat createWithResource(Context context, int resId) {
        if (context != null) {
            IconCompat rep = new IconCompat(2);
            rep.mInt1 = resId;
            rep.mObj1 = context;
            return rep;
        }
        throw new IllegalArgumentException("Context must not be null.");
    }

    public static IconCompat createWithBitmap(Bitmap bits) {
        if (bits != null) {
            IconCompat rep = new IconCompat(1);
            rep.mObj1 = bits;
            return rep;
        }
        throw new IllegalArgumentException("Bitmap must not be null.");
    }

    public static IconCompat createWithAdaptiveBitmap(Bitmap bits) {
        if (bits != null) {
            IconCompat rep = new IconCompat(5);
            rep.mObj1 = bits;
            return rep;
        }
        throw new IllegalArgumentException("Bitmap must not be null.");
    }

    public static IconCompat createWithData(byte[] data, int offset, int length) {
        if (data != null) {
            IconCompat rep = new IconCompat(3);
            rep.mObj1 = data;
            rep.mInt1 = offset;
            rep.mInt2 = length;
            return rep;
        }
        throw new IllegalArgumentException("Data must not be null.");
    }

    public static IconCompat createWithContentUri(String uri) {
        if (uri != null) {
            IconCompat rep = new IconCompat(4);
            rep.mObj1 = uri;
            return rep;
        }
        throw new IllegalArgumentException("Uri must not be null.");
    }

    public static IconCompat createWithContentUri(Uri uri) {
        if (uri != null) {
            return createWithContentUri(uri.toString());
        }
        throw new IllegalArgumentException("Uri must not be null.");
    }

    private IconCompat(int mType2) {
        this.mType = mType2;
    }

    public Icon toIcon() {
        int i = this.mType;
        if (i == 1) {
            return Icon.createWithBitmap((Bitmap) this.mObj1);
        }
        if (i == 2) {
            return Icon.createWithResource((Context) this.mObj1, this.mInt1);
        }
        if (i == 3) {
            return Icon.createWithData((byte[]) this.mObj1, this.mInt1, this.mInt2);
        }
        if (i == 4) {
            return Icon.createWithContentUri((String) this.mObj1);
        }
        if (i != 5) {
            throw new IllegalArgumentException("Unknown type");
        } else if (VERSION.SDK_INT >= 26) {
            return Icon.createWithAdaptiveBitmap((Bitmap) this.mObj1);
        } else {
            return Icon.createWithBitmap(createLegacyIconFromAdaptiveIcon((Bitmap) this.mObj1, false));
        }
    }

    @Deprecated
    public void addToShortcutIntent(Intent outIntent) {
        addToShortcutIntent(outIntent, null);
    }

    public void addToShortcutIntent(Intent outIntent, Drawable badge) {
        Bitmap icon;
        Bitmap icon2;
        int i = this.mType;
        if (i == 1) {
            icon = (Bitmap) this.mObj1;
            if (badge != null) {
                icon = icon.copy(icon.getConfig(), true);
            }
        } else if (i != 2) {
            if (i == 5) {
                icon = createLegacyIconFromAdaptiveIcon((Bitmap) this.mObj1, true);
            } else {
                throw new IllegalArgumentException("Icon type not supported for intent shortcuts");
            }
        } else if (badge == null) {
            outIntent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.fromContext((Context) this.mObj1, this.mInt1));
            return;
        } else {
            Context context = (Context) this.mObj1;
            Drawable dr = ContextCompat.getDrawable(context, this.mInt1);
            if (dr.getIntrinsicWidth() <= 0 || dr.getIntrinsicHeight() <= 0) {
                int size = ((ActivityManager) context.getSystemService("activity")).getLauncherLargeIconSize();
                icon2 = Bitmap.createBitmap(size, size, Config.ARGB_8888);
            } else {
                icon2 = Bitmap.createBitmap(dr.getIntrinsicWidth(), dr.getIntrinsicHeight(), Config.ARGB_8888);
            }
            dr.setBounds(0, 0, icon2.getWidth(), icon2.getHeight());
            dr.draw(new Canvas(icon2));
            icon = icon2;
        }
        if (badge != null) {
            int w = icon.getWidth();
            int h = icon.getHeight();
            badge.setBounds(w / 2, h / 2, w, h);
            badge.draw(new Canvas(icon));
        }
        outIntent.putExtra("android.intent.extra.shortcut.ICON", icon);
    }

    static Bitmap createLegacyIconFromAdaptiveIcon(Bitmap adaptiveIconBitmap, boolean addShadow) {
        int size = (int) (((float) Math.min(adaptiveIconBitmap.getWidth(), adaptiveIconBitmap.getHeight())) * DEFAULT_VIEW_PORT_SCALE);
        Bitmap icon = Bitmap.createBitmap(size, size, Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint(3);
        float center = ((float) size) * 0.5f;
        float radius = ICON_DIAMETER_FACTOR * center;
        if (addShadow) {
            float blur = ((float) size) * BLUR_FACTOR;
            paint.setColor(0);
            paint.setShadowLayer(blur, 0.0f, ((float) size) * KEY_SHADOW_OFFSET_FACTOR, 1023410176);
            canvas.drawCircle(center, center, radius, paint);
            paint.setShadowLayer(blur, 0.0f, 0.0f, 503316480);
            canvas.drawCircle(center, center, radius, paint);
            paint.clearShadowLayer();
        }
        paint.setColor(ViewCompat.MEASURED_STATE_MASK);
        BitmapShader shader = new BitmapShader(adaptiveIconBitmap, TileMode.CLAMP, TileMode.CLAMP);
        Matrix shift = new Matrix();
        shift.setTranslate((float) ((-(adaptiveIconBitmap.getWidth() - size)) / 2), (float) ((-(adaptiveIconBitmap.getHeight() - size)) / 2));
        shader.setLocalMatrix(shift);
        paint.setShader(shader);
        canvas.drawCircle(center, center, radius, paint);
        canvas.setBitmap(null);
        return icon;
    }
}
