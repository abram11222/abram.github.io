package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BurgundyPrimary
import com.example.ui.theme.GoldSecondary
import java.io.File

@Composable
fun MemberAvatar(
    photoPath: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = 18.sp,
    shape: Shape = CircleShape,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = GoldSecondary.copy(alpha = 0.7f)
) {
    val isRemoteUrl = !photoPath.isNullOrBlank() && (photoPath.startsWith("https://") || photoPath.startsWith("http://"))
    val fileExists = remember(photoPath) {
        if (!photoPath.isNullOrBlank() && !isRemoteUrl) {
            try { File(photoPath).exists() && File(photoPath).length() > 0 } catch (_: Exception) { false }
        } else false
    }

    val initial = remember(name) {
        name.trim().firstOrNull()?.toString() ?: ""
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if ((fileExists || isRemoteUrl) && photoPath != null) {
            AsyncImage(
                model = if (isRemoteUrl) photoPath else File(photoPath),
                contentDescription = "صورة $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                BurgundyPrimary,
                                BurgundyPrimary.copy(alpha = 0.85f),
                                GoldSecondary.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }
    }
}
