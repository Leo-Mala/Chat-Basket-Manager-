package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.rules.LiveScoringSide
import com.example.ui.theme.BasketOrange
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CourtBorder
import com.example.ui.theme.CourtDeepSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure presentation layer for the live match. It never creates scoring events or changes game
 * state: positions are derived only from the active quarter clock and the already-generated
 * scoring timeline. Because elapsedMillis stops while paused, the court freezes as well.
 */
@Composable
fun LiveCourtAnimation(
    userTeamName: String,
    opponentName: String,
    currentQuarter: Int,
    elapsedMillis: Long,
    possessionSide: LiveScoringSide,
    lastScoreSide: LiveScoringSide?,
    lastScorePoints: Int,
    lastScoreElapsedMillis: Long,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val possessionName = if (possessionSide == LiveScoringSide.USER) userTeamName else opponentName
    val lastScoreName = when (lastScoreSide) {
        LiveScoringSide.USER -> userTeamName
        LiveScoringSide.OPPONENT -> opponentName
        null -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CourtDeepSlate)
            .border(1.dp, CourtBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (possessionSide == LiveScoringSide.USER) BasketOrange else ElectricCyan)
                )
                Text(
                    text = "POSSE: $possessionName",
                    color = TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isPaused) "⏸ PAUSADO" else "Q$currentQuarter • AO VIVO",
                color = if (isPaused) ChampionshipGold else ElectricCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color(0xFFFFE0B2)
                val wood = Color(0xFFB7773F)
                val paint = Color(0xFFC9894C)
                val markerRadius = minOf(w, h) * 0.032f
                val ballRadius = markerRadius * 0.52f

                drawRoundRect(
                    color = wood,
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = lineColor,
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 3f)
                )

                val midX = w / 2f
                drawLine(lineColor, Offset(midX, 0f), Offset(midX, h), strokeWidth = 2.5f)
                drawCircle(lineColor, radius = h * 0.14f, center = Offset(midX, h / 2f), style = Stroke(width = 2.5f))
                drawCircle(lineColor, radius = h * 0.025f, center = Offset(midX, h / 2f), style = Stroke(width = 2f))

                val keyWidth = w * 0.16f
                val keyHeight = h * 0.40f
                val keyTop = (h - keyHeight) / 2f
                drawRect(paint, topLeft = Offset(0f, keyTop), size = Size(keyWidth, keyHeight))
                drawRect(lineColor, topLeft = Offset(0f, keyTop), size = Size(keyWidth, keyHeight), style = Stroke(width = 2.5f))
                drawRect(paint, topLeft = Offset(w - keyWidth, keyTop), size = Size(keyWidth, keyHeight))
                drawRect(lineColor, topLeft = Offset(w - keyWidth, keyTop), size = Size(keyWidth, keyHeight), style = Stroke(width = 2.5f))

                val freeThrowRadius = h * 0.12f
                drawCircle(lineColor, freeThrowRadius, Offset(keyWidth, h / 2f), style = Stroke(width = 2.5f))
                drawCircle(lineColor, freeThrowRadius, Offset(w - keyWidth, h / 2f), style = Stroke(width = 2.5f))

                val leftHoop = Offset(w * 0.075f, h / 2f)
                val rightHoop = Offset(w * 0.925f, h / 2f)
                drawLine(lineColor, Offset(w * 0.045f, h * 0.42f), Offset(w * 0.045f, h * 0.58f), strokeWidth = 4f)
                drawLine(lineColor, Offset(w * 0.955f, h * 0.42f), Offset(w * 0.955f, h * 0.58f), strokeWidth = 4f)
                drawCircle(BasketOrange, markerRadius * 0.55f, leftHoop, style = Stroke(width = 4f))
                drawCircle(BasketOrange, markerRadius * 0.55f, rightHoop, style = Stroke(width = 4f))

                drawArc(
                    color = lineColor,
                    startAngle = -65f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(-w * 0.08f, h * 0.08f),
                    size = Size(w * 0.37f, h * 0.84f),
                    style = Stroke(width = 2.5f)
                )
                drawArc(
                    color = lineColor,
                    startAngle = 115f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.71f, h * 0.08f),
                    size = Size(w * 0.37f, h * 0.84f),
                    style = Stroke(width = 2.5f)
                )

                val phase = ((elapsedMillis % 4_000L).toFloat() / 4_000f) * (2f * PI.toFloat())
                val userAttacksRight = currentQuarter % 2 == 1
                val offenseSide = possessionSide
                val offenseAttacksRight = if (offenseSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight

                fun mirroredX(x: Float, attacksRight: Boolean): Float = if (attacksRight) x else 1f - x

                fun offensePoint(index: Int, attacksRight: Boolean): Offset {
                    val baseX = floatArrayOf(0.39f, 0.47f, 0.56f, 0.63f, 0.70f)[index]
                    val baseY = floatArrayOf(0.22f, 0.76f, 0.47f, 0.31f, 0.65f)[index]
                    val dx = sin(phase + index * 0.83f) * 0.025f
                    val dy = sin(phase * 1.35f + index * 1.21f) * 0.035f
                    return Offset(
                        w * mirroredX((baseX + dx).coerceIn(0.12f, 0.88f), attacksRight),
                        h * (baseY + dy).coerceIn(0.10f, 0.90f)
                    )
                }

                fun defensePoint(index: Int, attacksRight: Boolean): Offset {
                    val baseX = floatArrayOf(0.55f, 0.61f, 0.66f, 0.72f, 0.68f)[index]
                    val baseY = floatArrayOf(0.25f, 0.73f, 0.45f, 0.34f, 0.62f)[index]
                    val dx = sin(phase + index * 1.07f + 0.7f) * 0.018f
                    val dy = sin(phase * 1.2f + index * 0.93f + 0.5f) * 0.028f
                    return Offset(
                        w * mirroredX((baseX + dx).coerceIn(0.12f, 0.88f), attacksRight),
                        h * (baseY + dy).coerceIn(0.10f, 0.90f)
                    )
                }

                val offenseColor = if (offenseSide == LiveScoringSide.USER) BasketOrange else ElectricCyan
                val defenseColor = if (offenseSide == LiveScoringSide.USER) ElectricCyan else BasketOrange
                val offensePositions = List(5) { offensePoint(it, offenseAttacksRight) }
                val defensePositions = List(5) { defensePoint(it, offenseAttacksRight) }

                defensePositions.forEach { point ->
                    drawCircle(Color.Black.copy(alpha = 0.28f), markerRadius + 3f, point + Offset(2f, 3f))
                    drawCircle(defenseColor, markerRadius, point)
                    drawCircle(Color.White.copy(alpha = 0.75f), markerRadius, point, style = Stroke(width = 2f))
                }
                offensePositions.forEachIndexed { index, point ->
                    drawCircle(Color.Black.copy(alpha = 0.28f), markerRadius + 3f, point + Offset(2f, 3f))
                    drawCircle(offenseColor, markerRadius, point)
                    drawCircle(
                        if (index == 0) ChampionshipGold else Color.White.copy(alpha = 0.75f),
                        markerRadius,
                        point,
                        style = Stroke(width = if (index == 0) 3.5f else 2f)
                    )
                }

                val scoreAge = if (lastScoreElapsedMillis >= 0L && elapsedMillis >= lastScoreElapsedMillis) {
                    elapsedMillis - lastScoreElapsedMillis
                } else {
                    Long.MAX_VALUE
                }
                val shotActive = lastScoreSide != null && scoreAge <= 900L
                val ballPosition = if (shotActive) {
                    val scorerAttacksRight = if (lastScoreSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight
                    val shotStart = if (lastScoreSide == offenseSide) offensePositions.first() else {
                        offensePoint(0, scorerAttacksRight)
                    }
                    val hoop = if (scorerAttacksRight) rightHoop else leftHoop
                    val progress = (scoreAge.toFloat() / 900f).coerceIn(0f, 1f)
                    val arcLift = sin(progress * PI.toFloat()) * h * 0.16f
                    Offset(
                        x = shotStart.x + (hoop.x - shotStart.x) * progress,
                        y = shotStart.y + (hoop.y - shotStart.y) * progress - arcLift
                    )
                } else {
                    val handler = offensePositions.first()
                    val bounce = sin(phase * 2.2f) * markerRadius * 0.55f
                    Offset(handler.x + markerRadius * 1.25f, handler.y + bounce)
                }

                drawCircle(Color.Black.copy(alpha = 0.30f), ballRadius + 2f, ballPosition + Offset(1.5f, 2f))
                drawCircle(BasketOrange, ballRadius, ballPosition)
                drawLine(Color(0xFF4A2412), ballPosition + Offset(-ballRadius, 0f), ballPosition + Offset(ballRadius, 0f), strokeWidth = 1.5f)
                drawLine(Color(0xFF4A2412), ballPosition + Offset(0f, -ballRadius), ballPosition + Offset(0f, ballRadius), strokeWidth = 1.5f)

                if (shotActive) {
                    val scoringAttacksRight = if (lastScoreSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight
                    val hoop = if (scoringAttacksRight) rightHoop else leftHoop
                    val pulse = 1f + (1f - (scoreAge.toFloat() / 900f).coerceIn(0f, 1f)) * 0.9f
                    drawCircle(
                        ChampionshipGold.copy(alpha = 0.8f),
                        markerRadius * 1.4f * pulse,
                        hoop,
                        style = Stroke(width = 4f)
                    )
                }
            }

            if (isPaused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏸ JOGO PAUSADO",
                        color = ChampionshipGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(BasketOrange))
                Text(userTeamName, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = if (lastScoreName == null || lastScorePoints <= 0) {
                    "A posse está em movimento"
                } else {
                    "Último lance: +$lastScorePoints • $lastScoreName"
                },
                color = if (lastScoreName == null) TextMuted else ChampionshipGold,
                fontSize = 9.sp,
                fontWeight = if (lastScoreName == null) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(opponentName, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.size(7.dp).clip(CircleShape).background(ElectricCyan))
            }
        }
    }
}
