package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import com.example.R
import com.example.BuildConfig
import kotlinx.coroutines.launch
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val CURRENT_APP_VERSION = "1.0.0"

// God Mode Color & Gradient palette extensions
val DarkToxicBg = Color(0xFF030A06) // Toxic forest black
val EmeraldGlow = Color(0xFF00FF88) // Glowing mint emerald neon
val DarkGlassCard = Color(0xCC0A1A0F) // Semi transparent glassy dark green-black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Collect central real-time AppState (Single Source of Truth)
    val appState by viewModel.appState.collectAsStateWithLifecycle()

    // Instantiate screen ViewModels representing clean MVVM structure
    val overviewViewModel = remember { OverviewViewModel(viewModel) }
    val productionViewModel = remember { ProductionViewModel(viewModel) }
    val stockViewModel = remember { StockViewModel(viewModel) }
    val workersViewModel = remember { WorkersViewModel(viewModel) }
    val biniyamAIViewModel = remember { BiniyamAIViewModel(viewModel) }

    val appContext = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    // Navigation selection: "Overview", "Production", "Stock", "Workers", "AI"
    var currentScreen by remember { mutableStateOf("Overview") }
    // Slide-out Drawer state
    var drawerOpen by remember { mutableStateOf(false) }

    // Dialog state hooks
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddMasterbatchDialog by remember { mutableStateOf(false) }
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var rawMaterialRecordDialogType by remember { mutableStateOf<String?>(null) } // "LD", "HD", "WASTE"

    if (showSplash) {
        AnwarSplashScreen(onFinished = { showSplash = false })
    } else {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(DarkToxicBg)
        ) {
            // Main Sliding Gesture detection for Side Drawer
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = DarkGlassCard.copy(alpha = 0.9f),
                            titleContentColor = Color.White
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldGlow, CircleShape)
                                        .shadow(4.dp, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ANWAR RECYCLE",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.5.sp,
                                    color = Color.White
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                drawerOpen = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Side Drawer",
                                    tint = EmeraldGlow
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(appContext, "ዳታ ቤዙ ከደመና ጋር ተመሳስሏል! (Database fully synchronized)", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "Cloud Connected Status",
                                    tint = EmeraldGlow
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    CustomBottomNavigationBar(
                        currentTab = currentScreen,
                        onTabSelected = { tab ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentScreen = tab
                        }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            // Swipe-right opens side drawer
                            if (dragAmount > 35f && !drawerOpen) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                drawerOpen = true
                            }
                        }
                    }
                ) {
                    ScreenContentTransition(
                        screen = currentScreen,
                        appState = appState,
                        overviewViewModel = overviewViewModel,
                        productionViewModel = productionViewModel,
                        stockViewModel = stockViewModel,
                        workersViewModel = workersViewModel,
                        biniyamAIViewModel = biniyamAIViewModel,
                        onAddProductTrigger = { showAddProductDialog = true },
                        onAddMasterbatchTrigger = { showAddMasterbatchDialog = true },
                        onAddRawMaterialTrigger = { type -> rawMaterialRecordDialogType = type }
                    )
                }
            }

            // Custom sliding drawer
            PremiumSideDrawer(
                isOpen = drawerOpen,
                appState = appState,
                onClose = { drawerOpen = false },
                onNavigate = { tab ->
                    currentScreen = tab
                    drawerOpen = false
                }
            )
        }
    }

    // Modal dialog insertions
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, size, color, pieces, weight, stock ->
                overviewViewModel.addNewProduct(name, size, color, 500, pieces, weight, stock)
                showAddProductDialog = false
            }
        )
    }

    if (showAddMasterbatchDialog) {
        AddMasterbatchDialog(
            onDismiss = { showAddMasterbatchDialog = false },
            onSave = { color, stock ->
                overviewViewModel.addNewMasterbatch(color, stock)
                showAddMasterbatchDialog = false
            }
        )
    }

    if (rawMaterialRecordDialogType != null) {
        RecordRawMaterialDialog(
            materialType = rawMaterialRecordDialogType!!,
            onDismiss = { rawMaterialRecordDialogType = null },
            onSave = { added, used ->
                overviewViewModel.recordRawMaterialActivity(rawMaterialRecordDialogType!!, used, added)
                rawMaterialRecordDialogType = null
            }
        )
    }
}

// Custom animated 5 tab Bottom navigation containing Ethiopian strings & animated icons
@Composable
fun CustomBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = DarkGlassCard.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF122C20))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabItem("Overview", "ማጠቃለያ", Icons.Default.Dashboard),
                TabItem("Production", "ምርት", Icons.Default.Factory),
                TabItem("Stock", "ክምችት", Icons.Default.Storage),
                TabItem("Workers", "ሰራተኞች", Icons.Default.People),
                TabItem("AI", "ቢኒያም AI", Icons.Default.Psychology)
            )

            tabs.forEach { tab ->
                val isActive = currentTab == tab.route
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.2f else 1.0f,
                    animationSpec = tween(300, easing = EaseInOutCirc),
                    label = "icon_scale"
                )
                val color by animateColorAsState(
                    targetValue = if (isActive) EmeraldGlow else Color(0xFF8C9E94),
                    animationSpec = tween(300),
                    label = "icon_color"
                )

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab.route) }
                        .weight(1f)
                        .testTag("nav_tab_${tab.route.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun ScreenContentTransition(
    screen: String,
    appState: AppState,
    overviewViewModel: OverviewViewModel,
    productionViewModel: ProductionViewModel,
    stockViewModel: StockViewModel,
    workersViewModel: WorkersViewModel,
    biniyamAIViewModel: BiniyamAIViewModel,
    onAddProductTrigger: () -> Unit,
    onAddMasterbatchTrigger: () -> Unit,
    onAddRawMaterialTrigger: (String) -> Unit
) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
            "Overview" -> OverviewScreenContent(appState, overviewViewModel, onAddProductTrigger, onAddMasterbatchTrigger, onAddRawMaterialTrigger)
            "Production" -> ProductionScreenContent(appState, productionViewModel)
            "Stock" -> StockScreenContent(appState, stockViewModel)
            "Workers" -> WorkersScreenContent(appState, workersViewModel)
            "AI" -> BiniyamAIScreenContent(appState, biniyamAIViewModel)
        }
    }
}

// CUSTOM SLIDING PREMIUM DRAWER
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PremiumSideDrawer(
    isOpen: Boolean,
    appState: AppState,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val drawerContext = androidx.compose.ui.platform.LocalContext.current
    if (isOpen) {
        // Scrim layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onClose() }
        ) {
            // Main sliding pane animation
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.78f)
                    .background(DarkGlassCard)
                    .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // ANWAR Large Logo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Brush.radialGradient(listOf(EmeraldGlow.copy(alpha = 0.4f), Color.Transparent)), CircleShape)
                                .border(BorderStroke(2.dp, EmeraldGlow), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "ANWAR PLUS",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Recycling Industry",
                                color = EmeraldGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Divider(color = Color(0xFF122C20))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Company Stats Summary Block
                    Text("ድርጅታዊ ስታትስቲክስ (Stats Summary)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatsSummaryTile("ጠቅላላ ሰራተኛ (Workers)", "${appState.workers.size} ንቁ")
                    StatsSummaryTile("የምርቶች አይነት (Products)", "${appState.products.size} አይነቶች")
                    StatsSummaryTile("የእቃ ክምችት (Stock bags)", "${appState.products.sumOf { it.currentStock }} ከረጢት")
                    StatsSummaryTile("ዛሬ የተመረተ (Today Produced)", "${appState.stats.totalFabricated} ከረጢት")

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color(0xFF122C20))
                    Spacer(modifier = Modifier.height(20.dp))

                    // Shortcuts/Quick Links
                    Text("ፈጣን መንገዶች (Quick Links)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    DrawerItem(Icons.Default.Dashboard, "ማጠቃለያ", "Overview", onNavigate)
                    DrawerItem(Icons.Default.Factory, "ምርት መዝግብ", "Production", onNavigate)
                    DrawerItem(Icons.Default.Storage, "የክምችት ማሳያ", "Stock", onNavigate)
                    DrawerItem(Icons.Default.People, "ሰራተኞች አስተዳድር", "Workers", onNavigate)
                    DrawerItem(Icons.Default.Psychology, "ቢኒያም AI ረዳት", "AI", onNavigate)

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(40.dp))

                    // App Version Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ANWAR APP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Version $CURRENT_APP_VERSION (stable)", color = Color(0xFF8C9E94), fontSize = 10.sp)
                        }
                        IconButton(onClick = {
                            Toast.makeText(drawerContext, "ከአካውንትዎ በትክክል ወጥተዋል! (Logged Out)", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Logout, "Logout", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, route: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(route) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color(0xFFE2E8F0), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatsSummaryTile(title: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFF8C9E94), fontSize = 12.sp)
        Text(valStr, color = EmeraldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// ==================== (PRIMARY SCREEN 1) OVERVIEW/HOME SCREEN ====================
@Composable
fun OverviewScreenContent(
    appState: AppState,
    viewModel: OverviewViewModel,
    onAddProduct: () -> Unit,
    onAddMasterbatch: () -> Unit,
    onRecordRawMaterial: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var liveTimeStr by remember { mutableStateOf("") }

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            liveTimeStr = format.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full width high-end hero card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(1.dp, Color(0xFF122C20))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "እለታዊ አጠቃላይ መረጃ",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ዛሬ፡ " + appState.selectedDate,
                                color = EmeraldGlow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Factory Live Status Pulsing Led
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (appState.factoryStatus == "ACTIVE") EmeraldGlow else Color.Red,
                                        CircleShape
                                    )
                                    .shadow(4.dp, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = liveTimeStr.ifBlank { "00:00:00" },
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "የፋብሪካው የስራ ሁኔታ (SCADA)፦",
                            color = Color(0xFF8C9E94),
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (appState.factoryStatus == "ACTIVE") "ACTIVE (በስራ ላይ)" else "IDLE (ማሽኖች ጠፍተዋል)",
                            color = if (appState.factoryStatus == "ACTIVE") EmeraldGlow else Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 4 metric cards in 2x2 grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeWorkersVal = appState.currentAttendance.count { it.status == "On Duty" }
                    val totalStockVal = appState.products.sumOf { it.currentStock }

                    OverviewMetricCard(
                        title = "የዛሬ ምርት",
                        subTitle = "Today Produced",
                        value = "${appState.stats.totalFabricated} ከረጢት",
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A1A0F), Color(0xFF030E07))),
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricCard(
                        title = "የዛሬ ሽያጭ",
                        subTitle = "Today Sold",
                        value = "${appState.stats.totalSold} ከረጢት",
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A1A0F), Color(0xFF030E07))),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val dutyCount = appState.currentAttendance.count { it.status == "On Duty" }
                    val stockSum = appState.products.sumOf { it.currentStock }

                    OverviewMetricCard(
                        title = "ንቁ ሰራተኞች",
                        subTitle = "On Duty Workers",
                        value = "$dutyCount ሰራተኛ",
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A1A0F), Color(0xFF030E07))),
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricCard(
                        title = "የቀረው ክምችት",
                        subTitle = "Total Stock Balance",
                        value = "$stockSum ማዳበሪያ",
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A1A0F), Color(0xFF030E07))),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Beautiful mini line chart detailing 7 days production trend
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(1.dp, Color(0xFF122C20))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ታሪካዊ ምርት ልኬት (7-Days Production Trend)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val trendPoints = remember(appState.allProductTransactions) {
                        calculateLast7DaysTrend(appState.allProductTransactions, appState.products)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            if (trendPoints.isNotEmpty()) {
                                val maxVal = (trendPoints.maxOrNull() ?: 1.0).coerceAtLeast(10.0)
                                val stepX = w / (trendPoints.size - 1).coerceAtLeast(1)
                                val path = Path()
                                val px40 = 40.dp.toPx()
                                val px10 = 10.dp.toPx()
                                val points = trendPoints.mapIndexed { idx, value ->
                                    val x = idx * stepX
                                    val y = h - (value / maxVal * (h - px40)).toFloat() - px10
                                    Offset(x, y)
                                }

                                path.moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    // Smooth bezier interpolation
                                    path.cubicTo(
                                        (prev.x + curr.x) / 2, prev.y,
                                        (prev.x + curr.x) / 2, curr.y,
                                        curr.x, curr.y
                                    )
                                }

                                // Draw line path
                                drawPath(
                                    path = path,
                                    color = EmeraldGlow,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Fill area underneath with bright neon vertical gradient
                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        listOf(EmeraldGlow.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )

                                // Draw glowing circles on vertex coordinates
                                points.forEach { pt ->
                                    drawCircle(
                                        color = EmeraldGlow,
                                        radius = 5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-7d", color = Color(0xFF8C9E94), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("-4d", color = Color(0xFF8C9E94), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("ትላንት", color = Color(0xFF8C9E94), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("ዛሬ", color = EmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Quick action floating buttons panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(1.dp, Color(0xFF122C20))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ፈጣን ድርጊቶች (Quick Actions Control)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverviewQuickButton(
                            label = "+ አዲስ ምርት",
                            color = Color(0xFF0F2618),
                            textColor = EmeraldGlow,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddProduct()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OverviewQuickButton(
                            label = "+ ጥሬ እቃ LD",
                            color = Color(0xFF0F1C26),
                            textColor = Color(0xFF7DD3FC),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRecordRawMaterial("LD")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OverviewQuickButton(
                            label = "+ ማስተርባች",
                            color = Color(0xFF261D0F),
                            textColor = BentoGold,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddMasterbatch()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Active Alerts Warning Strip
        item {
            val alerts = remember(appState) { calculateOverviewAlerts(appState) }
            if (alerts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF330C0C)),
                    border = BorderStroke(1.dp, Color(0xFFFF3B3B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning alerts",
                            tint = Color(0xFFFF3B3B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            alerts.take(2).forEach { alert ->
                                Text(
                                    text = "⚠️ $alert",
                                    color = Color(0xFFFF9494),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewMetricCard(
    title: String,
    subTitle: String,
    value: String,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(subTitle, color = Color(0xFF8C9E94), fontSize = 10.sp)
                }
                Text(
                    text = value,
                    color = EmeraldGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun OverviewQuickButton(
    label: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

fun calculateLast7DaysTrend(transactions: List<ProductTransaction>, products: List<Product>): List<Double> {
    val list = MutableList(7) { 0.0 }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    for (i in 0 until 7) {
        val targetDateStr = EthiopianCalendarHelper.shiftEthiopianDate(EthiopianCalendarHelper.getTodayEthiopianString(), i - 6)
        val fabricationToday = transactions.filter { it.date == targetDateStr }.sumOf { tr ->
            val p = products.find { it.id == tr.productId }
            val w = p?.bagWeightKg ?: 0.5
            tr.fabricated * w
        }
        list[i] = fabricationToday
    }
    return list
}

fun calculateOverviewAlerts(state: AppState): List<String> {
    val alerts = mutableListOf<String>()
    state.products.forEach { p ->
        if (p.currentStock < 15) {
            alerts.add("ምርት '${p.name}' ክምችት አልቋል! (${p.currentStock} ከረጢት ብቻ መረቱን ያሳያል)")
        }
    }
    state.rawMaterials.forEach { rm ->
        if (rm.currentStock < 350) {
            alerts.add("ጥሬ እቃ '${rm.type}' ክምችት ከ 350kg በታች ነው! (${rm.currentStock.toInt()}kg)")
        }
    }
    return alerts
}


// ==================== (PRIMARY SCREEN 2) PRODUCTION SCREEN ====================
@Composable
fun ProductionScreenContent(
    appState: AppState,
    viewModel: ProductionViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var editingProductForDailySheet by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Daily production static bar at top
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("እለታዊ አጠቃላይ የስራ ሉህ", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Today Production Metrics Summary", color = Color(0xFF8C9E94), fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ጠቅላላ የተመረተ", color = EmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${appState.stats.totalFabricated} ከረጢት", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ጠቅላላ የተሸጠ", color = BentoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${appState.stats.totalSold} ከረጢት", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("የተመዘገቡ የምርት አይነቶች (Product List)፦", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (appState.products.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("ምንም አልተመዘገበም... (No registered products)", color = Color(0xFF8C9E94), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appState.products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editingProductForDailySheet = product
                            }
                            .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Large product image placeholder / canvas vector
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFF0C1D13), RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.4f)), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(36.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    // Custom bag icon vector
                                    val path = Path().apply {
                                        moveTo(w * 0.3f, h * 0.2f)
                                        lineTo(w * 0.7f, h * 0.2f)
                                        lineTo(w * 0.85f, h * 0.8f)
                                        lineTo(w * 0.15f, h * 0.8f)
                                        close()
                                    }
                                    drawPath(path, color = EmeraldGlow, style = Stroke(width = 1.5.dp.toPx()))
                                    drawCircle(color = BentoGold, radius = 3.dp.toPx(), center = Offset(w / 2, h / 2))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "መጠን፡ ${product.size} | ቀለም፡ ${product.color} | ክብደት፡ ${product.bagWeightKg}kg",
                                    color = Color(0xFF8C9E94),
                                    fontSize = 12.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("አሁን ያለ ክምችት", color = Color(0xFF8C9E94), fontSize = 11.sp)
                                Text(
                                    text = "${product.currentStock} ማዳበሪያ",
                                    color = if (product.currentStock < 15) Color.Red else EmeraldGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingProductForDailySheet != null) {
        RecordProductActivityDialog(
            product = editingProductForDailySheet!!,
            onDismiss = { editingProductForDailySheet = null },
            onSave = { prodId, fab, sold, adj, note ->
                viewModel.recordProductDailyActivity(prodId, fab, sold, adj, note)
                editingProductForDailySheet = null
            },
            onDelete = { prod ->
                viewModel.deleteProduct(prod)
                editingProductForDailySheet = null
            }
        )
    }
}


// ==================== (PRIMARY SCREEN 3) STOCK SCREEN ====================
@Composable
fun StockScreenContent(
    appState: AppState,
    viewModel: StockViewModel
) {
    val haptic = LocalHapticFeedback.current
    var selectedSubTab by remember { mutableStateOf("RawMaterial") } // "RawMaterial", "Masterbatch", "ProductStock"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Swanky tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF030D06), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val subTabs = listOf(
                Pair("RawMaterial", "ጥሬ እቃ"),
                Pair("Masterbatch", "ማስተርባች"),
                Pair("ProductStock", "ምርት ክምችት")
            )
            subTabs.forEach { tab ->
                val isActive = selectedSubTab == tab.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) Color(0xFF0A1D11) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedSubTab = tab.first
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.second,
                        color = if (isActive) EmeraldGlow else Color(0xFF8C9E94),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedSubTab) {
            "RawMaterial" -> RawMaterialStockView(appState)
            "Masterbatch" -> MasterbatchStockView(appState, viewModel)
            "ProductStock" -> ProductStockGaugeView(appState)
        }
    }
}

@Composable
fun RawMaterialStockView(appState: AppState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        appState.rawMaterials.forEach { mat ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            when (mat.type.uppercase()) {
                                                "LD" -> Color(0xFF7DD3FC)
                                                "HD" -> EmeraldGlow
                                                else -> BentoGold
                                            }, CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ጥሬ እቃ (${mat.type.uppercase()})",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "ታርጌት፡ 3000 kg",
                                color = Color(0xFF8C9E94),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Radial Arc Gauge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 8.dp.toPx()
                                    val percent = (mat.currentStock / 3000.0).coerceIn(0.0, 1.0).toFloat()
                                    drawArc(
                                        color = Color(0xFF14241B),
                                        startAngle = -225f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = when (mat.type.uppercase()) {
                                            "LD" -> Color(0xFF7DD3FC)
                                            "HD" -> EmeraldGlow
                                            else -> BentoGold
                                        },
                                        startAngle = -225f,
                                        sweepAngle = 270f * percent,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "${((mat.currentStock / 3000.0) * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text("አሁን በክምችት ላይ ያለው መጠን፦", color = Color(0xFF8C9E94), fontSize = 12.sp)
                                Text(
                                    text = "${mat.currentStock.toInt()} ኪ.ግ (Kg)",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Small Sparkline showing imaginary transaction trend
                                SparklineTrendLine(
                                    materialTransactions = appState.allRawMaterialTransactions.filter { it.materialType == mat.type },
                                    color = when (mat.type.uppercase()) {
                                        "LD" -> Color(0xFF7DD3FC)
                                        "HD" -> EmeraldGlow
                                        else -> BentoGold
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterbatchStockView(appState: AppState, viewModel: StockViewModel) {
    if (appState.masterbatches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ምንም የቀለም ማስተርባች አልተመዘገበም... (No masterbatches)", color = Color(0xFF8C9E94), fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(appState.masterbatches) { mb ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(translateMasterbatchColor(mb.color), CircleShape)
                                    .border(BorderStroke(1.dp, Color.White), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(mb.color + " ቀለም ማስተርባች", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("አሁን ያለው ክምችት", color = Color(0xFF8C9E94), fontSize = 11.sp)
                            Text(
                                "${mb.currentStock.toInt()} ኪ.ግ (kg)",
                                color = EmeraldGlow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductStockGaugeView(appState: AppState) {
    if (appState.products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ምርት አልተመዘገበም...", color = Color(0xFF8C9E94), fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(appState.products) { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${p.currentStock}/1000 ከረጢት", color = Color(0xFF8C9E94), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // Linear glowing Gauge bar representing 0->1000 capacity
                        val capWidth = (p.currentStock / 1000.0).coerceIn(0.0, 1.0).toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color(0xFF0F1E14), RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(capWidth)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF00C6FF), EmeraldGlow)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .shadow(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SparklineTrendLine(materialTransactions: List<RawMaterialTransaction>, color: Color) {
    // Renders a simple, elegant Canvas Sparkline
    Canvas(
        modifier = Modifier
            .width(140.dp)
            .height(28.dp)
    ) {
        val w = size.width
        val h = size.height
        val fakePoints = if (materialTransactions.isEmpty()) {
            listOf(10.0, 25.0, 15.0, 40.0, 25.0, 60.0)
        } else {
            materialTransactions.map { it.added.coerceAtLeast(1.0) }.takeLast(8)
        }
        val maxVal = (fakePoints.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val stepX = w / (fakePoints.size - 1).coerceAtLeast(1)
        val path = Path()

        fakePoints.forEachIndexed { idx, value ->
            val cx = idx * stepX
            val cy = h - (value / maxVal * (h - 6.dp.toPx())).toFloat() - 3.dp.toPx()
            if (idx == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
        }
        drawPath(path, color = color.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))
    }
}

fun translateMasterbatchColor(str: String): Color {
    return when (str.lowercase().trim()) {
        "ቢጫ", "yellow" -> Color.Yellow
        "ቀይ", "red" -> Color.Red
        "ሰማያዊ", "blue" -> Color(0xFF3B82F6)
        "ጥቁር", "black" -> Color.Black
        "አረንጓዴ", "green" -> Color.Green
        else -> Color.White
    }
}


// ==================== (PRIMARY SCREEN 4) WORKERS SCREEN ====================
@Composable
fun WorkersScreenContent(
    appState: AppState,
    viewModel: WorkersViewModel
) {
    val haptic = LocalHapticFeedback.current
    var selectedWorkerTab by remember { mutableStateOf("Attendance") } // "Attendance", "Payroll"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Attendance/Payroll Tabswitcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF030D06), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            val tabs = listOf(Pair("Attendance", "መገኘት (Attendance)"), Pair("Payroll", "ደመወዝ (Payroll)"))
            tabs.forEach { tab ->
                val isActive = selectedWorkerTab == tab.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) Color(0xFF0A1D11) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedWorkerTab = tab.first
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.second,
                        color = if (isActive) EmeraldGlow else Color(0xFF8C9E94),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedWorkerTab == "Attendance") {
            WorkerAttendanceList(appState, viewModel)
        } else {
            WorkerPayrollDashboard(appState, viewModel)
        }
    }
}

@Composable
fun WorkerAttendanceList(appState: AppState, viewModel: WorkersViewModel) {
    val haptic = LocalHapticFeedback.current

    if (appState.workers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ምንም ሰራተኛ አልተመዘገበም... (No workers)", color = Color(0xFF8C9E94), fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(appState.workers) { worker ->
                val density = androidx.compose.ui.platform.LocalDensity.current
                val maxSwipePx = with(density) { 120.dp.toPx() }
                val triggerSwipePx = with(density) { 80.dp.toPx() }
                var dragOffset by remember { mutableStateOf(0f) }
                val currentStatusVal = appState.currentAttendance.find { it.workerId == worker.id }?.status ?: "የለም"

                // Swipe background card calculation
                val swipeBgColor = if (dragOffset > 0f) Color(0xFF0F381D) else if (dragOffset < 0f) Color(0xFF380F0F) else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(swipeBgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = if (dragOffset > 0) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dragOffset > 0) {
                            Text("👉 ON DUTY (ስራ ላይ)", color = EmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else if (dragOffset < 0) {
                            Text("ABSENT (ቀሪ) 👈", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .offset { IntOffset(dragOffset.roundToInt(), 0) }
                            .pointerInput(worker.id) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset = (dragOffset + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                                    },
                                    onDragEnd = {
                                        if (dragOffset > triggerSwipePx) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.recordAttendance(worker.id, "On Duty")
                                        } else if (dragOffset < -triggerSwipePx) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.recordAttendance(worker.id, "Absent")
                                        }
                                        dragOffset = 0f
                                    }
                                )
                            }
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar drawing with initials
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF0F2618), CircleShape)
                                    .border(BorderStroke(1.5.dp, EmeraldGlow), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = worker.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                                Text(
                                    text = initials.uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(worker.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "<< እዚህ ጋር ወደ ቀኝ ስላይድ (Duty) | ወደ ግራ (Absent) >>",
                                    color = Color(0xFF8C9E94),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (currentStatusVal) {
                                            "On Duty" -> Color(0xFF0F331D)
                                            "Absent" -> Color(0xFF330F0F)
                                            else -> Color(0xFF262626)
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (currentStatusVal == "On Duty") "በስራ ላይ" else if (currentStatusVal == "Absent") "Absent (ቀሪ)" else "ያልተሞላ",
                                    color = if (currentStatusVal == "On Duty") EmeraldGlow else if (currentStatusVal == "Absent") Color.Red else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerPayrollDashboard(appState: AppState, viewModel: WorkersViewModel) {
    val haptic = LocalHapticFeedback.current
    var editingSalaryWorker by remember { mutableStateOf<Worker?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Historical Total pay estimation
        val totalPay = appState.workers.sumOf { it.monthlySalary }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ጠቅላላ ወርሃዊ የደመወዝ ክፍያ (Payroll)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Total Month Salary Budget Estimation", color = Color(0xFF8C9E94), fontSize = 11.sp)
                }
                Text(
                    text = "${totalPay.toInt()} BIRR",
                    color = BentoGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text("የሰራተኞች የደመወዝ መግለጫ (Salary Setup):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(appState.workers) { worker ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            editingSalaryWorker = worker
                        }
                        .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(worker.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Click to customize rate", color = Color(0xFF8C9E94), fontSize = 11.sp)
                        }
                        Text(
                            text = "${worker.monthlySalary.toInt()} ETB/Month",
                            color = EmeraldGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    if (editingSalaryWorker != null) {
        var rateInput by remember { mutableStateOf(editingSalaryWorker!!.monthlySalary.toInt().toString()) }
        Dialog(onDismissRequest = { editingSalaryWorker = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(1.dp, Color(0xFF122C20))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("ደመወዝ ማስተካከያ (${editingSalaryWorker!!.name})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("ወርሃዊ ደመወዝ (Birr)", color = Color.White) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = Color(0xFF122C20),
                            unfocusedLabelColor = Color(0xFF8C9E94)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { editingSalaryWorker = null }) {
                            Text("ሰርዝ", color = Color.Red)
                        }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                            onClick = {
                                rateInput.toDoubleOrNull()?.let { doubleVal ->
                                    viewModel.updateWorkerSalary(editingSalaryWorker!!.id, doubleVal)
                                }
                                editingSalaryWorker = null
                            }
                        ) {
                            Text("አስቀምጥ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// ==================== (PRIMARY SCREEN 5) BINIYAM AI SCREEN ====================
data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val sender: String) // "user" or "model"

@Composable
fun BiniyamAIScreenContent(
    appState: AppState,
    viewModel: BiniyamAIViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isThinking by remember { mutableStateOf(false) }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "model",
                text = "ሰላም ጤና ይስጥልኝ! እኔ ቢኒያም (BINIYAM AI) እባላለሁ - የአንዋር ፕላስቲክ መልሶ ማምረቻ ኩባንያ (Anwar Plastic Recycle) ቀዳሚ ዲጂታል ረዳት። ዛሬ በምን ልርዳዎት?\n\nHello! I am BINIYAM, the official AI assistant of Anwar Plastic Recycle. Created by Biniyam, I am here to assist you manage and answer any questions regarding our production sheets, worker attendance, raw materials, and stock balances in both Amharic and English. How can I assist you today?"
            )
        )
    }

    var userInputText by remember { mutableStateOf("") }

    // TTS & Voice setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        val ttsInstance = TextToSpeech(context, listener)
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Voice launcher for speech to text
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                userInputText = spokenText
            }
        }
    }

    // Dynamic animation coordinates for Drift Network (Constellation Canvas)
    val infiniteTransition = rememberInfiniteTransition(label = "drift")
    val tick by infiniteTransition.animateFloat(
        initialValue =  0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    // Pre-declare static particles positioning offsetting on tick
    val particlesCount = 14
    val particles = remember {
        List(particlesCount) {
            Triple(
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat() * 1.5f + 0.5f
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Drifting background particles Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate active positions based on trigonometric offsets
            val pxList = particles.map { p ->
                val cx = p.first * w + 50.dp.toPx() * cos((tick * p.third).toDouble()).toFloat()
                val cy = p.second * h + 50.dp.toPx() * sin((tick * p.third).toDouble()).toFloat()
                Offset(cx.coerceIn(0f, w), cy.coerceIn(0f, h))
            }

            // Draw connecting links if distances < 200dp
            val threshold = 180.dp.toPx()
            for (i in pxList.indices) {
                for (j in i + 1 until pxList.size) {
                    val p1 = pxList[i]
                    val p2 = pxList[j]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist < threshold) {
                        val alpha = (1f - dist / threshold).coerceIn(0f, 1f) * 0.25f
                        drawLine(
                            color = EmeraldGlow,
                            start = p1,
                            end = p2,
                            strokeWidth = 1.dp.toPx(),
                            alpha = alpha
                        )
                    }
                }
            }

            // Draw glowing circles
            pxList.forEach { pt ->
                drawCircle(
                    color = EmeraldGlow,
                    radius = 4.dp.toPx(),
                    center = pt,
                    alpha = 0.4f
                )
            }
        }

        // Biniyam Chat UI container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Daily Auto-Briefing strip
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val reportMsg = buildEthiopianReportOverview(appState)
                        chatMessages.add(ChatMessage(sender = "model", text = reportMsg))
                        if (isTtsReady) {
                            tts?.speak(
                                "የእለት መረጃ ተዘጋጅቷል፡ " + reportMsg.take(150),
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                    .border(BorderStroke(1.dp, BentoGold.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = BentoGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("እለታዊ አውቶማቲክ ሪፖርት (Click for briefing)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("ቢኒያም AI - የዛሬ የፋብሪካ የስራ ማጠቃለያ", color = BentoGold, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.VolumeUp, null, tint = EmeraldGlow, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat Messages pane
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    val isModel = msg.sender == "model"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = if (isModel) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isModel) DarkGlassCard else Color(0xFF0F2B18),
                            border = BorderStroke(
                                1.dp,
                                if (isModel) Color(0xFF122C20) else EmeraldGlow.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                if (isModel) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    IconButton(
                                        onClick = {
                                            if (isTtsReady) {
                                                tts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isThinking) {
                Card(
                    modifier = Modifier.align(Alignment.Start),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard)
                ) {
                    Text("ቢኒያም AI እያሰበ ነው... (Thinking...)", color = EmeraldGlow, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Input Row & Voice Mic Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userInputText,
                    onValueChange = { userInputText = it },
                    placeholder = { Text("ቢኒያምን ጥያቄ ጠይቁት...", color = Color(0xFF8C9E94)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_input_text"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldGlow,
                        unfocusedBorderColor = Color(0xFF14241B)
                    )
                )

                // Sleek pulsing microphone ripple
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "ስለ አንዋር ሪሳይክል ይጠይቁ...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice input not ready", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF0F2B18), CircleShape)
                        .border(BorderStroke(1.5.dp, EmeraldGlow), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, "Voice Input", tint = EmeraldGlow)
                }

                IconButton(
                    onClick = {
                        if (userInputText.isNotBlank()) {
                            val userMsg = userInputText.trim()
                            chatMessages.add(ChatMessage(sender = "user", text = userMsg))
                            userInputText = ""
                            isThinking = true

                            // Launch asynchronous Gemini response retrieval
                            scope.launch {
                                val contextPrompt = buildContextPromptForGemini(appState)
                                val reply = GeminiBotService.getGeminiResponse(
                                    systemPrompt = contextPrompt,
                                    userPrompt = userMsg,
                                    history = chatMessages.map { Pair(it.sender, it.text) }
                                )
                                chatMessages.add(ChatMessage(sender = "model", text = reply))
                                isThinking = false

                                // Log AI query completed
                                viewModel.recordActivityLog("Biniyam AI", "Add", "ቢኒያም AI መልስ ሰጥቷል፡ $userMsg")
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(EmeraldGlow, CircleShape)
                ) {
                    Icon(Icons.Default.Send, "Send", tint = Color.Black)
                }
            }
        }
    }
}

fun buildEthiopianReportOverview(state: AppState): String {
    val totalProd = state.stats.totalFabricated
    val totalSold = state.stats.totalSold
    val activeWorkers = state.currentAttendance.count { it.status == "On Duty" }
    val stockLeft = state.products.sumOf { it.currentStock }

    return """
        📋 የቢኒያም AI ዕለታዊ አውቶማቲክ ሪፖርት (SCADA)
        ቀን፦ ${state.selectedDate} ዓ.ም.
        ክቡር ቢኒያም፣ የዛሬ የስራ ቀን የፋብሪካ እንቅስቃሴ አጠቃላይ ሪፖርት እንደሚከተለው ተጠናቅሯል፦
        1. ምርትና ሽያጭ፦
           - ጠቅላላ የተመረተ ምርት፦ $totalProd ከረጢት
           - ጠቅላላ የተሸጠ ምርት፦ $totalSold ከረጢት
        2. ሰራተኞች እና ክምችት፦
           - በስራ ገበታ ላይ የተገኙ፦ $activeWorkers ሰራተኞች
           - አጠቃላይ የክምችት መጠን፦ $stockLeft ማዳበሪያ
        ይህ ሪፖርት በቢኒያም AI በራስ-ሰር የተዘጋጀ ነው።
    """.trimIndent()
}

fun buildContextPromptForGemini(state: AppState): String {
    val sb = StringBuilder()
    sb.append("You are BINIYAM, the highly advanced AI oracle and SCADA assistant of Anwar Plastic Recycle factory in Addis Ababa, Ethiopia.\n")
    sb.append("You are created and owned by Biniyam, the supremo and chief administrator. Treat and mention Biniyam with deep respect and utmost pride as your creator and supreme admin.\n\n")
    sb.append("Current real time factory data state is listed below:\n")
    sb.append("Today's Ethiopian Date: ${state.selectedDate}\n")
    sb.append("Factory active status: ${state.factoryStatus}\n")
    sb.append("Registered Products count: ${state.products.size}\n")
    sb.append("Current raw materials state:\n")
    state.rawMaterials.forEach { rm ->
        sb.append("  - ${rm.type} stock: ${rm.currentStock} kg\n")
    }
    sb.append("Current product inventory totals:\n")
    state.products.forEach { p ->
        sb.append("  - ${p.name} (${p.color}): ${p.currentStock} bags\n")
    }
    sb.append("Daily summary stats: Today Produced: ${state.stats.totalFabricated} bags, Today Sold: ${state.stats.totalSold} bags.\n")
    sb.append("Return concise responses in Amharic preferably, paired with English if necessary, maintaining polite bilingual executive Oracle style.\n")
    return sb.toString()
}


// ==================== (DIALOG COMPONENT INPUTS) ====================
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, size: String, color: String, pieces: Int, weight: Double, stock: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var pieces by remember { mutableStateOf("100") }
    var weight by remember { mutableStateOf("0.5") }
    var stock by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ማስመዝገቢያ ፦ አዲስ ምርት ምዝገባ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("የምርቱ ስም (Product Name)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("መጠን (Size/Dimensions)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("ቀለም (Color)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("ክብደት (Bag Weight in Kg)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("የመነሻ ክምችት (Initial Stock Bags)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("ሰርዝ", color = Color.Red) }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                        onClick = {
                            if (name.isNotEmpty()) {
                                onSave(
                                    name,
                                    size,
                                    color,
                                    pieces.toIntOrNull() ?: 100,
                                    weight.toDoubleOrNull() ?: 0.5,
                                    stock.toIntOrNull() ?: 0
                                )
                            }
                        }
                    ) {
                        Text("መዝግብ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordProductActivityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (productId: Int, fabricated: Int, sold: Int, adjusted: Int, notes: String) -> Unit,
    onDelete: (Product) -> Unit
) {
    var fabricated by remember { mutableStateOf("0") }
    var sold by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("የእለት ሉህ መመዝገቢያ ፦ ${product.name}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)

                OutlinedTextField(
                    value = fabricated,
                    onValueChange = { fabricated = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("የተመረተ (Fabricated Bags)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                OutlinedTextField(
                    value = sold,
                    onValueChange = { sold = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("የተሸጠ (Sold Bags)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ተጨማሪ ማስታወሻ (Notes)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDelete(product) }) {
                        Icon(Icons.Default.Delete, "Delete Product", tint = Color.Red)
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("ሰርዝ", color = Color.Red) }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                            onClick = {
                                onSave(
                                    product.id,
                                    fabricated.toIntOrNull() ?: 0,
                                    sold.toIntOrNull() ?: 0,
                                    0,
                                    note
                                )
                            }
                        ) {
                            Text("አስቀምጥ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMasterbatchDialog(
    onDismiss: () -> Unit,
    onSave: (color: String, initialStock: Double) -> Unit
) {
    var color by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("ማስመዝገቢያ ፦ አዲስ ማስተርባች (ቀለም)", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("አዲስ ቀለም (Color)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("የመነሻ ክብደት (Weight in kg)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("ሰርዝ", color = Color.Red) }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                        onClick = {
                            if (color.isNotEmpty()) {
                                onSave(color, stock.toDoubleOrNull() ?: 0.0)
                            }
                        }
                    ) {
                        Text("መዝግብ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRawMaterialDialog(
    materialType: String,
    onDismiss: () -> Unit,
    onSave: (added: Double, used: Double) -> Unit
) {
    var added by remember { mutableStateOf("0") }
    var used by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(1.dp, Color(0xFF122C20))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("ጥሬ እቃ ማህደር ፦ $materialType", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                OutlinedTextField(
                    value = added,
                    onValueChange = { added = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("አዲስ የገባ መጠን (Added In Kg)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )
                OutlinedTextField(
                    value = used,
                    onValueChange = { used = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("ለማሽን የተሰጠ (Used In Kg)", color = Color.White) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGlow, unfocusedBorderColor = Color(0xFF122C20))
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("ሰርዝ", color = Color.Red) }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                        onClick = {
                            onSave(added.toDoubleOrNull() ?: 0.0, used.toDoubleOrNull() ?: 0.0)
                        }
                    ) {
                        Text("መዝግብ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ==================== (SPLASH ENTRANCE COMPONENT) ====================
@Composable
fun AnwarSplashScreen(onFinished: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scaleAnim = remember { Animatable(0.2f) }
    val opacityAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Spring bounce up the scaling
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            opacityAnim.animateTo(1f, animationSpec = tween(1200))
        }
        kotlinx.coroutines.delay(2600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkToxicBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                scaleX = scaleAnim.value,
                scaleY = scaleAnim.value,
                alpha = opacityAnim.value
            )
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Brush.radialGradient(listOf(EmeraldGlow.copy(alpha = 0.35f), Color.Transparent)), CircleShape)
                    .border(BorderStroke(2.dp, EmeraldGlow), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ANWAR RECYCLE",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 24.sp,
                letterSpacing = 2.5.sp
            )
            Text(
                text = "Official Factory Controller v1.0.0",
                color = EmeraldGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun toAmharicName(name: String): String {
    val clean = name.trim()
    if (clean.any { it.code in 0x1200..0x137F }) {
        return clean
    }
    val lower = clean.lowercase()
    if (lower.contains("abebe") && lower.contains("kebede")) return "አበበ ከበደ"
    if (lower.contains("anwar") && lower.contains("adem")) return "አንዋር አደም"
    if (lower.contains("chala") && lower.contains("gerba")) return "ቻላ ገርባ"
    if (lower.contains("soliana") && lower.contains("yared")) return "ሶሊያና ያሬድ"

    val dict = mapOf(
        "abebe" to "አበበ", "kebede" to "ከበደ", "anwar" to "አንዋር", "adem" to "አደም",
        "chala" to "ቻላ", "gerba" to "ገርባ", "soliana" to "ሶሊያና", "yared" to "ያሬድ",
        "almaz" to "አልማዝ", "aster" to "አስቴር", "bekele" to "በቀለ", "tesfaye" to "ተስፋዬ",
        "mesfin" to "መስፍን", "lema" to "ለማ", "hailu" to "ኃይሉ", "girma" to "ግርማ"
    )
    val parts = lower.split("\\s+".toRegex())
    val translatedParts = parts.map { part ->
         dict[part] ?: part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    return translatedParts.joinToString(" ")
}

object GeminiBotService {
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun getGeminiResponse(
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>>
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY") {
            return@withContext "የቢኒያም AI ቁልፍ አልተገኘም (Gemini API Key is missing in AI Studio Secrets)።"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        try {
            val requestJson = org.json.JSONObject()
            val contentsArr = org.json.JSONArray()

            // Map chat history safely
            history.forEach { item ->
                val turnObj = org.json.JSONObject()
                val roleStr = if (item.first == "user") "user" else "model"
                turnObj.put("role", roleStr)
                
                val partsArr = org.json.JSONArray()
                val partObj = org.json.JSONObject()
                partObj.put("text", item.second)
                partsArr.put(partObj)
                
                turnObj.put("parts", partsArr)
                contentsArr.put(turnObj)
            }

            // Append newest user message
            val newestTurn = org.json.JSONObject()
            newestTurn.put("role", "user")
            val newestParts = org.json.JSONArray()
            val newestPart = org.json.JSONObject()
            newestPart.put("text", userPrompt)
            newestParts.put(newestPart)
            newestTurn.put("parts", newestParts)
            contentsArr.put(newestTurn)

            requestJson.put("contents", contentsArr)

            if (systemPrompt.isNotBlank()) {
                val systemInstructionObj = org.json.JSONObject()
                val sPartsArr = org.json.JSONArray()
                val sPartObj = org.json.JSONObject()
                sPartObj.put("text", systemPrompt)
                sPartsArr.put(sPartObj)
                systemInstructionObj.put("parts", sPartsArr)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            // Force dynamic response configurations
            val configObj = org.json.JSONObject()
            configObj.put("temperature", 0.5f)
            requestJson.put("generationConfig", configObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "ስህተት ተፈጥሯል (Error Code: ${response.code})። እባክዎን ቆይተው እንደገና ይሞክሩ።\nConnection failed with code: ${response.code}."
                }

                val bodyStr = response.body?.string() ?: return@withContext "Error: System received an empty response body"
                val responseJson = org.json.JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val partsList = contentObj?.optJSONArray("parts")
                    if (partsList != null && partsList.length() > 0) {
                        return@withContext partsList.getJSONObject(0).optString("text", "No readable reply received.")
                    }
                }
                "ቢኒያም መልስ መስጠት አልቻለም (Unexpected payload format)።"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "ለመገናኘት አልተቻለም (Connection Failed): ${e.localizedMessage ?: "አልታወቀም "}"
        }
    }
}
