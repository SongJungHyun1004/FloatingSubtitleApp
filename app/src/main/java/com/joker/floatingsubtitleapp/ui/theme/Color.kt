package com.joker.floatingsubtitleapp.ui.theme

import androidx.compose.ui.graphics.Color

// 브랜드 컬러: "소통/번역" 느낌의 confident blue.
// Material 기본 베이스라인(퍼플, #6750A4)을 그대로 쓰면 "컴포즈 튜토리얼 앱" 느낌이 나서
// 이 앱만의 색으로 교체함. 다이나믹 컬러(Material You)도 기기별로 색이 제각각이라
// 브랜드 일관성이 깨지므로 쓰지 않는다(Theme.kt에서 dynamicColor = false 고정).
val BrandBlue = Color(0xFF2E5BFF)
val BrandBlueDark = Color(0xFF9DB4FF)

val BluePrimaryContainerLight = Color(0xFFE4EAFF)
val BluePrimaryContainerDark = Color(0xFF203063)

val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF15171C)
val SurfaceVariantLight = Color(0xFFF2F4F8)
val SurfaceVariantDark = Color(0xFF1F222A)

val TextPrimaryLight = Color(0xFF1B1D22)
val TextPrimaryDark = Color(0xFFECEDEF)
val TextMutedLight = Color(0xFF6B7280)
val TextMutedDark = Color(0xFF9AA1AC)

val OutlineLight = Color(0xFFE6E8EC)
val OutlineDark = Color(0xFF2A2D34)

val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenContainerLight = Color(0xFFE3F8EA)
val SuccessGreenContainerDark = Color(0xFF123A24)

val StopRed = Color(0xFFE0453C)
val StopRedContainerLight = Color(0xFFFDE7E5)
val StopRedContainerDark = Color(0xFF4A1F1C)