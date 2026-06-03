package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

const val CURRENT_APP_VERSION = "1.0.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Collect Room states from ViewModel
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val rawMaterials by viewModel.allRawMaterials.collectAsStateWithLifecycle()
    val masterbatches by viewModel.allMasterbatches.collectAsStateWithLifecycle()
    val workers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val attendanceList by viewModel.currentAttendance.collectAsStateWithLifecycle()
    val reportPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val workerStats by viewModel.workerAttendanceStats.collectAsStateWithLifecycle()
    val activityLogs by viewModel.allActivityLogs.collectAsStateWithLifecycle()
    val allWorkerAttendance by viewModel.allWorkerAttendance.collectAsStateWithLifecycle()
    val announcements by viewModel.allAnnouncements.collectAsStateWithLifecycle()
    val appVersionInfo by viewModel.appVersionState.collectAsStateWithLifecycle()

    // Active tab selection matching the Bento Grid HTML design: Daily Overview, Inventory, Workers, Activity Log
    var activeTab by remember { mutableStateOf("Daily Overview") }
    var workerScreenTab by remember { mutableStateOf("Attendance") } // "Attendance" or "Salary"

    // Luxury animations and haptics states
    var showSplash by remember { mutableStateOf(true) }
    var showSidebar by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Dialog trigger states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddMasterbatchDialog by remember { mutableStateOf(false) }
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var selectedProductForActivity by remember { mutableStateOf<Product?>(null) }
    var selectedRawMaterialForActivity by remember { mutableStateOf<RawMaterial?>(null) }
    var selectedMasterbatchForActivity by remember { mutableStateOf<Masterbatch?>(null) }
    var selectedMasterbatchForBagPurchase by remember { mutableStateOf<Masterbatch?>(null) }

    // Date picker state toggle
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var editingWorkerSalary by remember { mutableStateOf<Worker?>(null) }
    var salaryEditValue by remember { mutableStateOf("") }
    var showAddAnnouncementDialog by remember { mutableStateOf(false) }
    var showAppVersionDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("anwar_app_prefs", android.content.Context.MODE_PRIVATE) }
    var activeAnnouncementToShow by remember { mutableStateOf<Announcement?>(null) }
    var activeAppVersionToShow by remember { mutableStateOf<AppVersion?>(null) }

    LaunchedEffect(announcements, appVersionInfo, showSplash) {
        if (!showSplash) {
            val latestActive = announcements.firstOrNull { it.active }
            if (latestActive != null) {
                val key = "dismissed_announcement_${latestActive.id}"
                val wasDismissed = prefs.getBoolean(key, false)
                if (!wasDismissed) {
                    activeAnnouncementToShow = latestActive
                }
            }
            if (appVersionInfo != null) {
                val remoteVer = appVersionInfo!!.versionName
                if (isNewerVersion(CURRENT_APP_VERSION, remoteVer)) {
                    val key = "dismissed_version_${remoteVer}"
                    val wasDismissed = prefs.getBoolean(key, false)
                    val isMandatory = appVersionInfo!!.isMandatory
                    if (!wasDismissed || isMandatory) {
                        activeAppVersionToShow = appVersionInfo
                    }
                }
            }
        }
    }

    if (showSplash) {
        AnwarSplashScreen(onFinished = { showSplash = false })
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = BentoBg,
                modifier = modifier.fillMaxSize(),
                floatingActionButton = {
                    // Pulsing FAB matching requirement (8)
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        // Expanding pulse circle
                        Box(
                            modifier = Modifier
                                .size(64.dp * pulseScale)
                                .background(BentoForestGreen.copy(alpha = pulseAlpha), CircleShape)
                        )
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (activeTab == "Inventory") {
                                    showAddProductDialog = true
                                } else if (activeTab == "Workers") {
                                    showAddWorkerDialog = true
                                } else {
                                    showAddProductDialog = true
                                }
                            },
                            containerColor = BentoForestGreen,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("pulse_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Add Operation",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    // Glowing Custom Bottom Bar matching requirement (2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp)
                            .background(
                                Color(0xFF0C101B).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val navItems = listOf(
                                Triple("Daily Overview", Icons.Default.Home, "Console"),
                                Triple("Inventory", Icons.Default.List, "Inventory"),
                                Triple("Workers", Icons.Default.Person, "Workers"),
                                Triple("Activity Log", Icons.Default.Star, "Logs"),
                                Triple("BINIYAM", Icons.Default.Android, "AI")
                            )
                            navItems.forEach { (tabName, icon, label) ->
                                val isSelected = activeTab == tabName
                                val animatedIconTint by animateColorAsState(
                                    targetValue = if (isSelected) BentoForestGreen else Color(0xFF64748B),
                                    label = "nav_icon_tint"
                                )
                                val animatedTextTint by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color(0xFF64748B),
                                    label = "nav_text_tint"
                                )

                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            activeTab = tabName
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        Brush.radialGradient(
                                                            listOf(
                                                                BentoForestGreen.copy(alpha = 0.35f),
                                                                Color.Transparent
                                                            )
                                                        )
                                                    )
                                            )
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = animatedIconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = animatedTextTint,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(BentoBg, Color(0xFF04060C))))
                ) {
                    // --- BENTO GRID STYLE INTEGRATED HEADER ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSidebar = true
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu Sidebar",
                                        tint = BentoGold, // Premium gold hamburger menu icon
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_anwar_logo),
                                            contentDescription = "Anwar Company Logo",
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, BentoGold, CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "ANWAR",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = (-1).sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Glowing status dot - representing safe operational connection
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .size(8.dp)
                                                .background(BentoForestGreen, CircleShape)
                                        )
                                    }
                                    Text(
                                        text = "RECYCLING OPERATIONS CONSOLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSubText,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }

                            // Soft Green Modern Rounded Icon Badge
                            Box(
                                modifier = Modifier
                                    .background(BentoSoftGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .border(1.dp, BentoForestGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Factory Hub",
                                    tint = BentoForestGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                Spacer(modifier = Modifier.height(12.dp))

                // Date control panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { shiftDate(viewModel, selectedDate, -1) },
                        modifier = Modifier.testTag("prev_date_btn")
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day", tint = BentoForestGreen)
                    }

                    Row(
                        modifier = Modifier
                            .clickable { showDatePickerDialog = true }
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("select_date_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Date picker",
                            tint = BentoForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDateFriendly(selectedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                    }

                    IconButton(
                        onClick = { shiftDate(viewModel, selectedDate, 1) },
                        modifier = Modifier.testTag("next_date_btn")
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day", tint = BentoForestGreen)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- DETAILED LAYOUT ROUTING BASED ON CHOSEN BENTO SEGMENT ---
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val tabsList = listOf("Daily Overview", "Inventory", "Workers", "Activity Log", "BINIYAM")
                    val initialIndex = tabsList.indexOf(initialState)
                    val targetIndex = tabsList.indexOf(targetState)
                    if (targetIndex > initialIndex) {
                        slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut()
                    }
                },
                label = "smooth_screen_switch",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { targetScreen ->
                when (targetScreen) {
                    "Daily Overview" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            BentoGridOverviewPanel(
                                stats = stats,
                                products = products,
                                rawMaterials = rawMaterials,
                                masterbatches = masterbatches,
                                attendanceList = attendanceList,
                                workers = workers,
                                onWorkersTabSelect = { activeTab = "Workers" },
                                onProductClick = { selectedProductForActivity = it },
                                onRawMaterialClick = { selectedRawMaterialForActivity = it },
                                onMasterbatchClick = { selectedMasterbatchForActivity = it },
                                reportPeriod = reportPeriod,
                                onPeriodSelect = { viewModel.setReportPeriod(it) },
                                viewModel = viewModel,
                                onAddProductClick = { showAddProductDialog = true }
                            )
                        }
                    }
                    "Inventory" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp) // extra padding for bottom navigation
                        ) {
                            // --- SECTION 1: PRODUCTS INVENTORY ---
                            item {
                                InventoryHeaderSection(
                                    title = "PRODUCTS INVENTORY",
                                    subtitle = "Daily fabricated bags, sales and on-hand stocks",
                                    icon = Icons.Default.List,
                                    onAddClick = { showAddProductDialog = true },
                                    addButtonText = "+ Add Product",
                                    addBtnTag = "add_product_section_btn"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (products.isEmpty()) {
                                    EmptyStatePlaceholder("No products defined. Click + Add Product to start.")
                                } else {
                                    val chunkedProducts = products.chunked(2)
                                    chunkedProducts.forEach { rowProducts ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowProducts.forEach { product ->
                                                val pStats = stats.productSummary[product.id] ?: ProductAggStats(product.id, 0, 0, 0)
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ProductStockCard(
                                                        product = product,
                                                        stats = pStats,
                                                        onRecordClick = { selectedProductForActivity = product }
                                                    )
                                                }
                                            }
                                            if (rowProducts.size < 2) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }

                            // --- SECTION 2: RAW MATERIALSFEEDSTOCK ---
                            item {
                                InventoryHeaderSection(
                                    title = "RAW MATERIALSFEEDSTOCK",
                                    subtitle = "Daily feedstock consumption and arrivals",
                                    icon = Icons.Default.Build,
                                    onAddClick = null
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val materials = listOf("LD", "HD", "Waste")
                                    materials.forEach { mType ->
                                        val materialObj = rawMaterials.find { it.type.uppercase() == mType.uppercase() }
                                            ?: RawMaterial(mType, 0.0)

                                        val used = when (mType) {
                                            "LD" -> stats.ldUsed
                                            "HD" -> stats.hdUsed
                                            else -> stats.wasteUsed
                                        }
                                        val added = when (mType) {
                                            "LD" -> stats.ldAdded
                                            "HD" -> stats.hdAdded
                                            else -> stats.wasteAdded
                                        }

                                        RawMaterialMiniCard(
                                            modifier = Modifier.weight(1f),
                                            type = mType,
                                            rawMaterial = materialObj,
                                            used = used,
                                            added = added,
                                            onRecordClick = { selectedRawMaterialForActivity = materialObj }
                                        )
                                    }
                                }
                            }

                            // --- SECTION 3: MASTERBATCH COLOR TRACKING ---
                            item {
                                InventoryHeaderSection(
                                    title = "የማስተርባች መዝገብ",
                                    subtitle = "የቀለም ማስተርባች ክምችትና የቀን ፍጆታ መቆጣጠሪያ",
                                    icon = Icons.Default.Star,
                                    onAddClick = { showAddMasterbatchDialog = true },
                                    addButtonText = "+ አዲስ ማስተርባች",
                                    addBtnTag = "add_masterbatch_section_btn"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (masterbatches.isEmpty()) {
                                    EmptyStatePlaceholder("ምንም የተመዘገበ ማስተርባች የለም:: አዲስ ለመመዝገብ '+ አዲስ ማስተርባች' የሚለውን ይጫኑ::")
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        masterbatches.forEach { mb ->
                                            val mbStats = stats.masterbatchSummary[mb.id] ?: MasterbatchAggStats(mb.id, 0.0, 0.0)
                                            MasterbatchCard(
                                                masterbatch = mb,
                                                stats = mbStats,
                                                onRecordClick = { selectedMasterbatchForActivity = mb },
                                                onDeleteClick = { viewModel.deleteMasterbatch(mb) },
                                                onAddBoughtBagsClick = { selectedMasterbatchForBagPurchase = mb }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Workers" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "የሰራተኞች አስተዳደር",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp)
                                                .height(3.dp)
                                                .background(BentoForestGreen, RoundedCornerShape(2.dp))
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { showAddWorkerDialog = true },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = BentoForestGreen.copy(alpha = 0.2f),
                                            contentColor = BentoSoftGreen
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("add_worker_section_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ሰራተኛ መዝግብ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // segment sub-tabs for Attendance and Salary (Pill-shaped toggle with luxurious styling)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(50.dp))
                                        .border(1.dp, BentoBorder.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val options = listOf(
                                        "Attendance" to "የሰራተኞች መቆጣጠሪያ",
                                        "Salary" to "የደሞዝ መዝገብ"
                                    )
                                    options.forEach { (key, label) ->
                                        val isSel = workerScreenTab == key
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(50.dp))
                                                .background(if (isSel) BentoForestGreen else Color.Transparent)
                                                .clickable { workerScreenTab = key }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSel) Color.White else BentoSubText
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val parts = selectedDate.split("-")
                                val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: 2018
                                val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: 9
                                val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: 22

                                if (workerScreenTab == "Attendance") {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = BentoNeutralGray
                                        ),
                                        border = BorderStroke(1.dp, BentoBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            val friendlyDateStr = EthiopianCalendarHelper.formatEthiopianDateFriendly(selectedDate)
                                            Text(
                                                text = "ዕለታዊ የሰራተኞች መዝገብ፡ $friendlyDateStr",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoForestGreen,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )

                                            if (workers.isEmpty()) {
                                                EmptyStatePlaceholder("ምንም የተመዘገበ ሰራተኛ የለም።")
                                            } else {
                                                workers.forEach { worker ->
                                                    val stat = workerStats.find { it.workerId == worker.id }
                                                    val currentAttendanceOnDate = attendanceList.find { it.workerId == worker.id }

                                                    WorkerRowItem(
                                                        worker = worker,
                                                        stats = stat,
                                                        activeAttendance = currentAttendanceOnDate,
                                                        onStatusSelect = { status ->
                                                            viewModel.markWorkerAttendance(worker.id, status)
                                                        }
                                                    )
                                                    HorizontalDivider(
                                                        color = BentoBorder.copy(alpha = 0.3f),
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Premium countdown card showing automatically generated date details
                                    val monthNameAmh = EthiopianCalendarHelper.ETHIOPIAN_MONTHS.getOrNull(ethMonth - 1) ?: "ሰኔ"
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)
                                        ),
                                        border = BorderStroke(1.dp, BentoForestGreen.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "የ$monthNameAmh ወር $ethYear የደሞዝ ሁኔታ መከታተያ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoSoftGreen,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )

                                            val daysRemaining = 30 - ethDay
                                            if (ethDay == 30) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                        .border(1.dp, BentoForestGreen, RoundedCornerShape(12.dp))
                                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "🎉 ዛሬ የደሞዝ ቀን ነው!",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = BentoSoftGreen
                                                    )
                                                }
                                            } else if (daysRemaining > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                                        .border(1.dp, BentoBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "ደሞዝ ቀን፡ ቀን 30 - $daysRemaining ቀናት ይቀራሉ",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                                        .border(1.dp, BentoBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "የደሞዝ ክፍያ ቀን አልፏል (ቀን 30)",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BentoSubText
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (workers.isEmpty()) {
                                        EmptyStatePlaceholder("ምንም የተመዘገበ ሰራተኛ የለም።")
                                    } else {
                                        var totalPayroll = 0.0
                                        val activeWorkers = workers.filter { it.isActive }

                                        activeWorkers.forEachIndexed { idx, worker ->
                                            val currentYearMonthPrefix = String.format(Locale.US, "%04d-%02d-", ethYear, ethMonth)
                                            val workerMonthAtt = allWorkerAttendance.filter {
                                                it.workerId == worker.id && it.date.startsWith(currentYearMonthPrefix)
                                            }
                                            val absentDays = workerMonthAtt.count { it.status == "Absent" }
                                            val monthlySalary = worker.monthlySalary
                                            val dailySalary = monthlySalary / 30.0
                                            val deduction = absentDays * 2.0 * dailySalary
                                            val daysPassedThisMonth = minOf(30, ethDay)
                                            val daysWorkedSoFar = maxOf(0, daysPassedThisMonth - absentDays)
                                            val earnedSalary = maxOf(0.0, dailySalary * (daysPassedThisMonth - (absentDays * 2.0)))
                                            totalPayroll += earnedSalary

                                            // Premium customized worker card
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0xFF1E293B)
                                                ),
                                                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.2f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            brush = Brush.verticalGradient(
                                                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A).copy(alpha = 0.9f))
                                                            )
                                                        )
                                                        .drawBehind {
                                                            // Thin left border accent (red if absent, green if ok)
                                                            drawRect(
                                                                color = if (absentDays > 0) BentoAlertText else BentoForestGreen,
                                                                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                size = androidx.compose.ui.geometry.Size(4.dp.toPx(), this.size.height)
                                                            )
                                                        }
                                                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Left column: name, absent days, and monthly salary
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = toAmharicName(worker.name),
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )

                                                        Text(
                                                            text = "ያልተገኘበት፡ $absentDays ቀን",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = if (absentDays > 0) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (absentDays > 0) BentoAlertText else BentoSubText
                                                        )

                                                        Text(
                                                            text = "የተሰራ ቀን፡ $daysWorkedSoFar ቀን",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Medium,
                                                            color = BentoSoftGreen
                                                        )

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "የወር ደሞዝ: ${String.format("%.0f", monthlySalary)} ብር",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = BentoSubText
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    editingWorkerSalary = worker
                                                                    salaryEditValue = String.format("%.0f", monthlySalary)
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Edit Salary",
                                                                    tint = BentoSoftGreen,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Right column: Final earned salary and deduction
                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${String.format("%.2f", earnedSalary)} ብር",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (absentDays > 0) BentoAlertText else BentoSoftGreen
                                                        )

                                                        if (absentDays > 0) {
                                                            Text(
                                                                text = "ቅጣት፡ -${String.format("%.2f", deduction)} ብር",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BentoAlertText
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (idx < activeWorkers.size - 1) {
                                                HorizontalDivider(
                                                    color = BentoForestGreen.copy(alpha = 0.3f),
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            }
                                        }

                                        // Premium total payroll card
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp)
                                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF064E3B)
                                            ),
                                            border = BorderStroke(1.5.dp, BentoForestGreen)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ጠቅላላ ደሞዝ",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${String.format("%.2f", totalPayroll)} ብር",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = BentoSoftGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Activity Log" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            item {
                                ActivityLogSection(
                                    activityLogs = activityLogs,
                                    announcements = announcements,
                                    onDeleteAnnouncement = { viewModel.deleteAnnouncement(it) }
                                )
                            }
                        }
                    }
                    "BINIYAM" -> {
                        BiniyamBotScreen(
                            viewModel = viewModel,
                            stats = stats,
                            products = products,
                            rawMaterials = rawMaterials,
                            masterbatches = masterbatches,
                            workers = workers,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

        // --- CUSTOM SLIDE-IN SIDEBAR DRAWER (Tesla / Apple-like) ---
        AnimatedVisibility(
            visible = showSidebar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showSidebar = false } // tap background to close
            )
        }

        val sidebarWidth = 280.dp
        val sidebarOffset by animateDpAsState(
            targetValue = if (showSidebar) 0.dp else (-280).dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "sidebar_slide"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(sidebarWidth)
                .offset(x = sidebarOffset)
                .background(Color(0xFF030712)) // Deep luxury slate/black carbon color
                .border(BorderStroke(1.dp, BentoBorder))
                .clickable(enabled = false) {} // block click propagation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANWAR",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PRODUCTION TERMINAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoGold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = { showSidebar = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sidebar navigation links
                val sidebarItems = listOf(
                    Triple("Shift Settings", Icons.Default.Build, "Manage industrial calendar settings"),
                    Triple("Reports Engine", Icons.Default.Star, "Export PDF performance stats"),
                    Triple("Device Connection", Icons.Default.CheckCircle, "Active recycling console logs"),
                    Triple("System Secure", Icons.Default.Settings, "Platform adjustments & security options")
                )

                sidebarItems.forEach { (title, icon, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSidebar = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = BentoForestGreen, modifier = Modifier.size(24.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(desc, style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Administrative Announcement option
                HorizontalDivider(color = BentoBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BentoForestGreen.copy(alpha = 0.12f))
                        .border(1.dp, BentoForestGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSidebar = false
                            showAddAnnouncementDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = BentoForestGreen, modifier = Modifier.size(24.dp))
                    Column {
                        Text("📢 መልዕክት ልጥፍ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("አዲስ የኩባንያ መልዕክት መለጠፊያ ቦርድ", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                    }
                }

                // App Version Manager option (under hamburger menu)
                HorizontalDivider(color = BentoBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BentoForestGreen.copy(alpha = 0.12f))
                        .border(1.dp, BentoForestGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSidebar = false
                            showAppVersionDialog = true
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = BentoForestGreen, modifier = Modifier.size(24.dp))
                    Column {
                        Text("🛠️ App Version Manager", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("አፕሊኬሽን ስሪት መቀየሪያ (App Updates)", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "CONSOLE STABLE V3.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoSubText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SECURE PROTOCOL ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoForestGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "APP VERSION: v$CURRENT_APP_VERSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoSoftGreen,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }


    // --- ALL POPUP OVERLAYS / DIALOGS ---

    // 1. Android Native-like Date Picker Dialog Triggered inside
    if (showDatePickerDialog) {
        DatePickerFallbackDialog(
            currentDate = selectedDate,
            onDismiss = { showDatePickerDialog = false },
            onDateConfirm = { newDate ->
                viewModel.updateSelectedDate(newDate)
                showDatePickerDialog = false
            }
        )
    }

    // 2. Add New Product Dialog
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, size, color, counter, piecesPerBag, weight, initialStock ->
                viewModel.addNewProduct(name, size, color, counter, piecesPerBag, weight, initialStock)
                showAddProductDialog = false
            }
        )
    }

    // 3. Add Masterbatch Dialog
    if (showAddMasterbatchDialog) {
        AddMasterbatchDialog(
            onDismiss = { showAddMasterbatchDialog = false },
            onSave = { colorName, initialStock ->
                viewModel.addNewMasterbatch(colorName, initialStock)
                showAddMasterbatchDialog = false
            }
        )
    }

    // 4. Add Worker Dialog
    if (showAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onSave = { name, salary ->
                viewModel.addNewWorker(name, salary)
                showAddWorkerDialog = false
            }
        )
    }

    if (editingWorkerSalary != null) {
        AlertDialog(
            onDismissRequest = { editingWorkerSalary = null },
            title = {
                Text(
                    text = "${toAmharicName(editingWorkerSalary?.name ?: "")} - ደሞዝ ማስተካከያ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoSoftGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "አዲስ የወር ደሞዝ ያስገቡ (በብር):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = salaryEditValue,
                        onValueChange = { salaryEditValue = it },
                        label = { Text("ደሞዝ (Salary)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_worker_salary_value"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BentoForestGreen,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoForestGreen,
                            cursorColor = BentoForestGreen
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = salaryEditValue.toDoubleOrNull()
                        if (amt != null && editingWorkerSalary != null) {
                            viewModel.updateWorkerSalary(editingWorkerSalary!!.id, amt)
                            editingWorkerSalary = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoForestGreen),
                    modifier = Modifier.testTag("submit_salary_edit")
                ) {
                    Text("አስቀምጥ (Save)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWorkerSalary = null }) {
                    Text("ተው (Cancel)", color = BentoSubText)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // 5. Record Product Activity Dialog
    selectedProductForActivity?.let { product ->
        RecordProductActivityDialog(
            product = product,
            onDismiss = { selectedProductForActivity = null },
            onSave = { fabricated, sold, adjusted, notes ->
                viewModel.recordProductDailyActivity(product.id, fabricated, sold, adjusted, notes)
                selectedProductForActivity = null
            },
            onDelete = {
                viewModel.deleteProduct(product)
                selectedProductForActivity = null
            }
        )
    }

    // 6. Record Raw Material Activity Dialog
    selectedRawMaterialForActivity?.let { rawMaterial ->
        RecordRawMaterialActivityDialog(
            rawMaterialType = rawMaterial.type,
            onDismiss = { selectedRawMaterialForActivity = null },
            onSave = { used, added ->
                viewModel.recordRawMaterialActivity(rawMaterial.type, used, added)
                selectedRawMaterialForActivity = null
            }
        )
    }

    // 7. Record Masterbatch Activity Dialog
    selectedMasterbatchForActivity?.let { mb ->
        val mbStats = stats.masterbatchSummary[mb.id] ?: MasterbatchAggStats(mb.id, 0.0, 0.0)
        RecordMasterbatchActivityDialog(
            masterbatch = mb,
            stats = mbStats,
            onDismiss = { selectedMasterbatchForActivity = null },
            onSave = { used, bought, takenOut, returned ->
                viewModel.recordMasterbatchActivity(mb.id, used, bought, takenOut, returned)
                selectedMasterbatchForActivity = null
            }
        )
    }

    // 8. Add Bought Bags Dialog
    selectedMasterbatchForBagPurchase?.let { mb ->
        val mbStats = stats.masterbatchSummary[mb.id] ?: MasterbatchAggStats(mb.id, 0.0, 0.0)
        AddBoughtBagsDialog(
            masterbatch = mb,
            onDismiss = { selectedMasterbatchForBagPurchase = null },
            onSave = { bags ->
                val bagsInKg = bags * 25.0
                viewModel.recordMasterbatchActivity(
                    masterbatchId = mb.id,
                    used = mbStats.used,
                    bought = mbStats.bought + bagsInKg,
                    takenOut = mbStats.takenOut,
                    returned = mbStats.returned
                )
                selectedMasterbatchForBagPurchase = null
            }
        )
    }

    // 9. Startup Update Notification dialog
    activeAnnouncementToShow?.let { ann ->
        UpdateNotificationDialog(
            announcement = ann,
            onDismiss = {
                prefs.edit().putBoolean("dismissed_announcement_${ann.id}", true).apply()
                activeAnnouncementToShow = null
            }
        )
    }

    // 10. Post Announcement Dialog (Secure Admin tool)
    if (showAddAnnouncementDialog) {
        PostAnnouncementDialog(
            onDismiss = { showAddAnnouncementDialog = false },
            onPost = { title, message ->
                viewModel.postAnnouncement(title, message)
                showAddAnnouncementDialog = false
            }
        )
    }

    // 11. Startup Version Update notification dialog
    activeAppVersionToShow?.let { appVer ->
        VersionUpdatePopupDialog(
            appVersion = appVer,
            onDismiss = {
                prefs.edit().putBoolean("dismissed_version_${appVer.versionName}", true).apply()
                activeAppVersionToShow = null
            }
        )
    }

    // 12. App Version Manager Dialog (Secure Admin tool)
    if (showAppVersionDialog) {
        AdminAppVersionDialog(
            currentLatestVersionName = appVersionInfo?.versionName ?: "1.0.0",
            currentLatestApkUrl = appVersionInfo?.apkUrl ?: "github.com/guadebiniam-del/Biniyam/raw/main/.build-outputs/app-debug.apk",
            currentLatestChangelog = appVersionInfo?.changelog ?: "First version update released.",
            currentLatestIsMandatory = appVersionInfo?.isMandatory ?: false,
            onDismiss = { showAppVersionDialog = false },
            onUpdate = { newVersion, apkUrl, changelog, isMandatory ->
                viewModel.updateAppVersion(newVersion, apkUrl, changelog, isMandatory)
                showAppVersionDialog = false
            }
        )
    }
}
}


data class MachineConfig(
    val index: Int,
    val label: String,
    val xPercent: Float,
    val yPercent: Float,
    val isSop: Boolean = false,
    val defaultName: String
)

// --- BENTO GRID SYSTEM MAIN KPI PANEL ---
@Composable
fun BentoGridOverviewPanel(
    stats: AggregatedStats,
    products: List<Product>,
    rawMaterials: List<RawMaterial>,
    masterbatches: List<Masterbatch>,
    attendanceList: List<WorkerAttendance>,
    workers: List<Worker>,
    onWorkersTabSelect: () -> Unit,
    onProductClick: (Product) -> Unit,
    onRawMaterialClick: (RawMaterial) -> Unit,
    onMasterbatchClick: (Masterbatch) -> Unit,
    reportPeriod: MainViewModel.ReportPeriod,
    onPeriodSelect: (MainViewModel.ReportPeriod) -> Unit,
    viewModel: MainViewModel,
    onAddProductClick: () -> Unit
) {
    val selectedDateStr = viewModel.selectedDate.collectAsStateWithLifecycle().value
    val allTransactions = viewModel.allProductTransactions.collectAsStateWithLifecycle().value
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Let's compute weekly and monthly targets based on selectedDateStr and allTransactions
    val isSunday = remember(selectedDateStr) {
        val parts = selectedDateStr.split("-")
        val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: 2018
        val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: 9
        val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: 22
        val jdn = EthiopianCalendarHelper.ethiopianToJdn(ethYear, ethMonth, ethDay)
        (jdn + 1) % 7 == 0
    }

    // Calculate Week Range for Week totals
    val (weekBags, weekKg) = remember(allTransactions, products, selectedDateStr) {
        val (startStr, endStr) = EthiopianCalendarHelper.getRangeForEthiopianPeriod(selectedDateStr, "WEEKLY")
        val weekTrans = allTransactions.filter { it.date in startStr..endStr }
        val bags = weekTrans.sumOf { it.fabricated }
        val kg = weekTrans.sumOf { t ->
            val p = products.find { it.id == t.productId }
            t.fabricated * (p?.bagWeightKg ?: 0.5)
        }
        Pair(bags, kg)
    }

    // Calculate Month Range for Month totals
    val (monthBags, monthKg) = remember(allTransactions, products, selectedDateStr) {
        val (startStr, endStr) = EthiopianCalendarHelper.getRangeForEthiopianPeriod(selectedDateStr, "MONTHLY")
        val monthTrans = allTransactions.filter { it.date in startStr..endStr }
        val bags = monthTrans.sumOf { it.fabricated }
        val kg = monthTrans.sumOf { t ->
            val p = products.find { it.id == t.productId }
            t.fabricated * (p?.bagWeightKg ?: 0.5)
        }
        Pair(bags, kg)
    }

    // Map today's transactions for easy filling register
    val productTodayMap = remember(products, allTransactions, selectedDateStr) {
        products.associateWith { product ->
            allTransactions.find { it.productId == product.id && it.date == selectedDateStr }
        }
    }

    // Daily automatic calculations based on currently saved transactions
    val todayKgProduced = products.sumOf { p ->
        val trans = productTodayMap[p]
        (trans?.fabricated ?: 0) * p.bagWeightKg
    }
    val todayKgSold = products.sumOf { p ->
        val trans = productTodayMap[p]
        (trans?.sold ?: 0) * p.bagWeightKg
    }
    val warehouseTotalKg = products.sumOf { p ->
        p.currentStock * p.bagWeightKg
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val periods = remember {
            listOf(
                Pair(MainViewModel.ReportPeriod.Daily, "Day"),
                Pair(MainViewModel.ReportPeriod.Weekly, "Week"),
                Pair(MainViewModel.ReportPeriod.Monthly, "Month"),
                Pair(MainViewModel.ReportPeriod.Yearly, "Year")
            )
        }

        // Calculate Ethiopian weekly production list
        val weeklyDays = remember(selectedDateStr) {
            val parts = selectedDateStr.split("-")
            val ethYear = parts.getOrNull(0)?.toIntOrNull() ?: 2018
            val ethMonth = parts.getOrNull(1)?.toIntOrNull() ?: 9
            val ethDay = parts.getOrNull(2)?.toIntOrNull() ?: 22
            val jdn = EthiopianCalendarHelper.ethiopianToJdn(ethYear, ethMonth, ethDay)
            val dayOfWeek = (jdn + 1) % 7 // 0 is Sunday, 1 is Monday ... 6 is Saturday
            val offsetToMonday = if (dayOfWeek == 0) 6 else dayOfWeek - 1
            val mondayJdn = jdn - offsetToMonday
            (0..6).map { i ->
                val t = EthiopianCalendarHelper.jdnToEthiopian(mondayJdn + i)
                String.format(Locale.US, "%04d-%02d-%02d", t.first, t.second, t.third)
            }
        }

        val weeklyProductionData = remember(allTransactions, products, weeklyDays) {
            weeklyDays.map { day ->
                val dayTrans = allTransactions.filter { it.date == day }
                dayTrans.sumOf { t ->
                    val p = products.find { it.id == t.productId }
                    t.fabricated * (p?.bagWeightKg ?: 0.5)
                }
            }
        }

        // 1. Beautiful Animated Summary Card & Period Pill Switcher Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F121E)),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BentoLightGreen.copy(alpha = 0.25f),
                                Color(0xFF0F121E)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small animated factory/machine icon (Rotating Mechanical Gear on Canvas)
                        val infiniteRot = rememberInfiniteTransition(label = "gear_rot")
                        val rotationAngle by infiniteRot.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(5000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "gear_angle"
                        )

                        Canvas(modifier = Modifier.size(38.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val outerRadius = size.width * 0.45f
                            val innerRadius = size.width * 0.25f
                            val gearColor = BentoSoftGreen

                            rotate(rotationAngle, pivot = center) {
                                drawCircle(
                                    color = gearColor,
                                    radius = size.width * 0.12f,
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawCircle(
                                    color = gearColor,
                                    radius = innerRadius,
                                    center = center,
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                for (i in 0 until 8) {
                                    val angleDeg = i * 45f
                                    val angleRad = Math.toRadians(angleDeg.toDouble())
                                    val startX = center.x + innerRadius * Math.cos(angleRad).toFloat()
                                    val startY = center.y + innerRadius * Math.sin(angleRad).toFloat()
                                    val endX = center.x + outerRadius * Math.cos(angleRad).toFloat()
                                    val endY = center.y + outerRadius * Math.sin(angleRad).toFloat()
                                    drawLine(
                                        color = gearColor,
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "ANWAR INDUSTRIAL CONSOLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoGold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = EthiopianCalendarHelper.formatEthiopianDateFriendly(selectedDateStr),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    // Slide anim premium switcher
                    val selectedIndex = periods.indexOfFirst { it.first == reportPeriod }.coerceAtLeast(0)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF161618), RoundedCornerShape(20.dp))
                            .border(1.dp, BentoBorder, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        val indicatorOffset by animateDpAsState(
                            targetValue = (56.dp * selectedIndex),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "pill_offset"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .size(width = 54.dp, height = 26.dp)
                                .background(BentoForestGreen, RoundedCornerShape(16.dp))
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            periods.forEachIndexed { idx, (prodPeriod, label) ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 26.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onPeriodSelect(prodPeriod)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (reportPeriod == prodPeriod) Color.White else BentoSubText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- WEEKDAY SHIFT STATE & DATE ALERT HERO BRANDING ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSunday) BentoAlertBg.copy(alpha = 0.15f) else BentoLightGreen.copy(alpha = 0.15f)
            ),
            border = BorderStroke(1.dp, if (isSunday) BentoAlertBg.copy(alpha = 0.4f) else BentoForestGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isSunday) BentoAlertBg.copy(alpha = 0.3f) else BentoForestGreen.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSunday) "💤" else "⚙️",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = if (isSunday) "SUNDAY BREAK — OPERATIONS PAUSED" else "ACTIVE SHIFT RECORD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSunday) BentoAlertText else BentoForestGreen
                    )
                }
            }
        }

        // --- DYNAMIC PRODUCTION KPI SUMMARY COUNTERS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val neonTransition = rememberInfiniteTransition(label = "glow_kpi")
            val neonAlpha by neonTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "neon_pulse"
            )

            // Total Produced
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoLightGreen.copy(alpha = 0.25f)),
                border = BorderStroke(1.5.dp, BentoSoftGreen.copy(alpha = neonAlpha))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TOTAL PRODUCED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSoftGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedCounterText(
                        targetValue = todayKgProduced.toInt(),
                        suffix = " KG",
                        style = MaterialTheme.typography.displayMedium,
                        color = BentoSoftGreen
                    )
                    Text("Fabricated Today", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                }
            }

            // Total Sold
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F121E)),
                border = BorderStroke(1.5.dp, BentoForestGreen.copy(alpha = neonAlpha / 2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TOTAL SOLD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedCounterText(
                        targetValue = todayKgSold.toInt(),
                        suffix = " KG",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text("Shipped Today", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                }
            }
        }

        // --- LIVE ANIMATED INDUSTRIAL FACTORY FLOOR OVERVIEW ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07090E)),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANWAR INDUSTRIAL CONTROL CONSOLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoGold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Interactive SCADA floor telemetry & active lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val pulseAlphaTransition = rememberInfiniteTransition(label = "pulse_sys")
                            val sysAlpha by pulseAlphaTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "sys_alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BentoSoftGreen.copy(alpha = sysAlpha), CircleShape)
                            )
                            Text(
                                text = "SYSTEM ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = BentoSoftGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Factory Floor blueprint & machines assembly section
                val haptic = LocalHapticFeedback.current
                val machinesList = remember {
                    listOf(
                        // Row 1 (L-01 to L-05) - Film Extrusion
                        MachineConfig(0, "L-01", 0.05f, 0.20f, defaultName = "Film Line 01"),
                        MachineConfig(1, "L-02", 0.19f, 0.20f, defaultName = "Film Line 02"),
                        MachineConfig(2, "L-03", 0.33f, 0.20f, defaultName = "Film Line 03"),
                        MachineConfig(3, "L-04", 0.47f, 0.20f, defaultName = "Film Line 04"),
                        MachineConfig(4, "L-05", 0.61f, 0.20f, defaultName = "Film Line 05"),
                        // Row 2 (L-06 to L-10) - Film Extrusion
                        MachineConfig(5, "L-06", 0.05f, 0.58f, defaultName = "Film Line 06"),
                        MachineConfig(6, "L-07", 0.19f, 0.58f, defaultName = "Film Line 07"),
                        MachineConfig(7, "L-08", 0.33f, 0.58f, defaultName = "Film Line 08"),
                        MachineConfig(8, "L-09", 0.47f, 0.58f, defaultName = "Film Line 09"),
                        MachineConfig(9, "L-10", 0.61f, 0.58f, defaultName = "Film Line 10"),
                        // SOP Machine (S-01 on separate section right)
                        MachineConfig(10, "S-01", 0.83f, 0.39f, isSop = true, defaultName = "SOP Soap Line")
                    )
                }

                val filmProducts = remember(products) {
                    products.filter { !it.name.contains("soap", ignoreCase = true) && !it.name.contains("ሳሙና", ignoreCase = true) }
                }
                val soapProducts = remember(products) {
                    products.filter { it.name.contains("soap", ignoreCase = true) || it.name.contains("ሳሙና", ignoreCase = true) }
                }

                val machinePulseTransition = rememberInfiniteTransition(label = "pulse_telemetry")
                val glowAlpha by machinePulseTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_pulse"
                )
                val dotScale by machinePulseTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_scale"
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(Color(0xFF060910), RoundedCornerShape(16.dp))
                        .border(1.dp, BentoBorder, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight

                    // 1. Blueprint Grid & Barrier Partitions drawn under elements
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 0.8.dp.toPx()
                        val gridColor = BentoForestGreen.copy(alpha = 0.08f)

                        // Vertical Grid Lines
                        val xSteps = 10
                        for (i in 1 until xSteps) {
                            val x = size.width * (i.toFloat() / xSteps)
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = strokeW
                            )
                        }

                        // Horizontal Grid Lines
                        val ySteps = 8
                        for (i in 1 until ySteps) {
                            val y = size.height * (i.toFloat() / ySteps)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW
                            )
                        }

                        // Security barrier partition separating SOP soap machine
                        val partitionX = size.width * 0.77f
                        drawLine(
                            color = BentoGold.copy(alpha = 0.25f),
                            start = Offset(partitionX, 0f),
                            end = Offset(partitionX, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(20f, 15f), 
                                0f
                            )
                        )
                    }

                    // Centered High-Tech Watermark (Layered behind machines, above canvas grid lines)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ANWAR INDUSTRIAL FLOOR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.04f),
                            letterSpacing = 4.sp,
                            fontSize = 18.sp
                        )
                    }

                    // Department Titles Watermarked Top Left and Top Right
                    Text(
                        text = "DEPT A: FILM EXTRUSION ASSEMBLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoSoftGreen.copy(alpha = 0.22f),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
                    )

                    Text(
                        text = "DEPT B: SOAP (ሳሙና)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BentoGold.copy(alpha = 0.22f),
                        fontSize = 8.sp,
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd)
                    )

                    // 2. Machine Blocks Layout Mapping
                    machinesList.forEach { machine ->
                        val product = if (machine.isSop) {
                            soapProducts.getOrNull(0)
                        } else {
                            filmProducts.getOrNull(machine.index)
                        }

                        // Today's transaction telemetry stats
                        val todayTrans = product?.let { p ->
                            allTransactions.find { it.productId == p.id && it.date == selectedDateStr }
                        }
                        val todayBags = todayTrans?.fabricated ?: 0
                        val todayKg = todayBags * (product?.bagWeightKg ?: 0.5)
                        val isRunning = todayKg > 0.0

                        val mWidth = (containerWidth * 0.125f).coerceAtLeast(54.dp)
                        val mHeight = 64.dp

                        val startX = containerWidth * machine.xPercent
                        val startY = containerHeight * machine.yPercent

                        // Floating live KG counter
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = startX + (mWidth / 2f) - 24.dp,
                                        y = startY - 14.dp
                                    )
                                    .background(
                                        if (machine.isSop) BentoGold else BentoSoftGreen,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        0.5.dp,
                                        Color.White.copy(alpha = 0.4f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${todayKg.toInt()} kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        // Machine Console Block
                        Box(
                            modifier = Modifier
                                .offset(x = startX, y = startY)
                                .size(width = mWidth, height = mHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isRunning) {
                                        if (machine.isSop) Color(0xFF1E1705) else Color(0xFF061410)
                                    } else {
                                        Color(0xFF0B0D13)
                                    }
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isRunning) {
                                        if (machine.isSop) BentoGold.copy(alpha = glowAlpha) else BentoSoftGreen.copy(alpha = glowAlpha)
                                    } else if (product != null) {
                                        Color.White.copy(alpha = 0.15f)
                                    } else {
                                        Color.White.copy(alpha = 0.04f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (product != null) {
                                        onProductClick(product)
                                    } else {
                                        onAddProductClick()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Machine ID Label
                                Text(
                                    text = machine.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (machine.isSop) BentoGold else BentoSoftGreen,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Dynamic Assigned Product/State Label
                                Text(
                                    text = if (product != null) {
                                        product.name.take(7) + (if (product.name.length > 7) ".." else "")
                                    } else {
                                        "EMPTY"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (product != null) BentoSubText else Color.White.copy(alpha = 0.2f),
                                    fontSize = 7.sp,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // LED telemetry dot
                                Canvas(modifier = Modifier.size(6.dp)) {
                                    val ledColor = if (isRunning) {
                                        if (machine.isSop) BentoGold else BentoSoftGreen
                                    } else {
                                        Color(0xFFDC2626) // Red color for offline/idle
                                    }
                                    
                                    if (isRunning) {
                                        drawCircle(
                                            color = ledColor.copy(alpha = 0.35f),
                                            radius = size.width * dotScale
                                        )
                                    }
                                    drawCircle(
                                        color = ledColor,
                                        radius = size.width * 0.4f
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FACTORY PERFORMANCE SUMMARY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FACTORY PERFORMANCE SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoForestGreen,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Monthly & Weekly stats
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WEEKLY TOTALS", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${weekKg.toInt()} kg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                                Text("$weekBags bags fabricated", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                            }
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(BentoBorder))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("MONTHLY TOTALS", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${monthKg.toInt()} kg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                                Text("$monthBags bags fabricated", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                            }
                        }
                    }

                    // Right: Circular Progress Ring (Monthly Target progress fraction)
                    val monthlyTarget = 20000.0
                    val progressFraction = (monthKg / monthlyTarget).coerceIn(0.0, 1.0).toFloat()
                    val animatedProgressFraction by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = tween(1500, easing = FastOutSlowInEasing),
                        label = "circular_progress_anim"
                    )

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .border(1.dp, BentoBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(54.dp)) {
                            // Grey track ring
                            drawCircle(
                                color = BentoBorder,
                                radius = size.width / 2,
                                style = Stroke(width = 5.dp.toPx())
                            )
                            // Green glowing ring progress arc
                            drawArc(
                                color = BentoSoftGreen,
                                startAngle = -90f,
                                sweepAngle = animatedProgressFraction * 360f,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "TARGET",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = BentoSubText
                            )
                        }
                    }
                }
            }
        }

        // --- ALL ITEMS PRODUCTION SHIFT SHEET REGISTER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("shift_sheet_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRODUCT SHIFT SHEET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoForestGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Button(
                        onClick = { 
                            // Quick Action to Trigger host AddProduct modal
                            onProductClick(Product(id = 0, name = "", size = "30x40", color = "Red", counter = 500, piecesPerBag = 100, bagWeightKg = 0.25, currentStock = 0))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoLightGreen,
                            contentColor = BentoForestGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("quick_add_product_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Product Size", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Bag Size", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No product bag sizes defined. Add sizes to begin.", style = MaterialTheme.typography.bodySmall, color = BentoSubText)
                    }
                }

                products.forEach { product ->
                    val todayTrans = productTodayMap[product]
                    
                    var producedInput by remember(product.id, selectedDateStr) { 
                        mutableStateOf(if (todayTrans != null && todayTrans.fabricated > 0) todayTrans.fabricated.toString() else "") 
                    }
                    var soldInput by remember(product.id, selectedDateStr) { 
                        mutableStateOf(if (todayTrans != null && todayTrans.sold > 0) todayTrans.sold.toString() else "") 
                    }

                    val isSaved = remember(producedInput, soldInput, todayTrans) {
                        val fabVal = producedInput.toIntOrNull() ?: 0
                        val sldVal = soldInput.toIntOrNull() ?: 0
                        fabVal == (todayTrans?.fabricated ?: 0) && sldVal == (todayTrans?.sold ?: 0)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoBg.copy(alpha = 0.60f)),
                        border = BorderStroke(1.5.dp, BentoBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Row 1: Header (Product Name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${product.name} (${product.color})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextDark
                                )
                            }

                            // Dedicated Details & Stats Sheet (Size, Counter, Pieces Per Bag, Bag Weight, Stock)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, BentoBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp) // Generous padding/spacing between each detail line
                            ) {
                                // 1. Size
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Size",
                                        style = MaterialTheme.typography.bodyMedium, // Slightly larger text
                                        color = BentoSubText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.size}",
                                        style = MaterialTheme.typography.bodyMedium, // Slightly larger text
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 2. Counter
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Counter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BentoSubText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.counter}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 3. Pieces Per Bag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pieces Per Bag",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BentoSubText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.piecesPerBag}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 4. Bag Weight (kg)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bag Weight (kg)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BentoSubText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.bagWeightKg} kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 5. Current Stock Level (bags + kg in green)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Stock Level",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BentoSubText,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${product.currentStock} Bags",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoSoftGreen
                                        )
                                        Text(
                                            text = "(${String.format("%.1f", product.currentStock * product.bagWeightKg)} kg)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoSoftGreen
                                        )
                                    }
                                }
                            }

                            // Interactive Input Form Area
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Labels and Dynamic Calculated weight suffixes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val enteredFab = producedInput.toIntOrNull() ?: 0
                                    val fabSuffix = if (enteredFab > 0) " (${String.format("%.1f", enteredFab * product.bagWeightKg)} kg)" else ""
                                    Text(
                                        text = "የተመረተ$fabSuffix",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoForestGreen,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.weight(1.0f)
                                    )

                                    val enteredSold = soldInput.toIntOrNull() ?: 0
                                    val soldSuffix = if (enteredSold > 0) " (${String.format("%.1f", enteredSold * product.bagWeightKg)} kg)" else ""
                                    Text(
                                        text = "የተጫነ$soldSuffix",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoInfoText,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.weight(1.0f)
                                    )
                                    Spacer(modifier = Modifier.width(64.dp))
                                }

                                // Row 2: Inputs and save button in a sleek lower-height layout
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bags Produced Input (Smaller in height)
                                    OutlinedTextField(
                                        value = producedInput,
                                        onValueChange = { producedInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { 
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("0", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)) 
                                            }
                                        },
                                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                                        singleLine = true,
                                        modifier = Modifier
                                            .height(54.dp)
                                            .weight(1.0f)
                                            .testTag("sheet_fab_input_${product.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B),
                                            focusedBorderColor = BentoForestGreen,
                                            unfocusedBorderColor = BentoBorder,
                                            focusedPlaceholderColor = Color.Gray,
                                            unfocusedPlaceholderColor = Color.Gray
                                        )
                                    )

                                    // Bags Sold Input (Smaller in height)
                                    OutlinedTextField(
                                        value = soldInput,
                                        onValueChange = { soldInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { 
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("0", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)) 
                                            }
                                        },
                                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                                        singleLine = true,
                                        modifier = Modifier
                                            .height(54.dp)
                                            .weight(1.0f)
                                            .testTag("sheet_sold_input_${product.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B),
                                            focusedBorderColor = BentoForestGreen,
                                            unfocusedBorderColor = BentoBorder,
                                            focusedPlaceholderColor = Color.Gray,
                                            unfocusedPlaceholderColor = Color.Gray
                                        )
                                    )

                                    // Interactive Quick Save Action Button
                                    IconButton(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val fVal = producedInput.toIntOrNull() ?: 0
                                            val sVal = soldInput.toIntOrNull() ?: 0
                                            viewModel.recordProductDailyActivity(product.id, fVal, sVal, 0, "Daily entry update")
                                        },
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSaved) Color(0xFF065F46) else BentoForestGreen)
                                            .testTag("sheet_save_btn_${product.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Done,
                                            contentDescription = "Save values",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Card total summation line
                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BentoLightGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, BentoForestGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("DAILY SHEET GRAND TOTALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = BentoForestGreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total KG Manufactured today:", style = MaterialTheme.typography.labelSmall, color = BentoTextDark)
                        Text("${todayKgProduced.toInt()} KG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total KG Shipped/Sold today:", style = MaterialTheme.typography.labelSmall, color = BentoTextDark)
                        Text("${todayKgSold.toInt()} KG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoInfoText)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Remaining Warehouse Stock:", style = MaterialTheme.typography.labelSmall, color = BentoTextDark)
                        Text("${warehouseTotalKg.toInt()} KG", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                    }
                }
            }
        }

        // --- ROW 2: RAW FEEDSTOCK & MASTERBATCH AGENTS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // RAW FEEDSTOCK (weight = 0.5f)
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(144.dp)
                    .testTag("bento_feedstock_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "RAW MATERIALS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSubText,
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LDPE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoTextDark)
                            val added = stats.ldAdded
                            val used = stats.ldUsed
                            val diff = added - used
                            val indicatorText = if (diff >= 0.0) "+${diff.toInt()}kg" else "${diff.toInt()}kg"
                            val indicatorColor = if (diff >= 0.0) colorGreen() else colorRed()
                            Text(indicatorText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = indicatorColor)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HDPE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoTextDark)
                            val added = stats.hdAdded
                            val used = stats.hdUsed
                            val diff = added - used
                            val indicatorText = if (diff >= 0.0) "+${diff.toInt()}kg" else "${diff.toInt()}kg"
                            val indicatorColor = if (diff >= 0.0) colorGreen() else colorRed()
                            Text(indicatorText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = indicatorColor)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Waste", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoSubText)
                            val totalWaste = rawMaterials.find { it.type.uppercase() == "WASTE" }?.currentStock ?: 890.0
                            Text("${totalWaste.toInt()}kg", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BentoLightGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalStockKg = rawMaterials.sumOf { it.currentStock } / 1000.0
                        Text(
                            text = "${String.format("%.1ft", totalStockKg)} Total Stock",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoForestGreen
                        )
                    }
                }
            }

            // MASTERBATCH colors card (weight = 0.5f)
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(144.dp)
                    .testTag("bento_masterbatch_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                val totalMasterbatchUsed = masterbatches.sumOf { mb -> stats.masterbatchSummary[mb.id]?.used ?: 0.0 }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "MASTERBATCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSubText,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(2.dp, BentoForestGreen.copy(alpha = 0.4f), CircleShape)
                                .border(1.1.dp, BentoForestGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${totalMasterbatchUsed.toInt()}kg",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoForestGreen
                            )
                        }
                        Text(
                            text = "Used in 24h",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                    }

                    Button(
                        onClick = {
                            masterbatches.firstOrNull()?.let { onMasterbatchClick(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoLightGreen,
                            contentColor = BentoForestGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        Text("Refill", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
                  // --- ROW 3: WORKERS ON DUTY (SPAN-6) ---
        val workingCount = attendanceList.count { it.status == "On Duty" }
        val absCount = attendanceList.count { it.status == "Absent" }
        val sundayOffCount = attendanceList.count { it.status == "Sunday Off" }

        Card(
            onClick = onWorkersTabSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .testTag("bento_workers_banner_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeWorkers = workers.filter { it.isActive }
                if (activeWorkers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BentoForestGreen.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, BentoNeutralGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = BentoForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.wrapContentSize(),
                        horizontalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        val previewWorkers = activeWorkers.take(2)
                        previewWorkers.forEachIndexed { index, wk ->
                            val initials = if (wk.name.isNotBlank()) {
                                val parts = wk.name.trim().split("\\s+".toRegex())
                                if (parts.size >= 2) {
                                    (parts[0].take(1) + parts[1].take(1)).uppercase()
                                } else {
                                    parts[0].take(2).uppercase()
                                }
                            } else {
                                "?"
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BentoForestGreen.copy(alpha = if (index == 0) 0.15f else 0.3f), CircleShape)
                                    .border(2.dp, BentoNeutralGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoForestGreen
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(BentoForestGreen, CircleShape)
                                .border(2.dp, BentoNeutralGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${activeWorkers.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$workingCount Worker(s) On Duty",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoForestGreen
                    )
                    Text(
                        text = "$absCount absent • $sundayOffCount on Sunday off",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .background(BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Go to workers panel",
                        tint = BentoForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- ROW 4: ADJUSTMENTS & SOLD SHIPMENT ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val adjCount = stats.totalAdjusted
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .testTag("bento_adjustments_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoAlertBg.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, BentoAlertBg.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BentoAlertBg.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert logo",
                            tint = BentoAlertText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Adjustments",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BentoAlertText
                        )
                        val badgeTextValue = if (adjCount > 0) "+$adjCount bags" else if (adjCount < 0) "$adjCount bags" else "No issues today"
                        Text(
                            text = badgeTextValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoAlertText
                        )
                    }
                }
            }

            val shipCount = stats.totalSold
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .testTag("bento_sales_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoInfoBg.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, BentoInfoText.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BentoInfoBg.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Tag logo",
                            tint = BentoInfoText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Sold Today",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BentoInfoText
                        )
                        Text(
                            text = "$shipCount Bags shipped",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoInfoText
                        )
                    }
                }
            }
        }
    }
}


// --- REUSABLE COMPOSABLES FOR DASHBOARD BLOCKS ---

@Composable
fun InventoryHeaderSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAddClick: (() -> Unit)? = null,
    addButtonText: String = "",
    addBtnTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (onAddClick != null) {
            TextButton(
                onClick = onAddClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag(addBtnTag)
            ) {
                Text(addButtonText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ProductStockCard(
    product: Product,
    stats: ProductAggStats,
    onRecordClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3B1E293B) // Premium translucent space-gray glass effect
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF10B981), // Glowing Emerald
                    Color(0xFF10B981).copy(alpha = 0.1f) // Fading glow bottom
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Row: Product Name & Clear Prominent Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.size} • ${product.color} • ${product.bagWeightKg}kg",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = BentoSubText,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // High Contrast Stock Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (product.currentStock > 10) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            0.5.dp, 
                            if (product.currentStock > 10) Color(0xFF10B981) else Color(0xFFEF4444), 
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${product.currentStock} BAGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (product.currentStock > 10) Color(0xFF34D399) else Color(0xFFFCA5A5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats breakdown (Horizontal row of 4 columns, readable and all on 1 line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090D16), RoundedCornerShape(8.dp))
                    .border(0.5.dp, BentoBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statsList = listOf(
                    Triple("FABRICATED", "+${stats.fabricated}", BentoForestGreen),
                    Triple("SOLD", "-${stats.sold}", BentoAlertText),
                    Triple("ADJUSTED", "${if (stats.adjusted >= 0) "+" else ""}${stats.adjusted}", if (stats.adjusted >= 0) Color.White else BentoAlertText),
                    Triple("IN STOCK", "${product.currentStock}", BentoGold)
                )

                statsList.forEachIndexed { index, (label, value, color) ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(14.dp)
                                .background(BentoBorder.copy(alpha = 0.2f))
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            fontSize = 6.8.sp,
                            color = BentoSubText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = value,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = color,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action row scaled down to be compact and space-saving
            Button(
                onClick = onRecordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("record_product_btn_${product.id}"),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoLightGreen,
                    contentColor = BentoForestGreen
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Record Entry", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RawMaterialMiniCard(
    type: String,
    rawMaterial: RawMaterial,
    used: Double,
    added: Double,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("raw_material_card_${type}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = BentoForestGreen
                )
                IconButton(
                    onClick = onRecordClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add log",
                        tint = BentoForestGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Current stock
            Text("Left in Stock:", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
            Text(
                text = "${rawMaterial.currentStock.toInt()} kg",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = BentoTextDark
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = BentoBorder.copy(alpha = 0.5f)
            )

            // Dynamic Stats for scope
            Text(
                text = "Used: -${used.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = BentoAlertText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Added: +${added.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = BentoForestGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MasterbatchCard(
    masterbatch: Masterbatch,
    stats: MasterbatchAggStats,
    onRecordClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddBoughtBagsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("masterbatch_card_${masterbatch.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // makes the left accent bar fill to card height nicely!
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left visual accent bar representing masterbatch color
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = getMasterbatchColor(masterbatch.color),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )

            // Inner content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row (Color Name and Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = getMasterbatchColor(masterbatch.color),
                                    shape = CircleShape
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${masterbatch.color} ማስተርባች",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "መዝገብ አጥፋ",
                            tint = BentoAlertText.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Promo: Store Stock (በመጋዘን ያለ) shown large and bold in green
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "በመጋዘን ያለ",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val totalKg = masterbatch.currentStock
                    val fullBags = (totalKg / 25).toInt()
                    val remainingKg = (totalKg % 25).toInt()
                    val stockText = "$fullBags ከረጢት + $remainingKg ኪ.ግ"

                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSoftGreen
                    )
                }

                // Daily in/out shown clearly and evenly spaced
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "ከመጋዘን የወጣ",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                        Text(
                            text = "${stats.takenOut.toInt()} ኪ.ግ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ወደ መጋዘን የተመለሰ",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                        Text(
                            text = "${stats.returned.toInt()} ኪ.ግ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "የዕለት ፍጆታ",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoAlertText
                        )
                        Text(
                            text = "${stats.used.toInt()} ኪ.ግ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoAlertText
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // + bag ግዢ Button
                    Button(
                        onClick = onAddBoughtBagsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BentoSoftGreen.copy(alpha = 0.15f), contentColor = BentoSoftGreen),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BentoSoftGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ የተገዛ ቀለም ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Daily Ledger Log Button
                    Button(
                        onClick = onRecordClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "የዕለት ፍጆታ መዝግብ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerRowItem(
    worker: Worker,
    stats: WorkerAggStats?,
    activeAttendance: WorkerAttendance?,
    onStatusSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("worker_row_${worker.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = worker.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (worker.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            // If active, show stats range of that worker
            if (stats != null && worker.isActive) {
                Text(
                    text = "Attendance - On Duty: ${stats.daysOnDuty} days | Absent: ${stats.daysAbsent} days | Sun Off: ${stats.daysSundayOff}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            } else if (!worker.isActive) {
                Text(
                    text = "Left Company (Resigned in database)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right status toggle buttons
        if (worker.isActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val statuses = listOf(
                    Triple("On Duty", colorGreen(), "On Duty"),
                    Triple("Absent", colorRed(), "Absent"),
                    Triple("Sunday Off", colorBlue(), "Sunday Off"),
                    Triple("Left", colorGray(), "Left Company")
                )

                statuses.forEach { (statusName, badgeColor, description) ->
                    val isActiveStatus = activeAttendance?.status == statusName

                    Box(
                        modifier = Modifier
                            .clickable { onStatusSelect(statusName) }
                            .background(
                                color = if (isActiveStatus) badgeColor else badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActiveStatus) badgeColor else badgeColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("worker_opt_${worker.id}_$statusName")
                    ) {
                        Text(
                            text = statusName.substringBefore(" "), // Short labels: "On", "Abse", "Sun"
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveStatus) Color.White else badgeColor
                        )
                    }
                }
            }
        } else {
            // Already resigned worker, provide re-active button
            Button(
                onClick = { onStatusSelect("On Duty") },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Re-hire / Back", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}


// --- ALL POPUP OVERLAY IMPLEMENTATIONS ---

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, size: String, color: String, counter: Int, piecesPerBag: Int, weight: Double, stock: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("30x40") }
    var color by remember { mutableStateOf("Red") }
    var counterStr by remember { mutableStateOf("500") }
    var piecesStr by remember { mutableStateOf("100") }
    var weightStr by remember { mutableStateOf("0.25") }
    var initialStockStr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Label (e.g. Premium Shopping Bag)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_product_name")
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("Size (e.g., 30x40, 40x50)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (e.g. Red, Black, Blue)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = counterStr,
                        onValueChange = { counterStr = it },
                        label = { Text("Counter (e.g. 500)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = piecesStr,
                        onValueChange = { piecesStr = it },
                        label = { Text("Pieces/Bag (e.g. 100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight of 1 Bag in KG (e.g. 0.25)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialStockStr,
                    onValueChange = { initialStockStr = it },
                    label = { Text("Initial Stock (in bags)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val counterVal = counterStr.toIntOrNull() ?: 500
                    val piecesVal = piecesStr.toIntOrNull() ?: 100
                    val weight = weightStr.toDoubleOrNull() ?: 0.25
                    val stock = initialStockStr.toIntOrNull() ?: 0
                    if (name.isNotEmpty()) {
                        onSave(name, size, color, counterVal, piecesVal, weight, stock)
                    }
                },
                modifier = Modifier.testTag("diag_product_submit")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddMasterbatchDialog(
    onDismiss: () -> Unit,
    onSave: (color: String, initialStock: Double) -> Unit
) {
    var colorName by remember { mutableStateOf("") }
    var initialStockBagsStr by remember { mutableStateOf("2.0") }

    val computedInitialKg = remember(initialStockBagsStr) {
        val bags = initialStockBagsStr.toDoubleOrNull() ?: 0.0
        bags * 25.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("አዲስ ማስተርባች (ቀለም) መመዝገቢያ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it },
                    label = { Text("የማስተርባች ቀለም (Color Name: Green, Black...)") },
                    placeholder = { Text("ቀለም ያስገቡ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_mb_color")
                )
                OutlinedTextField(
                    value = initialStockBagsStr,
                    onValueChange = { initialStockBagsStr = it },
                    label = { Text("የመጀመሪያ ክምችት በከረጢት (Initial Bags)") },
                    supportingText = {
                        Text("→ ${computedInitialKg.toInt()} ኪ.ግ / kg (1 ከረጢት = 25 ኪ.ግ)", color = BentoSoftGreen, fontWeight = FontWeight.Bold)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (colorName.isNotEmpty()) {
                        onSave(colorName, computedInitialKg)
                    }
                },
                modifier = Modifier.testTag("diag_mb_submit")
            ) {
                Text("መዝግብ (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("አጥፋ (Cancel)") }
        }
    )
}

@Composable
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, monthlySalary: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var salaryStr by remember { mutableStateOf("10000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Employee Worker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Worker Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_worker_name")
                )
                OutlinedTextField(
                    value = salaryStr,
                    onValueChange = { salaryStr = it },
                    label = { Text("Monthly Salary (Birr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_worker_salary")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        val salary = salaryStr.toDoubleOrNull() ?: 10000.0
                        onSave(name.trim(), salary)
                    }
                },
                modifier = Modifier.testTag("diag_worker_submit")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordProductActivityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (fabricated: Int, sold: Int, adjusted: Int, notes: String) -> Unit,
    onDelete: () -> Unit
) {
    var fabStr by remember { mutableStateOf("") }
    var soldStr by remember { mutableStateOf("") }
    var adjStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDeletePinPrompt by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activity Log: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current Stock: ${product.currentStock} bags (1bag = ${product.bagWeightKg} kg)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = fabStr,
                    onValueChange = { fabStr = it },
                    label = { Text("Daily Fabricated (Bags)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_log_fab")
                )
                OutlinedTextField(
                    value = soldStr,
                    onValueChange = { soldStr = it },
                    label = { Text("Daily Sold (Bags)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_log_sold")
                )
                OutlinedTextField(
                    value = adjStr,
                    onValueChange = { adjStr = it },
                    label = { Text("Daily Adjusted Offset (e.g. -2 or 5)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_log_adj")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. damage waste, manual correction)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = fabStr.toIntOrNull() ?: 0
                    val s = soldStr.toIntOrNull() ?: 0
                    val a = adjStr.toIntOrNull() ?: 0
                    onSave(f, s, a, notes)
                },
                modifier = Modifier.testTag("diag_log_product_submit")
            ) {
                Text("Apply Activity")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showDeletePinPrompt = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("delete_product_trigger_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Product", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Product")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showDeletePinPrompt) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDeletePinPrompt = false },
            title = { Text("Confirm Product Deletion", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Deleting '${product.name}' is permanent. To prevent accidental deletion, please enter the administrator PIN (1234):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it
                            pinError = null
                        },
                        label = { Text("Security PIN") },
                        placeholder = { Text("••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_pin_input")
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == "1234") {
                            showDeletePinPrompt = false
                            onDelete()
                        } else {
                            pinError = "Incorrect PIN. Deletion denied."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("delete_confirm_btn")
                ) {
                    Text("Confirm Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeletePinPrompt = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordRawMaterialActivityDialog(
    rawMaterialType: String,
    onDismiss: () -> Unit,
    onSave: (used: Double, added: Double) -> Unit
) {
    var usedStr by remember { mutableStateOf("") }
    var addedStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activity Log: $rawMaterialType Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = usedStr,
                    onValueChange = { usedStr = it },
                    label = { Text("Used Today in Fabrication (kg)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_raw_used")
                )
                OutlinedTextField(
                    value = addedStr,
                    onValueChange = { addedStr = it },
                    label = { Text("Added Today / Purchased (kg)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_raw_added")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = usedStr.toDoubleOrNull() ?: 0.0
                    val a = addedStr.toDoubleOrNull() ?: 0.0
                    onSave(u, a)
                },
                modifier = Modifier.testTag("diag_raw_submit")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordMasterbatchActivityDialog(
    masterbatch: Masterbatch,
    stats: MasterbatchAggStats,
    onDismiss: () -> Unit,
    onSave: (used: Double, bought: Double, takenOut: Double, returned: Double) -> Unit
) {
    var takenOutStr by remember { mutableStateOf(if (stats.takenOut > 0.0) stats.takenOut.toInt().toString() else "") }
    var returnedStr by remember { mutableStateOf(if (stats.returned > 0.0) stats.returned.toInt().toString() else "") }

    val takenOut = takenOutStr.toDoubleOrNull() ?: 0.0
    val returned = returnedStr.toDoubleOrNull() ?: 0.0
    val netDailyUsage = maxOf(0.0, takenOut - returned)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("የዕለት ፍጆታ መመዝገቢያ: ${masterbatch.color}", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "የዕለት የማሽን አጠቃቀም እና የቀለም ፍጆታ መቆጣጠሪያ",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoSubText
                )

                OutlinedTextField(
                    value = takenOutStr,
                    onValueChange = { takenOutStr = it },
                    label = { Text("ከመጋዘን የወጣ - ኪ.ግ (Taken Out to Machine)") },
                    placeholder = { Text("የኪ.ግ ብዛት") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = returnedStr,
                    onValueChange = { returnedStr = it },
                    label = { Text("ወደ መጋዘን የተመለሰ - ኪ.ግ (Returned to Store)") },
                    placeholder = { Text("የኪ.ግ ብዛት") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Computed: Net Daily Usage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BentoAlertText.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "የዕለት ፍጆታ (Net Daily Usage)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoAlertText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${netDailyUsage.toInt()} ኪ.ግ (kg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoAlertText
                    )
                }

                // Computed: Remaining in store on confirmation
                val currentRemKg = maxOf(0.0, masterbatch.currentStock - netDailyUsage + stats.used)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BentoSoftGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "በመጋዘን ያለ (Remaining in Store)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSoftGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", currentRemKg / 25.0)} ከረጢት (${currentRemKg.toInt()} kg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoSoftGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(netDailyUsage, stats.bought, takenOut, returned)
                },
                modifier = Modifier.testTag("diag_mb_activity_submit")
            ) {
                Text("መዝግብ (Record)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("አጥፋ (Cancel)") }
        }
    )
}

@Composable
fun AddBoughtBagsDialog(
    masterbatch: Masterbatch,
    onDismiss: () -> Unit,
    onSave: (bags: Double) -> Unit
) {
    var bagsStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("አዲስ የከረጢት ግዢ መመዝገቢያ", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${masterbatch.color} ማስተርባች መጋዘን ውስጥ ለመጨመር የከረጢት ብዛት ያስገቡ (1 ከረጢት = 25 ኪ.ግ)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BentoSubText
                )
                OutlinedTextField(
                    value = bagsStr,
                    onValueChange = { bagsStr = it },
                    label = { Text("የተገዛ የከረጢት ብዛት (Bags)") },
                    placeholder = { Text("ከረጢት ብዛት") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bags = bagsStr.toDoubleOrNull() ?: 0.0
                    if (bags > 0) {
                        onSave(bags)
                    }
                }
            ) {
                Text("መዝግብ (Add)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("አጥፋ (Cancel)") }
        }
    )
}

@Composable
fun DatePickerFallbackDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onDateConfirm: (String) -> Unit
) {
    val initialParts = currentDate.split("-")
    var y by remember { mutableStateOf(initialParts.getOrNull(0)?.toIntOrNull() ?: 2018) }
    var m by remember { mutableStateOf(initialParts.getOrNull(1)?.toIntOrNull() ?: 9) }
    var d by remember { mutableStateOf(initialParts.getOrNull(2)?.toIntOrNull() ?: 22) }

    val todayParts = remember { EthiopianCalendarHelper.getTodayEthiopianString().split("-") }
    val todayYear = todayParts.getOrNull(0)?.toIntOrNull() ?: 2018
    val todayMonth = todayParts.getOrNull(1)?.toIntOrNull() ?: 9
    val todayDay = todayParts.getOrNull(2)?.toIntOrNull() ?: 22

    val maxDay = remember(y, m) {
        if (m == 13) {
            if (EthiopianCalendarHelper.isEthiopianLeapYear(y)) 6 else 5
        } else {
            30
        }
    }

    LaunchedEffect(maxDay) {
        if (d > maxDay) {
            d = maxDay
        }
    }

    val startWeekday = remember(y, m) {
        val firstDayJdn = EthiopianCalendarHelper.ethiopianToJdn(y, m, 1)
        (firstDayJdn + 1) % 7
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "የቀን መምረጫ (Date Picker)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BentoForestGreen
                )

                // Month / Year selector header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (m == 1) {
                                m = 13
                                y--
                            } else {
                                m--
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = BentoForestGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentMonthName = EthiopianCalendarHelper.ETHIOPIAN_MONTHS.getOrNull(m - 1) ?: ""
                        Text(
                            text = currentMonthName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark
                        )
                        Text(
                            text = "$y ዓ.ም.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoSubText
                        )
                    }

                    IconButton(
                        onClick = {
                            if (m == 13) {
                                m = 1
                                y++
                            } else {
                                m++
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = BentoForestGreen
                        )
                    }
                }

                // Quick Year shifting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (y > 2000) y-- },
                        modifier = Modifier.height(32.dp).testTag("dp_prev_year_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoForestGreen),
                        border = BorderStroke(1.dp, BentoBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("-1 ዓመት", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { if (y < 2100) y++ },
                        modifier = Modifier.height(32.dp).testTag("dp_next_year_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoForestGreen),
                        border = BorderStroke(1.dp, BentoBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+1 ዓመት", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                // Short weekdays block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val amharicShortWeekdays = listOf("እሁድ", "ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "አርብ", "ቅዳሜ")
                    amharicShortWeekdays.forEach { wd ->
                        Text(
                            text = wd.take(3), // Limit width safely
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoSubText
                        )
                    }
                }

                // Grid of Days
                val totalSlots = startWeekday + maxDay
                val rowsCount = (totalSlots + 6) / 7

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (r in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until 7) {
                                val slotIndex = r * 7 + c
                                if (slotIndex < startWeekday || slotIndex >= totalSlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val dayNum = slotIndex - startWeekday + 1
                                    val isSelectedDay = (dayNum == d)
                                    val isToday = (y == todayYear && m == todayMonth && dayNum == todayDay)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelectedDay -> BentoForestGreen
                                                    isToday -> BentoSoftGreen
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelectedDay) 1.dp else 0.dp,
                                                color = if (isToday && !isSelectedDay) BentoForestGreen else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                d = dayNum
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelectedDay || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelectedDay -> Color.White
                                                isToday -> BentoForestGreen
                                                else -> BentoTextDark
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                // Bottom actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            y = todayYear
                            m = todayMonth
                            d = todayDay
                        },
                        modifier = Modifier.testTag("dp_today_btn")
                    ) {
                        Text("ዛሬ (Today)", color = BentoForestGreen, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = BentoSubText)
                        }
                        Button(
                            onClick = {
                                val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
                                onDateConfirm(formatted)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dp_submit")
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}


// --- UTILITIES & COLOR CONVERTERS ---

private fun shiftDate(viewModel: MainViewModel, currentDateStr: String, offsetDays: Int) {
    try {
        val newDate = EthiopianCalendarHelper.shiftEthiopianDate(currentDateStr, offsetDays)
        viewModel.updateSelectedDate(newDate)
    } catch (e: Exception) {
        // Safe fall back
    }
}

private fun formatDateFriendly(dateStr: String): String {
    return try {
        EthiopianCalendarHelper.formatEthiopianDateFriendly(dateStr)
    } catch (e: Exception) {
        dateStr
    }
}

private fun getMasterbatchColor(colorName: String): Color {
    return when (colorName.lowercase().trim()) {
        "black" -> Color(0xFF1E1E1E)
        "white" -> Color(0xFFEEEEEE)
        "red" -> Color(0xFFD32F2F)
        "blue" -> Color(0xFF1976D2)
        "green" -> Color(0xFF388E3C)
        "yellow" -> Color(0xFFFBC02D)
        "orange" -> Color(0xFFF57C00)
        "purple" -> Color(0xFF7B1FA2)
        else -> Color(0xFF9E9E9E) // Gray baseline
    }
}

private fun Color.Companion.whiteOrFallback(): Color {
    return Color.White
}

private fun colorGreen(): Color = Color(0xFF2E7D32)
private fun colorRed(): Color = Color(0xFFC62828)
private fun colorBlue(): Color = Color(0xFF1565C0)
private fun colorGray(): Color = Color(0xFF555555)

// Simple mapping for icon safety
@Composable
fun imageIconsList(label: String): androidx.compose.ui.graphics.vector.ImageVector? {
    return when (label) {
        "recycle" -> Icons.Default.Refresh
        else -> null
    }
}

// --- LIVE ACTIVITY LOGS COMPOSABLE ---
@Composable
fun ActivityLogSection(
    activityLogs: List<ActivityLog>,
    announcements: List<Announcement> = emptyList(),
    onDeleteAnnouncement: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedActionFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Product", "Raw Material", "Masterbatch", "Worker")
    val actions = listOf("All", "Add", "Edit", "Delete")

    // Filter logic
    val filteredLogs = activityLogs.filter { log ->
        val matchesSearch = log.description.contains(searchQuery, ignoreCase = true) ||
                log.deviceName.contains(searchQuery, ignoreCase = true) ||
                log.ethiopianDateTime.contains(searchQuery, ignoreCase = true)
        
        val matchesCategory = selectedCategoryFilter == "All" || log.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesAction = selectedActionFilter == "All" || log.actionType.equals(selectedActionFilter, ignoreCase = true)
        
        matchesSearch && matchesCategory && matchesAction
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("activity_logs_section_container")
    ) {
        // Section Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BentoForestGreen.copy(alpha = 0.15f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Live Activity",
                            tint = BentoForestGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "LIVE ACTIVITY MONITOR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Track logs across all connected devices in Ethiopian calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoSubText
                        )
                    }
                }
            }
        }

        // Render current posted announcements if available
        if (announcements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F121E)), // Deep luxury border highlighted container
                border = BorderStroke(1.dp, BentoForestGreen.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "📢 ማስታወቂያ ሰሌዳ (Active Announcements)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoGold
                    )
                    
                    announcements.forEach { ann ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161A29), RoundedCornerShape(12.dp))
                                .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(BentoSoftGreen, CircleShape)
                                    )
                                    Text(
                                        text = ann.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ann.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ቀን፡ ${ann.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BentoSubText
                                )
                            }
                            if (onDeleteAnnouncement != null) {
                                IconButton(
                                    onClick = { onDeleteAnnouncement(ann.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Announcement",
                                        tint = BentoAlertText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Stats overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats 1: Total records
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoForestGreen, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("TOTAL RECORDS", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                        Text("${activityLogs.size} logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = BentoForestGreen)
                    }
                }
            }
            // Stats 2: Connected Devices
            val uniqueDevicesCount = activityLogs.map { it.deviceName }.distinct().size
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(BentoInfoBg.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = BentoInfoText, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("DEVICES", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                        Text("$uniqueDevicesCount devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = BentoInfoText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("log_search_input"),
            placeholder = { Text("Search logs or devices...", color = BentoSubText, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoSubText) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BentoNeutralGray.copy(alpha = 0.5f),
                unfocusedContainerColor = BentoNeutralGray.copy(alpha = 0.5f),
                focusedBorderColor = BentoForestGreen,
                unfocusedBorderColor = BentoBorder
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters badges row: Category
        Text(
            text = "Filter by Category:",
            style = MaterialTheme.typography.labelMedium,
            color = BentoSubText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) BentoForestGreen else BentoNeutralGray)
                        .border(1.dp, if (isSelected) BentoForestGreen else BentoBorder, CircleShape)
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_cat_$cat")
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF0F172A) else BentoSubText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filters badges row: Action
        Text(
            text = "Filter by Action:",
            style = MaterialTheme.typography.labelMedium,
            color = BentoSubText,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            actions.forEach { act ->
                val isSelected = selectedActionFilter == act
                val actColor = when (act) {
                    "Add" -> BentoForestGreen
                    "Edit" -> Color(0xFFF59E0B)
                    "Delete" -> Color(0xFFEF4444)
                    else -> BentoInfoText
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) actColor.copy(alpha = 0.25f) else BentoNeutralGray)
                        .border(1.dp, if (isSelected) actColor else BentoBorder, CircleShape)
                        .clickable { selectedActionFilter = act }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("filter_act_$act")
                ) {
                    Text(
                        text = act,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) actColor else BentoSubText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Logs dynamic list display
        if (filteredLogs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "No logs",
                        tint = BentoSubText,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recorded activity logs match the selected filter criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BentoSubText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredLogs.forEach { log ->
                    ActivityLogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
fun ActivityLogItemCard(log: ActivityLog) {
    val (icon, tintColor) = when (log.actionType) {
        "Add" -> Pair(Icons.Default.Add, BentoForestGreen)
        "Delete" -> Pair(Icons.Default.Delete, Color(0xFFEF4444))
        else -> Pair(Icons.Default.Edit, Color(0xFFF59E0B)) // Edit/Adjustment
    }

    val catIcon = when (log.category) {
        "Product" -> Icons.Default.Build
        "Raw Material" -> Icons.Default.List
        "Masterbatch" -> Icons.Default.Edit
        "Worker" -> Icons.Default.Person
        else -> Icons.Default.CheckCircle // Attendance or general
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Row metadata: Action icon + type, Category, device
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(tintColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, tintColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = log.actionType,
                            tint = tintColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Text(
                        text = log.actionType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = tintColor
                    )
                }

                // Category badge
                Box(
                    modifier = Modifier
                        .background(BentoInfoBg.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, BentoInfoText.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            catIcon,
                            contentDescription = log.category,
                            tint = BentoInfoText,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = log.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoInfoText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main description
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = BentoBorder.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: device + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Device",
                        tint = BentoSubText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = log.deviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText
                    )
                }

                // Ethiopian DateTime
                Text(
                    text = log.ethiopianDateTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimatedCounterText(
    targetValue: Int,
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight = FontWeight.Bold
) {
    var animatedValue by remember { mutableStateOf(0) }
    LaunchedEffect(targetValue) {
        val animation = androidx.compose.animation.core.Animatable(0f)
        animation.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1500,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) {
            animatedValue = this.value.toInt()
        }
    }
    Text(
        text = "$animatedValue$suffix",
        style = style,
        fontWeight = fontWeight,
        color = color
    )
}

@Composable
fun AnwarSplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var startExitAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Animate progress bar from 0 to 1 over 2.2 seconds
        val duration = 2200f
        val steps = 50
        val delayTime = (duration / steps).toLong()
        for (i in 1..steps) {
            kotlinx.coroutines.delay(delayTime)
            progress = i / steps.toFloat()
        }
        startExitAnimation = true
        kotlinx.coroutines.delay(400) // allow fadeout animation
        onFinished()
    }

    val alpha by animateFloatAsState(
        targetValue = if (startExitAnimation) 0f else 1f,
        animationSpec = tween(400),
        label = "splash_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (startExitAnimation) 0.9f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Anwar Logo Badge
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color.Black, shape = CircleShape)
                    .border(2.dp, BentoGold, shape = CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_anwar_logo),
                    contentDescription = "Anwar Company Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "ANWAR",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "RECYCLING OPERATIONS CONSOLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37), // Luxury Gold
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Premium linear progress indicator
            Column(
                modifier = Modifier.width(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "BOOTING STATUS ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}


// --- BINIYAM CHAT SCREEN COMPOSABLE ---

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun BiniyamBotScreen(
    viewModel: MainViewModel,
    stats: AggregatedStats,
    products: List<Product>,
    rawMaterials: List<RawMaterial>,
    masterbatches: List<Masterbatch>,
    workers: List<Worker>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var inputText by remember { mutableStateOf("") }
    
    // Voice Input & Output states
    var isAmharicInput by remember { mutableStateOf(true) }
    var currentlySpeakingMsgId by remember { mutableStateOf<String?>(null) }
    
    // Setup Text-To-Speech instance
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

    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    sender = "model",
                    text = "ሰላም! ሰላም! እኔ ቢኒያም (BINIYAM) እባላለሁ - የአንዋር ፕላስቲክ መልሶ ማምረቻ ኩባንያ (Anwar Plastic Recycle) ብቸኛ ዲጂታል ረዳት። በቢኒያም የተሠራሁት እኔ፣ ስለ ምርት ሂደቶች፣ ስለ ክምችት መጠን (Stock)፣ ስለ ጥሬ ዕቃዎች እና ስለ ሠራተኞቻችን በማንኛውም ሰዓት በ አማርኛ ወይም በእንግሊዝኛ መረጃ መስጠት እችላለሁ። ዛሬ በምን ልርዳዎት?\n\nHello! I am BINIYAM, the official AI assistant of Anwar Plastic Recycle. Created by Biniyam, I am here to help you manage and answer any questions regarding our production sheets, worker attendance, raw materials, and stock balances in both Amharic and English. How can I assist you today?"
                )
            )
        }
    }
    var isThinking by remember { mutableStateOf(false) }

    // Detect if text contains Ge'ez script (Amharic characters)
    fun isAmharic(text: String): Boolean {
        for (char in text) {
            if (char.code in 0x1200..0x137F) {
                return true
            }
        }
        return false
    }

    // Clean up markdown tags from the text before voice pronouncement
    fun cleanTextForTts(text: String): String {
        return text.replace("**", "")
            .replace("*", "")
            .replace("###", "")
            .replace("##", "")
            .replace("#", "")
            .replace("`", "")
            .replace("_", "")
    }

    // Core read out loud speak function
    fun speakMessage(message: ChatMessage) {
        if (currentlySpeakingMsgId == message.id) {
            tts?.stop()
            currentlySpeakingMsgId = null
            return
        }
        
        tts?.stop()
        val textToSpeak = cleanTextForTts(message.text)
        val isAmh = isAmharic(textToSpeak)
        
        val ttsEngine = tts
        if (ttsEngine != null && isTtsReady) {
            val locale = if (isAmh) Locale("am", "ET") else Locale.US
            val langResult = ttsEngine.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.w("BiniyamBot", "Locale $locale not fully supported/available, running default.")
                ttsEngine.setLanguage(Locale.US)
            }
            
            // Premium tone & speed adjustment
            ttsEngine.setPitch(1.05f)
            ttsEngine.setSpeechRate(0.95f)

            ttsEngine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    currentlySpeakingMsgId = message.id
                }

                override fun onDone(utteranceId: String?) {
                    if (currentlySpeakingMsgId == message.id) {
                        currentlySpeakingMsgId = null
                    }
                }

                @Deprecated("Deprecated")
                override fun onError(utteranceId: String?) {
                    if (currentlySpeakingMsgId == message.id) {
                        currentlySpeakingMsgId = null
                    }
                }
            })

            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, message.id)
            }
            ttsEngine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, message.id)
            currentlySpeakingMsgId = message.id
        }
    }

    // Speech-To-Text compose launcher activity result listener
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                inputText = spokenText
            }
        }
    }

    fun startVoiceInput(isAmharicSelected: Boolean) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (isAmharicSelected) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "am-ET")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "am-ET")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "am-ET")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "በአማርኛ ይናገሩ... / Speak Amharic...")
            } else {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in English...")
            }
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("BiniyamBot", "Speech recognition launch failed: ${e.message}")
            Toast.makeText(context, "Speech recognizer is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Auto scroll state
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Suggested quick actions
    val suggestions = listOf(
        "የአሁኑን የክምችት መጠን ማጠቃለያ ስጠኝ" to "ያለውን የክምችት (Stock) ሁኔታ በዝርዝር ንገረኝ።",
        "የዛሬው ምርትና ሽያጭ እንዴት ነው?" to "የዛሬውን የምርት এবং የሽያጭ ሁኔታ (Fabricated and Sold) አጠቃላይ መረጃ ስጠኝ።",
        "ስለ ጥሬ ዕቃዎች (Raw Materials) ንገረኝ" to "ወቅታዊ ያለውን የጥሬ ዕቃዎች ደረጃ (LD, HD, Waste Stock) ንገረኝ።",
        "የሠራተኞች ሁኔታ እንዴት ነው?" to "በድርጅቱ ውስጥ ያሉትን የሠራተኞች ሁኔታ እና መገኘታቸውን ንገረኝ።"
    )

    fun sendMessage(textToSend: String) {
        if (textToSend.isBlank() || isThinking) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        
        messages.add(ChatMessage(sender = "user", text = textToSend))
        isThinking = true
        inputText = ""

        coroutineScope.launch {
            val liveDate = viewModel.selectedDate.value
            val systemPrompt = generateSystemPrompt(
                products = products,
                rawMaterials = rawMaterials,
                masterbatches = masterbatches,
                workers = workers,
                activeDate = liveDate,
                stats = stats
            )
            
            // Build chat query history for the bot context
            val historyList = messages.drop(1).dropLast(1).map { Pair(it.sender, it.text) }

            val response = GeminiBotService.getGeminiResponse(
                systemPrompt = systemPrompt,
                userPrompt = textToSend,
                history = historyList
            )

            val newMsg = ChatMessage(sender = "model", text = response)
            messages.add(newMsg)
            isThinking = false
            // Auto-speak response
            speakMessage(newMsg)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04060C)) // Ultra luxury jet black background
            .padding(12.dp)
    ) {
        // AI Profile Card (Apple Style Glassmorphic)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(
                    color = Color(0xFF0F172A).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, BentoForestGreen.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Robot Icon Wrapper with subtle gold aura indicator
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(1.5.dp, BentoGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "BINIYAM robot",
                        tint = BentoForestGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    // Live green pulse dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                            .border(1.5.dp, Color.Black, CircleShape)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BINIYAM AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(BentoForestGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, BentoForestGreen, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OFFICIAL BOT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoForestGreen
                            )
                        }
                    }
                    Text(
                        text = "Authorized assistant owned by Biniyam",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText
                    )
                }
            }
        }

        // Messages List Area
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(messages) { message ->
                val isMe = message.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .background(
                                color = if (isMe) Color(0xFF1F2937) else Color(0xFF03311E).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMe) 18.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 18.dp
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (isMe) Color(0xFF374151) else BentoForestGreen.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMe) 18.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 18.dp
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            if (!isMe) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = null,
                                            tint = BentoGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "BINIYAM AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoGold
                                        )
                                    }
                                    IconButton(
                                        onClick = { speakMessage(message) },
                                        modifier = Modifier.size(24.dp).testTag("speech_speak_btn_${message.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (currentlySpeakingMsgId == message.id) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Hear message out loud",
                                            tint = if (currentlySpeakingMsgId == message.id) BentoGold else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
                            border = BorderStroke(1.dp, BentoForestGreen.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = BentoForestGreen
                                )
                                Text(
                                    text = "BINIYAM is analyzing terminal...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BentoSubText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Suggestions Horizontal Scroll (when idle)
        if (!isThinking) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(suggestions) { (label, promptText) ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0C101B), shape = RoundedCornerShape(16.dp))
                            .border(0.5.dp, BentoForestGreen.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .clickable {
                                sendMessage(promptText)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = BentoGold, modifier = Modifier.size(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Input Box area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .background(Color(0xFF0C101B), RoundedCornerShape(24.dp))
                .border(1.dp, BentoForestGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Input & Custom language toggle controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Speech Input Target selection indicator/toggle
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isAmharicInput) BentoForestGreen.copy(alpha = 0.15f) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isAmharicInput) BentoForestGreen else Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            isAmharicInput = !isAmharicInput
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isAmharicInput) "አማርኛ" else "EN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAmharicInput) BentoForestGreen else Color.White
                    )
                }

                // Voice Mic Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        startVoiceInput(isAmharicInput)
                    },
                    modifier = Modifier.size(36.dp).testTag("biniyam_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone voice input",
                        tint = if (isAmharicInput) BentoForestGreen else BentoGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "ቢኒያምን ይጠይቁ / Ask BINIYAM...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BentoSubText
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("biniyam_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )

            IconButton(
                onClick = { sendMessage(inputText) },
                enabled = inputText.isNotBlank() && !isThinking,
                modifier = Modifier.testTag("biniyam_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send prompt button",
                    tint = if (inputText.isNotBlank() && !isThinking) BentoForestGreen else BentoSubText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(84.dp)) // padding for floating active bar
    }
}

// System Prompt Helper for Biniyam context injection
private fun generateSystemPrompt(
    products: List<Product>,
    rawMaterials: List<RawMaterial>,
    masterbatches: List<Masterbatch>,
    workers: List<Worker>,
    activeDate: String,
    stats: AggregatedStats
): String {
    val sb = StringBuilder()
    sb.append("You are BINIYAM, the official AI chat assistant of Anwar Plastic Recycle Company. ")
    sb.append("You are owned and created by Biniyam, the chief administrator and owner of the company.\n")
    sb.append("Your duty is to answer questions about the production, inventory stock, raw materials, masterbatches, and workers of the company. ")
    sb.append("You must answer in a helpful, friendly, and extremely professional manner. ")
    sb.append("You are fully bilingual and can speak, understand, and write perfectly in both Amharic (አማርኛ) and English. Always reply in the language the user asks you in, or mix them gracefully if appropriate (such as explaining Amharic terms in English or vice-versa).\n\n")
    
    sb.append("=== CURRENT LIVE DATA IN THE PLASTIC RECYCLING SYSTEM ===\n")
    sb.append("Active Ethiopian Calendar Date: $activeDate\n\n")
    
    sb.append("1. PRODUCTS STOCK IN INVENTORY:\n")
    if (products.isEmpty()) {
        sb.append("- No products registered in system yet.\n")
    } else {
        products.forEach { p ->
            sb.append("- Name: ${p.name}, Size: ${p.size}, Color: ${p.color}, Current Stock: ${p.currentStock} bags (Counter: ${p.counter}, Pieces/Bag: ${p.piecesPerBag}, Bag Weight: ${p.bagWeightKg} kg, ID: ${p.id})\n")
        }
    }
    sb.append("\n")

    sb.append("2. RAW MATERIALS LEVEL:\n")
    if (rawMaterials.isEmpty()) {
        sb.append("- No raw materials recorded.\n")
    } else {
        rawMaterials.forEach { r ->
            sb.append("- Material Type: ${r.type}, Current Stock Level: ${r.currentStock} kg\n")
        }
    }
    sb.append("\n")

    sb.append("3. MASTERBATCH PIGMENTS BASE:\n")
    if (masterbatches.isEmpty()) {
        sb.append("- No masterbatches registered.\n")
    } else {
        masterbatches.forEach { m ->
            sb.append("- Color: ${m.color}, Current Stock: ${m.currentStock} kg (ID: ${m.id})\n")
        }
    }
    sb.append("\n")

    sb.append("4. WORKERS ON DUTY:\n")
    if (workers.isEmpty()) {
        sb.append("- No workers registered in system.\n")
    } else {
        val activeCount = workers.count { it.isActive }
        sb.append("- Total Registered Workers: ${workers.size} (Active: $activeCount)\n")
        workers.forEach { w ->
            sb.append("  * Name: ${w.name}, Join Date: ${w.joinDate}, Status: ${if (w.isActive) "Active" else "Left"}\n")
        }
    }
    sb.append("\n")

    sb.append("5. RECENT AGGREGATED METRICS (Production sheets performance from ${stats.startDate} to ${stats.endDate}):\n")
    sb.append("- Total Fabricated Products: ${stats.totalFabricated} bags\n")
    sb.append("- Total Sold Products: ${stats.totalSold} bags\n")
    sb.append("- Total Adjusted Products: ${stats.totalAdjusted} bags\n")
    sb.append("- LD Material (Used: ${stats.ldUsed} kg, Added: ${stats.ldAdded} kg)\n")
    sb.append("- HD Material (Used: ${stats.hdUsed} kg, Added: ${stats.hdAdded} kg)\n")
    sb.append("- Waste/West Material (Used: ${stats.wasteUsed} kg, Added: ${stats.wasteAdded} kg)\n\n")

    sb.append("=== INSTRUCTIONS ===\n")
    sb.append("- Be conversational and write clearly. Maintain an elegant, premium look.\n")
    sb.append("- When asked who owns you or Biniyam, always reply clearly and with pride that Biniyam is your creator and owner, who also owns Anwar Plastic Recycle Company.\n")
    sb.append("- In Amharic, write using perfect, polite Amharic (e.g. use 'ይችላሉ', 'እባክዎን', 'እንኳን ደህና መጡ').")
    
    return sb.toString()
}


// Network service for direct REST call using OkHttp and native org.json
object GeminiBotService {
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun getGeminiResponse(systemPrompt: String, userPrompt: String, history: List<Pair<String, String>> = emptyList()): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "አዝናለሁ! የቢኒያም AI ሰርቨር ቁልፍ (Gemini API Key) በሲስተሙ ውስጥ አልተገኘም። እባክዎን በ AI Studio Secrets በኩል 'GEMINI_API_KEY' ማስገባትዎን ያረጋግጡ።\n\nSorry, the Gemini API Key is missing or not configured in Secrets. Please configure GEMINI_API_KEY in active Secrets to enable AI queries."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            val requestJson = org.json.JSONObject()
            val contentsArray = org.json.JSONArray()

            // Map and add previous history
            for (turn in history) {
                val roleName = if (turn.first == "user") "user" else "model"
                val turnObj = org.json.JSONObject()
                turnObj.put("role", roleName)
                
                val partsArr = org.json.JSONArray()
                val partObj = org.json.JSONObject()
                partObj.put("text", turn.second)
                partsArr.put(partObj)
                
                turnObj.put("parts", partsArr)
                contentsArray.put(turnObj)
            }

            // Add the newest query
            val newestTurn = org.json.JSONObject()
            newestTurn.put("role", "user")
            val partsArr = org.json.JSONArray()
            val partObj = org.json.JSONObject()
            partObj.put("text", userPrompt)
            partsArr.put(partObj)
            newestTurn.put("parts", partsArr)
            contentsArray.put(newestTurn)

            requestJson.put("contents", contentsArray)

            // Inject system instruction if provided
            if (systemPrompt.isNotBlank()) {
                val systemInstructionObj = org.json.JSONObject()
                val sPartsArr = org.json.JSONArray()
                val sPartObj = org.json.JSONObject()
                sPartObj.put("text", systemPrompt)
                sPartsArr.put(sPartObj)
                systemInstructionObj.put("parts", sPartsArr)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            // Force dynamic response
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNotificationDialog(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("update_notification_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)), // Pure carbon black dark theme
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ANWAR logo at top
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(2.dp, BentoGold, shape = CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_anwar_logo),
                        contentDescription = "Anwar Company Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                // Title "📢 notification" in bold green
                Text(
                    text = "📢 notification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSoftGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = BentoBorder)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // The announcement message in white text
                    Text(
                        text = announcement.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ቀን: ${announcement.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Green "update" button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoSoftGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("diag_announcement_dismiss")
                ) {
                    Text(
                        text = "update",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PostAnnouncementDialog(
    onDismiss: () -> Unit,
    onPost: (title: String, message: String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📢 መልዕክት መለጠፊያ (Post Announcement)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "መልዕክቱን ለመለጠፍ እባክዎ የአድሚን የይለፍ ቃል (1234) ያስገቡ:",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoSubText
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it
                        showError = false
                    },
                    label = { Text("Admin PIN (e.g. 1234)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth().testTag("admin_post_pin")
                )

                if (showError) {
                    Text(
                        "የገቡት የይለፍ ቃል የተሳሳተ ነው!",
                        color = BentoAlertText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("አርዕስት (Announcement Title)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    placeholder = { Text("e.g. 📢 Operations Update") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_post_title")
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("መልዕክት (Message)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    placeholder = { Text("e.g. Shift 2 timings will be changed...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("admin_post_message")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText == "1234") {
                        if (titleText.isNotBlank() && messageText.isNotBlank()) {
                            onPost(titleText, messageText)
                        }
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoSoftGreen, contentColor = Color.Black)
            ) {
                Text("ለጥፍ (Post)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("አጥፋ (Cancel)", color = Color.White)
            }
        }
    )
}

fun isNewerVersion(local: String, remote: String): Boolean {
    val localParts = local.split(".").map { it.trim().toIntOrNull() ?: 0 }
    val remoteParts = remote.split(".").map { it.trim().toIntOrNull() ?: 0 }
    val length = maxOf(localParts.size, remoteParts.size)
    for (i in 0 until length) {
        val l = localParts.getOrNull(i) ?: 0
        val r = remoteParts.getOrNull(i) ?: 0
        if (r > l) return true
        if (l > r) return false
    }
    return false
}

@Composable
fun VersionUpdatePopupDialog(
    appVersion: AppVersion,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(
        onDismissRequest = {
            if (!appVersion.isMandatory) {
                onDismiss()
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !appVersion.isMandatory,
            dismissOnClickOutside = !appVersion.isMandatory
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("version_update_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ANWAR logo at top
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.Black, shape = CircleShape)
                        .border(2.dp, BentoGold, shape = CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.img_anwar_logo),
                        contentDescription = "Anwar Company Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                // Title "New Version Available!"
                Text(
                    text = "New Version Available!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSoftGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = BentoBorder)

                // Current version and New version numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Version", style = MaterialTheme.typography.bodySmall, color = BentoSubText)
                        Text("v$CURRENT_APP_VERSION", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BentoSoftGreen,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Latest Version", style = MaterialTheme.typography.bodySmall, color = BentoSubText)
                        Text("v${appVersion.versionName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = BentoGold)
                    }
                }

                if (appVersion.isMandatory) {
                    Box(
                        modifier = Modifier
                            .background(BentoAlertText.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, BentoAlertText.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠️ This is a mandatory update to continue using the app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoAlertText,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Release notes / changelog text
                if (appVersion.changelog.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "What's New:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BentoGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appVersion.changelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Green "Update Now" button
                Button(
                    onClick = {
                        val finalUrl = appVersion.apkUrl.ifBlank { "https://github.com/guadebiniam-del/Biniyam/raw/main/.build-outputs/app-debug.apk" }
                        val urlToOpen = if (finalUrl.startsWith("http://") || finalUrl.startsWith("https://")) {
                            finalUrl
                        } else {
                            "https://$finalUrl"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(urlToOpen))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open browser link: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoSoftGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("diag_version_update_now_btn")
                ) {
                    Text(
                        text = "Update Now",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // "Later" button only if update is not mandatory
                if (!appVersion.isMandatory) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().testTag("diag_version_update_later_btn")
                    ) {
                        Text(
                            "Later",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAppVersionDialog(
    currentLatestVersionName: String,
    currentLatestApkUrl: String,
    currentLatestChangelog: String,
    currentLatestIsMandatory: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (version: String, apkUrl: String, changelog: String, isMandatory: Boolean) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var versionText by remember { mutableStateOf(currentLatestVersionName) }
    var apkUrlText by remember { mutableStateOf(currentLatestApkUrl) }
    var changelogText by remember { mutableStateOf(currentLatestChangelog) }
    var isMandatory by remember { mutableStateOf(currentLatestIsMandatory) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🛠️ App Version Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "ይህ ክፍል የአፕሊኬሽን እትም ለመለወጥና ለማሻሻል ያገለግላል። (የአድሚን ክፍያ ቃል (1234) ያስገቡ):",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoSubText
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it
                        showError = false
                    },
                    label = { Text("Admin PIN (e.g. 1234)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth().testTag("admin_version_pin")
                )

                if (showError) {
                    Text(
                        "የገቡት የይለፍ ቃል የተሳሳተ ነው!",
                        color = BentoAlertText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = versionText,
                    onValueChange = { versionText = it },
                    label = { Text("Latest Version Number (e.g. 1.0.1)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_version_number")
                )

                OutlinedTextField(
                    value = apkUrlText,
                    onValueChange = { apkUrlText = it },
                    label = { Text("APK Link", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_version_apk_url")
                )

                OutlinedTextField(
                    value = changelogText,
                    onValueChange = { changelogText = it },
                    label = { Text("የለውጥ ዝርዝር (Changelog / Release Notes)", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoSoftGreen,
                        unfocusedBorderColor = BentoBorder
                    ),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("admin_version_changelog")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mandatory Update",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "ፎርስድ አፕዴት (ለመጠቀም መዘመን አለበት)",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                    }
                    Switch(
                        checked = isMandatory,
                        onCheckedChange = { isMandatory = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BentoSoftGreen,
                            checkedTrackColor = BentoForestGreen
                        ),
                        modifier = Modifier.testTag("admin_version_mandatory_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText == "1234") {
                        if (versionText.isNotBlank() && apkUrlText.isNotBlank()) {
                            onUpdate(versionText, apkUrlText, changelogText, isMandatory)
                        }
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoSoftGreen, contentColor = Color.Black)
            ) {
                Text("አዘምን (Confirm)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ማጣሪያ (Cancel)", color = Color.White)
            }
        }
    )
}
