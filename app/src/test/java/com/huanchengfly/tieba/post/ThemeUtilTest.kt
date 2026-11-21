package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.ThemeNameMapper
import com.huanchengfly.tieba.post.utils.ThemeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeUtilTest {
    @Test
    fun isNightMode_identifiesDarkThemes() {
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_DEFAULT))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_BLACK))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_BLUE))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_PURPLE))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_PINK))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_RED))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_CUSTOM))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_TRANSLUCENT))
        assertFalse(ThemeNameMapper.isNightMode(ThemeUtil.THEME_TRANSLUCENT_DARK))

        assertTrue(ThemeNameMapper.isNightMode(ThemeUtil.THEME_BLUE_DARK))
        assertTrue(ThemeNameMapper.isNightMode(ThemeUtil.THEME_GREY_DARK))
        assertTrue(ThemeNameMapper.isNightMode(ThemeUtil.THEME_AMOLED_DARK))
    }

    @Test
    fun isTranslucentTheme_identifiesTranslucentVariants() {
        assertTrue(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_TRANSLUCENT))
        assertTrue(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_TRANSLUCENT_LIGHT))
        assertTrue(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_TRANSLUCENT_DARK))

        assertFalse(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_DEFAULT))
        assertFalse(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_AMOLED_DARK))
        assertFalse(ThemeNameMapper.isTranslucentTheme(ThemeUtil.THEME_CUSTOM))
    }

    @Test
    fun isDynamicTheme_identifiesDynamicSuffix() {
        assertTrue(ThemeNameMapper.isDynamicTheme("tieba_dynamic"))
        assertTrue(ThemeNameMapper.isDynamicTheme("blue_dark_dynamic"))
        assertFalse(ThemeNameMapper.isDynamicTheme("tieba"))
        assertFalse(ThemeNameMapper.isDynamicTheme("dynamic_theme"))
    }

    @Test
    fun normalizeThemeName_handlesKnownAndUnknown() {
        assertEquals(ThemeUtil.THEME_DEFAULT, ThemeNameMapper.normalizeThemeName(null))
        assertEquals(ThemeUtil.THEME_DEFAULT, ThemeNameMapper.normalizeThemeName("unknown_theme"))
        assertEquals(ThemeUtil.THEME_DEFAULT, ThemeNameMapper.normalizeThemeName("TIEBA"))
        assertEquals(ThemeUtil.THEME_AMOLED_DARK, ThemeNameMapper.normalizeThemeName("AMOLED_DARK"))
        assertEquals(ThemeUtil.THEME_CUSTOM, ThemeNameMapper.normalizeThemeName("custom"))
    }

    @Test
    fun getThemeByName_mapsToValidStyles() {
        assertEquals(R.style.TiebaLite_Tieba, ThemeNameMapper.getThemeByName(ThemeUtil.THEME_DEFAULT))
        assertEquals(R.style.TiebaLite_Dark_Amoled, ThemeNameMapper.getThemeByName(ThemeUtil.THEME_AMOLED_DARK))
        assertEquals(R.style.TiebaLite_Translucent_Light, ThemeNameMapper.getThemeByName(ThemeUtil.THEME_TRANSLUCENT))
        assertEquals(R.style.TiebaLite_Translucent_Dark, ThemeNameMapper.getThemeByName(ThemeUtil.THEME_TRANSLUCENT_DARK))
        assertEquals(R.style.TiebaLite_Custom, ThemeNameMapper.getThemeByName(ThemeUtil.THEME_CUSTOM))
    }
}
