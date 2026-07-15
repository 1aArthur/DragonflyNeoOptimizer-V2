package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AIRecommendation
import com.example.data.database.OptimizerLog
import com.example.data.database.SystemMetric
import com.example.data.database.TweakConfig
import com.example.data.executor.PrivilegeMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.AIState
import com.example.ui.viewmodel.OptimizerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var onPermissionResult: (() -> Unit)? = null

    fun setOnPermissionResultCallback(callback: (() -> Unit)?) {
        onPermissionResult = callback
    }

    private val permissionListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, _ ->
        runOnUiThread {
            onPermissionResult?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {}
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainNavigationContainer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onPermissionResult?.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {}
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigationContainer(
    modifier: Modifier = Modifier,
    viewModel: OptimizerViewModel = viewModel()
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context as? MainActivity
        activity?.setOnPermissionResultCallback {
            viewModel.detectPermissions()
        }
        onDispose {
            activity?.setOnPermissionResultCallback(null)
        }
    }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val privilegeMode by viewModel.privilegeMode.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AppHeader(
                privilegeMode = privilegeMode,
                onRefreshMode = { viewModel.detectPermissions() }
            )

            // Content Area with Premium Fluid Transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (activeTab == "dashboard" || activeTab == "tweaks" && initialState == "settings") {
                            (slideInHorizontally(animationSpec = tween(220)) { width -> -width } + fadeIn()) with
                                    (slideOutHorizontally(animationSpec = tween(220)) { width -> width } + fadeOut())
                        } else {
                            (slideInHorizontally(animationSpec = tween(220)) { width -> width } + fadeIn()) with
                                    (slideOutHorizontally(animationSpec = tween(220)) { width -> -width } + fadeOut())
                        }
                    }
                ) { targetTab ->
                    when (targetTab) {
                        "dashboard" -> DashboardScreen(viewModel = viewModel)
                        "tweaks" -> TweaksScreen(viewModel = viewModel)
                        "ai" -> AIScreen(viewModel = viewModel)
                        "logs" -> LogsScreen(viewModel = viewModel)
                        "settings" -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }

            // Bottom Navigation Bar
            AppBottomNavBar(
                activeTab = activeTab,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
        }
    }
}

@Composable
fun AppHeader(
    privilegeMode: PrivilegeMode,
    onRefreshMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "DRAGONFLY",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberPrimary,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NeoOptimizer",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = " V2",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary
                )
            }
        }

        // Privilege Level Glowing Badge
        val (badgeText, badgeColor) = when (privilegeMode) {
            PrivilegeMode.ROOT -> "ROOT ACCESS" to CyberWarning
            PrivilegeMode.SHIZUKU -> "SHIZUKU MODE" to CyberTertiary
            PrivilegeMode.LIMITED -> "LIMITED" to CyberSecondary
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.1f))
                .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onRefreshMode() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .drawBehind {
                        drawCircle(
                            color = badgeColor,
                            alpha = 0.4f,
                            radius = size.minDimension * 1.5f
                        )
                    }
            )
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = badgeColor
            )
        }
    }
}

@Composable
fun AppBottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = CyberSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val navItems = listOf(
            Triple("dashboard", "Painel", Icons.Filled.Dashboard),
            Triple("tweaks", "Ajustes", Icons.Filled.Speed),
            Triple("ai", "IA Core", Icons.Filled.AutoAwesome),
            Triple("logs", "Term", Icons.Filled.History),
            Triple("settings", "Segurança", Icons.Filled.Shield)
        )

        navItems.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = activeTab == route,
                onClick = { onTabSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (activeTab == route) CyberPrimary else Color.White.copy(alpha = 0.4f)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == route) FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == route) CyberPrimary else Color.White.copy(alpha = 0.4f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyberPrimary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_btn_$route")
            )
        }
    }
}

@Composable
fun DashboardScreen(viewModel: OptimizerViewModel) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val tweaks by viewModel.tweaks.collectAsStateWithLifecycle()
    val privilegeMode by viewModel.privilegeMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val currentCpu = metrics.firstOrNull()?.cpuUsagePercent ?: 32f
    val currentRam = metrics.firstOrNull()?.ramUsagePercent ?: 58f
    val currentTemp = metrics.firstOrNull()?.batteryTempCelsius ?: 34.5f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image banner incorporating Dragonfly logo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CyberSurface, CyberBackground)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "ESTADO DO MOTOR",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Otimizador Seguro Ativo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dragonfly analisando métricas de latência, RAM e bateria continuamente.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Dragonfly circular logo display
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(1.dp, CyberPrimary.copy(alpha = 0.3f), CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Safe image representation
                        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync, // Decorative or actual custom image
                                contentDescription = "Logo",
                                tint = CyberPrimary,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Live Performance Metrics row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricIndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "CPU LOAD",
                    value = "${"%.1f".format(currentCpu)}%",
                    color = CyberPrimary,
                    icon = Icons.Filled.Memory,
                    progress = currentCpu / 100f
                )
                MetricIndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "RAM USED",
                    value = "${"%.1f".format(currentRam)}%",
                    color = CyberSecondary,
                    icon = Icons.Filled.Memory,
                    progress = currentRam / 100f
                )
                MetricIndicatorCard(
                    modifier = Modifier.weight(1f),
                    title = "BAT TEMP",
                    value = "${"%.1f".format(currentTemp)}°C",
                    color = if (currentTemp > 40f) CyberWarning else CyberTertiary,
                    icon = Icons.Filled.Thermostat,
                    progress = (currentTemp - 20f) / 30f
                )
            }
        }

        // Real-time animated system load chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTÓRICO DE CARGA EM TEMPO REAL",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Live Chart Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        MetricsChart(metrics = metrics)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberPrimary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CPU Load", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyberSecondary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RAM Usage", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Quick Boost central button
        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        tweaks.forEach { tweak ->
                            if (!tweak.isApplied) {
                                viewModel.applyTweak(tweak.key)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("quick_boost_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = "Boost")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACELERAÇÃO GERAL (QUICK BOOST)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Educational notice depending on mode
        item {
            val noticeText = when (privilegeMode) {
                PrivilegeMode.LIMITED -> "Você está rodando no modo limitado sem privilégios. Algumas otimizações de nível profundo requerem Shizuku ou root para serem executadas com sucesso."
                PrivilegeMode.SHIZUKU -> "Shizuku ativado com sucesso! Você tem permissões privilegiadas ADB completas. Otimizações de nível avançado estão prontas para rodar."
                PrivilegeMode.ROOT -> "Root ativado com sucesso! Permissões de superusuário completas e profundas liberadas para modificação de parâmetros de kernel."
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = when (privilegeMode) {
                            PrivilegeMode.LIMITED -> CyberSecondary.copy(alpha = 0.2f)
                            else -> CyberTertiary.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (privilegeMode == PrivilegeMode.LIMITED) Icons.Filled.Info else Icons.Filled.CheckCircle,
                        contentDescription = "Notice",
                        tint = if (privilegeMode == PrivilegeMode.LIMITED) CyberPrimary else CyberTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = noticeText,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun MetricsChart(metrics: List<SystemMetric>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (metrics.size < 2) return@Canvas

        val maxPoints = 15
        val reversedMetrics = metrics.take(maxPoints).reversed()
        val widthBetweenPoints = size.width / (maxPoints - 1)
        val height = size.height

        val cpuPath = Path()
        val ramPath = Path()

        reversedMetrics.forEachIndexed { index, systemMetric ->
            val x = index * widthBetweenPoints
            
            // CPU Point (y is calculated from top-left, so 100% load is at y=0, 0% is at y=height)
            val cpuY = height - (systemMetric.cpuUsagePercent / 100f * height)
            if (index == 0) {
                cpuPath.moveTo(x, cpuY)
            } else {
                cpuPath.lineTo(x, cpuY)
            }

            // RAM Point
            val ramY = height - (systemMetric.ramUsagePercent / 100f * height)
            if (index == 0) {
                ramPath.moveTo(x, ramY)
            } else {
                ramPath.lineTo(x, ramY)
            }
        }

        // Draw grids
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw lines
        drawPath(
            path = cpuPath,
            color = CyberPrimary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = ramPath,
            color = CyberSecondary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MetricIndicatorCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    progress: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            // Miniature premium linear indicator
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(600)
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = color,
                trackColor = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun TweaksScreen(viewModel: OptimizerViewModel) {
    val tweaks by viewModel.tweaks.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Tudo") }

    val categories = listOf("Tudo", "Performance", "Bateria", "Rede")
    val filteredTweaks = if (selectedCategory == "Tudo") {
        tweaks
    } else {
        tweaks.filter { it.category.lowercase() == selectedCategory.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "MOTOR DE AJUSTES NATIVO",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = CyberPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Otimizações de Sistema",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyberPrimary.copy(alpha = 0.15f) else CyberSurface)
                        .border(
                            1.dp,
                            if (isSelected) CyberPrimary else BorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CyberPrimary else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tweaks Scrollable List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTweaks.isEmpty()) {
                item {
                    EmptyTweaksState()
                }
            } else {
                items(filteredTweaks, key = { it.key }) { tweak ->
                    TweakItemCard(
                        tweak = tweak,
                        onApply = { viewModel.applyTweak(tweak.key) },
                        onRevert = { viewModel.revertTweak(tweak.key) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun EmptyTweaksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "Nenhum ajuste nesta categoria",
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TweakItemCard(
    tweak: TweakConfig,
    onApply: () -> Unit,
    onRevert: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (tweak.isApplied) CyberPrimary.copy(alpha = 0.3f) else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (tweak.category.lowercase()) {
                        "performance" -> Icons.Filled.Bolt
                        "bateria" -> Icons.Filled.BatteryChargingFull
                        "rede" -> Icons.Filled.NetworkCheck
                        else -> Icons.Filled.Speed
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (tweak.isApplied) CyberPrimary else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = tweak.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = tweak.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tweak.isApplied) CyberPrimary else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Switch micro-interaction
                Switch(
                    checked = tweak.isApplied,
                    onCheckedChange = { checked ->
                        if (checked) onApply() else onRevert()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberPrimary,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color.Black
                    ),
                    modifier = Modifier.testTag("tweak_switch_${tweak.key}")
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = tweak.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Valor: ${tweak.value} (Original: ${tweak.originalValue ?: "1.0"})",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberPrimary
                        )
                        val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(tweak.lastAppliedTime))
                        Text(
                            text = "Modificado: $date",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIScreen(viewModel: OptimizerViewModel) {
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()

    val currentCpu = metrics.firstOrNull()?.cpuUsagePercent ?: 42f
    val currentRam = metrics.firstOrNull()?.ramUsagePercent ?: 61f
    val currentTemp = metrics.firstOrNull()?.batteryTempCelsius ?: 33f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Banner header
        item {
            Column {
                Text(
                    text = "NÚCLEO DE INTELIGÊNCIA ARTIFICIAL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dragonfly AI Core",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Live stats analyzer card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ESTATÍSTICAS DA IA",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A IA analisa continuamente as flutuações de hardware para prescrever o melhor perfil de desempenho sem drenar bateria.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    
                    // Button to execute Gemini API scan
                    Button(
                        onClick = { viewModel.runFullAIAnalysis() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_scan_button"),
                        enabled = aiState !is AIState.Loading
                    ) {
                        if (aiState is AIState.Loading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("PROCESSANDO MÉTRICAS...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SOLICITAR RECOMENDAÇÃO INTELIGENTE (IA)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // AI Status messages
        item {
            when (val state = aiState) {
                is AIState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSecondary.copy(alpha = 0.05f))
                            .border(1.dp, CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Enviando telemetria (CPU: ${"%.1f".format(currentCpu)}%, RAM: ${"%.1f".format(currentRam)}%, Temp: ${"%.1f".format(currentTemp)}°C) para o modelo de baixa latência do Gemini...",
                            fontSize = 12.sp,
                            color = CyberPrimary
                        )
                    }
                }
                is AIState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberWarning.copy(alpha = 0.05f))
                            .border(1.dp, CyberWarning.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Falha na análise: ${state.error}",
                            fontSize = 12.sp,
                            color = CyberWarning
                        )
                    }
                }
                is AIState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberTertiary.copy(alpha = 0.05f))
                            .border(1.dp, CyberTertiary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Sucesso: O modelo Gemini Flash processou o perfil com sucesso!",
                            fontSize = 12.sp,
                            color = CyberTertiary
                        )
                    }
                }
                AIState.Idle -> {}
            }
        }

        // Recommendations List Title
        item {
            Text(
                text = "SUGESTÕES PRESCRITAS PELA IA",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // AI Recommendations
        if (recommendations.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
                        Text(
                            text = "Nenhuma prescrição de IA gerada ainda.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Clique no botão acima para coletar as telemetrias e gerar as otimizações por IA.",
                            fontSize = 10.sp,
                            color = TextSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recommendations) { rec ->
                RecommendationCard(
                    rec = rec,
                    onApply = { viewModel.applyAIRecommendation(rec) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun RecommendationCard(
    rec: AIRecommendation,
    onApply: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (rec.isApplied) CyberTertiary.copy(alpha = 0.3f) else CyberPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = CyberPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = rec.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Confidence indicator pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(rec.confidence * 100).toInt()}% Confiança",
                        fontSize = 9.sp,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = rec.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ajuste: ${rec.tweakKey} = ${rec.suggestedValue}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberPrimary
                )

                if (rec.isApplied) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(14.dp))
                        Text("APLICADO", fontSize = 10.sp, color = CyberTertiary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("apply_rec_${rec.id}")
                    ) {
                        Text("APLICAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LogsScreen(viewModel: OptimizerViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TERMINAL DE DIAGNÓSTICO",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Logs de Execução",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Clear logs button
            IconButton(
                onClick = { viewModel.clearLogHistory() },
                modifier = Modifier.testTag("clear_logs_button")
            ) {
                Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = "Clear", tint = CyberWarning)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Console-style list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "sh$ _ terminal ocioso.\nNenhum comando executado recentemente.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(logs) { log ->
                    ConsoleLogLine(log = log)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ConsoleLogLine(log: OptimizerLog) {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val textStyle = FontFamily.Monospace
    val color = when (log.status) {
        "SUCCESS" -> CyberTertiary
        "FAILURE" -> CyberWarning
        "REVERTED" -> CyberPrimary
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[$date]",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = textStyle
            )
            Text(
                text = log.status,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = textStyle
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "cmd$ ${log.actionName}",
            fontSize = 11.sp,
            color = Color.White,
            fontFamily = textStyle,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = log.details,
            fontSize = 10.sp,
            color = TextSecondary,
            fontFamily = textStyle,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Duração: ${log.durationMs}ms",
            fontSize = 9.sp,
            color = CyberSecondary,
            fontFamily = textStyle
        )
    }
}

@Composable
fun SettingsScreen(viewModel: OptimizerViewModel) {
    val tweaks by viewModel.tweaks.collectAsStateWithLifecycle()
    val privilegeMode by viewModel.privilegeMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showConfirmationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SEGURANÇA E RESTAURAÇÃO",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configurações de Segurança",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Whitelist Information
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SISTEMA DE FILTRO E SEGURANÇA",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "O DragonflyNeo V2 possui um sandbox de comando que impede injeção maliciosa de binários ou scripts destrutivos. Todas as ações passam por uma lista branca estrita que valida parâmetros e rejeita tentativas de adulteração do sistema operacional.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Emergency Rollback Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberWarning.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = CyberWarning, modifier = Modifier.size(18.dp))
                        Text(
                            text = "MODO DE EMERGÊNCIA (ROLLBACK)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberWarning
                        )
                    }

                    Text(
                        text = "Se você notar qualquer instabilidade ou comportamento indesejado em seu dispositivo Android, este botão irá reverter IMEDIATAMENTE todos os tweaks ativos de volta para os valores padrão de fábrica do Android.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showConfirmationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberWarning, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_rollback_button")
                    ) {
                        Text("DESFAZER TUDO (RESTAURAÇÃO COMPLETA)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Shizuku Integration Panel
        item {
            val isShizukuRunning by viewModel.isShizukuRunning.collectAsStateWithLifecycle()
            val hasShizukuPermission by viewModel.hasShizukuPermission.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (hasShizukuPermission) CyberTertiary.copy(alpha = 0.3f) else CyberWarning.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PAINEL DE INTEGRAÇÃO SHIZUKU",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isShizukuRunning) {
                        Text(
                            text = "Status: 🔴 DESCONECTADO (Mecanismo Shizuku não detectado).",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberWarning
                        )
                        Text(
                            text = "Certifique-se de que o Shizuku está instalado e iniciado via Wi-Fi Debugging ou ADB em seu aparelho antes de tentar usar.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    } else if (!hasShizukuPermission) {
                        Text(
                            text = "Status: 🟡 AUTORIZAÇÃO PENDENTE (O mecanismo está ativo, mas o app não autorizado).",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Yellow
                        )
                        Text(
                            text = "Clique no botão abaixo para abrir a tela de autorização do Shizuku e conceder acesso.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { viewModel.requestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AUTORIZAR SHIZUKU AGORA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        Text(
                            text = "Status: 🟢 CONECTADO E AUTORIZADO (Acesso via Shizuku ativo).",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTertiary
                        )
                        Text(
                            text = "Todas as otimizações do sistema serão executadas de forma instantânea com privilégios de sistema.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // About Shizuku / Root Education card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "INSTRUÇÕES DE PRIVILÉGIOS",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Shizuku: Permite que aplicativos de terceiros usem APIs do sistema diretamente com privilégios ADB via Wi-Fi Debugging.\n" +
                                "• Root (su): Fornece acesso administrativo total e irrestrito ao kernel Linux do Android para modificação de governadores de CPU.\n" +
                                "• Limited Fallback: Executa comandos seguros em nível de usuário. Garante utilidade do aplicativo mesmo em aparelhos sem privilégios.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Confirmation Modal Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = "Confirmar Restauração Completa?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja reverter todos os tweaks aplicados? Seus parâmetros do sistema serão restaurados instantaneamente para as configurações originais.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        coroutineScope.launch {
                            tweaks.forEach { tweak ->
                                if (tweak.isApplied) {
                                    viewModel.revertTweak(tweak.key)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberWarning, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RESTAURAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text("CANCELAR", color = Color.White)
                }
            },
            containerColor = CyberSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        )
    }
}
