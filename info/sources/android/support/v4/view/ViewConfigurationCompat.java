package android.support.v4.view;

import android.content.Context;
import android.os.Build.VERSION;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewConfiguration;
import java.lang.reflect.Method;

public final class ViewConfigurationCompat {
    private static final String TAG = "ViewConfigCompat";
    private static Method sGetScaledScrollFactorMethod;

    static {
        if (VERSION.SDK_INT == 25) {
            try {
                sGetScaledScrollFactorMethod = ViewConfiguration.class.getDeclaredMethod("getScaledScrollFactor", new Class[0]);
            } catch (Exception e) {
                Log.i(TAG, "Could not find method getScaledScrollFactor() on ViewConfiguration");
            }
        }
    }

    @Deprecated
    public static int getScaledPagingTouchSlop(ViewConfiguration config) {
        return config.getScaledPagingTouchSlop();
    }

    @Deprecated
    public static boolean hasPermanentMenuKey(ViewConfiguration config) {
        return config.hasPermanentMenuKey();
    }

    public static float getScaledHorizontalScrollFactor(ViewConfiguration config, Context context) {
        if (VERSION.SDK_INT >= 26) {
            return config.getScaledHorizontalScrollFactor();
        }
        return getLegacyScrollFactor(config, context);
    }

    public static float getScaledVerticalScrollFactor(ViewConfiguration config, Context context) {
        if (VERSION.SDK_INT >= 26) {
            return config.getScaledVerticalScrollFactor();
        }
        return getLegacyScrollFactor(config, context);
    }

    private static float getLegacyScrollFactor(ViewConfiguration config, Context context) {
        if (VERSION.SDK_INT >= 25) {
            Method method = sGetScaledScrollFactorMethod;
            if (method != null) {
                try {
                    return (float) ((Integer) method.invoke(config, new Object[0])).intValue();
                } catch (Exception e) {
                    Log.i(TAG, "Could not find method getScaledScrollFactor() on ViewConfiguration");
                }
            }
        }
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(16842829, outValue, true)) {
            return outValue.getDimension(context.getResources().getDisplayMetrics());
        }
        return 0.0f;
    }

    private ViewConfigurationCompat() {
    }
}