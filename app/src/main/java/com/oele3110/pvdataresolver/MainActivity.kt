package com.oele3110.pvdataresolver


import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.oele3110.pvdataresolver.domain.EnergyValues
import com.oele3110.pvdataresolver.domain.Line
import com.oele3110.pvdataresolver.domain.Node
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin


val dummyValues = EnergyValues(
    sumPvPowerInverterDc = 8223,
    sumOutputInverterAc = 3213,
    sumBatteryChargeDischargeDc = -313,
    gridPowerTotal = 123,
    homeConsumption = 1534,
    sumWallboxChargePowerTotal = 542,
    powerHeaterRod = 754, houseConsumption = 300, powerHeating = 723, powerAc = 211
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //WebSocketApp()
            EnergyFlowScreen(values = dummyValues)
        }
    }
}

@Composable
fun WebSocketApp() {
    val webSocketClient = remember { WebSocketClient() }
    val message by webSocketClient.messages.collectAsState() // live update of the messages
    val connectionStatus by webSocketClient.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (!connectionStatus) {
                    webSocketClient.connect()
                } else {
                    webSocketClient.disconnect()
                }
            }) {
            Text(if (connectionStatus) "Close" else "Connect")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

val nodes = listOf(
    Node("PV", R.drawable.pv, 0.5f, 0.1f),
    Node("Battery", R.drawable.battery, 0.15f, 0.225f),
    Node("Inverter", R.drawable.inverter, 0.5f, 0.225f),
    Node("SEM", R.drawable.sem, 0.5f, 0.35f),
    Node("Grid", R.drawable.grid, 0.85f, 0.35f),
    Node("Wallbox", R.drawable.wallbox, 0.15f, 0.475f),
    Node("Home", R.drawable.house_day, 0.5f, 0.475f),
    Node("Heater", R.drawable.water_heater, 0.85f, 0.475f),
    // when only home consumption is shown, use this setting, otherwise the ones below
    Node("House Consumption", R.drawable.house_consumption, 0.5f, 0.6f),
    //Node("House Consumption", R.drawable.house_consumption, 0.5f, 0.7f),
    //Node("Heating", R.drawable.heating, 0.15f, 0.6f),
    //Node("AC", R.drawable.ac, 0.85f, 0.6f)
)

val lines = listOf(
    Line("PV", "Inverter") { it.sumPvPowerInverterDc },
    Line("Inverter", "SEM") { it.sumOutputInverterAc },
    Line("Inverter", "Battery") { it.sumBatteryChargeDischargeDc },
    Line("SEM", "Grid") { it.gridPowerTotal },
    Line("SEM", "Home") { it.homeConsumption },
    Line("Home", "Wallbox") { it.sumWallboxChargePowerTotal },
    Line("Home", "Heater") { it.powerHeaterRod },
    // when only home consumption is shown, use this setting, otherwise the ones below
    Line("Home", "House Consumption") { it.houseConsumption },
    //Line("Home", "House Consumption", textOffsetY = 0.4f) { it.houseConsumption },
    //Line("Home", "Heating") { it.powerHeating },
    //Line("Home", "AC") { it.powerAc }
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun EnergyFlowScreen(values: EnergyValues) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val iconSizePx = with(LocalDensity.current) { 64.dp.toPx() }
        val iconHalf = iconSizePx / 2

        val nodePositions = nodes.associateBy({ it.name }, { Offset(widthPx * it.col, heightPx * it.row) })

        val transition = rememberInfiniteTransition(label = "")
        val progress by transition.animateFloat(
            initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            ), label = ""
        )

        val progressCorner by transition.animateFloat(
            initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearEasing)
            ), label = ""
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            lines.forEach { line ->
                val start = nodePositions[line.from] ?: return@forEach
                val end = nodePositions[line.to] ?: return@forEach

                val value = line.valueProvider(values)

                val rawVector = end - start
                val length = rawVector.getDistance()
                val unit = if (length != 0f) rawVector / length else Offset.Zero

                val adjustedStart: Offset
                val adjustedEnd: Offset

                if (value >= 0) {
                    adjustedStart = start + unit * iconHalf
                    adjustedEnd = end - unit * iconHalf
                } else {
                    adjustedStart = end + (-unit) * iconHalf
                    adjustedEnd = start - (-unit) * iconHalf
                }

                if (adjustedStart.x != adjustedEnd.x && adjustedStart.y != adjustedEnd.y) {


                    val newAdjustedStart = Offset(
                        if (adjustedStart.x != adjustedEnd.x) start.x + iconHalf * sign(adjustedEnd.x - adjustedStart.x) else start.x,
                        if (adjustedStart.y != adjustedEnd.y) start.y + iconHalf * sign(adjustedEnd.y - adjustedStart.y) else start.y
                    )
                    val newAdjustedEnd = Offset(
                        x = if (adjustedStart.x != adjustedEnd.x) end.x - iconHalf * sign(adjustedEnd.x - adjustedStart.x) else start.x,
                        y = if (adjustedStart.y != adjustedEnd.y) end.y else start.y
                    )

                    drawCornerLineAndArrow(newAdjustedStart, newAdjustedEnd)
                    drawCornerText(newAdjustedStart, newAdjustedEnd, value)
                    drawCornerAnimatedDots(progressCorner, value, newAdjustedStart, newAdjustedEnd)
                } else {
                    drawStraightLineAndArrow(adjustedStart, adjustedEnd)
                    drawStraightText(adjustedStart, adjustedEnd, value, iconHalf, length * line.textOffsetX, length * line.textOffsetY)
                    drawStraightAnimatedDots(progress, value, rawVector, adjustedStart, adjustedEnd)
                }
            }
        }

        nodes.forEach { node ->
            val pos = nodePositions[node.name]!!
            Box(
                Modifier
                    .offset { IntOffset((pos.x - iconHalf).toInt(), (pos.y - iconHalf).toInt()) }
                    .size(64.dp)) {
                Image(
                    painterResource(node.icon), contentDescription = node.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun DrawScope.drawStraightText(
    adjustedStart: Offset, adjustedEnd: Offset, value: Int, iconHalf: Float, textOffsetX: Float = 0f, textOffsetY: Float = 0f
) {
    if (adjustedStart.x == adjustedEnd.x) {
        // if line is vertical, adjust text position to have it right from the line
        drawTextOnLine(
            "$value W", (adjustedStart.x + adjustedEnd.x + textOffsetX) / 2 + iconHalf, (adjustedStart.y + adjustedEnd.y + textOffsetY) / 2
        )
    } else {
        // if line is horizontal, adjust text position to have it above the line
        drawTextOnLine(
            "$value W", (adjustedStart.x + adjustedEnd.x) / 2, (adjustedStart.y + adjustedEnd.y) / 2 - 20f
        )
    }
}

private fun DrawScope.drawCornerText(start: Offset, end: Offset, value: Int) {
    val corner = Offset(start.x, end.y)
    drawTextOnLine(
        "$value W", (corner.x + end.x) / 2, end.y - 20f
    )
}

private fun DrawScope.drawStraightLineAndArrow(
    adjustedStart: Offset, adjustedEnd: Offset
) {
    // line is drawn directly from adjustedStart to adjustedEnd
    drawLine(colorBlue, adjustedStart, adjustedEnd, strokeWidth = 6f, cap = StrokeCap.Round)
    drawArrow(adjustedEnd, adjustedStart)
}


private fun DrawScope.drawCornerLineAndArrow(start: Offset, end: Offset) {
    val corner = Offset(start.x, end.y)

    // vertical
    drawLine(colorBlue, Offset(start.x, start.y), Offset(corner.x, corner.y), 6f, cap = StrokeCap.Round)
    // horizontal
    drawLine(colorBlue, Offset(corner.x, corner.y), Offset(end.x, end.y), 6f, cap = StrokeCap.Round)

    drawArrow(end, Offset(corner.x, corner.y))
}

private fun DrawScope.drawArrow(end: Offset, beforeEnd: Offset) {
    val angle = atan2(
        (end.y - beforeEnd.y).toDouble(), (end.x - beforeEnd.x).toDouble()
    ).toFloat()

    val arrowSize = 30f
    val arrowCenter = end

    val leftWing = Offset(
        (arrowCenter.x - arrowSize * cos((angle - Math.PI / 6)).toFloat()), (arrowCenter.y - arrowSize * sin((angle - Math.PI / 6)).toFloat())
    )
    val rightWing = Offset(
        (arrowCenter.x - arrowSize * cos((angle + Math.PI / 6)).toFloat()), (arrowCenter.y - arrowSize * sin((angle + Math.PI / 6)).toFloat())
    )

    drawLine(colorBlue, leftWing, arrowCenter, strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(colorBlue, rightWing, arrowCenter, strokeWidth = 6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawStraightAnimatedDots(
    progress: Float, value: Int, rawVector: Offset, start: Offset, end: Offset
) {
    // this offset makes sure that the dots are not exceeding the arrow
    val endOffset = if (value > 0) rawVector * 0.05f else -rawVector * 0.05f

    listOf(
        Triple(0.0f, 0.6f, 12f), Triple(0.1f, 0.7f, 9f), Triple(0.2f, 0.8f, 6f)
    ).forEach { (s, e, size) ->
        if (progress in s..e) {
            val localProgress = ((progress - s) / (e - s)).coerceIn(0f, 1f)
            val dot = start + (end - start - endOffset) * localProgress
            drawCircle(colorBlue, size, center = dot)
        }
    }
}

private fun DrawScope.drawCornerAnimatedDots(progress: Float, value: Int, start: Offset, end: Offset) {
    val corner = Offset(start.x, end.y)

    val rawVector = end - corner
    // this offset makes sure that the dots are not exceeding the arrow
    val endOffset = if (value > 0) rawVector * 0.1f else -rawVector * 0.1f

    val dotAnimations = listOf(
        Triple(0.0f, 0.6f, 12f), // (start, end, size)
        Triple(0.1f, 0.7f, 9f), Triple(0.2f, 0.8f, 6f)
    )

    dotAnimations.forEach { (animStart, animEnd, size) ->
        if (progress in animStart..animEnd) {
            val localProgress = ((progress - animStart) / (animEnd - animStart)).coerceIn(0f, 1f)
            val dot = if (localProgress < 0.5f) {
                val p = localProgress / 0.5f
                start + (corner - start) * p
            } else {
                val p = (localProgress - 0.5f) / 0.5f
                corner + (end - corner - endOffset) * p
            }
            drawCircle(colorBlue, size, center = dot)
        }
    }
}

val colorBlue: Color = Color(43, 146, 214)

fun DrawScope.drawTextOnLine(text: String, x: Float, y: Float) {
    drawContext.canvas.nativeCanvas.drawText(
        text, x, y, Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 36f
            textAlign = Paint.Align.CENTER
        })
}

@Preview(showBackground = true)
@Composable
fun EnergyFlowScreenPreview() {
    EnergyFlowScreen(values = dummyValues)
}
