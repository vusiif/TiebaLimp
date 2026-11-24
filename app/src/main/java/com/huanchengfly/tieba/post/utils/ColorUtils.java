package com.huanchengfly.tieba.post.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;

public final class ColorUtils {
    public static int getDarkerColor(@ColorInt int color) {
        return getDarkerColor(color, 0.1f);
    }

    public static int getDarkerColor(@ColorInt int color, float i) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv); // convert to hsv
            // make darker
            hsv[1] = Math.min(1.0f, Math.max(0.0f, hsv[1] + i)); // more saturation
            hsv[2] = Math.min(1.0f, Math.max(0.0f, hsv[2] - i)); // less brightness
            return Color.HSVToColor(hsv);
        } catch (Throwable t) {
            int a = (color >>> 24) & 0xFF;
            int r = Math.max(0, (int) (((color >>> 16) & 0xFF) * (1.0f - i)));
            int g = Math.max(0, (int) (((color >>> 8) & 0xFF) * (1.0f - i)));
            int b = Math.max(0, (int) ((color & 0xFF) * (1.0f - i)));
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    public static int setLuminance(@ColorInt int color, @FloatRange(from = 0f, to = 1f) float luminance) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = luminance;
            return Color.HSVToColor(hsv);
        } catch (Throwable t) {
            int a = (color >>> 24) & 0xFF;
            int val = (int) (255 * Math.min(1.0f, Math.max(0.0f, luminance)));
            return (a << 24) | (val << 16) | (val << 8) | val;
        }
    }

    @ColorInt
    public static int alpha(@ColorInt int color, @IntRange(from = 0, to = 255) int alpha) {
        try {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        } catch (Throwable t) {
            return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        }
    }

    public static int getLighterColor(@ColorInt int color) {
        return getLighterColor(color, 0.1f);
    }

    public static int getLighterColor(@ColorInt int color, float i) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv); // convert to hsv
            // make lighter
            hsv[1] = Math.min(1.0f, Math.max(0.0f, hsv[1] - i)); // less saturation
            hsv[2] = Math.min(1.0f, Math.max(0.0f, hsv[2] + i)); // more brightness
            return Color.HSVToColor(hsv);
        } catch (Throwable t) {
            int a = (color >>> 24) & 0xFF;
            int r = Math.min(255, (int) (((color >>> 16) & 0xFF) * (1.0f + i)));
            int g = Math.min(255, (int) (((color >>> 8) & 0xFF) * (1.0f + i)));
            int b = Math.min(255, (int) ((color & 0xFF) * (1.0f + i)));
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    public static int greifyColor(@ColorInt int color, @FloatRange(from = 0f, to = 1f) float sat) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[1] = hsv[1] - sat;
            hsv[2] = hsv[2] - (sat / 3);
            return Color.HSVToColor(hsv);
        } catch (Throwable t) {
            int a = (color >>> 24) & 0xFF;
            int gray = (int) ((((color >>> 16) & 0xFF) * 0.299f) + (((color >>> 8) & 0xFF) * 0.587f) + ((color & 0xFF) * 0.114f));
            return (a << 24) | (gray << 16) | (gray << 8) | gray;
        }
    }
}
