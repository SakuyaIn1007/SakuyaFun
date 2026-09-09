package com.sakuya.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val DarkColorScheme = darkColorScheme(
    primary = SakuyaPrimaryDark,
    onPrimary = SakuyaOnPrimaryDark,
    primaryContainer = SakuyaPrimaryContainerDark,
    onPrimaryContainer = SakuyaOnPrimaryContainerDark,
    inversePrimary = SakuyaInversePrimaryDark,
    secondary = SakuyaSecondaryDark,
    onSecondary = SakuyaOnSecondaryDark,
    secondaryContainer = SakuyaSecondaryContainerDark,
    onSecondaryContainer = SakuyaOnSecondaryContainerDark,
    tertiary = SakuyaTertiaryDark,
    onTertiary = SakuyaOnTertiaryDark,
    tertiaryContainer = SakuyaTertiaryContainerDark,
    onTertiaryContainer = SakuyaOnTertiaryContainerDark,
    background = SakuyaBackgroundDark,
    onBackground = SakuyaOnBackgroundDark,
    surface = SakuyaSurfaceDark,
    onSurface = SakuyaOnSurfaceDark,
    surfaceVariant = SakuyaSurfaceVariantDark,
    onSurfaceVariant = SakuyaOnSurfaceVariantDark,
    inverseSurface = SakuyaInverseSurfaceDark,
    inverseOnSurface = SakuyaInverseOnSurfaceDark,
    error = SakuyaErrorDark,
    onError = SakuyaOnErrorDark,
    errorContainer = SakuyaErrorContainerDark,
    onErrorContainer = SakuyaOnErrorContainerDark,
    outline = SakuyaOutlineDark,
    outlineVariant = SakuyaOutlineVariantDark,
    scrim = androidx.compose.ui.graphics.Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = SakuyaPrimary,
    onPrimary = SakuyaOnPrimary,
    primaryContainer = SakuyaPrimaryContainer,
    onPrimaryContainer = SakuyaOnPrimaryContainer,
    inversePrimary = SakuyaInversePrimary,
    secondary = SakuyaSecondary,
    onSecondary = SakuyaOnSecondary,
    secondaryContainer = SakuyaSecondaryContainer,
    onSecondaryContainer = SakuyaOnSecondaryContainer,
    tertiary = SakuyaTertiary,
    onTertiary = SakuyaOnTertiary,
    tertiaryContainer = SakuyaTertiaryContainer,
    onTertiaryContainer = SakuyaOnTertiaryContainer,
    background = SakuyaBackground,
    onBackground = SakuyaOnBackground,
    surface = SakuyaSurface,
    onSurface = SakuyaOnSurface,
    surfaceVariant = SakuyaSurfaceVariant,
    onSurfaceVariant = SakuyaOnSurfaceVariant,
    inverseSurface = SakuyaInverseSurface,
    inverseOnSurface = SakuyaInverseOnSurface,
    error = SakuyaError,
    onError = SakuyaOnError,
    errorContainer = SakuyaErrorContainer,
    onErrorContainer = SakuyaOnErrorContainer,
    outline = SakuyaOutline,
    outlineVariant = SakuyaOutlineVariant,
    scrim = androidx.compose.ui.graphics.Color.Black
)

@Composable
fun SakuyaInAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
){
    val colorScheme = when{
        dynamicColor && Build.VERSION.SDK_INT >= 31 ->{
            val context = LocalContext.current
            if(darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(ProvidesSakuyaTokens provides if (darkTheme) DarkTokens else LightTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
