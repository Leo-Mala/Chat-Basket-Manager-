package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GameButton(
    text: String,
    icon: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = Brush.horizontalGradient(listOf(BasketOrange, BasketDarkOrange))
) {
    val shape = RoundedCornerShape(16.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = modifier
            .height(54.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, ambientColor = BasketOrange.copy(alpha = 0.3f), spotColor = BasketOrange.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = if (enabled) Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f)))
                        else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Transparent)),
                shape = shape
            )
            .clip(shape)
            .background(
                if (enabled) gradient else Brush.horizontalGradient(listOf(CourtLightSlate, CourtDeepSlate))
            ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = TextWhite,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TextGray
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = if (enabled) TextWhite else TextMuted
            )
        }
    }
}

