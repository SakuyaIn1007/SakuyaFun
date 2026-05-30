package com.sakuya.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SakuyaPrimaryDark,
    secondary = SakuyaPrimaryDark,
    onTertiary = Color(0xFFa3a3a3),
    background = Color(0xFF1F2020),
    onBackground = Color(0xFFFFFFFF),
    outline = Color(0xFF9F9F9F),
    inverseSurface = Color(0xFF1F2020),
    inverseOnSurface = Color(0xFF32C2C2D)
)

private val LightColorScheme = lightColorScheme(
    primary = SakuyaPrimary,
    secondary = SakuyaSecondary,
    tertiary = SakuyaTertiary,
    // ☀️ 背景色：稍微带一点点暖意或者柔和感的奶灰白（比纯白柔和多啦）
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF2D3748), // 👈 文字换成深灰，绝对不用死黑，非常护眼

    // ✨ 核心对比色微调：换成充满高级感的“迷雾深海蓝”！
    // 降低纯黑度，融入冷色调，这样跟背景撞在一起时，会有一种磨砂玻璃般的柔和过渡喵！
    inverseSurface = Color(0xFF34495E),
    inverseOnSurface = Color(0xFFF7FAFC) // 反转文字也用刚才的浅灰白，绝对不刺眼

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}