package com.joker.floatingsubtitleapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 외부 폰트 파일을 번들하지 않고(용량/권한 이슈 회피) 시스템 기본 산세리프(Roboto 계열)를
// 쓰되, weight·letterSpacing을 용도별로 명확히 구분해서 "기본값을 그대로 쓴 느낌"을 없앤다.
val FloatingSubtitleTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.1.sp
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    )
}