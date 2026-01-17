package com.whiteboard.animator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whiteboard.animator.data.model.VideoStyle

/**
 * Style selector component for choosing video visual style.
 * Displays style options as horizontal cards with preview colors.
 */
@Composable
fun StyleSelector(
    selectedStyle: VideoStyle,
    onStyleSelected: (VideoStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Video Style",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(VideoStyle.entries) { style ->
                StyleCard(
                    style = style,
                    isSelected = style == selectedStyle,
                    onClick = { onStyleSelected(style) }
                )
            }
        }
    }
}

@Composable
private fun StyleCard(
    style: VideoStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val styleInfo = getStyleInfo(style)
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Preview box with style colors
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(styleInfo.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                // Draw a simple "squiggle" to represent the style
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(styleInfo.lineColor, RoundedCornerShape(2.dp))
                )
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = styleInfo.lineColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
            
            Text(
                text = styleInfo.displayName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private data class StyleInfo(
    val displayName: String,
    val backgroundColor: Color,
    val lineColor: Color
)

private fun getStyleInfo(style: VideoStyle): StyleInfo {
    return when (style) {
        VideoStyle.WHITEBOARD -> StyleInfo(
            displayName = "Whiteboard",
            backgroundColor = Color.White,
            lineColor = Color.Black
        )
        VideoStyle.CARTOON -> StyleInfo(
            displayName = "Cartoon",
            backgroundColor = Color(0xFFFFF9C4), // Light yellow
            lineColor = Color(0xFF1565C0) // Blue
        )
        VideoStyle.REALISTIC -> StyleInfo(
            displayName = "Realistic",
            backgroundColor = Color(0xFFF5F5F5), // Light grey
            lineColor = Color(0xFF424242) // Dark grey
        )
        VideoStyle.CHALKBOARD -> StyleInfo(
            displayName = "Chalkboard",
            backgroundColor = Color(0xFF2E7D32), // Dark green
            lineColor = Color.White
        )
        VideoStyle.MINIMAL -> StyleInfo(
            displayName = "Minimal",
            backgroundColor = Color(0xFFFAFAFA), // Off-white
            lineColor = Color(0xFF757575) // Grey
        )
    }
}

/**
 * Compact style chip for inline selection.
 */
@Composable
fun StyleChip(
    style: VideoStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val styleInfo = getStyleInfo(style)
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(styleInfo.displayName) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(styleInfo.backgroundColor)
                    .then(
                        Modifier.background(
                            color = styleInfo.lineColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                    )
            )
        },
        modifier = modifier
    )
}
