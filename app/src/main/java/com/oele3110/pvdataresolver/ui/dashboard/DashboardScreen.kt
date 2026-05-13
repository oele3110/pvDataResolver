package com.oele3110.pvdataresolver.ui.dashboard

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Paint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oele3110.pvdataresolver.R
import com.oele3110.pvdataresolver.data.model.BatteryData
import com.oele3110.pvdataresolver.data.model.EnergyData
import com.oele3110.pvdataresolver.data.model.HeaterData
import com.oele3110.pvdataresolver.data.model.InverterData
import com.oele3110.pvdataresolver.data.model.SmartmeterData
import com.oele3110.pvdataresolver.data.model.WallboxData
import com.oele3110.pvdataresolver.domain.Line
import com.oele3110.pvdataresolver.domain.Node
import com.oele3110.pvdataresolver.domain.TextPosition
import com.oele3110.pvdataresolver.ui.theme.PvDataResolverTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

private val dummyValues = EnergyData(
    inverter = InverterData(powerAcW = 3213f, powerDcW = 8223f, homeConsumptionFromPvW = 2000f),
    smartmeter = SmartmeterData(gridPowerW = 123f, homeConsumptionW = 1534f),
    wallbox = WallboxData(powerW = 542f, activeChargeMode = 5),
    battery = BatteryData(powerW = -313f, stateOfChargePct = 79f),
    heater = HeaterData(powerW = 754f, temp1C = 63f)
)

private const val ALPHA = 0.8f
val colorBlue: Color = Color(27, 175, 232, 255)

fun formatWatts(value: Float): String {
    val abs = kotlin.math.abs(value)
    return if (abs >= 1000f) {
        String.format(java.util.Locale.ROOT, "%.1f kW", value / 1000f)
    } else {
        "${value.toInt()} W"
    }
}

private fun buildNodes(showCarSoc: Boolean) = listOf(
    Node("PV", R.drawable.pv, 0.5f, 0.5f),
    Node(
        "Battery", R.drawable.battery, 0.15f, 2f,
        text = { "${(it.battery?.stateOfChargePct ?: 0f).toInt()} %" },
        textPosition = TextPosition.BOTTOM
    ),
    Node("Inverter", R.drawable.inverter, 0.5f, 2f),
    Node("SEM", R.drawable.sem, 0.5f, 3.5f),
    Node("Grid", R.drawable.grid, 0.85f, 3.5f),
    Node(
        "Wallbox",
        icon = { if ((it.wallbox?.statusCode ?: 0) in 2..5) R.drawable.bulli else R.drawable.wallbox },
        col = 0.15f, row = 5f,
        text = {
            val status = getWallboxStatus(it.wallbox?.statusCode ?: 0)
            if (showCarSoc) "$status\n${it.consumers?.carSoc?.toInt() ?: "--"}%" else status
        },
        textPosition = TextPosition.BOTTOM
    ),
    Node("Home", R.drawable.house_day, 0.5f, 5f),
    Node(
        "Heater", R.drawable.water_heater, 0.85f, 5f,
        text = { "${it.heater?.temp1C?.toInt() ?: 0}°" },
        textPosition = TextPosition.BOTTOM
    ),
    Node("House Consumption", R.drawable.house_consumption, 0.5f, 6.5f),
)

fun getChargeModeIcon(mode: Int): Int? = when (mode) {
    1 -> R.drawable.charge_mode_lock
    2 -> R.drawable.charge_mode_power
    3 -> R.drawable.charge_mode_solar
    4 -> R.drawable.charge_mode_solar_plus
    else -> null
}

fun getWallboxStatus(mode: Int): String = when (mode) {
    2 -> "Verbunden"
    3 -> "Pause"
    4 -> "Init."
    5 -> "Laden"
    6 -> "Fehler"
    7 -> "Service"
    else -> "Nicht verb."
}

private val lines = listOf(
    Line(
        "PV", "Inverter",
        valueProvider = { it.inverter?.powerDcW ?: 0f },
        valueStringProvider = { formatWatts(it.inverter?.powerDcW ?: 0f) }
    ),
    Line(
        "Inverter", "SEM",
        valueProvider = { it.inverter?.powerAcW ?: 0f },
        valueStringProvider = { formatWatts(it.inverter?.powerAcW ?: 0f) }
    ),
    Line(
        "Battery", "Inverter",
        valueProvider = { it.battery?.powerW ?: 0f },
        valueStringProvider = { formatWatts(it.battery?.powerW ?: 0f) }
    ),
    Line(
        "Grid", "SEM",
        valueProvider = { it.smartmeter?.gridPowerW ?: 0f },
        valueStringProvider = { formatWatts(it.smartmeter?.gridPowerW ?: 0f) }
    ),
    Line(
        "SEM", "Home",
        valueProvider = { it.smartmeter?.homeConsumptionW ?: 0f },
        valueStringProvider = { formatWatts(it.smartmeter?.homeConsumptionW ?: 0f) },
        textOffsetY = 0.4f
    ),
    Line(
        "Home", "Wallbox",
        valueProvider = { it.wallbox?.powerW ?: 0f },
        valueStringProvider = { formatWatts(it.wallbox?.powerW ?: 0f) },
        textOffsetY = 0.4f
    ),
    Line(
        "Home", "Heater",
        valueProvider = { it.heater?.powerW ?: 0f },
        valueStringProvider = { formatWatts(it.heater?.powerW ?: 0f) },
        textOffsetY = 0.4f
    ),
    Line(
        "Home", "House Consumption",
        valueProvider = {
            (it.smartmeter?.homeConsumptionW ?: 0f) - (it.wallbox?.powerW ?: 0f) - (it.heater?.powerW ?: 0f)
        },
        valueStringProvider = {
            formatWatts((it.smartmeter?.homeConsumptionW ?: 0f) - (it.wallbox?.powerW ?: 0f) - (it.heater?.powerW ?: 0f))
        },
        textOffsetY = 0.4f
    ),
)

@Composable
fun DashboardScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val energyData by viewModel.energyData.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isConnected by viewModel.connectionStatus.collectAsState()
    val showCarSoc by viewModel.showCarSoc.collectAsState()

    // Reconnect when returning to this screen (e.g. after login)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.connectIfNeeded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    EnergyScreen(
        values = energyData,
        isLoggedIn = isLoggedIn,
        isConnected = isConnected,
        onLogin = onNavigateToLogin,
        onLogout = { viewModel.logout() },
        showCarSoc = showCarSoc,
        onToggleCarSoc = { viewModel.toggleCarSoc() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(
    values: EnergyData,
    isLoggedIn: Boolean = false,
    isConnected: Boolean = false,
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    showCarSoc: Boolean = true,
    onToggleCarSoc: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Energy Flow Dashboard", style = MaterialTheme.typography.headlineLarge) },
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (showCarSoc) "Auto SOC ausblenden" else "Auto SOC einblenden") },
                            onClick = { onToggleCarSoc(); menuExpanded = false }
                        )
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text("Abmelden") },
                                onClick = { menuExpanded = false; onLogout() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Anmelden") },
                                onClick = { menuExpanded = false; onLogin() }
                            )
                        }
                    }
                }
            }
        )
    }, content = { innerPadding ->
        when {
            !isLoggedIn -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Nicht angemeldet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Bitte über das Menü anmelden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            !isConnected -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Verbinde...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                ) {
                    EnergyFlowCard(values, showCarSoc)
                    StatusCard()
                    StatusCard()
                    StatusCard()
                }
            }
        }
    })
}

@Composable
private fun StatusCard() {
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
            Text("Placeholder für zukünftige Inhalte.")
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun EnergyFlowCard(values: EnergyData, showCarSoc: Boolean = true) {
    val nodes = buildNodes(showCarSoc)
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
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
                label = ""
            )
            val progressCorner by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
                label = ""
            )

            val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()
            Canvas(modifier = Modifier.fillMaxSize()) {
                lines.forEach { line ->
                    val start = nodePositions[line.from] ?: return@forEach
                    val end = nodePositions[line.to] ?: return@forEach
                    val value = line.valueProvider(values)
                    val valueString = line.valueStringProvider(values)
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
                        val newStart = Offset(
                            start.x + iconHalf * sign(adjustedEnd.x - adjustedStart.x),
                            start.y + iconHalf * sign(adjustedEnd.y - adjustedStart.y)
                        )
                        val newEnd = Offset(
                            end.x - iconHalf * sign(adjustedEnd.x - adjustedStart.x),
                            end.y
                        )
                        if (value != 0f) {
                            drawCornerLineAndArrow(newStart, newEnd)
                            drawCornerText(newStart, newEnd, valueString, onBackgroundColor)
                            drawCornerAnimatedDots(progressCorner, value, newStart, newEnd)
                        }
                    } else {
                        if (value != 0f) {
                            drawStraightLineAndArrow(adjustedStart, adjustedEnd)
                            drawStraightText(
                                adjustedStart, adjustedEnd, valueString, onBackgroundColor,
                                iconHalf, length * line.textOffsetX, length * line.textOffsetY
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
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset {
                                IntOffset((pos.x - iconHalf).toInt(), (pos.y - 1.5 * iconHalf).toInt())
                            }
                        ) {
                            if (nodeText != null) Text(
                                nodeText,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Image(painterResource(node.icon(values)), node.name, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                        }
                    }

                    TextPosition.BOTTOM -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset {
                                IntOffset((pos.x - iconHalf).toInt(), (pos.y - iconHalf).toInt())
                            }
                        ) {
                            Image(painterResource(node.icon(values)), node.name, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                            if (nodeText != null) Text(
                                nodeText,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    TextPosition.NONE -> {
                        Box(Modifier.offset { IntOffset((pos.x - iconHalf).toInt(), (pos.y - iconHalf).toInt()) }) {
                            Image(painterResource(node.icon(values)), node.name, Modifier.size(64.dp), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }

            val chargeModeRes = getChargeModeIcon(values.wallbox?.activeChargeMode ?: 0)
            if (chargeModeRes != null) {
                val wallboxPos = nodePositions["Wallbox"]!!
                val chargeModeHalfPx = with(LocalDensity.current) { 24.dp.toPx() }
                Box(Modifier.offset {
                    IntOffset(
                        (wallboxPos.x - chargeModeHalfPx).toInt(),
                        (wallboxPos.y - iconHalf * 2.3f).toInt()
                    )
                }) {
                    Image(
                        painterResource(chargeModeRes),
                        contentDescription = "Charge Mode",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStraightText(
    start: Offset, end: Offset, valueString: String, onBackgroundColor: Int,
    iconHalf: Float, textOffsetX: Float, textOffsetY: Float
) {
    if (start.x == end.x) {
        drawTextOnLine(valueString, (start.x + end.x + textOffsetX) / 2 + iconHalf, (start.y + end.y + textOffsetY) / 2, onBackgroundColor)
    } else {
        drawTextOnLine(valueString, (start.x + end.x) / 2, (start.y + end.y) / 2 - 20f, onBackgroundColor)
    }
}

private fun DrawScope.drawCornerText(start: Offset, end: Offset, valueString: String, onBackgroundColor: Int) {
    val corner = Offset(start.x, end.y)
    drawTextOnLine(valueString, (corner.x + end.x) / 2, end.y - 20f, onBackgroundColor)
}

private fun DrawScope.drawStraightLineAndArrow(start: Offset, end: Offset) {
    drawLine(colorBlue, start, end, strokeWidth = 6f, cap = StrokeCap.Round)
    drawArrow(end, start)
}

private fun DrawScope.drawCornerLineAndArrow(start: Offset, end: Offset) {
    val corner = Offset(start.x, end.y)
    drawLine(colorBlue, start, corner, 6f, cap = StrokeCap.Round)
    drawLine(colorBlue, corner, end, 6f, cap = StrokeCap.Round)
    drawArrow(end, corner)
}

private fun DrawScope.drawArrow(end: Offset, beforeEnd: Offset) {
    val angle = atan2((end.y - beforeEnd.y).toDouble(), (end.x - beforeEnd.x).toDouble()).toFloat()
    val arrowSize = 30f
    val leftWing = Offset(
        end.x - arrowSize * cos((angle - Math.PI / 6)).toFloat(),
        end.y - arrowSize * sin((angle - Math.PI / 6)).toFloat()
    )
    val rightWing = Offset(
        end.x - arrowSize * cos((angle + Math.PI / 6)).toFloat(),
        end.y - arrowSize * sin((angle + Math.PI / 6)).toFloat()
    )
    drawLine(colorBlue, leftWing, end, strokeWidth = 6f, cap = StrokeCap.Round)
    drawLine(colorBlue, rightWing, end, strokeWidth = 6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawStraightAnimatedDots(
    progress: Float, value: Float, rawVector: Offset, start: Offset, end: Offset
) {
    val endOffset = if (value > 0) rawVector * 0.05f else -rawVector * 0.05f
    listOf(Triple(0.0f, 0.6f, 12f), Triple(0.1f, 0.7f, 9f), Triple(0.2f, 0.8f, 6f))
        .forEach { (s, e, size) ->
            if (progress in s..e) {
                val p = ((progress - s) / (e - s)).coerceIn(0f, 1f)
                drawCircle(colorBlue, size, center = start + (end - start - endOffset) * p, alpha = ALPHA)
            }
        }
}

private fun DrawScope.drawCornerAnimatedDots(progress: Float, value: Float, start: Offset, end: Offset) {
    val corner = Offset(start.x, end.y)
    val endOffset = if (value > 0) (end - corner) * 0.1f else -(end - corner) * 0.1f
    listOf(Triple(0.0f, 0.6f, 12f), Triple(0.1f, 0.7f, 9f), Triple(0.2f, 0.8f, 6f))
        .forEach { (animStart, animEnd, size) ->
            if (progress in animStart..animEnd) {
                val localP = ((progress - animStart) / (animEnd - animStart)).coerceIn(0f, 1f)
                val dot = if (localP < 0.5f) {
                    start + (corner - start) * (localP / 0.5f)
                } else {
                    corner + (end - corner - endOffset) * ((localP - 0.5f) / 0.5f)
                }
                drawCircle(colorBlue, size, center = dot, alpha = ALPHA)
            }
        }
}

private fun DrawScope.drawTextOnLine(text: String, x: Float, y: Float, onBackgroundColor: Int) {
    drawContext.canvas.nativeCanvas.drawText(
        text, x, y, Paint().apply {
            color = onBackgroundColor
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light")
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun DashboardPreview() {
    PvDataResolverTheme {
        EnergyScreen(values = dummyValues)
    }
}
