package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.graphics.Typeface
import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.App

val BebasFontFamily: FontFamily by lazy {
    FontFamily(Typeface.createFromAsset(App.INSTANCE.assets, "bebas.ttf"))
}

internal val DefaultTextStyle = TextStyle.Default.copy()

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    subtitle1 = DefaultTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    button = DefaultTextStyle.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    )
)