package com.joker.floatingsubtitleapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BrandBlue,
    secondaryContainer = SuccessGreenContainerLight,
    onSecondaryContainer = SuccessGreen,
    tertiaryContainer = StopRedContainerLight,
    onTertiaryContainer = StopRed,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
    error = StopRed,
    errorContainer = StopRedContainerLight,
    onError = SurfaceLight,
    onErrorContainer = StopRed
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = TextPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BrandBlueDark,
    secondaryContainer = SuccessGreenContainerDark,
    onSecondaryContainer = SuccessGreen,
    tertiaryContainer = StopRedContainerDark,
    onTertiaryContainer = StopRed,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = StopRed,
    errorContainer = StopRedContainerDark,
    onError = TextPrimaryDark,
    onErrorContainer = StopRed
)

/**
 * 앱 전용 브랜드 테마.
 *
 * Material baseline(디폴트 퍼플)이나 Android 12+ 다이나믹 컬러를 그대로 쓰면
 * "컴포즈 샘플 앱" 느낌이 강해서, 이 앱만의 블루 브랜드 컬러를 고정으로 쓴다
 * (dynamicColor 파라미터 없음 - 의도적으로 기기 월페이퍼 색을 반영하지 않음).
 */
@Composable
fun FloatingSubtitleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FloatingSubtitleTypography,
        content = content
    )
}