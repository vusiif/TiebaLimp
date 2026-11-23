package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.DisplayModeUtil
import com.huanchengfly.tieba.post.utils.ThemeNameMapper
import com.huanchengfly.tieba.post.utils.ThemeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayModeUtilTest {

    @Test
    fun testDisplayModeUtilNullSafety() {
        val refreshRate = DisplayModeUtil.getDisplayRefreshRate(null)
        assertEquals(60.0f, refreshRate, 0.01f)

        // Null activity should safely return without throwing any exception
        DisplayModeUtil.adaptHighRefreshRate(null)
    }

    @Test
    fun testThemeNameMapperNightModeDetection() {
        assertTrue(ThemeNameMapper.isNightMode("amoled_dark"))
        assertTrue(ThemeNameMapper.isNightMode("grey_dark"))
        assertTrue(ThemeNameMapper.isNightMode("blue_dark"))
        assertFalse(ThemeNameMapper.isNightMode("tieba"))
        assertFalse(ThemeNameMapper.isNightMode("pink"))
        assertFalse(ThemeNameMapper.isNightMode("translucent"))
    }

    @Test
    fun testThemeNameMapperTranslucentDetection() {
        assertTrue(ThemeNameMapper.isTranslucentTheme("translucent"))
        assertTrue(ThemeNameMapper.isTranslucentTheme("translucent_light"))
        assertTrue(ThemeNameMapper.isTranslucentTheme("translucent_dark"))
        assertFalse(ThemeNameMapper.isTranslucentTheme("tieba"))
        assertFalse(ThemeNameMapper.isTranslucentTheme("amoled_dark"))
    }

    @Test
    fun testThemeNameMapperDynamicThemeDetection() {
        assertTrue(ThemeNameMapper.isDynamicTheme("tieba_dynamic"))
        assertTrue(ThemeNameMapper.isDynamicTheme("amoled_dark_dynamic"))
        assertFalse(ThemeNameMapper.isDynamicTheme("tieba"))
        assertFalse(ThemeNameMapper.isDynamicTheme("amoled_dark"))
    }

    @Test
    fun testThemeNameMapperNormalization() {
        assertEquals(ThemeUtil.THEME_DEFAULT, ThemeNameMapper.normalizeThemeName(null))
        assertEquals(ThemeUtil.THEME_DEFAULT, ThemeNameMapper.normalizeThemeName("invalid_theme_xyz"))
        assertEquals(ThemeUtil.THEME_AMOLED_DARK, ThemeNameMapper.normalizeThemeName("amoled_dark"))
        assertEquals(ThemeUtil.THEME_TRANSLUCENT, ThemeNameMapper.normalizeThemeName("TRANSLUCENT"))
    }
}
