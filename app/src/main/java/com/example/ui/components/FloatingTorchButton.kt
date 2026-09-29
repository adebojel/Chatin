package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FloatingTorchButton(
    isTorchOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TorchPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTorchOn) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isTorchOn) Color(0xFFFFD600) else Color(0xFF1E293B).copy(alpha = 0.85f),
        label = "TorchBgColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isTorchOn) Color(0xFF0F172A) else Color(0xFFE2E8F0),
        label = "TorchIconColor"
    )

    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(48.dp)
            .shadow(if (isTorchOn) 12.dp else 4.dp, CircleShape)
            .scale(if (isTorchOn) pulseScale else 1f)
            .background(bgColor, CircleShape)
            .testTag("floating_torch_button")
    ) {
        Icon(
            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = if (isTorchOn) "Matikan Senter" else "Nyalakan Senter Cepat",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
