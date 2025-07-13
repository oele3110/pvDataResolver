package com.oele3110.pvdataresolver


import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.oele3110.pvdataresolver.domain.EnergyValues
import com.oele3110.pvdataresolver.domain.Line
import com.oele3110.pvdataresolver.domain.Node
import com.oele3110.pvdataresolver.domain.TextPosition
import com.oele3110.pvdataresolver.ui.theme.PvDataResolverTheme
import com.oele3110.pvdataresolver.websocket.DummyWebSocketClient
import com.oele3110.pvdataresolver.websocket.WebSocketClient
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin


val dummyValues = EnergyValues(
    sumPvPowerInverterDc = 8223f, "",
    sumOutputInverterAc = 3213f, "",
    sumBatteryChargeDischargeDc = -313f, "",
    gridPowerTotal = 123f, "",
    homeConsumption = 1534f, "",
    sumWallboxChargePowerTotal = 542f, "",
    powerHeaterRod = 754f, "",
    houseConsumption = 300f, "",
    powerHeating = 723f, "",
    batteryCapacity = 79f, "",
    temperatureHeaterRod = 63f, "",
    wallboxConnectionStatus = 5,
)
const val alpha = 0.8f
val colorBlue: Color = Color(27, 175, 232, 255)


class MainActivity : ComponentActivity() {
    private val webSocketClient = DummyWebSocketClient(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        webSocketClient.connect()

        setContent {
            PvDataResolverTheme {
                SetStatusBarColor()
                //WebSocketApp()
                val energyValues by webSocketClient.data.collectAsState()
                EnergyScreen(values = energyValues)
            }

        }
    }

    @Composable
    private fun SetStatusBarColor() {
        val color = MaterialTheme.colorScheme.background
        val window = this.window
        window.statusBarColor = color.toArgb()
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = color.luminance() > 0.5f
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
    Node("PV", R.drawable.pv, 0.5f, 0.5f),
    Node("Battery", R.drawable.battery, 0.15f, 2f, text = { "${it.batteryCapacity} %" }, textPosition = TextPosition.BOTTOM),
    Node("Inverter", R.drawable.inverter, 0.5f, 2f),
    Node("SEM", R.drawable.sem, 0.5f, 3.5f),
    Node("Grid", R.drawable.grid, 0.85f, 3.5f),
    Node(
        "Wallbox",
        R.drawable.wallbox,
        0.15f,
        5f,
        text = { getWallboxConnectionStatus(it.wallboxConnectionStatus) },
        textPosition = TextPosition.BOTTOM
    ),
    Node("Home", R.drawable.house_day, 0.5f, 5f),
    Node(
        "Heater", R.drawable.water_heater,
        0.85f,
        5f,
        text = { "${it.temperatureHeaterRod}°" },
        textPosition = TextPosition.BOTTOM
    ),
    // when only home consumption is shown, use this setting, otherwise the ones below
    Node("House Consumption", R.drawable.house_consumption, 0.5f, 6.5f),
    //Node("House Consumption", R.drawable.house_consumption, 0.5f, 7.5f),
    //Node("Heating", R.drawable.heating, 0.15f, 6.25f),
    //Node("AC", R.drawable.ac, 0.85f, 6.25f)
)

fun getWallboxConnectionStatus(wallboxConnectionStatus: Int): String {
    return when (wallboxConnectionStatus) {
        2 -> "Verbunden"
        3 -> "Pause"
        4 -> "Init."
        5 -> "Laden"
        6 -> "Fehler"
        7 -> "Service"
        else -> "Nicht verb."
    }
}

val lines = listOf(
    Line("PV", "Inverter") { it.sumPvPowerInverterDc },
    Line("Inverter", "SEM") { it.sumOutputInverterAc },
    Line("Inverter", "Battery") { it.sumBatteryChargeDischargeDc },
    Line("Grid", "SEM") { it.gridPowerTotal },
    Line("SEM", "Home") { it.homeConsumption },
    Line("Home", "Wallbox") { it.sumWallboxChargePowerTotal },
    Line("Home", "Heater") { it.powerHeaterRod },
    // when only home consumption is shown, use this setting, otherwise the ones below
    Line("Home", "House Consumption") { it.houseConsumption },
    //Line("Home", "House Consumption", textOffsetY = 0.4f) { it.houseConsumption },
    //Line("Home", "Heating") { it.powerHeating },
    //Line("Home", "AC") { it.powerAc }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//fun EnergyScreen(values: EnergyValues) {
fun EnergyScreen(values: EnergyValues) {
    val scrollState = rememberScrollState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Energy Flow Dashboard", style = MaterialTheme.typography.headlineLarge) })
    }, content = { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            EnergyFlowScreen(values)
            StatusScreen()
            StatusScreen()
            StatusScreen()
        }
    })
}

@Composable
@Preview
private fun StatusScreen() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status Screen", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("This is a placeholder for the status screen.")
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun EnergyFlowScreen(values: EnergyValues) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        val rowHeight = 80.dp
        val topPadding = 16.dp
        val bottomPadding = 16.dp

        val maxRow = nodes.maxOf { it.row }
        val totalHeight = topPadding + 4 * bottomPadding + (maxRow * rowHeight)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
            val topPaddingPx = with(LocalDensity.current) { topPadding.toPx() }
            val iconSizePx = with(LocalDensity.current) { 64.dp.toPx() }
            val iconHalf = iconSizePx / 2


            val nodePositions = nodes.associateBy({ it.name }, { node ->
                Offset(widthPx * node.col, topPaddingPx + (node.row * rowHeightPx))
            })

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

            val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()
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

                        if (value != 0f) {
                            drawCornerLineAndArrow(newAdjustedStart, newAdjustedEnd)
                            drawCornerText(newAdjustedStart, newAdjustedEnd, value, onBackgroundColor)
                            drawCornerAnimatedDots(progressCorner, value, newAdjustedStart, newAdjustedEnd)
                        }
                    } else {
                        if (value != 0f) {
                            drawStraightLineAndArrow(adjustedStart, adjustedEnd)
                            drawStraightText(
                                adjustedStart, adjustedEnd, value, onBackgroundColor, iconHalf, length * line.textOffsetX, length * line.textOffsetY
                            )
                            drawStraightAnimatedDots(progress, value, rawVector, adjustedStart, adjustedEnd)
                        }
                    }
                }
            }

            nodes.forEach { node ->
                val pos = nodePositions[node.name]!!
                val nodeText = node.text(values)

                when (node.textPosition) {
                    TextPosition.TOP -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset {
                                IntOffset((pos.x - iconHalf).toInt(), (pos.y - 1.5 * iconHalf).toInt())
                            }) {
                            if (nodeText != null) {
                                Text(nodeText, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            }
                            Image(
                                painterResource(node.icon),
                                contentDescription = node.name,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    TextPosition.BOTTOM -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset {
                                IntOffset((pos.x - iconHalf).toInt(), (pos.y - iconHalf).toInt())
                            }) {
                            Image(
                                painterResource(node.icon),
                                contentDescription = node.name,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                            if (nodeText != null) {
                                Text(nodeText, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    TextPosition.NONE -> {
                        Box(
                            Modifier.offset {
                                IntOffset((pos.x - iconHalf).toInt(), (pos.y - iconHalf).toInt())
                            }) {
                            Image(
                                painterResource(node.icon),
                                contentDescription = node.name,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

        }
    }
}

private fun DrawScope.drawStraightText(
    adjustedStart: Offset,
    adjustedEnd: Offset,
    value: Float,
    onBackgroundColor: Int,
    iconHalf: Float,
    textOffsetX: Float = 0f,
    textOffsetY: Float = 0f
) {
    if (adjustedStart.x == adjustedEnd.x) {
        // if line is vertical, adjust text position to have it right from the line
        drawTextOnLine(
            "${abs(value)} W",
            (adjustedStart.x + adjustedEnd.x + textOffsetX) / 2 + iconHalf,
            (adjustedStart.y + adjustedEnd.y + textOffsetY) / 2,
            onBackgroundColor
        )
    } else {
        // if line is horizontal, adjust text position to have it above the line
        drawTextOnLine(
            "${abs(value)} W", (adjustedStart.x + adjustedEnd.x) / 2, (adjustedStart.y + adjustedEnd.y) / 2 - 20f, onBackgroundColor
        )
    }
}

private fun DrawScope.drawCornerText(start: Offset, end: Offset, value: Float, onBackgroundColor: Int) {
    val corner = Offset(start.x, end.y)
    drawTextOnLine(
        "${abs(value)} W", (corner.x + end.x) / 2, end.y - 20f, onBackgroundColor
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
    progress: Float, value: Float, rawVector: Offset, start: Offset, end: Offset
) {
    // this offset makes sure that the dots are not exceeding the arrow
    val endOffset = if (value > 0) rawVector * 0.05f else -rawVector * 0.05f

    listOf(
        Triple(0.0f, 0.6f, 12f), Triple(0.1f, 0.7f, 9f), Triple(0.2f, 0.8f, 6f)
    ).forEach { (s, e, size) ->
        if (progress in s..e) {
            val localProgress = ((progress - s) / (e - s)).coerceIn(0f, 1f)
            val dot = start + (end - start - endOffset) * localProgress
            drawCircle(colorBlue, size, center = dot, alpha = alpha)
        }
    }
}

private fun DrawScope.drawCornerAnimatedDots(progress: Float, value: Float, start: Offset, end: Offset) {
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
            drawCircle(colorBlue, size, center = dot, alpha = alpha)
        }
    }
}


fun DrawScope.drawTextOnLine(text: String, x: Float, y: Float, onBackgroundColor: Int) {
    drawContext.canvas.nativeCanvas.drawText(
        text, x, y, Paint().apply {
            color = onBackgroundColor
            textSize = 36f
            textAlign = Paint.Align.CENTER
        })
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewLight")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "PreviewDark")
@Composable
fun EnergyFlowScreenPreview() {
    PvDataResolverTheme {
        EnergyScreen(values = dummyValues)
    }
}
