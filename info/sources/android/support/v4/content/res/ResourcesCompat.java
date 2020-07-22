package android.support.v4.content.res;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.graphics.TypefaceCompat;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.util.TypedValue;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public final class ResourcesCompat {
    private static final String TAG = "ResourcesCompat";

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(Typeface typeface);

        public final void callbackSuccessAsync(final Typeface typeface, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        public final void callbackFailAsync(final int reason, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    public static Drawable getDrawable(Resources res, int id, Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    public static Drawable getDrawableForDensity(Resources res, int id, int density, Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawableForDensity(id, density, theme);
        }
        if (VERSION.SDK_INT >= 15) {
            return res.getDrawableForDensity(id, density);
        }
        return res.getDrawable(id);
    }

    public static int getColor(Resources res, int id, Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColor(id, theme);
        }
        return res.getColor(id);
    }

    public static ColorStateList getColorStateList(Resources res, int id, Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColorStateList(id, theme);
        }
        return res.getColorStateList(id);
    }

    public static Typeface getFont(Context context, int id) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), 0, null, null, false);
    }

    public static void getFont(Context context, int id, FontCallback fontCallback, Handler handler) throws NotFoundException {
        Preconditions.checkNotNull(fontCallback);
        if (context.isRestricted()) {
            fontCallback.callbackFailAsync(-4, handler);
            return;
        }
        loadFont(context, id, new TypedValue(), 0, fontCallback, handler, false);
    }

    public static Typeface getFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback, Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface != null || fontCallback != null) {
            return typeface;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Font resource ID #0x");
        sb.append(Integer.toHexString(id));
        sb.append(" could not be retrieved.");
        throw new NotFoundException(sb.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x00f5  */
    private static Typeface loadFont(Context context, Resources wrapper, TypedValue value, int id, int style, FontCallback fontCallback, Handler handler, boolean isRequestFromLayoutInflator) {
        String file;
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i = id;
        int i2 = style;
        FontCallback fontCallback2 = fontCallback;
        Handler handler2 = handler;
        String str = TAG;
        if (typedValue.string != null) {
            String file2 = typedValue.string.toString();
            if (!file2.startsWith("res/")) {
                if (fontCallback2 != null) {
                    fontCallback2.callbackFailAsync(-3, handler2);
                }
                return null;
            }
            Typeface typeface = TypefaceCompat.findFromCache(resources, i, i2);
            if (typeface != null) {
                if (fontCallback2 != null) {
                    fontCallback2.callbackSuccessAsync(typeface, handler2);
                }
                return typeface;
            }
            try {
                if (file2.toLowerCase().endsWith(".xml")) {
                    try {
                        FamilyResourceEntry familyEntry = FontResourcesParserCompat.parse(resources.getXml(i), resources);
                        if (familyEntry == null) {
                            try {
                                Log.e(str, "Failed to find font-family tag");
                                if (fontCallback2 != null) {
                                    fontCallback2.callbackFailAsync(-3, handler2);
                                }
                                return null;
                            } catch (XmlPullParserException e) {
                                e = e;
                                Context context2 = context;
                                Typeface typeface2 = typeface;
                                file = file2;
                                StringBuilder sb = new StringBuilder();
                                sb.append("Failed to parse xml resource ");
                                sb.append(file);
                                Log.e(str, sb.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            } catch (IOException e2) {
                                e = e2;
                                Context context3 = context;
                                Typeface typeface3 = typeface;
                                file = file2;
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("Failed to read xml resource ");
                                sb2.append(file);
                                Log.e(str, sb2.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            }
                        } else {
                            Typeface typeface4 = typeface;
                            file = file2;
                            try {
                                return TypefaceCompat.createFromResourcesFamilyXml(context, familyEntry, wrapper, id, style, fontCallback, handler, isRequestFromLayoutInflator);
                            } catch (XmlPullParserException e3) {
                                e = e3;
                                Context context4 = context;
                                StringBuilder sb3 = new StringBuilder();
                                sb3.append("Failed to parse xml resource ");
                                sb3.append(file);
                                Log.e(str, sb3.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            } catch (IOException e4) {
                                e = e4;
                                Context context5 = context;
                                StringBuilder sb22 = new StringBuilder();
                                sb22.append("Failed to read xml resource ");
                                sb22.append(file);
                                Log.e(str, sb22.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            }
                        }
                    } catch (XmlPullParserException e5) {
                        e = e5;
                        Typeface typeface5 = typeface;
                        file = file2;
                        Context context6 = context;
                        StringBuilder sb32 = new StringBuilder();
                        sb32.append("Failed to parse xml resource ");
                        sb32.append(file);
                        Log.e(str, sb32.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    } catch (IOException e6) {
                        e = e6;
                        Typeface typeface6 = typeface;
                        file = file2;
                        Context context7 = context;
                        StringBuilder sb222 = new StringBuilder();
                        sb222.append("Failed to read xml resource ");
                        sb222.append(file);
                        Log.e(str, sb222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    }
                } else {
                    Typeface typeface7 = typeface;
                    file = file2;
                    try {
                        Typeface typeface8 = TypefaceCompat.createFromResourcesFontFile(context, resources, i, file, i2);
                        if (fontCallback2 != null) {
                            if (typeface8 != null) {
                                try {
                                    fontCallback2.callbackSuccessAsync(typeface8, handler2);
                                } catch (XmlPullParserException e7) {
                                    e = e7;
                                    Typeface typeface9 = typeface8;
                                    StringBuilder sb322 = new StringBuilder();
                                    sb322.append("Failed to parse xml resource ");
                                    sb322.append(file);
                                    Log.e(str, sb322.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                } catch (IOException e8) {
                                    e = e8;
                                    Typeface typeface10 = typeface8;
                                    StringBuilder sb2222 = new StringBuilder();
                                    sb2222.append("Failed to read xml resource ");
                                    sb2222.append(file);
                                    Log.e(str, sb2222.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                }
                            } else {
                                fontCallback2.callbackFailAsync(-3, handler2);
                            }
                        }
                        return typeface8;
                    } catch (XmlPullParserException e9) {
                        e = e9;
                        StringBuilder sb3222 = new StringBuilder();
                        sb3222.append("Failed to parse xml resource ");
                        sb3222.append(file);
                        Log.e(str, sb3222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    } catch (IOException e10) {
                        e = e10;
                        StringBuilder sb22222 = new StringBuilder();
                        sb22222.append("Failed to read xml resource ");
                        sb22222.append(file);
                        Log.e(str, sb22222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    }
                }
            } catch (XmlPullParserException e11) {
                e = e11;
                Context context8 = context;
                Typeface typeface11 = typeface;
                file = file2;
                StringBuilder sb32222 = new StringBuilder();
                sb32222.append("Failed to parse xml resource ");
                sb32222.append(file);
                Log.e(str, sb32222.toString(), e);
                if (fontCallback2 != null) {
                    fontCallback2.callbackFailAsync(-3, handler2);
                }
                return null;
            } catch (IOException e12) {
                e = e12;
                Context context9 = context;
                Typeface typeface12 = typeface;
                file = file2;
                StringBuilder sb222222 = new StringBuilder();
                sb222222.append("Failed to read xml resource ");
                sb222222.append(file);
                Log.e(str, sb222222.toString(), e);
                if (fontCallback2 != null) {
                }
                return null;
            }
        } else {
            Context context10 = context;
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Resource \"");
            sb4.append(resources.getResourceName(i));
            sb4.append("\" (");
            sb4.append(Integer.toHexString(id));
            sb4.append(") is not a Font: ");
            sb4.append(value);
            throw new NotFoundException(sb4.toString());
        }
    }

    private ResourcesCompat() {
    }
}
