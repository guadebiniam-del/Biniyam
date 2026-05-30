package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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

    // Active tab selection matching the Bento Grid HTML design: Daily Overview, Inventory, Workers, Activity Log
    var activeTab by remember { mutableStateOf("Daily Overview") }

    // Dialog trigger states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddMasterbatchDialog by remember { mutableStateOf(false) }
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var selectedProductForActivity by remember { mutableStateOf<Product?>(null) }
    var selectedRawMaterialForActivity by remember { mutableStateOf<RawMaterial?>(null) }
    var selectedMasterbatchForActivity by remember { mutableStateOf<Masterbatch?>(null) }

    // Date picker state toggle
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BentoBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BentoBg, Color(0xFF050B14))))
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Horizontal scrollable Tab Category Tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabsList = listOf("Daily Overview", "Inventory", "Workers", "Activity Log")
                    tabsList.forEach { tabName ->
                        val isSelected = activeTab == tabName
                        
                        // Premium color animation sequence
                        val animatedBgColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) BentoForestGreen else Color(0xFF1E293B).copy(alpha = 0.5f),
                            label = "tab_pill_bg"
                        )
                        val animatedTextColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF0F172A) else BentoSubText,
                            label = "tab_pill_text"
                        )
                        val animatedBorderColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) BentoForestGreen else BentoBorder,
                            label = "tab_pill_border"
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(animatedBgColor)
                                .clickable { activeTab = tabName }
                                .border(1.dp, animatedBorderColor, CircleShape)
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .testTag("tab_pill_$tabName")
                        ) {
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = animatedTextColor
                            )
                        }
                    }
                }
            }

            // --- DETAILED LAYOUT ROUTING BASED ON CHOSEN BENTO SEGMENT ---
            if (activeTab == "Daily Overview") {
                // RENDER GORGEOUS COMPACT MAIN BENTO GRID FOR DAILY REPORT KPI METRICS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                        viewModel = viewModel
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (activeTab == "Inventory") {
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

                            Spacer(modifier = Modifier.height(8.dp))

                            if (products.isEmpty()) {
                                EmptyStatePlaceholder("No products defined. Click + Add Product to start.")
                            } else {
                                products.forEach { product ->
                                    val pStats = stats.productSummary[product.id] ?: ProductAggStats(product.id, 0, 0, 0)
                                    ProductStockCard(
                                        product = product,
                                        stats = pStats,
                                        onRecordClick = { selectedProductForActivity = product },
                                        onDeleteClick = { viewModel.deleteProduct(product) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // --- SECTION 2: RAW MATERIALS ---
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
                                title = "MASTERBATCH COLOR AGENTS",
                                subtitle = "Industrial pigments used in extrusion process",
                                icon = Icons.Default.Star,
                                onAddClick = { showAddMasterbatchDialog = true },
                                addButtonText = "+ Pigment Color",
                                addBtnTag = "add_masterbatch_section_btn"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (masterbatches.isEmpty()) {
                                EmptyStatePlaceholder("No Masterbatch colors added. Click + Pigment Color.")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    masterbatches.forEach { mb ->
                                        val mbStats = stats.masterbatchSummary[mb.id] ?: MasterbatchAggStats(mb.id, 0.0, 0.0)
                                        MasterbatchCard(
                                            masterbatch = mb,
                                            stats = mbStats,
                                            onRecordClick = { selectedMasterbatchForActivity = mb },
                                            onDeleteClick = { viewModel.deleteMasterbatch(mb) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeTab == "Workers") {
                        // --- SECTION 4: WORKERS CONTROLLER ---
                        item {
                            InventoryHeaderSection(
                                title = "WORKERS CONTROLLER",
                                subtitle = "Schedule tracking. Sunday off, attendance lists",
                                icon = Icons.Default.Person,
                                onAddClick = { showAddWorkerDialog = true },
                                addButtonText = "+ Add Worker",
                                addBtnTag = "add_worker_section_btn"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = BentoNeutralGray
                                ),
                                border = BorderStroke(1.dp, BentoBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Daily Roster: ${formatDateFriendly(selectedDate)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BentoForestGreen
                                    )
                                    Text(
                                        text = "Work shift active 24/7. Tap icons to mark attendance state for today.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoSubText,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (workers.isEmpty()) {
                                        EmptyStatePlaceholder("No workers registered yet.")
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
                                                color = BentoBorder.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activeTab == "Activity Log") {
                        item {
                            ActivityLogSection(
                                activityLogs = activityLogs
                            )
                        }
                    }
                }
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
            onSave = { name ->
                viewModel.addNewWorker(name)
                showAddWorkerDialog = false
            }
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
        RecordMasterbatchActivityDialog(
            masterbatch = mb,
            onDismiss = { selectedMasterbatchForActivity = null },
            onSave = { used, bought ->
                viewModel.recordMasterbatchActivity(mb.id, used, bought)
                selectedMasterbatchForActivity = null
            }
        )
    }
}


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
    viewModel: MainViewModel
) {
    val selectedDateStr = viewModel.selectedDate.collectAsStateWithLifecycle().value
    val allTransactions = viewModel.allProductTransactions.collectAsStateWithLifecycle().value

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
        
        // Date active range indicator & Segment Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Viewing details: ${stats.startDate} to ${stats.endDate}",
                style = MaterialTheme.typography.labelSmall,
                color = BentoSubText,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, BentoBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val periods = listOf(
                    Pair(MainViewModel.ReportPeriod.Daily, "Day"),
                    Pair(MainViewModel.ReportPeriod.Weekly, "Week"),
                    Pair(MainViewModel.ReportPeriod.Monthly, "Month"),
                    Pair(MainViewModel.ReportPeriod.Yearly, "Year")
                )
                periods.forEach { (periodType, label) ->
                    val isSelected = reportPeriod == periodType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) BentoForestGreen else Color.Transparent)
                            .clickable { onPeriodSelect(periodType) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("scope_tab_$label")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else BentoSubText
                        )
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
                    Text(
                        text = if (isSunday) "All years work schedule operates Monday-Saturday. Enjoy your weekly rest!" else "Fill industrial bag production, weights, sales numbers and stock targets seamlessly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText
                    )
                }
            }
        }

        // --- DYNAMIC PRODUCTION KPI SUMMARY COUNTERS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoLightGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, BentoForestGreen.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TOTAL PRODUCED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = BentoForestGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${todayKgProduced.toInt()} KG", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = BentoForestGreen)
                    Text("Fabricated Today", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoInfoBg.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, BentoInfoText.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TOTAL SOLD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = BentoInfoText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${todayKgSold.toInt()} KG", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = BentoInfoText)
                    Text("Shipped Today", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                }
            }
        }

        // --- WEEKLY AND MONTHLY PRODUCTION ACHIEVEMENTS ---
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
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WEEKLY TOTALS", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                        Text("${weekKg.toInt()} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                        Text("$weekBags bags fabricated", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                    }
                    Box(modifier = Modifier.width(1.dp).height(50.dp).background(BentoBorder))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MONTHLY TOTALS", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                        Text("${monthKg.toInt()} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = BentoTextDark)
                        Text("$monthBags bags fabricated", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
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
                        Text(
                            text = "Record item counts for selected day",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
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
                        colors = CardDefaults.cardColors(containerColor = BentoBg.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Product Title & Spec Indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "${product.name} (${product.color})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextDark
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(BentoLightGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Size: ${product.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                                        }
                                        Text("•", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        Text("Counter: ${product.counter}", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        Text("•", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        Text("${product.piecesPerBag} Pcs/Bag", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        Text("•", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        Text("Unit: ${product.bagWeightKg} kg", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                    }
                                }

                                // Warehouse calculation
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Warehouse Remaining", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                    Text(
                                        text = "${product.currentStock} bags",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoForestGreen
                                    )
                                    Text(
                                        text = "(${String.format("%.1f", product.currentStock * product.bagWeightKg)} kg)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoSubText
                                    )
                                }
                            }

                            HorizontalDivider(color = BentoBorder.copy(alpha = 0.3f))

                            // Inputs & Auto Calculations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Bags Produced:", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        val enteredFab = producedInput.toIntOrNull() ?: 0
                                        if (enteredFab > 0) {
                                            Text(" (${String.format("%.1f", enteredFab * product.bagWeightKg)} kg)", style = MaterialTheme.typography.labelSmall, color = BentoForestGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    OutlinedTextField(
                                        value = producedInput,
                                        onValueChange = { producedInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        modifier = Modifier.height(48.dp).fillMaxWidth().testTag("sheet_fab_input_${product.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = BentoNeutralGray,
                                            unfocusedContainerColor = BentoNeutralGray,
                                            focusedBorderColor = BentoForestGreen,
                                            unfocusedBorderColor = BentoBorder
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Bags Sold:", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                                        val enteredSold = soldInput.toIntOrNull() ?: 0
                                        if (enteredSold > 0) {
                                            Text(" (${String.format("%.1f", enteredSold * product.bagWeightKg)} kg)", style = MaterialTheme.typography.labelSmall, color = BentoInfoText, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    OutlinedTextField(
                                        value = soldInput,
                                        onValueChange = { soldInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        modifier = Modifier.height(48.dp).fillMaxWidth().testTag("sheet_sold_input_${product.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = BentoNeutralGray,
                                            unfocusedContainerColor = BentoNeutralGray,
                                            focusedBorderColor = BentoForestGreen,
                                            unfocusedBorderColor = BentoBorder
                                        )
                                    )
                                }

                                // Interactive Quick Save Action Button with Saved Visual State
                                Column {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    IconButton(
                                        onClick = {
                                            val fVal = producedInput.toIntOrNull() ?: 0
                                            val sVal = soldInput.toIntOrNull() ?: 0
                                            viewModel.recordProductDailyActivity(product.id, fVal, sVal, 0, "Daily entry update")
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSaved) Color(0xFF065F46) else BentoForestGreen
                                            ).testTag("sheet_save_btn_${product.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Done,
                                            contentDescription = "Save values",
                                            tint = if (isSaved) Color(0xFF15803D) else Color.White,
                                            modifier = Modifier.size(16.dp)
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
                    .height(180.dp)
                    .testTag("bento_feedstock_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RAW MATERIALS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSubText,
                        letterSpacing = 1.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LDPE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BentoTextDark)
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
                            Text("HDPE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BentoTextDark)
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
                            Text("Waste", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BentoSubText)
                            val totalWaste = rawMaterials.find { it.type.uppercase() == "WASTE" }?.currentStock ?: 890.0
                            Text("${totalWaste.toInt()}kg", style = MaterialTheme.typography.labelSmall, color = BentoSubText)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BentoLightGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, BentoForestGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(vertical = 4.dp),
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
                    .height(180.dp)
                    .testTag("bento_masterbatch_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                val totalMasterbatchUsed = masterbatches.sumOf { mb -> stats.masterbatchSummary[mb.id]?.used ?: 0.0 }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MASTERBATCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoSubText,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(3.dp, BentoForestGreen.copy(alpha = 0.4f), CircleShape)
                                .border(1.5.dp, BentoForestGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${totalMasterbatchUsed.toInt()}kg",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoForestGreen
                            )
                        }
                    }

                    Text(
                        text = "Used in 24h",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Button(
                        onClick = {
                            masterbatches.firstOrNull()?.let { onMasterbatchClick(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoLightGreen,
                            contentColor = BentoForestGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        Text("Refill", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
            }  }
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
    onRecordClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(BentoLightGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Size: ${product.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                        }
                        Box(
                            modifier = Modifier
                                .background(BentoInfoBg.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Color: ${product.color}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BentoInfoText)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "1 Bag = ${product.bagWeightKg} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText
                        )
                    }
                }

                // Close/Delete Action trigger
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Product",
                        tint = BentoAlertText.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, BentoBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FABRICATED", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "+${stats.fabricated} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoForestGreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SOLD", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "-${stats.sold} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoAlertText
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ADJUSTED", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${if (stats.adjusted >= 0) "+" else ""}${stats.adjusted}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.adjusted >= 0) BentoTextDark else BentoAlertText
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN STOCK", style = MaterialTheme.typography.labelSmall, color = BentoSubText, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${product.currentStock} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (product.currentStock > 10) BentoForestGreen else BentoAlertText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Button(
                onClick = onRecordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("record_product_btn_${product.id}"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoLightGreen,
                    contentColor = BentoForestGreen
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record Today's Activity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("masterbatch_card_${masterbatch.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BentoNeutralGray),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pigment Color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getMasterbatchColor(masterbatch.color),
                        shape = CircleShape
                    )
                    .border(1.5.dp, BentoBorder, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${masterbatch.color} Pigment Masterbatch",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextDark
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Used: -${stats.used.toInt()} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoAlertText
                    )
                    Text(
                        text = "Bought: +${stats.bought.toInt()} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoForestGreen
                    )
                    Text(
                        text = "Stock: ${masterbatch.currentStock.toInt()} kg left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                }
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRecordClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add activity log",
                        tint = BentoForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = BentoAlertText.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
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
    var initialStockStr by remember { mutableStateOf("50.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Masterbatch Color Agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = colorName,
                    onValueChange = { colorName = it },
                    label = { Text("Color Name (e.g., Green, Black, White)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_mb_color")
                )
                OutlinedTextField(
                    value = initialStockStr,
                    onValueChange = { initialStockStr = it },
                    label = { Text("Initial Stock in kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = initialStockStr.toDoubleOrNull() ?: 0.0
                    if (colorName.isNotEmpty()) {
                        onSave(colorName, stock)
                    }
                },
                modifier = Modifier.testTag("diag_mb_submit")
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
fun AddWorkerDialog(
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Employee Worker") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Worker Full Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diag_worker_name")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(name.trim())
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
    onSave: (fabricated: Int, sold: Int, adjusted: Int, notes: String) -> Unit
) {
    var fabStr by remember { mutableStateOf("") }
    var soldStr by remember { mutableStateOf("") }
    var adjStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    onDismiss: () -> Unit,
    onSave: (used: Double, bought: Double) -> Unit
) {
    var usedStr by remember { mutableStateOf("") }
    var boughtStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Activity Log: ${masterbatch.color} Masterbatch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = usedStr,
                    onValueChange = { usedStr = it },
                    label = { Text("Used Today in Extrusion (kg)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_mb_used")
                )
                OutlinedTextField(
                    value = boughtStr,
                    onValueChange = { boughtStr = it },
                    label = { Text("Bought/Restocked Today (kg)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_mb_bought")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = usedStr.toDoubleOrNull() ?: 0.0
                    val b = boughtStr.toDoubleOrNull() ?: 0.0
                    onSave(u, b)
                },
                modifier = Modifier.testTag("diag_mb_activity_submit")
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
    activityLogs: List<ActivityLog>
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
