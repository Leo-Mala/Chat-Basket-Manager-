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
import com.example.domain.rules.LiveCourtOrganicMotion
import com.example.domain.rules.LiveCourtPlayPlanner
import com.example.domain.rules.LiveCourtPlayStyle
import com.example.domain.rules.LiveCourtPossessionPhase
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
import kotlin.math.sqrt

/**
 * Presentation-only live court. The match engine still decides every score. This component only
 * visualizes the already-scheduled possession and therefore never changes score, stats, tactics,
 * lineups or saved state. The elapsed match clock also drives the animation, so pause freezes it.
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

    val possessionStartMillis = if (lastScoreElapsedMillis >= 0L) lastScoreElapsedMillis else 0L
    val possessionDuration = nextScoreElapsedMillis - possessionStartMillis
    val playProgress = if (playPlan != null && possessionDuration > 0L) {
        ((elapsedMillis - possessionStartMillis).toFloat() / possessionDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val possessionFlow = if (playPlan != null && possessionDuration > 0L) {
        LiveCourtPlayPlanner.flow(
            progress = playProgress,
            possessionDurationMillis = possessionDuration,
            plannedPassCount = playPlan.passCount
        )
    } else {
        null
    }
    val phaseLabel = when (possessionFlow?.phase) {
        LiveCourtPossessionPhase.TRANSITION -> "Transição"
        LiveCourtPossessionPhase.SETUP -> "Organizando • $playLabel"
        LiveCourtPossessionPhase.ACTION,
        LiveCourtPossessionPhase.FINISH -> playLabel
        null -> playLabel
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

                val userAttacksRight = currentQuarter % 2 == 1
                val offenseSide = possessionSide
                val offenseAttacksRight = if (offenseSide == LiveScoringSide.USER) userAttacksRight else !userAttacksRight
                val eventSeed = nextScoreElapsedMillis.coerceAtLeast(0L) + currentQuarter * 1_003L

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

                fun organicTarget(base: Offset, index: Int): Offset {
                    val rawX = (((eventSeed + index * 37L) % 9L).toFloat() - 4f) * 0.0024f
                    val rawY = (((eventSeed / 3L + index * 53L) % 11L).toFloat() - 5f) * 0.0032f
                    return Offset(
                        (base.x + rawX).coerceIn(0.10f, 0.90f),
                        (base.y + rawY).coerceIn(0.07f, 0.93f)
                    )
                }

                val sameSideRepeat = lastScoreSide != null && lastScoreSide == possessionSide
                val baseOffense = listOf(
                    Offset(0.55f, 0.50f),
                    Offset(0.62f, 0.20f),
                    Offset(0.62f, 0.80f),
                    Offset(0.74f, 0.09f),
                    Offset(0.74f, 0.91f)
                ).mapIndexed { index, point -> organicTarget(point, index) }

                val backcourtTransition = if (sameSideRepeat) {
                    listOf(
                        Offset(0.40f, 0.50f),
                        Offset(0.46f, 0.23f),
                        Offset(0.46f, 0.77f),
                        Offset(0.52f, 0.12f),
                        Offset(0.52f, 0.88f)
                    )
                } else {
                    listOf(
                        Offset(0.23f, 0.50f),
                        Offset(0.30f, 0.23f),
                        Offset(0.30f, 0.77f),
                        Offset(0.39f, 0.12f),
                        Offset(0.39f, 0.88f)
                    )
                }.mapIndexed { index, point -> organicTarget(point, index) }

                val rawTargetOffense = when (playPlan?.style) {
                    LiveCourtPlayStyle.FREE_THROW -> listOf(
                        Offset(0.835f, 0.50f), Offset(0.79f, 0.32f), Offset(0.79f, 0.68f), Offset(0.72f, 0.39f), Offset(0.72f, 0.61f)
                    )
                    LiveCourtPlayStyle.DRIVE -> listOf(
                        Offset(0.885f, 0.50f), Offset(0.63f, 0.18f), Offset(0.63f, 0.82f), Offset(0.75f, 0.08f), Offset(0.75f, 0.92f)
                    )
                    LiveCourtPlayStyle.CUT -> listOf(
                        Offset(0.65f, 0.52f), Offset(0.875f, 0.42f), Offset(0.62f, 0.80f), Offset(0.75f, 0.09f), Offset(0.73f, 0.91f)
                    )
                    LiveCourtPlayStyle.PICK_AND_ROLL -> listOf(
                        Offset(0.70f, 0.38f), Offset(0.62f, 0.18f), Offset(0.62f, 0.82f), Offset(0.75f, 0.08f), Offset(0.845f, 0.61f)
                    )
                    LiveCourtPlayStyle.POST_UP -> listOf(
                        Offset(0.62f, 0.50f), Offset(0.62f, 0.18f), Offset(0.62f, 0.82f), Offset(0.75f, 0.09f), Offset(0.845f, 0.65f)
                    )
                    LiveCourtPlayStyle.MID_RANGE -> listOf(
                        Offset(0.63f, 0.50f), Offset(0.62f, 0.18f), Offset(0.785f, 0.66f), Offset(0.75f, 0.09f), Offset(0.73f, 0.91f)
                    )
                    LiveCourtPlayStyle.CORNER_THREE -> listOf(
                        Offset(0.62f, 0.50f), Offset(0.63f, 0.25f), Offset(0.63f, 0.76f), Offset(0.80f, 0.08f), Offset(0.75f, 0.91f)
                    )
                    LiveCourtPlayStyle.WING_THREE -> listOf(
                        Offset(0.62f, 0.50f), Offset(0.76f, 0.25f), Offset(0.63f, 0.77f), Offset(0.75f, 0.09f), Offset(0.75f, 0.91f)
                    )
                    LiveCourtPlayStyle.TOP_THREE -> listOf(
                        Offset(0.70f, 0.50f), Offset(0.63f, 0.20f), Offset(0.63f, 0.80f), Offset(0.75f, 0.09f), Offset(0.75f, 0.91f)
                    )
                    null -> baseOffense
                }
                val targetOffense = rawTargetOffense.mapIndexed { index, point -> organicTarget(point, index) }

                val transitionProgress = if (playPlan == null) 1f else possessionFlow?.transitionProgress ?: 1f
                val setupProgress = possessionFlow?.setupProgress ?: 1f
                val actionProgress = possessionFlow?.actionProgress ?: 1f
                val finishProgress = possessionFlow?.finishProgress ?: 0f
                val sequenceProgress = possessionFlow?.sequenceProgress ?: 1f
                val effectivePassCount = possessionFlow?.effectivePassCount ?: playPlan?.passCount ?: 0

                fun stagedActionProgress(index: Int): Float {
                    if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) return smooth((playProgress - 0.10f) / 0.42f)
                    val base = when (playPlan?.style) {
                        LiveCourtPlayStyle.CUT -> if (index == 1) (actionProgress - 0.12f) / 0.88f else actionProgress
                        LiveCourtPlayStyle.PICK_AND_ROLL -> if (index == 4) (actionProgress - 0.16f) / 0.84f else actionProgress
                        LiveCourtPlayStyle.POST_UP -> if (index == 4) (actionProgress - 0.10f) / 0.90f else actionProgress
                        LiveCourtPlayStyle.MID_RANGE -> if (index == 2) (actionProgress - 0.14f) / 0.86f else actionProgress
                        else -> actionProgress
                    }
                    val individualDelay = index * 0.025f
                    return smooth((base - individualDelay) / (1f - individualDelay))
                }

                val offenseNormalized = baseOffense.mapIndexed { index, base ->
                    val transitionStart = if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) base else backcourtTransition[index]
                    val arranged = lerp(transitionStart, base, smooth(transitionProgress))
                    val moved = lerp(arranged, targetOffense[index], stagedActionProgress(index))
                    val sample = LiveCourtOrganicMotion.playerSample(
                        playerIndex = index,
                        elapsedMillis = elapsedMillis,
                        actionProgress = maxOf(setupProgress, actionProgress),
                        eventElapsedMillis = eventSeed
                    )
                    val movementScale = if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) 0f else 1f
                    Offset(
                        (moved.x + sample.xOffset * movementScale).coerceIn(0.10f, 0.90f),
                        (moved.y + sample.yOffset * movementScale).coerceIn(0.07f, 0.93f)
                    )
                }

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
                val helperIndex = when (playPlan?.style) {
                    LiveCourtPlayStyle.DRIVE -> 2
                    LiveCourtPlayStyle.CUT -> 4
                    LiveCourtPlayStyle.PICK_AND_ROLL -> 2
                    LiveCourtPlayStyle.POST_UP -> 1
                    else -> -1
                }

                val defenseBase = listOf(
                    Offset(0.60f, 0.54f),
                    Offset(0.66f, 0.25f),
                    Offset(0.65f, 0.75f),
                    Offset(0.77f, 0.16f),
                    Offset(0.77f, 0.84f)
                ).mapIndexed { index, point -> organicTarget(point, index + 2) }

                val defenseNormalized = if (playPlan?.style == LiveCourtPlayStyle.FREE_THROW) {
                    listOf(
                        Offset(0.79f, 0.27f), Offset(0.79f, 0.73f), Offset(0.73f, 0.34f), Offset(0.73f, 0.66f), Offset(0.58f, 0.50f)
                    )
                } else {
                    offenseNormalized.mapIndexed { index, offense ->
                        val reaction = LiveCourtOrganicMotion.defenderReactionProgress(actionProgress, index, eventSeed)
                        val gapX = 0.042f + index * 0.004f
                        val gapY = when (index) {
                            0 -> 0.040f
                            1 -> -0.050f
                            2 -> 0.048f
                            3 -> -0.032f
                            else -> 0.034f
                        }
                        var target = Offset(
                            (offense.x + gapX).coerceAtMost(0.895f),
                            (offense.y + gapY).coerceIn(0.07f, 0.93f)
                        )
                        if (index == helperIndex && actionProgress > 0.58f) {
                            val collapse = smooth((actionProgress - 0.58f) / 0.42f)
                            val shooterY = offenseNormalized[shooterIndex].y
                            val helpTarget = Offset(0.80f, if (shooterY < 0.50f) 0.40f else 0.60f)
                            target = lerp(target, helpTarget, collapse * 0.60f)
                        }
                        val reacted = lerp(defenseBase[index], target, reaction)
                        val defensiveDrift = LiveCourtOrganicMotion.playerSample(
                            playerIndex = 4 - index,
                            elapsedMillis = elapsedMillis + 170L,
                            actionProgress = reaction,
                            eventElapsedMillis = eventSeed + 211L
                        )
                        Offset(
                            (reacted.x + defensiveDrift.xOffset * 0.65f).coerceIn(0.10f, 0.90f),
                            (reacted.y + defensiveDrift.yOffset * 0.65f).coerceIn(0.07f, 0.93f)
                        )
                    }
                }

                val offensePositions = offenseNormalized.map(::courtPoint)
                val defensePositions = defenseNormalized.map(::courtPoint)
                val offenseColor = if (offenseSide == LiveScoringSide.USER) BasketOrange else ElectricCyan
                val defenseColor = if (offenseSide == LiveScoringSide.USER) ElectricCyan else BasketOrange

                fun currentHandlerIndex(): Int {
                    if (playPlan == null || possessionFlow?.phase == LiveCourtPossessionPhase.TRANSITION) return 0
                    val p = sequenceProgress
                    return when (playPlan.style) {
                        LiveCourtPlayStyle.FREE_THROW,
                        LiveCourtPlayStyle.DRIVE,
                        LiveCourtPlayStyle.TOP_THREE -> if (playPlan.style == LiveCourtPlayStyle.DRIVE && effectivePassCount > 0 && p in 0.42f..0.58f) 1 else 0
                        LiveCourtPlayStyle.CUT -> if (effectivePassCount <= 1) {
                            if (p >= 0.72f) 1 else 0
                        } else when {
                            p < 0.42f -> 0
                            p < 0.73f -> 2
                            else -> 1
                        }
                        LiveCourtPlayStyle.PICK_AND_ROLL,
                        LiveCourtPlayStyle.POST_UP -> if (p >= 0.72f || (playPlan.style == LiveCourtPlayStyle.POST_UP && p >= 0.62f)) 4 else 0
                        LiveCourtPlayStyle.MID_RANGE -> if (effectivePassCount <= 1) {
                            if (p >= 0.70f) 2 else 0
                        } else when {
                            p < 0.40f -> 0
                            p < 0.71f -> 1
                            else -> 2
                        }
                        LiveCourtPlayStyle.CORNER_THREE -> if (effectivePassCount <= 1) {
                            if (p >= 0.70f) 3 else 0
                        } else when {
                            p < 0.39f -> 0
                            p < 0.70f -> 2
                            else -> 3
                        }
                        LiveCourtPlayStyle.WING_THREE -> if (effectivePassCount <= 1) {
                            if (p >= 0.70f) 1 else 0
                        } else when {
                            p < 0.39f -> 0
                            p < 0.70f -> 2
                            else -> 1
                        }
                    }
                }
                val handlerIndex = currentHandlerIndex()

                defensePositions.forEach { point ->
                    drawCircle(Color.Black.copy(alpha = 0.28f), markerRadius + 3f, point + Offset(2f, 3f))
                    drawCircle(defenseColor, markerRadius, point)
                    drawCircle(Color.White.copy(alpha = 0.75f), markerRadius, point, style = Stroke(width = 2f))
                }
                offensePositions.forEachIndexed { index, point ->
                    drawCircle(Color.Black.copy(alpha = 0.28f), markerRadius + 3f, point + Offset(2f, 3f))
                    drawCircle(offenseColor, markerRadius, point)
                    drawCircle(
                        if (index == handlerIndex) ChampionshipGold else Color.White.copy(alpha = 0.75f),
                        markerRadius,
                        point,
                        style = Stroke(width = if (index == handlerIndex) 3.5f else 2f)
                    )
                }

                fun bouncedAt(index: Int): Offset {
                    val player = offensePositions[index]
                    val bounce = LiveCourtOrganicMotion.dribbleBounce(elapsedMillis, index) * markerRadius
                    val side = if ((index + currentQuarter) % 2 == 0) 1f else -1f
                    return Offset(player.x + markerRadius * 1.10f * side, player.y + bounce)
                }

                fun curvedPass(from: Int, to: Int, localProgress: Float, seedOffset: Long): Offset {
                    val t = smooth(localProgress)
                    val start = offensePositions[from]
                    val end = offensePositions[to]
                    val direct = lerp(start, end, t)
                    val dx = end.x - start.x
                    val dy = end.y - start.y
                    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val bendPixels = LiveCourtOrganicMotion.passCurve(t, eventSeed + seedOffset) * h
                    return Offset(
                        direct.x + (-dy / length) * bendPixels,
                        direct.y + (dx / length) * bendPixels
                    )
                }

                val hoop = if (offenseAttacksRight) rightHoop else leftHoop
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
                    val sideDrift = sin(t * PI.toFloat()) * h * 0.012f * if ((eventSeed and 1L) == 0L) 1f else -1f
                    return Offset(
                        x = start.x + (hoop.x - start.x) * t,
                        y = start.y + (hoop.y - start.y) * t - arcLift + sideDrift
                    )
                }

                val ballPosition = when {
                    playPlan == null -> bouncedAt(0)
                    possessionFlow?.phase == LiveCourtPossessionPhase.FINISH -> shot(finishProgress)
                    possessionFlow?.phase == LiveCourtPossessionPhase.TRANSITION && playPlan.style != LiveCourtPlayStyle.FREE_THROW -> bouncedAt(0)
                    else -> {
                        val p = sequenceProgress
                        when (playPlan.style) {
                            LiveCourtPlayStyle.FREE_THROW -> offensePositions[0] + Offset(markerRadius * 0.9f, 0f)
                            LiveCourtPlayStyle.DRIVE -> if (effectivePassCount == 0) bouncedAt(0) else when {
                                p < 0.26f -> bouncedAt(0)
                                p < 0.42f -> curvedPass(0, 1, (p - 0.26f) / 0.16f, 11L)
                                p < 0.58f -> bouncedAt(1)
                                p < 0.72f -> curvedPass(1, 0, (p - 0.58f) / 0.14f, 17L)
                                else -> bouncedAt(0)
                            }
                            LiveCourtPlayStyle.CUT -> if (effectivePassCount <= 1) when {
                                p < 0.50f -> bouncedAt(0)
                                p < 0.72f -> curvedPass(0, 1, (p - 0.50f) / 0.22f, 23L)
                                else -> bouncedAt(1)
                            } else when {
                                p < 0.25f -> bouncedAt(0)
                                p < 0.42f -> curvedPass(0, 2, (p - 0.25f) / 0.17f, 29L)
                                p < 0.57f -> bouncedAt(2)
                                p < 0.73f -> curvedPass(2, 1, (p - 0.57f) / 0.16f, 31L)
                                else -> bouncedAt(1)
                            }
                            LiveCourtPlayStyle.PICK_AND_ROLL -> when {
                                p < 0.52f -> bouncedAt(0)
                                p < 0.72f -> curvedPass(0, 4, (p - 0.52f) / 0.20f, 37L)
                                else -> bouncedAt(4)
                            }
                            LiveCourtPlayStyle.POST_UP -> when {
                                p < 0.40f -> bouncedAt(0)
                                p < 0.62f -> curvedPass(0, 4, (p - 0.40f) / 0.22f, 41L)
                                else -> bouncedAt(4)
                            }
                            LiveCourtPlayStyle.MID_RANGE -> if (effectivePassCount <= 1) when {
                                p < 0.48f -> bouncedAt(0)
                                p < 0.70f -> curvedPass(0, 2, (p - 0.48f) / 0.22f, 43L)
                                else -> bouncedAt(2)
                            } else when {
                                p < 0.24f -> bouncedAt(0)
                                p < 0.40f -> curvedPass(0, 1, (p - 0.24f) / 0.16f, 47L)
                                p < 0.55f -> bouncedAt(1)
                                p < 0.71f -> curvedPass(1, 2, (p - 0.55f) / 0.16f, 53L)
                                else -> bouncedAt(2)
                            }
                            LiveCourtPlayStyle.CORNER_THREE -> if (effectivePassCount <= 1) when {
                                p < 0.48f -> bouncedAt(0)
                                p < 0.70f -> curvedPass(0, 3, (p - 0.48f) / 0.22f, 59L)
                                else -> bouncedAt(3)
                            } else when {
                                p < 0.23f -> bouncedAt(0)
                                p < 0.39f -> curvedPass(0, 2, (p - 0.23f) / 0.16f, 61L)
                                p < 0.54f -> bouncedAt(2)
                                p < 0.70f -> curvedPass(2, 3, (p - 0.54f) / 0.16f, 67L)
                                else -> bouncedAt(3)
                            }
                            LiveCourtPlayStyle.WING_THREE -> if (effectivePassCount <= 1) when {
                                p < 0.48f -> bouncedAt(0)
                                p < 0.70f -> curvedPass(0, 1, (p - 0.48f) / 0.22f, 71L)
                                else -> bouncedAt(1)
                            } else when {
                                p < 0.23f -> bouncedAt(0)
                                p < 0.39f -> curvedPass(0, 2, (p - 0.23f) / 0.16f, 73L)
                                p < 0.54f -> bouncedAt(2)
                                p < 0.70f -> curvedPass(2, 1, (p - 0.54f) / 0.16f, 79L)
                                else -> bouncedAt(1)
                            }
                            LiveCourtPlayStyle.TOP_THREE -> if (effectivePassCount <= 1) bouncedAt(0) else when {
                                p < 0.25f -> bouncedAt(0)
                                p < 0.42f -> curvedPass(0, 1, (p - 0.25f) / 0.17f, 83L)
                                p < 0.56f -> bouncedAt(1)
                                p < 0.72f -> curvedPass(1, 0, (p - 0.56f) / 0.16f, 89L)
                                else -> bouncedAt(0)
                            }
                        }
                    }
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
                    phaseLabel
                } else {
                    "$phaseLabel • último +$lastScorePoints"
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
