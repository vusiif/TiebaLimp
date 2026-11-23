package com.huanchengfly.tieba.post.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public class DisplayUtil {
    /**
     * 将px值转换为dp值
     */
    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 将dp值转换为px值
     */
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 将px值转换为sp值
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将sp值转换为px值
     */
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 获取屏幕宽度
     */
    @SuppressWarnings("deprecation")
    public static int getScreenWidthPixels(Activity context) {
        if (context == null) return 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                android.view.WindowMetrics metrics = context.getWindowManager().getCurrentWindowMetrics();
                return metrics.getBounds().width();
            } catch (Throwable ignored) {
            }
        }
        DisplayMetrics metric = new DisplayMetrics();
        try {
            context.getWindowManager().getDefaultDisplay().getMetrics(metric);
            return metric.widthPixels;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * 获取屏幕高度
     */
    @SuppressWarnings("deprecation")
    public static int getScreenHeightPixels(Activity context) {
        if (context == null) return 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                android.view.WindowMetrics metrics = context.getWindowManager().getCurrentWindowMetrics();
                return metrics.getBounds().height();
            } catch (Throwable ignored) {
            }
        }
        DisplayMetrics metric = new DisplayMetrics();
        try {
            context.getWindowManager().getDefaultDisplay().getMetrics(metric);
            return metric.heightPixels;
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
