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
import androidx.compose.ui.unit.TextUnit
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
val DarkToxicBg = Color(0xFF000000) // Pure void black #000000
val EmeraldGlow = Color(0xFF00FF88) // Glowing mint emerald neon
val DarkGlassCard = Color(0xFF050505) // Pure black card background #050505

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
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                ambientColor = EmeraldGlow,
                spotColor = EmeraldGlow
            ),
        color = DarkGlassCard,
        border = BorderStroke(1.dp, EmeraldGlow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(EmeraldGlow, CircleShape)
                                    .shadow(4.dp, CircleShape)
                            )
                        }
                    }
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
        AnimateCountUpText(
            valueString = valStr,
            color = Color(0xFFFFD700), // Premium cosmic gold
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ==================== (PRIMARY SCREEN 1) OVERVIEW/HOME SCREEN ====================
data class PremiumAlert(val text: String, val type: AlertType)
enum class AlertType { CRITICAL_STOCK, SALARY_COUNTDOWN }

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
            val format = SimpleDateFormat("hh:mm:ss a", Locale.US)
            liveTimeStr = format.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // Dynamic animation coordinates for Drift Network Background
    val infiniteTransition = rememberInfiniteTransition(label = "overview_drift")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    // Pre-declare static drift coordinates (18 premium nodes)
    val particlesCount = 18
    val particles = remember {
        List(particlesCount) {
            Triple(
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat() * 1.4f + 0.4f
            )
        }
    }

    // Typewriter state management
    var typewriterText by remember { mutableStateOf("") }
    val typewriterPhrases = listOf(
        "OPERATING EFFICIENTLY",
        "LEADING WITH INNOVATION",
        "የፕላስቲክ ምርት ቁጥጥር",
        "ANWAR FACTORY SYSTEM"
    )

    LaunchedEffect(Unit) {
        var phraseIndex = 0
        while (true) {
            val word = typewriterPhrases[phraseIndex]
            // Type characters forward
            for (i in 0..word.length) {
                typewriterText = word.substring(0, i)
                kotlinx.coroutines.delay(75)
            }
            kotlinx.coroutines.delay(1500) // Pause on complete word
            // Backspace delete
            for (i in word.length downTo 0) {
                typewriterText = word.substring(0, i)
                kotlinx.coroutines.delay(35)
            }
            kotlinx.coroutines.delay(400)
            phraseIndex = (phraseIndex + 1) % typewriterPhrases.size
        }
    }

    // Entrance Animation coordination scales via graphicsLayer
    var animateEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntrance = true
    }

    val heroAlpha by animateFloatAsState(targetValue = if (animateEntrance) 1f else 0f, animationSpec = tween(650, delayMillis = 100), label = "hero_a")
    val heroSlide by animateFloatAsState(targetValue = if (animateEntrance) 0f else 70f, animationSpec = tween(650, delayMillis = 100), label = "hero_s")

    val metricsAlpha by animateFloatAsState(targetValue = if (animateEntrance) 1f else 0f, animationSpec = tween(650, delayMillis = 280), label = "metrics_a")
    val metricsSlide by animateFloatAsState(targetValue = if (animateEntrance) 0f else 70f, animationSpec = tween(650, delayMillis = 280), label = "metrics_s")

    val chartAlpha by animateFloatAsState(targetValue = if (animateEntrance) 1f else 0f, animationSpec = tween(650, delayMillis = 440), label = "chart_a")
    val chartSlide by animateFloatAsState(targetValue = if (animateEntrance) 0f else 70f, animationSpec = tween(650, delayMillis = 440), label = "chart_s")

    val alertsAlpha by animateFloatAsState(targetValue = if (animateEntrance) 1f else 0f, animationSpec = tween(650, delayMillis = 600), label = "alerts_a")
    val alertsSlide by animateFloatAsState(targetValue = if (animateEntrance) 0f else 70f, animationSpec = tween(650, delayMillis = 600), label = "alerts_s")

    // Retrieve active counts
    val isFactoryActive = appState.factoryStatus == "ACTIVE" || appState.stats.totalFabricated > 0
    val dutyCount = appState.currentAttendance.count { it.status == "On Duty" }
    val stockSum = appState.products.sumOf { it.currentStock }

    // Parse alerts
    val premiumAlerts = remember(appState) {
        val list = mutableListOf<PremiumAlert>()
        
        // Critical stock alerts (product stock < 15)
        appState.products.forEach { p ->
            if (p.currentStock < 15) {
                list.add(PremiumAlert("የምርት '${p.name}' ክምችት አልቋል! (${p.currentStock} ከረጢት)", AlertType.CRITICAL_STOCK))
            }
        }
        
        // Critical raw material alerts (rm < 350)
        appState.rawMaterials.forEach { rm ->
            if (rm.currentStock < 350) {
                list.add(PremiumAlert("የጥሬ እቃ '${rm.type}' ክምችት እጅግ ዝቅተኛ ነው! (${rm.currentStock.toInt()}kg ቀርቷል)", AlertType.CRITICAL_STOCK))
            }
        }

        // Salary countdown (usually end of Ethiopian month - day 30)
        val parts = appState.selectedDate.split("-")
        val currentDay = parts.getOrNull(2)?.toIntOrNull() ?: 1
        if (currentDay <= 30) {
            val daysToSalary = 30 - currentDay
            if (daysToSalary == 0) {
                list.add(PremiumAlert("የዛሬ ቀን ታላቁ የሰራተኞች ደሞዝ መክፈያ እለት ነው! ([ደሞዝ ክፈት/PAY ACTIVE])", AlertType.SALARY_COUNTDOWN))
            } else if (daysToSalary <= 10) {
                list.add(PremiumAlert("የደመወዝ ዝግጅት መቁጠሪያ፡ ለክፍያ ቀን $daysToSalary ቀናት ቀርተዋል!", AlertType.SALARY_COUNTDOWN))
            }
        }
        
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Animated circuit particle background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pxList = particles.map { p ->
                val cx = p.first * w + 35.dp.toPx() * cos((tick * p.third).toDouble()).toFloat()
                val cy = p.second * h + 35.dp.toPx() * sin((tick * p.third).toDouble()).toFloat()
                Offset(cx.coerceIn(0f, w), cy.coerceIn(0f, h))
            }

            // Draw link connections
            val threshold = 180.dp.toPx()
            for (i in pxList.indices) {
                for (j in i + 1 until pxList.size) {
                    val p1 = pxList[i]
                    val p2 = pxList[j]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist < threshold) {
                        val alpha = (1f - dist / threshold).coerceIn(0f, 1f) * 0.15f
                        drawLine(
                            color = Color(0xFF00FF88),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.dp.toPx(),
                            alpha = alpha
                        )
                    }
                }
            }

            // Draw circuit dot junctions
            pxList.forEach { pt ->
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = 3.dp.toPx(),
                    center = pt,
                    alpha = 0.38f
                )
            }
        }

        // 2. Main scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = heroAlpha, translationY = heroSlide)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Small green eyebrow
                    Text(
                        text = "የፋብሪካ አሠራር ቁጥጥር ስርዓት",
                        color = Color(0xFF00FF88),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Big glowing title
                    Text(
                        text = buildAnnotatedString {
                            append("ANWAR ")
                            withStyle(style = SpanStyle(color = Color(0xFF00FF88), fontWeight = FontWeight.Black)) {
                                append("PLASTIC")
                            }
                            append(" RECYCLE")
                        },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.shadow(
                            elevation = 8.dp,
                            spotColor = Color(0xFF00FF88),
                            ambientColor = Color(0xFF00FF88)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Typewriter Console strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF041008))
                            .border(BorderStroke(0.8.dp, Color(0xFF00FF88).copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = typewriterText,
                            color = Color(0xFF00FF88),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        
                        // Blinking terminal cursor
                        val cursorTransition = rememberInfiniteTransition(label = "cursor")
                        val cursorAlpha by cursorTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(400, easing = EaseInOutSine), RepeatMode.Reverse),
                            label = "cursor_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .height(13.dp)
                                .offset(x = 3.dp)
                                .background(Color(0xFF00FF88).copy(alpha = cursorAlpha))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pill-shaped status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF09140C))
                            .border(BorderStroke(1.dp, if (isFactoryActive) Color(0xFF00FF88).copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)), RoundedCornerShape(100.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Pulsing led
                        val ledTransition = rememberInfiniteTransition(label = "led")
                        val ledScale by ledTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
                            label = "led_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .graphicsLayer(scaleX = ledScale, scaleY = ledScale)
                                .background(if (isFactoryActive) Color(0xFF00FF88) else Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFactoryActive) "FACTORY ACTIVE · በስራ ላይ" else "FACTORY IDLE · ቆሟል",
                            color = if (isFactoryActive) Color(0xFF00FF88) else Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Metric Cards Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = metricsAlpha, translationY = metricsSlide),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OverviewMetricCardGodMode(
                            label = "የዛሬ ምርት",
                            subLabel = "Today's Production",
                            valueKey = "${appState.stats.totalFabricated} ከረጢት",
                            numberColor = Color(0xFF00FF88),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewMetricCardGodMode(
                            label = "የዛሬ ሽያጭ",
                            subLabel = "Today's Sales",
                            valueKey = "${appState.stats.totalSold} ከረጢት",
                            numberColor = Color(0xFFFFD700),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OverviewMetricCardGodMode(
                            label = "ንቁ ሰራተኞች",
                            subLabel = "On Duty Workers",
                            valueKey = "$dutyCount ሰራተኛ",
                            numberColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewMetricCardGodMode(
                            label = "ጠቅላላ ክምችት",
                            subLabel = "Total Stock Balance",
                            valueKey = "$stockSum ማዳበሪያ",
                            numberColor = Color(0xFF00FF88),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Chart Section
            item {
                val trendPoints = remember(appState.allProductTransactions) {
                    calculateLast7DaysTrend(appState.allProductTransactions, appState.products)
                }
                val maxVal = remember(trendPoints) {
                    (trendPoints.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = chartAlpha, translationY = chartSlide)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF00FF88).copy(alpha = 0.1f), spotColor = Color(0xFF00FF88).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                    border = BorderStroke(0.8.dp, Color(0xFF333333))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "ታሪካዊ ምርት ልኬት (7-Days Production)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom Bar Chart Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val labels = getAmharicWeekdays()
                            trendPoints.forEachIndexed { index, value ->
                                val barHeightFraction = if (maxVal > 0) (value / maxVal).toFloat() else 0f
                                val animatedHeightFraction by animateFloatAsState(
                                    targetValue = barHeightFraction,
                                    animationSpec = tween(durationMillis = 800 + index * 80, easing = EaseOutQuad),
                                    label = "bar_$index"
                                )
                                
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "${value.toInt()}",
                                        color = if (index == 6) Color(0xFF00FF88) else Color.White.copy(alpha = 0.75f),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .width(18.dp)
                                            .height(95.dp * animatedHeightFraction.coerceAtLeast(0.01f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF00FF88), Color(0xFF042915))
                                                )
                                            )
                                            .border(
                                                BorderStroke(0.5.dp, if (index == 6) Color(0xFF00FF88) else Color(0xFF00FF88).copy(alpha = 0.3f)),
                                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = labels.getOrNull(index) ?: "",
                                        color = if (index == 6) Color(0xFF00FF88) else Color(0xFF8C9E94),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick actions controller card (Optional actions helper)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                    border = BorderStroke(0.8.dp, Color(0xFF222222))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ፈጣን የክትትል የድርጊት መቆጣጠሪያ (Quick Controls)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OverviewQuickButton(
                                label = "+ አዲስ ምርት",
                                color = Color(0xFF041007),
                                textColor = Color(0xFF00FF88),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddProduct()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OverviewQuickButton(
                                label = "+ ጥሬ እቃ LD",
                                color = Color(0xFF030D14),
                                textColor = Color(0xFF00E5FF),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRecordRawMaterial("LD")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OverviewQuickButton(
                                label = "+ ማስተርባች",
                                color = Color(0xFF141003),
                                textColor = Color(0xFFFFD700),
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

            // Alerts Strip
            item {
                if (premiumAlerts.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = alertsAlpha, translationY = alertsSlide),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⚡ የስርዓት ማሳሰቢያዎች (Smart Alerts System)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        premiumAlerts.forEach { alert ->
                            val isCritical = alert.type == AlertType.CRITICAL_STOCK
                            val bgColor = if (isCritical) Color(0xFF1E0606) else Color(0xFF1E1706)
                            val borderGlow = if (isCritical) Color(0xFFFF3B3B) else Color(0xFFFFD700)
                            val textColor = if (isCritical) Color(0xFFFF9494) else Color(0xFFFDE047)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = borderGlow, spotColor = borderGlow),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                border = BorderStroke(1.dp, borderGlow.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = "Alert Indicator",
                                        tint = borderGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = alert.text,
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scroll Hint indicator
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SCROLL",
                        color = Color(0xFF00FF88).copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val scrollTransition = rememberInfiniteTransition(label = "scroll_glow")
                    val lineOffset by scrollTransition.animateFloat(
                        initialValue = -10f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "offset"
                    )

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(30.dp)
                            .background(Color(0xFF222222))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (10.dp + lineOffset.dp))
                                .width(2.dp)
                                .height(10.dp)
                                .background(Color(0xFF00FF88))
                        )
                    }
                }
            }
        }
    }
}

// Helpers
fun getAmharicWeekdays(): List<String> {
    val days = listOf("እሁ", "ሰኞ", "ማክ", "ረቡ", "ሐሙ", "አርብ", "ቅዳ")
    val todayWeekDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val result = mutableListOf<String>()
    for (i in 0 until 7) {
        val calculatedIdx = (todayWeekDay - 1 - (6 - i) + 14) % 7
        result.add(days[calculatedIdx])
    }
    return result
}

@Composable
fun OverviewMetricCardGodMode(
    label: String,
    subLabel: String,
    valueKey: String,
    numberColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = numberColor.copy(alpha = 0.15f),
                spotColor = numberColor.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
        border = BorderStroke(0.8.dp, Color(0xFF222222))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Modern Top Shine Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                numberColor.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subLabel,
                        color = Color(0xFF8C9E94),
                        fontSize = 10.sp
                    )
                }

                AnimateCountUpText(
                    valueString = valueKey,
                    color = numberColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

fun calculateLast7DaysTrend(transactions: List<ProductTransaction>, products: List<Product>): List<Double> {
    val list = MutableList(7) { 0.0 }
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
    var selectedWorkerTab by remember { mutableStateOf("Attendance") } // "Attendance", "Salary"
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var editingSalaryWorker by remember { mutableStateOf<Worker?>(null) }

    // Animated green particle network background
    val infiniteTransition = rememberInfiniteTransition(label = "workers_drift")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    val particlesCount = 15
    val particles = remember {
        List(particlesCount) {
            Triple(
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat() * 1.5f + 0.3f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Drifting Network Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pxList = particles.map { p ->
                val cx = p.first * w + 30.dp.toPx() * cos((tick * p.third).toDouble()).toFloat()
                val cy = p.second * h + 30.dp.toPx() * sin((tick * p.third).toDouble()).toFloat()
                Offset(cx.coerceIn(0f, w), cy.coerceIn(0f, h))
            }

            // Draw links
            val threshold = 160.dp.toPx()
            for (i in pxList.indices) {
                for (j in i + 1 until pxList.size) {
                    val p1 = pxList[i]
                    val p2 = pxList[j]
                    val dist = sqrt(((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)).toDouble()).toFloat()
                    if (dist < threshold) {
                        val alpha = (1f - dist / threshold).coerceIn(0f, 1f) * 0.12f
                        drawLine(
                            color = Color(0xFF00FF88),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.dp.toPx(),
                            alpha = alpha
                        )
                    }
                }
            }

            // Draw points
            pxList.forEach { pt ->
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = 2.5.dp.toPx(),
                    center = pt,
                    alpha = 0.3f
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050505), RoundedCornerShape(100.dp))
                    .border(BorderStroke(1.dp, Color(0xFF222222)), RoundedCornerShape(100.dp))
                    .padding(4.dp)
            ) {
                val tabs = listOf(Pair("Attendance", "መገኘት (Attendance)"), Pair("Salary", "ደመወዝ (Salary)"))
                tabs.forEach { tab ->
                    val isActive = selectedWorkerTab == tab.first
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isActive) Color(0xFF041c0d) else Color.Transparent)
                            .border(
                                BorderStroke(0.8.dp, if (isActive) Color(0xFF00FF88).copy(alpha = 0.6f) else Color.Transparent),
                                RoundedCornerShape(100.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedWorkerTab = tab.first
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.second,
                            color = if (isActive) Color(0xFF00FF88) else Color(0xFF8C9E94),
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedWorkerTab == "Attendance") {
                WorkersAttendanceTab(
                    appState = appState,
                    viewModel = viewModel,
                    onAddClicked = { showAddWorkerDialog = true }
                )
            } else {
                WorkersSalaryTab(
                    appState = appState,
                    viewModel = viewModel,
                    onEditSalaryClicked = { editingSalaryWorker = it }
                )
            }
        }
    }

    // Add Worker Dialog
    if (showAddWorkerDialog) {
        var workerName by remember { mutableStateOf("") }
        var salaryInput by remember { mutableStateOf("10000") }

        Dialog(onDismissRequest = { showAddWorkerDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "አዲስ ሰራተኛ መመዝገቢያ (New Worker)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    OutlinedTextField(
                        value = workerName,
                        onValueChange = { workerName = it },
                        label = { Text("ሙሉ ስም (Full Name)", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("ወርሃዊ ደመወዝ (Birr)", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddWorkerDialog = false }) {
                            Text("ሰርዝ", color = Color(0xFFFF3B3B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
                            onClick = {
                                if (workerName.isNotBlank()) {
                                    viewModel.addNewWorker(workerName.trim(), salaryInput.toDoubleOrNull() ?: 10000.0)
                                }
                                showAddWorkerDialog = false
                            }
                        ) {
                            Text("መዝግብ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Salary Rate Dialog
    if (editingSalaryWorker != null) {
        var rateInput by remember(editingSalaryWorker) { mutableStateOf(editingSalaryWorker!!.monthlySalary.toInt().toString()) }
        Dialog(onDismissRequest = { editingSalaryWorker = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "ወርሃዊ ደመወዝ ማስተካከያ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "የ '${editingSalaryWorker!!.name}' ወርሃዊ ክፍያ እዚህ ያሻሽሉ።",
                        color = Color(0xFF8C9E94),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("ወርሃዊ ክፍያ (Birr)", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color(0xFF222222),
                            focusedLabelColor = Color(0xFF00FF88),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { editingSalaryWorker = null }) {
                            Text("ሰርዝ", color = Color(0xFFFF3B3B), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
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

@Composable
fun WorkersAttendanceTab(
    appState: AppState,
    viewModel: WorkersViewModel,
    onAddClicked: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var listAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        listAnimated = true
    }

    val attendanceOnDuty = appState.currentAttendance.count { it.status == "On Duty" }
    val attendanceAbsent = appState.currentAttendance.count { it.status == "Absent" }
    val totalWorkersCount = appState.workers.size

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Page header & Add worker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "የሰራተኞች አስተዳደር",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .background(Color(0xFF00FF88))
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddClicked()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF041c0d),
                    contentColor = Color(0xFF00FF88)
                ),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add worker",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF00FF88)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ሰራተኛ ጨምር",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceSummaryCard(
                title = "በስራ ላይ",
                enTitle = "On Duty",
                value = "$attendanceOnDuty",
                color = Color(0xFF00FF88),
                modifier = Modifier.weight(1f)
            )
            AttendanceSummaryCard(
                title = "ቀርቷል",
                enTitle = "Absent",
                value = "$attendanceAbsent",
                color = Color(0xFFFF3B3B),
                modifier = Modifier.weight(1f)
            )
            AttendanceSummaryCard(
                title = "ጠቅላላ",
                enTitle = "Total",
                value = "$totalWorkersCount",
                color = Color(0xFFFFD700),
                modifier = Modifier.weight(1f)
            )
        }

        // Workers List
        if (appState.workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ምንም ሰራተኛ አልተመዘገበም... (No workers available)",
                    color = Color(0xFF8C9E94),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(appState.workers) { index, worker ->
                    val workerStatus = appState.currentAttendance.find { it.workerId == worker.id }?.status ?: "የለም"

                    // Smooth 300ms transitions on properties
                    val animatedBgColor by animateColorAsState(
                        targetValue = when (workerStatus) {
                            "On Duty" -> Color(0xFF031408)
                            "Absent" -> Color(0xFF140303)
                            else -> Color(0xFF050505)
                        },
                        animationSpec = tween(300),
                        label = "cardBg"
                    )
                    val animatedBorderColor by animateColorAsState(
                        targetValue = when (workerStatus) {
                            "On Duty" -> Color(0xFF00FF88).copy(alpha = 0.5f)
                            "Absent" -> Color(0xFFFF3B3B).copy(alpha = 0.5f)
                            else -> Color(0xFF222222)
                        },
                        animationSpec = tween(300),
                        label = "cardBorder"
                    )

                    // Card entrance scaling & offset logic
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (listAnimated) 1f else 0f,
                        animationSpec = tween(400, delayMillis = index * 50),
                        label = "cardAlpha"
                    )
                    val cardSlide by animateFloatAsState(
                        targetValue = if (listAnimated) 0f else 40f,
                        animationSpec = tween(400, delayMillis = index * 50),
                        label = "cardSlide"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = cardAlpha
                                translationY = cardSlide
                            }
                            .shadow(
                                elevation = if (workerStatus != "የለም") 6.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = when (workerStatus) {
                                    "On Duty" -> Color(0xFF00FF88)
                                    "Absent" -> Color(0xFFFF3B3B)
                                    else -> Color.Transparent
                                },
                                spotColor = when (workerStatus) {
                                    "On Duty" -> Color(0xFF00FF88)
                                    "Absent" -> Color(0xFFFF3B3B)
                                    else -> Color.Transparent
                                }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
                        border = BorderStroke(0.8.dp, animatedBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Left border glow
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        when (workerStatus) {
                                            "On Duty" -> Color(0xFF00FF88)
                                            "Absent" -> Color(0xFFFF3B3B)
                                            else -> Color(0xFF333333)
                                        }
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Circular avatar
                                    val initials = worker.name.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(Color(0xFF0A0A0A), CircleShape)
                                            .border(
                                                BorderStroke(1.2.dp, if (workerStatus == "On Duty") Color(0xFF00FF88) else if (workerStatus == "Absent") Color(0xFFFF3B3B) else Color(0xFF444444)),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials.uppercase(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = worker.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${appState.selectedDate} · ${if (workerStatus == "On Duty") "በስራ ላይ" else if (workerStatus == "Absent") "ቀሪ (Absent)" else "ያልተመዘገበ"}",
                                            color = Color(0xFF8C9E94),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // "በስራ" Pill
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(if (workerStatus == "On Duty") Color(0xFF00FF88) else Color.Transparent)
                                            .border(
                                                BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.6f)),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.recordAttendance(worker.id, "On Duty")
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "በስራ",
                                            color = if (workerStatus == "On Duty") Color.Black else Color(0xFF00FF88),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // "ቀረ" Pill
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(if (workerStatus == "Absent") Color(0xFFFF3B3B) else Color.Transparent)
                                            .border(
                                                BorderStroke(1.dp, Color(0xFFFF3B3B).copy(alpha = 0.6f)),
                                                RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.recordAttendance(worker.id, "Absent")
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ቀረ",
                                            color = if (workerStatus == "Absent") Color.Black else Color(0xFFFF3B3B),
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
    }
}

@Composable
fun WorkersSalaryTab(
    appState: AppState,
    viewModel: WorkersViewModel,
    onEditSalaryClicked: (Worker) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var listAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        listAnimated = true
    }

    // Days counter computations
    val parts = appState.selectedDate.split("-")
    val ethiopianDaysPassed = parts.getOrNull(2)?.toIntOrNull() ?: 1
    val daysRemaining = (30 - ethiopianDaysPassed).coerceIn(0, 30)
    val yearMonthStr = "${parts.getOrNull(0)}-${parts.getOrNull(1)}"

    // Calculate total Payroll sum
    val totalEarnedCalculated = appState.workers.sumOf { worker ->
        val daily = worker.monthlySalary / 30.0
        val absents = appState.allWorkerAttendance.count {
            it.workerId == worker.id && it.date.startsWith(yearMonthStr) && it.status == "Absent"
        }
        val earned = (daily * ethiopianDaysPassed) - (absents * 2 * daily)
        earned.coerceAtLeast(0.0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Countdown Ring Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF050505))
                .border(BorderStroke(0.8.dp, Color(0xFF222222)), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "የክፍያ መቁጠሪያ (Days Until Payday)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "በወሩ 30ኛው ቀን ላይ ለሁሉም ሰራተኞች የደመወዝ ክፍያ ይከናወናል። ዛሬ የወሩ $ethiopianDaysPassed ቀን ነው።",
                    color = Color(0xFF8C9E94),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(105.dp)
                    .weight(0.9f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(85.dp)) {
                    drawCircle(
                        color = Color(0xFF151816),
                        style = Stroke(width = 8.dp.toPx())
                    )
                    val sweep = 360f * (daysRemaining / 30f)
                    drawArc(
                        color = Color(0xFFFFD700),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$daysRemaining",
                        color = Color(0xFFFFD700),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ቀሪ ቀናት",
                        color = Color(0xFF8C9E94),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Salary Cards List Header
        Text(
            text = "የደመወዝ ዝርዝር መግለጫ (Current Salaries)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        if (appState.workers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ምንም ሰራተኛ አልተመዘገበም... (No workers loaded)",
                    color = Color(0xFF8C9E94),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                itemsIndexed(appState.workers) { index, worker ->
                    val dailySalary = worker.monthlySalary / 30.0
                    val absentDays = appState.allWorkerAttendance.count {
                        it.workerId == worker.id && it.date.startsWith(yearMonthStr) && it.status == "Absent"
                    }

                    // Formula: earnedSalary = (monthlySalary ÷ 30) × ethiopianDaysPassed - (absentDays × 2 × dailySalary)
                    val earnedSalary = (dailySalary * ethiopianDaysPassed) - (absentDays * 2 * dailySalary)
                    val earnedSalaryVal = earnedSalary.coerceAtLeast(0.0)
                    val deductionAmount = absentDays * 2 * dailySalary

                    // Smooth transition alpha/slide
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (listAnimated) 1f else 0f,
                        animationSpec = tween(400, delayMillis = index * 50),
                        label = "salCardAlpha"
                    )
                    val cardSlide by animateFloatAsState(
                        targetValue = if (listAnimated) 0f else 40f,
                        animationSpec = tween(400, delayMillis = index * 50),
                        label = "salCardSlide"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = cardAlpha
                                translationY = cardSlide
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                        border = BorderStroke(0.8.dp, Color(0xFF222222))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Left border indicator (green if absentCount == 0, red if > 0)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(if (absentDays > 0) Color(0xFFFF3B3B) else Color(0xFF00FF88))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Amharic Name
                                    Text(
                                        text = worker.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Absent badge (red if absent, green if 0)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (absentDays > 0) Color(0xFF260D0D) else Color(0xFF0D2611))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (absentDays > 0) "⚠️ $absentDays ቀን ቀርቷል" else "✓ ምንም የቀረ የለም (0)",
                                            color = if (absentDays > 0) Color(0xFFFF3B3B) else Color(0xFF00FF88),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Monthly salary edit icon trigger
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEditSalaryClicked(worker)
                                        }
                                    ) {
                                        Text(
                                            text = "ወርሃዊ ደሞዝ፡ ${worker.monthlySalary.toInt()} ETB",
                                            color = Color(0xFF8C9E94),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit rate",
                                            tint = Color(0xFF00FF88),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    if (deductionAmount > 0.0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ማቅነሻ (Deduction): -${deductionAmount.toInt()} ETB",
                                            color = Color(0xFFFF3B3B),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Earned Salary Amount Large
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "የተገኘ ደሞዝ",
                                        color = Color(0xFF8C9E94),
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${earnedSalaryVal.toInt()} ETB",
                                        color = if (absentDays > 0) Color(0xFFFF3B3B) else Color(0xFF00FF88),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Total Payroll card at bottom with dark green background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFF00FF88).copy(alpha = 0.15f),
                    spotColor = Color(0xFF00FF88).copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF041c0d)),
            border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ጠቅላላ የተገኘ ደሞዝ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ጠቅላላ ወርሃዊ የደመወዝ ክፍያ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimateCountUpText(
                    valueString = "${totalEarnedCalculated.toInt()} ETB",
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}


@Composable
fun AttendanceSummaryCard(
    title: String,
    enTitle: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.15f),
                spotColor = color.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
        border = BorderStroke(0.8.dp, Color(0xFF222222))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Modern Top Shine Line for Gold/Green/Red summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.05f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = enTitle,
                        color = Color(0xFF8C9E94),
                        fontSize = 9.sp
                    )
                }

                AnimateCountUpText(
                    valueString = value,
                    color = color,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
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
    var isListening by remember { mutableStateOf(false) }
    var showTerminalInput by remember { mutableStateOf(false) }

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
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                val userMsg = spokenText.trim()
                chatMessages.add(ChatMessage(sender = "user", text = userMsg))
                userInputText = ""
                isThinking = true

                scope.launch {
                    val contextPrompt = buildContextPromptForGemini(appState)
                    val reply = GeminiBotService.getGeminiResponse(
                        systemPrompt = contextPrompt,
                        userPrompt = userMsg,
                        history = chatMessages.map { Pair(it.sender, it.text) }
                    )
                    chatMessages.add(ChatMessage(sender = "model", text = reply))
                    isThinking = false

                    // Auto-read response for authentic voice-first experience
                    if (isTtsReady) {
                        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                    }

                    // Log activity
                    viewModel.recordActivityLog("Biniyam AI", "Add", "ቢኒያም AI መልስ ሰጥቷል፡ $userMsg")
                }
            }
        }
    }

    // Dynamic animation coordinates for Drift Network (Deep Neural Network Plexus Landscape)
    val infiniteTransition = rememberInfiniteTransition(label = "drift_neural")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    // Glowing heartbeat plexus pulse to represent active process or idle status
    val restPulseTransition = rememberInfiniteTransition(label = "rest_pulse")
    val plexusPulse by restPulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plexus_pulse"
    )

    // Wave ripple animation parameters triggered during voice/thinking
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(600)
        ),
        label = "wave2"
    )
    val wave3Progress by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1200)
        ),
        label = "wave3"
    )

    // Pre-declare and layout static particle coordinates (24 high-end nodes)
    val particlesCount = 24
    val particles = remember {
        List(particlesCount) {
            Triple(
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat(),
                kotlin.random.Random.nextFloat() * 1.6f + 0.4f
            )
        }
    }

    // Terminal typing visual state
    var activeTypewrittenText by remember { mutableStateOf("") }
    val lastMessage = chatMessages.lastOrNull()

    LaunchedEffect(lastMessage) {
        if (lastMessage != null && lastMessage.sender == "model") {
            activeTypewrittenText = ""
            val fullText = lastMessage.text
            // Fast intelligent chunk typewriting
            for (i in 1..fullText.length) {
                activeTypewrittenText = fullText.substring(0, i)
                kotlinx.coroutines.delay(10)
            }
            activeTypewrittenText = fullText
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(chatMessages.size, activeTypewrittenText) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val onMicClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "ስለ አንዋር ሪሳይክል ይጠይቁ (Ask Biniyam)...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "የድምፅ ግብዓት አልተዘጋጀም (Voice input not ready)", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Drifting neural background particles & pulsing connecting link Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val activeMultiplier = if (isListening || isThinking) plexusPulse * 1.4f else plexusPulse

            // Map particle drifts
            val pxList = particles.map { p ->
                val cx = p.first * w + 45.dp.toPx() * cos((tick * p.third).toDouble()).toFloat()
                val cy = p.second * h + 45.dp.toPx() * sin((tick * p.third).toDouble()).toFloat()
                Offset(cx.coerceIn(0f, w), cy.coerceIn(0f, h))
            }

            // Draw neural connections
            val threshold = 210.dp.toPx()
            for (i in pxList.indices) {
                for (j in i + 1 until pxList.size) {
                    val p1 = pxList[i]
                    val p2 = pxList[j]
                    val dx = p1.x - p2.x
                    val dy = p1.y - p2.y
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist < threshold) {
                        val baseAlpha = (1f - dist / threshold).coerceIn(0f, 1f)
                        val pulseAlpha = baseAlpha * 0.18f * activeMultiplier
                        drawLine(
                            color = Color(0xFF00FF88),
                            start = p1,
                            end = p2,
                            strokeWidth = (1.1.dp.toPx() * activeMultiplier),
                            alpha = pulseAlpha
                        )
                    }
                }
            }

            // Draw glowing synapse nodes with pulsating core radii
            pxList.forEach { pt ->
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = (4.dp.toPx() * activeMultiplier),
                    center = pt,
                    alpha = 0.35f * activeMultiplier
                )
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = (2.dp.toPx() * activeMultiplier),
                    center = pt,
                    alpha = 0.6f
                )
            }
        }

        // 2. Cosmic Darkness vignette overlay to keep text fully legible
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.94f)),
                        center = Offset.Unspecified
                    )
                )
        )

        // 3. Central Glassmorphism terminal dialogue box
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.52f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF00FF88).copy(alpha = 0.2f),
                        spotColor = Color(0xFF00FF88).copy(alpha = 0.4f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A1A0F).copy(alpha = 0.82f)
                ),
                border = BorderStroke(1.2.dp, Color(0xFF00FF88).copy(alpha = 0.38f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Futuristic Glass Card Console Status Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (isThinking) Color(0xFFFFD700) else if (isListening) Color(0xFF00FF88) else Color(0xFF00FF88).copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .shadow(4.dp, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isThinking) "BINIYAM_SYS @ CORES_PROCESSING" else if (isListening) "BINIYAM_SYS @ CAPTURING_AUDIO" else "BINIYAM_SYS @ IDLE_STBY",
                                color = if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88).copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "PORT_4031_SYS",
                            color = Color.White.copy(alpha = 0.25f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(1.dp)
                            .background(Color(0xFF00FF88).copy(alpha = 0.15f))
                    )

                    // Typewriter/Console Scrollable Chat Dialogue
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(chatMessages) { idx, msg ->
                            val isLast = idx == chatMessages.lastIndex
                            val isModel = msg.sender == "model"

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (isModel) "BINIYAM_AI_SYS_ORACLE >" else "LOCAL_USER_SPK >",
                                    color = if (isModel) Color(0xFF00FF88).copy(alpha = 0.8f) else Color(0xFFFFD700).copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                val displayText = if (isModel && isLast) activeTypewrittenText else msg.text

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = displayText,
                                        color = if (isModel) Color.White else Color(0xFFE2EBE5),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    if (isModel && isLast && activeTypewrittenText.length < msg.text.length) {
                                        val cursorTransition = rememberInfiniteTransition(label = "cursor")
                                        val cursorAlpha by cursorTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(450, easing = EaseInOutSine),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "alpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(7.dp)
                                                .height(14.dp)
                                                .offset(x = 3.dp)
                                                .background(Color(0xFF00FF88).copy(alpha = cursorAlpha))
                                        )
                                    }
                                }

                                if (isModel) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                if (isTtsReady) {
                                                    tts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak response voice",
                                                tint = Color(0xFF00FF88).copy(alpha = 0.6f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "REPLAY VOICE",
                                                color = Color(0xFF00FF88).copy(alpha = 0.6f),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
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

            // 4. Manual Text Input (Optional / Expandable Console field)
            AnimatedVisibility(
                visible = showTerminalInput,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TextField(
                    value = userInputText,
                    onValueChange = { userInputText = it },
                    placeholder = {
                        Text(
                            "ቢኒያምን እዚህ ይጻፉለት (Type a custom query to Biniyam)...",
                            color = Color(0xFF7A9483),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF041008),
                        unfocusedContainerColor = Color(0xFF020904),
                        focusedIndicatorColor = Color(0xFF00FF88),
                        unfocusedIndicatorColor = Color(0xFF03160A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (userInputText.isNotBlank()) {
                                    val userMsg = userInputText.trim()
                                    chatMessages.add(ChatMessage(sender = "user", text = userMsg))
                                    userInputText = ""
                                    isThinking = true
                                    showTerminalInput = false

                                    scope.launch {
                                        val contextPrompt = buildContextPromptForGemini(appState)
                                        val reply = GeminiBotService.getGeminiResponse(
                                            systemPrompt = contextPrompt,
                                            userPrompt = userMsg,
                                            history = chatMessages.map { Pair(it.sender, it.text) }
                                        )
                                        chatMessages.add(ChatMessage(sender = "model", text = reply))
                                        isThinking = false

                                        if (isTtsReady) {
                                            tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                                        }

                                        viewModel.recordActivityLog("Biniyam AI", "Add", "ቢኒያም AI መልስ ሰጥቷል፡ $userMsg")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, "Send terminal message", tint = Color(0xFF00FF88))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. Voice Pulse Studio Terminal Controller at Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 76.dp), // Height class offset
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Label
                Text(
                    text = if (isThinking) "BINIYAM AI ORACLE ENGAGED" else if (isListening) "🎙️ LISTENING / RECORDING VOICE..." else "TAP MIC KEY TO COMMUNICATE",
                    color = if (isThinking) Color(0xFFFFD700) else if (isListening) Color(0xFF00FF88) else Color(0xFF6B8074),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Large interactive glowing microphone button built with concentric feedback waves
                Box(
                    modifier = Modifier.size(135.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening || isThinking) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = Offset(size.width / 2, size.height / 2)
                            val baseWaveColor = if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88)

                            // Wave Ripple Ring 1 (Inner expanding)
                            drawCircle(
                                color = baseWaveColor.copy(alpha = (1f - (wave1Progress - 1f) / 2.5f).coerceIn(0f, 1f) * 0.42f),
                                radius = (35.dp.toPx() * wave1Progress),
                                center = centerOffset,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Wave Ripple Ring 2 (Middle expanding)
                            drawCircle(
                                color = baseWaveColor.copy(alpha = (1f - (wave2Progress - 1f) / 2.5f).coerceIn(0f, 1f) * 0.42f),
                                radius = (35.dp.toPx() * wave2Progress),
                                center = centerOffset,
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // Wave Ripple Ring 3 (Outer expanding)
                            drawCircle(
                                color = baseWaveColor.copy(alpha = (1f - (wave3Progress - 1f) / 2.5f).coerceIn(0f, 1f) * 0.42f),
                                radius = (35.dp.toPx() * wave3Progress),
                                center = centerOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    } else {
                        // Resting smooth organic pulse halo
                        val restWaveTransition = rememberInfiniteTransition(label = "pulse_mic_idle")
                        val restProgress by restWaveTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.28f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "progress"
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerOffset = Offset(size.width / 2, size.height / 2)
                            drawCircle(
                                color = Color(0xFF00FF88).copy(alpha = (1.28f - restProgress) * 0.28f),
                                radius = (35.dp.toPx() * restProgress),
                                center = centerOffset,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // Floating microphone central sphere trigger icon button
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(
                                elevation = if (isListening) 24.dp else 10.dp,
                                shape = CircleShape,
                                ambientColor = if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88),
                                spotColor = if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88)
                            )
                            .background(
                                if (isListening) Color(0xFF00FF88) else Color(0xFF0A1A0F),
                                CircleShape
                            )
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88)
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isThinking) Icons.Default.AutoAwesome else Icons.Default.Mic,
                            contentDescription = "Biniyam Voice Terminal Trigger",
                            tint = if (isListening) Color.Black else if (isThinking) Color(0xFFFFD700) else Color(0xFF00FF88),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Console Options Actions row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showTerminalInput) "[ DEACTIVATE KEYBOARD ]" else "[ INITIATE KEYBOARD TYPE ]",
                        color = Color(0xFF00FF88).copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTerminalInput = !showTerminalInput
                            }
                            .padding(8.dp)
                    )

                    Text(
                        text = "[ SYSTEM LIVE STATUS REPORT ]",
                        color = Color(0xFFFFD700).copy(alpha = 0.65f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
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
                            .padding(8.dp)
                    )
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
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF00FF88), spotColor = Color(0xFF00FF88)),
            border = BorderStroke(1.dp, Color(0xFF00FF88))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ማስመዝገቢያ ፦ አዲስ ምርት ምዝገባ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)

                AnwarTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "የምርቱ ስም (Product Name)"
                )
                AnwarTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = "መጠን (Size/Dimensions)"
                )
                AnwarTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = "ቀለም (Color)"
                )
                AnwarTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = "ክብደት (Bag Weight in Kg)"
                )
                AnwarTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = "የመነሻ ክምችት (Initial Stock Bags)"
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

    // Gravitational ring pulse animation
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 40.dp.value,
        targetValue = 200.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    var typedText by remember { mutableStateOf("") }
    val fullText = "INITIALIZING ANWAR SYSTEMS..."

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

        // Typewriter effect
        for (i in 1..fullText.length) {
            typedText = fullText.substring(0, i)
            kotlinx.coroutines.delay(65)
        }

        kotlinx.coroutines.delay(1200)

        // Implode screen transition (shrink fast to 0f, then trigger layout transition)
        scaleAnim.animateTo(0.0f, animationSpec = tween(550, easing = EaseInBack))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Pure void black #000000 screen
        contentAlignment = Alignment.Center
    ) {
        // Green event horizon pulsing rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            drawCircle(
                color = Color(0xFF00FF88).copy(alpha = pulseAlpha * 0.45f),
                radius = pulseRadius.dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00FF88).copy(alpha = (pulseAlpha + 0.3f).coerceAtMost(1f) * 0.25f),
                radius = (pulseRadius * 0.62f).dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                scaleX = scaleAnim.value,
                scaleY = scaleAnim.value,
                alpha = opacityAnim.value
            )
        ) {
            // ANWAR Logo Glowing particle circle
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .shadow(16.dp, CircleShape, ambientColor = Color(0xFF00FF88), spotColor = Color(0xFF00FF88))
                    .background(Color(0xFF050505), CircleShape)
                    .border(BorderStroke(2.dp, Color(0xFF00FF88)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "ANWAR CONTROL",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 28.sp,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UNIVERSE GOD MODE V1.0.0",
                color = Color(0xFFFFD700), // Cosmic Gold #FFD700
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Monospace character typewriting
            Text(
                text = typedText,
                color = Color(0xFF00FF88),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
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

@Composable
fun AnimateCountUpText(
    valueString: String,
    color: Color,
    fontSize: TextUnit = 18.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    // Parse the first group of digits in valueString
    val numericPart = remember(valueString) {
        valueString.takeWhile { it.isDigit() }.toIntOrNull() ?: valueString.filter { it.isDigit() }.toIntOrNull()
    }
    val nonNumericSuffix = remember(valueString) {
        if (numericPart != null) {
            val numStr = numericPart.toString()
            val startIdx = valueString.indexOf(numStr)
            if (startIdx != -1) {
                valueString.substring(startIdx + numStr.length)
            } else {
                valueString.replace(numStr, "")
            }
        } else {
            valueString
        }
    }
    val nonNumericPrefix = remember(valueString) {
        if (numericPart != null) {
            val numStr = numericPart.toString()
            val startIdx = valueString.indexOf(numStr)
            if (startIdx > 0) {
                valueString.substring(0, startIdx)
            } else ""
        } else ""
    }

    if (numericPart == null) {
        Text(
            text = valueString,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            modifier = modifier
        )
    } else {
        var animatedValue by remember { mutableStateOf(0) }
        LaunchedEffect(numericPart) {
            // Smoothly animate from 0 to numericPart
            val steps = 30
            val delayDuration = (600 / steps).toLong()
            for (step in 1..steps) {
                animatedValue = (numericPart * step) / steps
                kotlinx.coroutines.delay(delayDuration)
            }
            animatedValue = numericPart
        }
        Text(
            text = "$nonNumericPrefix$animatedValue$nonNumericSuffix",
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            modifier = modifier
        )
    }
}

@Composable
fun AnwarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF8C9E94)) },
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = EmeraldGlow,
            unfocusedIndicatorColor = EmeraldGlow.copy(alpha = 0.4f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}
