package com.huanchengfly.tieba.post

import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.clearDynamicTonalPaletteCache
import com.huanchengfly.tieba.post.ui.common.theme.compose.clearThemeColorsCache
import com.huanchengfly.tieba.post.ui.common.theme.compose.getColorPalette
import com.huanchengfly.tieba.post.ui.common.theme.compose.isDynamicTonalPaletteCached
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThemeOptimizationTest {

    @Before
    fun setUp() {
        clearThemeColorsCache()
    }

    @After
    fun tearDown() {
        clearThemeColorsCache()
    }

    @Test
    fun testDynamicTonalPaletteCacheClear() {
        clearDynamicTonalPaletteCache()
        assertFalse(isDynamicTonalPaletteCached())
    }

    @Test
    fun testMaterialColorsCacheMemoization() {
        val testColors = ExtendedColors(
            theme = "test_custom_theme",
            isNightMode = false,
            primary = Color(0xFF1E88E5),
            onPrimary = Color.White,
            accent = Color(0xFFFF4081),
            onAccent = Color.White,
            background = Color(0xFFF5F5F5),
            text = Color(0xFF212121),
            card = Color.White,
        )

        // 1. First lookup builds the Material Colors
        val palette1 = getColorPalette(darkTheme = false, extendedColors = testColors)
        assertNotNull(palette1)
        assertEquals(testColors.accent, palette1.primary)
        assertEquals(testColors.primary, palette1.secondary)
        assertFalse(palette1.isLight.not()) // Light colors

        // 2. Second lookup must return the EXACT cached singleton from materialColorsCache
        val palette2 = getColorPalette(darkTheme = false, extendedColors = testColors)
        assertSame(palette1, palette2)

        // 3. Dark theme lookup
        val darkPalette1 = getColorPalette(darkTheme = true, extendedColors = testColors)
        assertNotNull(darkPalette1)
        assertTrue(darkPalette1.isLight.not()) // Dark colors

        val darkPalette2 = getColorPalette(darkTheme = true, extendedColors = testColors)
        assertSame(darkPalette1, darkPalette2)

        // 4. Cache clear invalidates
        clearThemeColorsCache()
        val paletteAfterClear = getColorPalette(darkTheme = false, extendedColors = testColors)
        assertNotNull(paletteAfterClear)
    }

    @Test
    fun testAppThemeDelegateClearCacheSafety() {
        // Verify cache clear execution runs safely across all internal stores
        AppThemeDelegate.clearCache()
        assertFalse(isDynamicTonalPaletteCached())
    }
}
