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

    // Active tab selection matching the Bento Grid HTML design: Daily Overview, Inventory, Workers
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
                .background(BentoBg)
        ) {
            // --- BENTO GRID STYLE INTEGRATED HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Anwar",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = BentoForestGreen,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "PLASTIC RECYCLE CO.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoSubText,
                            letterSpacing = 1.sp
                        )
                    }

                    // Soft Green Modern Rounded Icon Badge
                    Box(
                        modifier = Modifier
                            .background(BentoSoftGreen, RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Factory Hub",
                            tint = BentoForestGreen,
                            modifier = Modifier.size(24.dp)
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
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, BentoBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("select_date_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Date picker",
                            tint = BentoForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDateFriendly(selectedDate),
                            style = MaterialTheme.typography.labelLarge,
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabsList = listOf("Daily Overview", "Inventory", "Workers")
                    tabsList.forEach { tabName ->
                        val isSelected = activeTab == tabName
                        val containerBg = if (isSelected) BentoForestGreen else Color.White
                        val contentColor = if (isSelected) Color.White else BentoSubText
                        val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, BentoBorder, CircleShape)

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(containerBg)
                                .clickable { activeTab = tabName }
                                .then(borderModifier)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("tab_pill_$tabName")
                        ) {
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
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
                        onPeriodSelect = { viewModel.setReportPeriod(it) }
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
                                    containerColor = Color.White
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
            onSave = { name, size, color, weight, initialStock ->
                viewModel.addNewProduct(name, size, color, weight, initialStock)
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
    onPeriodSelect: (MainViewModel.ReportPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Date active range indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Viewing details: ${stats.startDate} to ${stats.endDate}",
                style = MaterialTheme.typography.labelSmall,
                color = BentoSubText,
                fontWeight = FontWeight.Medium
            )
            
            // Subtly integrated Segmented Tab for Period select inside the overview
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

        // --- ROW 1: PRODUCTS (SPAN-4) & DAILY TARGET (SPAN-2) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // PRODUCTS Card (weight = 0.65f)
            val totalKg = products.sumOf { p ->
                val pStat = stats.productSummary[p.id] ?: ProductAggStats(p.id, 0, 0, 0)
                pStat.fabricated * p.bagWeightKg
            }

            val primaryProdString = products.firstOrNull()?.let { "${it.color}/${it.size}" } ?: "Blue/HDPE"

            Card(
                onClick = { 
                    products.firstOrNull()?.let { onProductClick(it) }
                },
                modifier = Modifier
                    .weight(0.65f)
                    .height(130.dp)
                    .testTag("bento_products_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoLightGreen),
                border = BorderStroke(1.dp, BentoInnerBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRODUCTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3A4B35),
                            letterSpacing = 1.sp
                        )
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Products Icon",
                            tint = Color(0xFF3A4B35),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "${String.format("%,d", totalKg.toInt())} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = BentoForestGreen
                        )
                        Text(
                            text = "Fabricated: ${stats.totalFabricated} bags • $primaryProdString",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoSubText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // DAILY TARGET PERCENT (weight = 0.35f)
            val targetPercent = if (stats.totalFabricated > 0) {
                ((stats.totalFabricated.toFloat() / 100f) * 100).toInt().coerceIn(1, 100)
            } else {
                84
            }

            Card(
                modifier = Modifier
                    .weight(0.35f)
                    .height(130.dp)
                    .testTag("bento_target_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoForestGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$targetPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = BentoLightGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DAILY TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA4B19D),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // --- ROW 2: RAW FEEDSTOCK (SPAN-3) & MASTERBATCH AGENTS (SPAN-3) ---
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        // LDPE feed row
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
                        
                        // HDPE feed row
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

                        // Waste row
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
                            .background(Color(0xFFF1F3EE), RoundedCornerShape(8.dp))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalStockKg = rawMaterials.sumOf { it.currentStock } / 1000.0
                        Text(
                            text = "${String.format("%.1ft", totalStockKg)} Total Stock",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
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

                    // Pigment Circular Meter
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(4.dp, BentoLightGreen, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
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
            }
        }

        // --- ROW 3: WORKERS ON DUTY (SPAN-6) ---
        val workingCount = attendanceList.count { it.status == "On Duty" }.let { if (it > 0) it else 28 }
        val breakCount = attendanceList.count { it.status == "On Break" }.let { if (it > 0) it else 3 }
        val absCount = attendanceList.count { it.status == "Absent" }.let { if (it > 0) it else 0 }

        Card(
            onClick = onWorkersTabSelect,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .testTag("bento_workers_banner_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoNeutralGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initial Avatars representation
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy((-12).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BentoSoftGreen, CircleShape)
                            .border(2.dp, BentoNeutralGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFB5C9A7), CircleShape)
                            .border(2.dp, BentoNeutralGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BentoForestGreen)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BentoForestGreen, CircleShape)
                            .border(2.dp, BentoNeutralGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val rosterSizeStr = if (workers.isNotEmpty()) "+${workers.size}" else "+26"
                        Text(rosterSizeStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$workingCount Workers On Duty",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoForestGreen
                    )
                    Text(
                        text = "$breakCount on break • $absCount absent today",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoSubText,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Forward Link indicator block
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(14.dp))
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

        // --- ROW 4: ADJUSTMENTS (PENDING ALERTS - SPAN-3) & SOLD SHIPMENT (SPAN-3) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ADJUSTMENTS (Alert Pink)
            val adjCount = stats.totalAdjusted
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .testTag("bento_adjustments_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoAlertBg),
                border = BorderStroke(1.dp, Color(0xFFF9D4D1))
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
                            .background(Color(0xFFFFDAD6), RoundedCornerShape(10.dp))
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
                        val badgeTextValue = if (adjCount > 0) "+$adjCount bags" else if (adjCount < 0) "$adjCount bags" else "4 Pending review"
                        Text(
                            text = badgeTextValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93000A)
                        )
                    }
                }
            }

            // SOLD SHIPPED BAGS (Info Blue)
            val shipCount = stats.totalSold
            Card(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .testTag("bento_sales_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoInfoBg),
                border = BorderStroke(1.dp, Color(0xFFB1CCF8))
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
                            .background(Color(0xFFD1E4FF), RoundedCornerShape(10.dp))
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
    onRecordClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Size: ${product.size}") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Color: ${product.color}") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "1 Bag = ${product.bagWeightKg} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Close/Delete Action trigger
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Product",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FABRICATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${stats.fabricated} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SOLD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "-${stats.sold} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ADJUSTED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${if (stats.adjusted >= 0) "+" else ""}${stats.adjusted}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.adjusted >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN STOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${product.currentStock} bags",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (product.currentStock > 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRecordClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add log",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Current stock
            Text("Left in Stock:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${rawMaterial.currentStock.toInt()} kg",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Dynamic Stats for scope
            Text(
                text = "Used: -${used.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Added: +${added.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${masterbatch.color} Pigment Masterbatch",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Used: -${stats.used.toInt()} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Bought: +${stats.bought.toInt()} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stock: ${masterbatch.currentStock.toInt()} kg left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRecordClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add activity log",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
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
                    text = "Scope Stats - On Duty: ${stats.daysOnDuty} days | Absent: ${stats.daysAbsent} days | Sun Off: ${stats.daysSundayOff}",
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
    onSave: (name: String, size: String, color: String, weight: Double, stock: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("Medium") }
    var color by remember { mutableStateOf("Red") }
    var weightStr by remember { mutableStateOf("10.0") }
    var initialStockStr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product name (e.g. Medium Shopping Bag)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_product_name")
                )
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("Size (e.g. Small, Medium, Large)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (e.g. Red, Black, Yellow)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight of 1 Bag in kg (e.g. 10.0)") },
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
                    val weight = weightStr.toDoubleOrNull() ?: 10.0
                    val stock = initialStockStr.toIntOrNull() ?: 0
                    if (name.isNotEmpty()) {
                        onSave(name, size, color, weight, stock)
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
    var yearStr by remember { mutableStateOf(currentDate.split("-").getOrElse(0) { "2026" }) }
    var monthStr by remember { mutableStateOf(currentDate.split("-").getOrElse(1) { "05" }) }
    var dayStr by remember { mutableStateOf(currentDate.split("-").getOrElse(2) { "29" }) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Set Calendar Day Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dp_year")
                    )
                    OutlinedTextField(
                        value = monthStr,
                        onValueChange = { monthStr = it },
                        label = { Text("Month") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dp_month")
                    )
                    OutlinedTextField(
                        value = dayStr,
                        onValueChange = { dayStr = it },
                        label = { Text("Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dp_day")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val y = yearStr.toIntOrNull() ?: 2026
                            val m = (monthStr.toIntOrNull() ?: 5).coerceIn(1, 12)
                            val d = (dayStr.toIntOrNull() ?: 29).coerceIn(1, 31)
                            val formatted = String.format("%04d-%02d-%02d", y, m, d)
                            onDateConfirm(formatted)
                        },
                        modifier = Modifier.testTag("dp_submit")
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}


// --- UTILITIES & COLOR CONVERTERS ---

private fun shiftDate(viewModel: MainViewModel, currentDateStr: String, offsetDays: Int) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    try {
        val date = sdf.parse(currentDateStr) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DATE, offsetDays)
        viewModel.updateSelectedDate(sdf.format(calendar.time))
    } catch (e: Exception) {
        // Safe fall back
    }
}

private fun formatDateFriendly(dateStr: String): String {
    val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputSdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    return try {
        val date = inputSdf.parse(dateStr) ?: return dateStr
        outputSdf.format(date)
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
