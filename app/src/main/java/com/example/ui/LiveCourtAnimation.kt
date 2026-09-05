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
import com.example.domain.rules.LiveCourtPlayPlanner
import com.example.domain.rules.LiveCourtPlayStyle
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
 * state. The match engine still decides every point; this component only stages the already-known
 * next scoring event as a richer visual possession. Because elapsedMillis stops while paused, the
 * entire court freezes with the match clock.
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
    nextScorePoints: Int,
    nextScoreElapsedMillis: Long,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val possessionName = if (possessionSide == LiveScoringSide.USER) userTeamName else opponentName
    val lastScoreName = when (lastScoreSide) {
        LiveScoringSide.USER -> userTeamName
        LiveScoringSide.OPPONENT -> opponentName
        null -> null
    }
    val playPlan = if (nextScorePoints in 1..3 && nextScoreElapsedMillis >= 0L) {
        LiveCourtPlayPlanner.plan(
            points = nextScorePoints,
            side = possessionSide,
            quarter = currentQuarter,
            eventElapsedMillis = nextScoreElapsedMillis
        )
    } else {
        null
    }
    val playLabel = when (playPlan?.style) {
        LiveCourtPlayStyle.FREE_THROW -> "Lance livre"
        LiveCourtPlayStyle.DRIVE -> "Infiltração"
        LiveCourtPlayStyle.CUT -> "Corte para a cesta"
        LiveCourtPlayStyle.PICK_AND_ROLL -> "Pick-and-roll"
        LiveCourtPlayStyle.POST_UP -> "Jogo no poste"
        LiveCourtPlayStyle.MID_RANGE -> "Média distância"
        LiveCourtPlayStyle.CORNER_THREE,
        LiveCourtPlayStyle.WING_THREE,
        LiveCourtPlayStyle.TOP_THREE -> "Movimentação no perímetro"
        null -> "Posse em movimento"
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

                drawRoundRect(color = wood, cornerRadius = CornerRadius(12f, 12f))
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

                val phase = ((elapsedMillis % 2_000L).toFloat() / 2_000f) * (2f * PI.toFloat())
                val userAttacksRight = currentQuarter % 2 == 1
                val offenseSide = possessionSide
                val offenseAttacksRight = if (offenseSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight

                fun smooth(value: Float): Float {
                    val t = value.coerceIn(0f, 1f)
                    return t * t * (3f - 2f * t)
                }

                fun lerp(a: Offset, b: Offset, progress: Float): Offset {
                    val t = progress.coerceIn(0f, 1f)
                    return Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                }

                fun courtPoint(normalized: Offset): Offset {
                    val normalizedX = if (offenseAttacksRight) normalized.x else 1f - normalized.x
                    return Offset(w * normalizedX, h * normalized.y)
                }

                val possessionStartMillis = if (lastScoreElapsedMillis >= 0L) lastScoreElapsedMillis else 0L
                val possessionDuration = nextScoreElapsedMillis - possessionStartMillis
                val playProgress = if (playPlan != null && possessionDuration > 0L) {
                    ((elapsedMillis - possessionStartMillis).toFloat() / possessionDuration.toFloat()).coerceIn(0f, 1f)
                } else {
                    ((elapsedMillis % 1_800L).toFloat() / 1_800f).coerceIn(0f, 1f)
                }

                val baseOffense = listOf(
                    Offset(0.55f, 0.50f),
                    Offset(0.64f, 0.24f),
                    Offset(0.64f, 0.76f),
                    Offset(0.76f, 0.14f),
                    Offset(0.76f, 0.86f)
                )

                val targetOffense = when (playPlan?.style) {
                    LiveCourtPlayStyle.FREE_THROW -> listOf(
                        Offset(0.835f, 0.50f),
                        Offset(0.77f, 0.37f),
                        Offset(0.77f, 0.63f),
                        Offset(0.73f, 0.43f),
                        Offset(0.73f, 0.57f)
                    )
                    LiveCourtPlayStyle.DRIVE -> listOf(
                        Offset(0.885f, 0.50f),
                        Offset(0.68f, 0.20f),
                        Offset(0.68f, 0.80f),
                        Offset(0.78f, 0.13f),
                        Offset(0.78f, 0.87f)
                    )
                    LiveCourtPlayStyle.CUT -> listOf(
                        Offset(0.62f, 0.50f),
                        Offset(0.885f, 0.43f),
                        Offset(0.66f, 0.75f),
                        Offset(0.78f, 0.15f),
                        Offset(0.76f, 0.84f)
                    )
                    LiveCourtPlayStyle.PICK_AND_ROLL -> listOf(
                        Offset(0.69f, 0.39f),
                        Offset(0.67f, 0.20f),
                        Offset(0.66f, 0.79f),
                        Offset(0.78f, 0.14f),
                        Offset(0.885f, 0.56f)
                    )
                    LiveCourtPlayStyle.POST_UP -> listOf(
                        Offset(0.61f, 0.50f),
                        Offset(0.67f, 0.22f),
                        Offset(0.67f, 0.78f),
                        Offset(0.78f, 0.14f),
                        Offset(0.845f, 0.61f)
                    )
                    LiveCourtPlayStyle.MID_RANGE -> listOf(
                        Offset(0.61f, 0.50f),
                        Offset(0.68f, 0.23f),
                        Offset(0.79f, 0.64f),
                        Offset(0.78f, 0.15f),
                        Offset(0.75f, 0.83f)
                    )
                    LiveCourtPlayStyle.CORNER_THREE -> listOf(
                        Offset(0.61f, 0.50f),
                        Offset(0.68f, 0.28f),
                        Offset(0.68f, 0.73f),
                        Offset(0.80f, 0.10f),
                        Offset(0.77f, 0.86f)
                    )
                    LiveCourtPlayStyle.WING_THREE -> listOf(
                        Offset(0.61f, 0.50f),
                        Offset(0.73f, 0.27f),
                        Offset(0.67f, 0.75f),
                        Offset(0.79f, 0.12f),
                        Offset(0.76f, 0.85f)
                    )
                    LiveCourtPlayStyle.TOP_THREE -> listOf(
                        Offset(0.69f, 0.50f),
                        Offset(0.67f, 0.24f),
                        Offset(0.67f, 0.76f),
                        Offset(0.79f, 0.13f),
                        Offset(0.79f, 0.87f)
                    )
                    null -> baseOffense
                }

                fun stagedPlayerProgress(index: Int): Float {
                    return when (playPlan?.style) {
                        LiveCourtPlayStyle.CUT -> if (index == 1) smooth((playProgress - 0.25f) / 0.55f) else smooth(playProgress)
                        LiveCourtPlayStyle.PICK_AND_ROLL -> if (index == 4) smooth((playProgress - 0.22f) / 0.58f) else smooth(playProgress)
                        LiveCourtPlayStyle.POST_UP -> if (index == 4) smooth((playProgress - 0.28f) / 0.48f) else smooth(playProgress)
                        LiveCourtPlayStyle.MID_RANGE -> if (index == 2) smooth((playProgress - 0.24f) / 0.50f) else smooth(playProgress)
                        LiveCourtPlayStyle.FREE_THROW -> smooth(playProgress * 2f)
                        else -> smooth(playProgress)
                    }
                }

                val offenseNormalized = baseOffense.mapIndexed { index, start ->
                    val moved = lerp(start, targetOffense[index], stagedPlayerProgress(index))
                    val movementScale = if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) 0f else 1f
                    Offset(
                        (moved.x + sin(phase + index * 0.9f) * 0.0045f * movementScale).coerceIn(0.10f, 0.91f),
                        (moved.y + sin(phase * 1.15f + index * 1.2f) * 0.008f * movementScale).coerceIn(0.08f, 0.92f)
                    )
                }

                val defenseNormalized = if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) {
                    listOf(
                        Offset(0.79f, 0.32f),
                        Offset(0.79f, 0.68f),
                        Offset(0.74f, 0.38f),
                        Offset(0.74f, 0.62f),
                        Offset(0.60f, 0.50f)
                    )
                } else {
                    offenseNormalized.mapIndexed { index, offense ->
                        var x = (offense.x + 0.035f).coerceAtMost(0.90f)
                        var y = (offense.y + if (index % 2 == 0) 0.018f else -0.018f).coerceIn(0.08f, 0.92f)
                        val collapsing = playPlan?.style in setOf(
                            LiveCourtPlayStyle.DRIVE,
                            LiveCourtPlayStyle.CUT,
                            LiveCourtPlayStyle.PICK_AND_ROLL,
                            LiveCourtPlayStyle.POST_UP
                        ) && playProgress > 0.52f && index < 3
                        if (collapsing) {
                            val collapse = smooth((playProgress - 0.52f) / 0.34f)
                            val collapseTarget = Offset(0.84f, 0.41f + index * 0.09f)
                            val collapsed = lerp(Offset(x, y), collapseTarget, collapse)
                            x = collapsed.x
                            y = collapsed.y
                        }
                        Offset(x, y)
                    }
                }

                val offensePositions = offenseNormalized.map(::courtPoint)
                val defensePositions = defenseNormalized.map(::courtPoint)
                val offenseColor = if (offenseSide == LiveScoringSide.USER) BasketOrange else ElectricCyan
                val defenseColor = if (offenseSide == LiveScoringSide.USER) ElectricCyan else BasketOrange

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

                fun bouncedAt(index: Int): Offset {
                    val player = offensePositions[index]
                    val bounce = sin(phase * 2.5f) * markerRadius * 0.45f
                    return Offset(player.x + markerRadius * 1.15f, player.y + bounce)
                }

                fun pass(from: Int, to: Int, localProgress: Float): Offset =
                    lerp(offensePositions[from], offensePositions[to], smooth(localProgress))

                val hoop = if (offenseAttacksRight) rightHoop else leftHoop
                val shooterIndex = when (playPlan?.style) {
                    LiveCourtPlayStyle.FREE_THROW,
                    LiveCourtPlayStyle.DRIVE,
                    LiveCourtPlayStyle.TOP_THREE -> 0
                    LiveCourtPlayStyle.CUT,
                    LiveCourtPlayStyle.WING_THREE -> 1
                    LiveCourtPlayStyle.MID_RANGE -> 2
                    LiveCourtPlayStyle.CORNER_THREE -> 3
                    LiveCourtPlayStyle.PICK_AND_ROLL,
                    LiveCourtPlayStyle.POST_UP -> 4
                    null -> 0
                }

                fun shot(localProgress: Float): Offset {
                    val start = offensePositions[shooterIndex]
                    val t = smooth(localProgress)
                    val arcLift = sin(t * PI.toFloat()) * h * when (playPlan?.style) {
                        LiveCourtPlayStyle.DRIVE,
                        LiveCourtPlayStyle.CUT,
                        LiveCourtPlayStyle.PICK_AND_ROLL,
                        LiveCourtPlayStyle.POST_UP -> 0.075f
                        LiveCourtPlayStyle.FREE_THROW,
                        LiveCourtPlayStyle.MID_RANGE -> 0.12f
                        else -> 0.16f
                    }
                    return Offset(
                        x = start.x + (hoop.x - start.x) * t,
                        y = start.y + (hoop.y - start.y) * t - arcLift
                    )
                }

                val ballPosition = when (playPlan?.style) {
                    LiveCourtPlayStyle.FREE_THROW -> when {
                        playProgress < 0.72f -> offensePositions[0] + Offset(markerRadius * 0.9f, 0f)
                        else -> shot((playProgress - 0.72f) / 0.28f)
                    }
                    LiveCourtPlayStyle.DRIVE -> if (playPlan.passCount == 0) {
                        if (playProgress < 0.82f) bouncedAt(0) else shot((playProgress - 0.82f) / 0.18f)
                    } else {
                        when {
                            playProgress < 0.18f -> bouncedAt(0)
                            playProgress < 0.30f -> pass(0, 1, (playProgress - 0.18f) / 0.12f)
                            playProgress < 0.42f -> bouncedAt(1)
                            playProgress < 0.56f -> pass(1, 0, (playProgress - 0.42f) / 0.14f)
                            playProgress < 0.82f -> bouncedAt(0)
                            else -> shot((playProgress - 0.82f) / 0.18f)
                        }
                    }
                    LiveCourtPlayStyle.CUT -> if (playPlan.passCount <= 1) {
                        when {
                            playProgress < 0.42f -> bouncedAt(0)
                            playProgress < 0.64f -> pass(0, 1, (playProgress - 0.42f) / 0.22f)
                            playProgress < 0.84f -> bouncedAt(1)
                            else -> shot((playProgress - 0.84f) / 0.16f)
                        }
                    } else {
                        when {
                            playProgress < 0.24f -> bouncedAt(0)
                            playProgress < 0.38f -> pass(0, 2, (playProgress - 0.24f) / 0.14f)
                            playProgress < 0.55f -> bouncedAt(2)
                            playProgress < 0.70f -> pass(2, 1, (playProgress - 0.55f) / 0.15f)
                            playProgress < 0.84f -> bouncedAt(1)
                            else -> shot((playProgress - 0.84f) / 0.16f)
                        }
                    }
                    LiveCourtPlayStyle.PICK_AND_ROLL -> when {
                        playProgress < 0.55f -> bouncedAt(0)
                        playProgress < 0.72f -> pass(0, 4, (playProgress - 0.55f) / 0.17f)
                        playProgress < 0.84f -> bouncedAt(4)
                        else -> shot((playProgress - 0.84f) / 0.16f)
                    }
                    LiveCourtPlayStyle.POST_UP -> when {
                        playProgress < 0.30f -> bouncedAt(0)
                        playProgress < 0.48f -> pass(0, 4, (playProgress - 0.30f) / 0.18f)
                        playProgress < 0.83f -> bouncedAt(4)
                        else -> shot((playProgress - 0.83f) / 0.17f)
                    }
                    LiveCourtPlayStyle.MID_RANGE -> when {
                        playProgress < 0.25f -> bouncedAt(0)
                        playProgress < 0.40f -> pass(0, 1, (playProgress - 0.25f) / 0.15f)
                        playProgress < 0.56f -> bouncedAt(1)
                        playProgress < 0.70f -> pass(1, 2, (playProgress - 0.56f) / 0.14f)
                        playProgress < 0.82f -> bouncedAt(2)
                        else -> shot((playProgress - 0.82f) / 0.18f)
                    }
                    LiveCourtPlayStyle.CORNER_THREE -> when {
                        playProgress < 0.20f -> bouncedAt(0)
                        playProgress < 0.34f -> pass(0, 2, (playProgress - 0.20f) / 0.14f)
                        playProgress < 0.50f -> bouncedAt(2)
                        playProgress < 0.66f -> pass(2, 3, (playProgress - 0.50f) / 0.16f)
                        playProgress < 0.81f -> bouncedAt(3)
                        else -> shot((playProgress - 0.81f) / 0.19f)
                    }
                    LiveCourtPlayStyle.WING_THREE -> when {
                        playProgress < 0.20f -> bouncedAt(0)
                        playProgress < 0.34f -> pass(0, 2, (playProgress - 0.20f) / 0.14f)
                        playProgress < 0.50f -> bouncedAt(2)
                        playProgress < 0.66f -> pass(2, 1, (playProgress - 0.50f) / 0.16f)
                        playProgress < 0.81f -> bouncedAt(1)
                        else -> shot((playProgress - 0.81f) / 0.19f)
                    }
                    LiveCourtPlayStyle.TOP_THREE -> when {
                        playProgress < 0.18f -> bouncedAt(0)
                        playProgress < 0.32f -> pass(0, 1, (playProgress - 0.18f) / 0.14f)
                        playProgress < 0.48f -> bouncedAt(1)
                        playProgress < 0.64f -> pass(1, 0, (playProgress - 0.48f) / 0.16f)
                        playProgress < 0.81f -> bouncedAt(0)
                        else -> shot((playProgress - 0.81f) / 0.19f)
                    }
                    null -> bouncedAt(0)
                }

                drawCircle(Color.Black.copy(alpha = 0.30f), ballRadius + 2f, ballPosition + Offset(1.5f, 2f))
                drawCircle(BasketOrange, ballRadius, ballPosition)
                drawLine(Color(0xFF4A2412), ballPosition + Offset(-ballRadius, 0f), ballPosition + Offset(ballRadius, 0f), strokeWidth = 1.5f)
                drawLine(Color(0xFF4A2412), ballPosition + Offset(0f, -ballRadius), ballPosition + Offset(0f, ballRadius), strokeWidth = 1.5f)

                val scoreAge = if (lastScoreElapsedMillis >= 0L && elapsedMillis >= lastScoreElapsedMillis) {
                    elapsedMillis - lastScoreElapsedMillis
                } else {
                    Long.MAX_VALUE
                }
                if (lastScoreSide != null && scoreAge <= 450L) {
                    val scoringAttacksRight = if (lastScoreSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight
                    val scoredHoop = if (scoringAttacksRight) rightHoop else leftHoop
                    val pulse = 1f + (1f - (scoreAge.toFloat() / 450f).coerceIn(0f, 1f)) * 0.9f
                    drawCircle(
                        ChampionshipGold.copy(alpha = 0.8f),
                        markerRadius * 1.4f * pulse,
                        scoredHoop,
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
                    playLabel
                } else {
                    "$playLabel • último +$lastScorePoints"
                },
                color = if (playPlan == null) TextMuted else ChampionshipGold,
                fontSize = 9.sp,
                fontWeight = if (playPlan == null) FontWeight.Normal else FontWeight.Bold,
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
