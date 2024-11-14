package com.simplified.wsstatussaver.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.simplified.wsstatussaver.R

val manropeFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Manrope"),
        fontProvider = GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        ),
    )
)

// Default Material 3 typography values
val baseline = Typography()

val WhatSaveTypografy = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = manropeFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = manropeFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = manropeFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = manropeFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = manropeFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = manropeFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = manropeFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = manropeFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = manropeFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = manropeFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = manropeFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = manropeFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = manropeFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = manropeFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = manropeFontFamily),
)

