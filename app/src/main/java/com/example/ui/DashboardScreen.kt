package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.util.UUID

// Premium Color Constants
val PureBlack = Color(0xFF000000)
val DarkGray = Color(0xFF070B08)
val EmeraldGlow = Color(0xFF00FF88)
val DarkGlassCard = Color(0xFF0C100D)
val DarkGlassInner = Color(0xFF131714)
val GrayBorder = Color(0xFF1B241F)
val LightText = Color(0xFFE8ECE9)
val MutedText = Color(0xFF8C9E94)

// Ethiopian Months list
val ET_MONTHS = listOf(
    "መስከረም", "ጥቅምት", "ህዳር", "ታህሳስ", "ጥር", "የካቲት", "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf("Overview") } // "Overview", "Production", "Stock", "Workers", "AI"

    val products by viewModel.products.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()

    val rawMaterialsStock by viewModel.rawMaterialsStock.collectAsState()
    val masterbatchStock by viewModel.masterbatchStock.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color(0xFF040605))
                    .border(BorderStroke(0.5.dp, GrayBorder), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("Overview", "ዳሽቦርድ", Icons.Default.Dashboard),
                    Triple("Production", "ምርት", Icons.Default.TrendingUp),
                    Triple("Stock", "ክምችት", Icons.Default.Layers),
                    Triple("Workers", "ሰራተኞች", Icons.Default.People),
                    Triple("AI", "ቢኒያም AI", Icons.Default.Chat)
                ).forEach { (tab, label, icon) ->
                    val isSelected = selectedTab == tab
                    
                    // Bouncing active icon scale animation
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "navIconScale"
                    )
                    
                    // Smooth glow dot width/scale animation
                    val glowOffset by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0f,
                        animationSpec = tween(250),
                        label = "glowOffset"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material.ripple.rememberRipple(bounded = false, radius = 24.dp)
                            ) {
                                selectedTab = tab
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) EmeraldGlow else MutedText,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = if (isSelected) EmeraldGlow else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Premium emerald glow line/dot under active navigation tab
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .height(4.dp)
                                .width(12.dp)
                                .graphicsLayer {
                                    alpha = glowOffset
                                    scaleX = glowOffset
                                }
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            EmeraldGlow,
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(100.dp)
                                )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(paddingValues)
        ) {
            // Main views routing
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "MainScreenTabs"
            ) { targetTab ->
                when (targetTab) {
                    "Overview" -> OverviewTab(
                        products = products,
                        workers = workers,
                        rawMaterial = rawMaterialsStock,
                        masterbatch = masterbatchStock,
                        attendanceList = attendanceList
                    )
                    "Production" -> ProductionTab(
                        viewModel = viewModel,
                        products = products
                    )
                    "Stock" -> StockTab(
                        viewModel = viewModel,
                        products = products,
                        rawMaterial = rawMaterialsStock,
                        masterbatch = masterbatchStock
                    )
                    "Workers" -> WorkersTab(
                        viewModel = viewModel,
                        workers = workers,
                        attendanceList = attendanceList,
                        currentMonth = currentMonth,
                        currentYear = currentYear
                    )
                    "AI" -> AiTab(
                        viewModel = viewModel,
                        messages = chatMessages
                    )
                }
            }
        }
    }
}

// ==========================================
// MOTION GRAPHICS & GOD MODE ANIMATIONS HELPERS
// ==========================================

class RecycleParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float)

@Composable
fun ParticleBackground() {
    val particles = remember {
        List(18) {
            RecycleParticle(
                x = (0..100).random().toFloat() / 100f,
                y = (0..100).random().toFloat() / 100f,
                vx = ((1..4).random().toFloat() * if (Math.random() > 0.5) 1f else -1f) / 15000f,
                vy = ((1..4).random().toFloat() * if (Math.random() > 0.5) 1f else -1f) / 15000f,
                size = (3..7).random().toFloat()
            )
        }
    }
    
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTime ->
                particles.forEach { p ->
                    p.x = (p.x + p.vx).let { if (it < 0f) 1f else if (it > 1f) 0f else it }
                    p.y = (p.y + p.vy).let { if (it < 0f) 1f else if (it > 1f) 0f else it }
                }
                tick = frameTime
            }
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Connection lines between nearby moving particles
        for (i in particles.indices) {
            val p1 = particles[i]
            val px1 = p1.x * w
            val py1 = p1.y * h
            
            for (j in i + 1 until particles.size) {
                val p2 = particles[j]
                val px2 = p2.x * w
                val py2 = p2.y * h
                
                val dist = Math.hypot((px1 - px2).toDouble(), (py1 - py2).toDouble()).toFloat()
                val maxDist = 180.dp.toPx()
                if (dist < maxDist) {
                    val alpha = (1f - (dist / maxDist)).coerceIn(0f, 1f) * 0.12f
                    drawLine(
                        color = Color(0xFF00FF88),
                        start = Offset(px1, py1),
                        end = Offset(px2, py2),
                        strokeWidth = 0.8.dp.toPx(),
                        alpha = alpha
                    )
                }
            }
            
            // Draw floating green particle points
            drawCircle(
                color = Color(0xFF00FF88),
                radius = p1.size.dp.toPx() / 2,
                center = Offset(px1, py1),
                alpha = 0.2f
            )
        }
    }
}

@Composable
fun RadarScanLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        val y = progress * h
        
        // 40% Opacity Sweeping thin green radar scanner scan line
        drawLine(
            color = Color(0xFF00FF88),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1.2.dp.toPx(),
            alpha = 0.40f
        )
        
        // Subtle glow trailer above the scanner
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00FF88).copy(alpha = 0.08f), Color.Transparent),
                startY = y,
                endY = (y - 45.dp.toPx()).coerceAtLeast(0f)
            ),
            size = Size(w, 45.dp.toPx())
        )
    }
}

@Composable
fun AnimateCountUpText(
    valueString: String,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontWeight: FontWeight = FontWeight.Black
) {
    // Graceful check and extraction of digits for count up
    val numericValue = valueString.filter { it.isDigit() }.toIntOrNull() ?: 0
    val nonNumericValue = valueString.filter { !it.isDigit() }
    
    var animateCount by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateCount = true
    }
    
    val animatedCountVal by animateIntAsState(
        targetValue = if (animateCount) numericValue else 0,
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "metricCount"
    )
    
    Text(
        text = if (nonNumericValue.isNotEmpty()) "$animatedCountVal$nonNumericValue" else "$animatedCountVal",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily
    )
}

@Composable
fun TypewriterText(phrases: List<String>) {
    var phraseIdx by remember { mutableStateOf(0) }
    var textToDisplay by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    
    LaunchedEffect(phraseIdx, isDeleting) {
        val currentPhrase = phrases[phraseIdx]
        if (!isDeleting) {
            // Typing speed: 80ms
            for (i in 1..currentPhrase.length) {
                textToDisplay = currentPhrase.take(i)
                kotlinx.coroutines.delay(80)
            }
            // Delay at fully typed before starting delete
            kotlinx.coroutines.delay(2000)
            isDeleting = true
        } else {
            // Deleting speed: 40ms
            for (i in currentPhrase.length downTo 0) {
                textToDisplay = currentPhrase.take(i)
                kotlinx.coroutines.delay(40)
            }
            isDeleting = false
            phraseIdx = (phraseIdx + 1) % phrases.size
        }
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = textToDisplay,
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        // Blinking green terminal cursor block
        val cursorTransition = rememberInfiniteTransition(label = "terminal_cursor")
        val cursorAlpha by cursorTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursorAlpha"
        )
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .width(2.dp)
                .height(13.dp)
                .background(EmeraldGlow)
                .graphicsLayer { alpha = cursorAlpha }
        )
    }
}

@Composable
fun ProductStockBarChart(products: List<Product>) {
    if (products.isEmpty()) return
    val maxStock = (products.maxOfOrNull { it.currentStock } ?: 1).coerceAtLeast(1)
    
    var chartAnimateTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        chartAnimateTriggered = true
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = EmeraldGlow.copy(alpha = 0.08f),
                spotColor = EmeraldGlow.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGlassCard.copy(alpha = 0.9f)),
        border = BorderStroke(0.6.dp, Color(0xFF1E2E24))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "የእቃዎች ክምችት ንፅፅር ቻርት (Recycle Stocks Chart)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                products.forEachIndexed { index, product ->
                    val cleanLabelName = product.name
                        .replace("avocado", "", ignoreCase = true)
                        .replace("Avocado", "", ignoreCase = true)
                        .replace("አቮካዶ", "", ignoreCase = true)
                        .trim()
                        .let { if (it.isEmpty()) "ሸርጅን" else it }

                    val stockRatio = product.currentStock.toFloat() / maxStock.toFloat()
                    
                    // Height animating from 0% to real height with a 1.5-second cubic ease animation
                    val animatedHeightProgress by animateFloatAsState(
                        targetValue = if (chartAnimateTriggered) stockRatio else 0f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                        ),
                        label = "chartBarProgress"
                    )
                    
                    val infiniteShimmer = rememberInfiniteTransition(label = "shimmer_bar_$index")
                    val shimmerProgress by infiniteShimmer.animateFloat(
                        initialValue = -1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmerProgress"
                    )
                    
                    val levelColor = when {
                        stockRatio > 0.40 -> EmeraldGlow
                        stockRatio >= 0.15 -> Color(0xFFFFD700)
                        else -> Color(0xFFFF3B3B)
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${product.currentStock}",
                            color = levelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Animated bar representation with Shimmer light sweep
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(animatedHeightProgress.coerceAtLeast(0.01f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .drawWithContent {
                                    drawContent()
                                    
                                    // continuous sweep shimmer sweep light effect
                                    val h = size.height
                                    val yOffset = shimmerProgress * h
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.35f),
                                                Color.Transparent
                                            ),
                                            startY = yOffset,
                                            endY = yOffset + 24.dp.toPx()
                                        ),
                                        size = Size(size.width, 24.dp.toPx())
                                    )
                                }
                                .background(
                                    Brush.verticalGradient(
                                        listOf(levelColor, levelColor.copy(alpha = 0.15f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.hoverLightReveal(): Modifier {
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    val glowAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val type = event.type
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                        touchPos = change.position
                    }
                    when (type) {
                        androidx.compose.ui.input.pointer.PointerEventType.Enter,
                        androidx.compose.ui.input.pointer.PointerEventType.Press,
                        androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                            coroutineScope.launch {
                                glowAlpha.animateTo(0.24f, androidx.compose.animation.core.tween(150))
                            }
                        }
                        androidx.compose.ui.input.pointer.PointerEventType.Exit,
                        androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                            coroutineScope.launch {
                                glowAlpha.animateTo(0f, androidx.compose.animation.core.tween(250))
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val pos = touchPos
            if (pos != null && glowAlpha.value > 0f) {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFF00FF88).copy(alpha = glowAlpha.value), Color.Transparent),
                        center = pos,
                        radius = size.maxDimension * 0.7f
                    ),
                    radius = size.maxDimension * 0.7f,
                    center = pos,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Screen
                )
            }
        }
}

@Composable
fun FlowingArrow(modifier: Modifier = Modifier, progress: Float) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val startY = height / 2f
        
        // Draw base dim green arrow line
        drawLine(
            color = Color(0xFF122C20),
            start = Offset(0f, startY),
            end = Offset(width, startY),
            strokeWidth = 3f
        )
        
        // Draw arrowhead pointing to the right
        val arrowSize = 10f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width, startY)
            lineTo(width - arrowSize, startY - arrowSize * 0.7f)
            lineTo(width - arrowSize, startY + arrowSize * 0.7f)
            close()
        }
        drawPath(path, color = Color(0xFF00FF88).copy(alpha = 0.5f))
        
        // Draw flowing light dot (shimmer)
        val shimmerX = width * progress
        drawCircle(
            color = Color.White,
            radius = 3.5f,
            center = Offset(shimmerX, startY)
        )
        // Soft outer radial green pulse halo
        drawCircle(
            color = Color(0xFF00FF88).copy(alpha = 0.5f),
            radius = 7.5f,
            center = Offset(shimmerX, startY)
        )
    }
}

@Composable
fun FlowStepCard(
    title: String,
    sub: String,
    iconString: String,
    isActive: Boolean,
    pulseColor: Color,
    modifier: Modifier = Modifier,
    pulseScale: Float = 1f,
    rotationSpec: Float = 0f
) {
    val activeBorderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF00FF88) else Color(0xFF122C20),
        animationSpec = tween(400),
        label = "stepBorder"
    )
    val activeGlowFactor by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0f,
        animationSpec = tween(400),
        label = "stepGlow"
    )
    val cardBackground by animateColorAsState(
        targetValue = if (isActive) Color(0xFF0A1811) else Color(0xFF040605),
        animationSpec = tween(400),
        label = "stepBackground"
    )

    Box(
        modifier = modifier
            .padding(1.dp)
            .graphicsLayer {
                scaleX = if (iconString != "⚙️") pulseScale else 1f
                scaleY = if (iconString != "⚙️") pulseScale else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .border(
                BorderStroke(if (isActive) 1.5.dp else 1.dp, activeBorderColor),
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        if (iconString == "⚙️") {
                            rotationZ = rotationSpec
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconString,
                    fontSize = 20.sp
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(pulseColor.copy(alpha = 0.16f * activeGlowFactor))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                color = if (isActive) Color(0xFF00FF88) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Text(
                text = sub,
                color = if (isActive) Color.White else MutedText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TransformFlowSection(
    rawMaterial: Int,
    masterbatch: Int,
    totalBags: Int
) {
    var activeStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            activeStep = (activeStep + 1) % 4
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "flowPulse")
    val bluePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blue"
    )
    val pinkPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pink"
    )
    val greenPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1420, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "green"
    )
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear"
    )
    val arrowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverLightReveal()
            .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040605)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Flow Process",
                        tint = EmeraldGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "የእቃዎች አመራረት ሂደት ፍሰት (Factory Transformation Flow)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF122C20))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ባለ 4-ባህሪ ሪሳይክል ፍሰት",
                        color = Color(0xFF00FF88),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowStepCard(
                    title = "ጥሬ እቃ (LD)",
                    sub = "$rawMaterial kg",
                    iconString = "🧊",
                    isActive = activeStep == 0,
                    pulseScale = bluePulse,
                    pulseColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                
                FlowingArrow(
                    modifier = Modifier.width(16.dp).height(24.dp),
                    progress = arrowProgress
                )
                
                FlowStepCard(
                    title = "ቀለም",
                    sub = "$masterbatch kg",
                    iconString = "🎨",
                    isActive = activeStep == 1,
                    pulseScale = pinkPulse,
                    pulseColor = Color(0xFFE91E63),
                    modifier = Modifier.weight(1f)
                )
                
                FlowingArrow(
                    modifier = Modifier.width(16.dp).height(24.dp),
                    progress = arrowProgress
                )
                
                FlowStepCard(
                    title = "ማሽን (⚙️)",
                    sub = "በስራ ላይ",
                    iconString = "⚙️",
                    isActive = activeStep == 2,
                    rotationSpec = gearRotation,
                    pulseColor = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                
                FlowingArrow(
                    modifier = Modifier.width(16.dp).height(24.dp),
                    progress = arrowProgress
                )
                
                FlowStepCard(
                    title = "ከረጢት (Bag)",
                    sub = "$totalBags pcs",
                    iconString = "🛍️",
                    isActive = activeStep == 3,
                    pulseScale = greenPulse,
                    pulseColor = Color(0xFF00FF88),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AlertCardSection(criticalProductsCount: Int) {
    if (criticalProductsCount == 0) return
    
    val pulseTransition = rememberInfiniteTransition(label = "alert_pulse")
    val pulseValue by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )
    
    // Pulse color border sweeping between deep gold and warning red every 3 seconds
    val alertBorderColor by pulseTransition.animateColor(
        initialValue = Color(0xFFFF3B3B),
        targetValue = Color(0xFFFFD700),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertBorderColor"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverLightReveal()
            .shadow(
                elevation = (4 * pulseValue).dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140707).copy(alpha = 0.85f)),
        border = BorderStroke(
            width = (1 + pulseValue).dp,
            color = alertBorderColor.copy(alpha = pulseValue)
        )
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF381010)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF3B3B),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "የክምችት እጥረት ማስጠንቀቂያ (Critical Stock Alert)",
                    color = Color(0xFFFFABAB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "በዳሽቦርዱ ውስጥ $criticalProductsCount ምርቶች ከወሳኝ መጠን በታች አነስተኛ ክምችት ላይ ናቸው!",
                    color = Color(0xFFD6BCBC),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: OVERVIEW SCREEN
// ==========================================
@Composable
fun OverviewTab(
    products: List<Product>,
    workers: List<Worker>,
    rawMaterial: Int,
    masterbatch: Int,
    attendanceList: List<Attendance>
) {
    var animateOnLoad by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateOnLoad = true }

    // Entrance and scale/fade values for hero title
    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { bannerVisible = true }
    
    val bannerScale by animateFloatAsState(
        targetValue = if (bannerVisible) 1f else 0.8f, // Zooms from scale 0.8 to 1.0 as requested
        animationSpec = tween(durationMillis = 1200, easing = EaseOutBack),
        label = "bannerScale"
    )
    val bannerAlpha by animateFloatAsState(
        targetValue = if (bannerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "bannerAlpha"
    )
    
    // Eyebrow text fades in first
    val eyebrowAlpha by animateFloatAsState(
        targetValue = if (bannerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 100), // earlier fade in
        label = "eyebrowAlpha"
    )

    // Calculate critical items for alert systems
    val criticalStockCount = products.count { it.currentStock < 10 }

    // Scroll state for custom scroll-linked parallax
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollOffset = remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset
            } else {
                350
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // PARTICLE BACKGROUND: Floating connected nodes in green
        ParticleBackground()
        
        // SCAN LINE: Continuous horizontal sweeping laser
        RadarScanLine()

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // HERO ENTRANCE: Company Banner with parallax background, rings, zoom scale, eyebrow fade, and Typewriter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = bannerScale
                            scaleY = bannerScale
                            alpha = bannerAlpha
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color(0xFF122C20)), RoundedCornerShape(16.dp))
                        .hoverLightReveal()
                ) {
                    // Parallax background glow (moves at 30% of scroll speed)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                translationY = scrollOffset.value * 0.30f
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF04180E), PureBlack),
                                    center = Offset(200f, 100f),
                                    radius = 500f
                                )
                            )
                    )
                    
                    // Parallax rings (moves at 12% of scroll speed, expands/contracts with scroll)
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                translationY = scrollOffset.value * 0.12f
                                val scaleFactor = 1.0f - (scrollOffset.value / 1200f).coerceIn(0f, 0.4f)
                                scaleX = scaleFactor
                                scaleY = scaleFactor
                            }
                    ) {
                        val center = Offset(size.width * 0.85f, size.height * 0.5f)
                        drawCircle(
                            color = Color(0xFF00FF88).copy(alpha = 0.12f),
                            radius = 110f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = Color(0xFF00FF88).copy(alpha = 0.07f),
                            radius = 170f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                        )
                        drawCircle(
                            color = Color(0xFF00FF88).copy(alpha = 0.03f),
                            radius = 230f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Eyebrow text fading in first
                        Text(
                            text = "አንዋር ፕላስติก መልሶ ማምረቻ",
                            color = EmeraldGlow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer { alpha = eyebrowAlpha }
                        )
                        // Hero Title: Zoom and fade animation
                        Text(
                            text = "ANWAR PLASTIC RECYCLE CO.",
                            color = Color.White,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Typewriter text cycling animation
                        TypewriterText(
                            phrases = listOf(
                                "የክምችት መቆጣጠሪያ እና የሰራተኞች ቁጥጥር ዳሽቦርድ...",
                                "ቀጣይነት ያለው አረንጓዴ ልማት እና ሪሳይክሊንግ...",
                                "የምርት መጠን ፈጣን ክትትል ኮንሶል..."
                            )
                        )
                    }
                }
            }

            // ALERT CARDS warning section (Pulses Gold & Warning Red every 3 seconds)
            item {
                AlertCardSection(criticalProductsCount = criticalStockCount)
            }

            // LD TO PLASTIC BAG TRANSFORM SECTION (Shows flow + 4 sequential active cycling lights)
            item {
                TransformFlowSection(
                    rawMaterial = rawMaterial,
                    masterbatch = masterbatch,
                    totalBags = products.sumOf { it.currentStock }
                )
            }

            item {
                // METRIC CARDS LAYER 1 (Staggered 200ms delays on load)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MetricCard(
                            title = "ጥሬ እቃ ክምችት",
                            enTitle = "Raw Materials",
                            value = "$rawMaterial ኪ.ግ",
                            color = Color(0xFF00FF88),
                            icon = Icons.Default.Layers,
                            index = 0 // staggered delay index 0 * 200 ms
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        MetricCard(
                            title = "ቀለም (Masterbatch)",
                            enTitle = "Masterbatch",
                            value = "$masterbatch ኪ.ግ",
                            color = Color(0xFFFFD700),
                            icon = Icons.Default.Palette,
                            index = 1 // staggered delay index 1 * 200 ms
                        )
                    }
                }
            }

            item {
                // METRIC CARDS LAYER 2 (Staggered 200ms delays on load)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val totalBags = products.sumOf { it.currentStock }
                        MetricCard(
                            title = "አጠቃላይ ምርት",
                            enTitle = "Total Bags Stock",
                            value = "$totalBags",
                            color = Color(0xFF00E5FF),
                            icon = Icons.Default.Inventory,
                            index = 2 // staggered delay index 2 * 200 ms
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val activeWorkers = workers.size
                        MetricCard(
                            title = "ንቁ ሰራተኞች",
                            enTitle = "Active Workers",
                            value = "$activeWorkers",
                            color = Color(0xFFE040FB),
                            icon = Icons.Default.People,
                            index = 3 // staggered delay index 3 * 200 ms
                        )
                    }
                }
            }

            // CHART BARS SECTION with shimmer sweeping light sweeps
            item {
                ProductStockBarChart(products)
            }

            item {
                Text(
                    text = "የምርት ደረጃ ፈጣን ቁጥጥር (Product Stock Level)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            itemsIndexed(products) { idx, p ->
                val weightTotal = p.currentStock * p.bagWeightKg
                
                val productCleanName = p.name
                    .replace("avocado", "", ignoreCase = true)
                    .replace("Avocado", "", ignoreCase = true)
                    .replace("አቮካዶ", "", ignoreCase = true)
                    .trim()
                    .let { if (it.isEmpty()) "ሸርጅን" else it }

                // Low stock condition check
                val isCritStock = p.currentStock < 10
                
                // Border pulses red if low stock card, else dim green
                val pulseBorderVal = rememberInfiniteTransition(label = "p_border_$idx")
                val animatedBorderAlpha by pulseBorderVal.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "borderPulseAlpha"
                )
                
                val finalCardBorderColor = if (isCritStock) {
                    Color(0xFFFF3B3B).copy(alpha = animatedBorderAlpha)
                } else {
                    GrayBorder
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverLightReveal()
                        .graphicsLayer {
                            alpha = if (animateOnLoad) 1f else 0f
                            translationY = if (animateOnLoad) 0f else 30f
                        }
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkGlassCard.copy(alpha = 0.85f)),
                    border = BorderStroke(if (isCritStock) 1.2.dp else 0.5.dp, finalCardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = productCleanName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "መጠን፡ ${p.size} · ክብደት፡ ${p.bagWeightKg} ኪ.ግ",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${p.currentStock} ከረጢት",
                                color = if (isCritStock) Color(0xFFFF3B3B) else EmeraldGlow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ጠቅላላ ክብደት፡ $weightTotal ኪ.ግ",
                                color = MutedText,
                                fontSize = 9.sp,
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
fun MetricCard(
    title: String,
    enTitle: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    index: Int
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }
    
    // Each card slides up from below with a staggered delay based on its index
    val slideY by animateFloatAsState(
        targetValue = if (animateIn) 0f else 60f,
        animationSpec = tween(durationMillis = 800, delayMillis = index * 200, easing = EaseOutQuad),
        label = "metricSlide"
    )
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = index * 200, easing = EaseOutQuad),
        label = "metricAlpha"
    )
    
    var isHovered by remember { mutableStateOf(false) }
    
    // Lift cards up 3.dp on pointer hover & tap
    val liftDp by animateDpAsState(
        targetValue = if (isHovered) (-3).dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "metricLift"
    )
    
    // Dynamic green border glow on hover/lift state
    val glowColorAnim by animateColorAsState(
        targetValue = if (isHovered) EmeraldGlow.copy(alpha = 0.85f) else GrayBorder,
        animationSpec = tween(250),
        label = "metricGlowBorder"
    )
    
    val finalGlowWidth = if (isHovered) 1.2.dp else 0.6.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverLightReveal()
            .graphicsLayer {
                translationY = slideY
                alpha = alphaAnim
            }
            .offset(y = liftDp)
            .shadow(
                elevation = if (isHovered) 12.dp else 6.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (isHovered) EmeraldGlow.copy(alpha = 0.25f) else color.copy(alpha = 0.1f),
                spotColor = if (isHovered) EmeraldGlow.copy(alpha = 0.35f) else color.copy(alpha = 0.15f)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val type = event.type
                        if (type == androidx.compose.ui.input.pointer.PointerEventType.Enter) {
                            isHovered = true
                        } else if (type == androidx.compose.ui.input.pointer.PointerEventType.Exit) {
                            isHovered = false
                        } else if (type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            isHovered = true
                        } else if (type == androidx.compose.ui.input.pointer.PointerEventType.Release) {
                            isHovered = false
                        }
                    }
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040705).copy(alpha = 0.85f)),
        border = BorderStroke(finalGlowWidth, glowColorAnim)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(enTitle, color = MutedText, fontSize = 9.sp)
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            // Numerical animated count up
            AnimateCountUpText(
                valueString = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// SCREEN 2: PRODUCTION SHEET TAB
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionTab(viewModel: MainViewModel, products: List<Product>) {
    var selectedProductForStockUpdate by remember { mutableStateOf<Product?>(null) }
    var stockInputText by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // State for creating new product
    var newProdName by remember { mutableStateOf("") }
    var newProdSize by remember { mutableStateOf("Medium") }
    var newProdWeight by remember { mutableStateOf("25.0") }
    var newProdStock by remember { mutableStateOf("0") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ዕለታዊ የምርት መመዝገቢያ (Production Logs & Adjustments)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("አዲስ ምርት", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                text = "የእያንዳንዱን ምርት የከረጢት ብዛት ለማስተካከል የምርት ስሙን ይጫኑ፦",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        items(products) { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedProductForStockUpdate = p
                        stockInputText = p.currentStock.toString()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(0.6.dp, GrayBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(p.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("መጠን፡ ${p.size}  •  ክብደት፡ ${p.bagWeightKg} ኪ.ግ", color = MutedText, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${p.currentStock}",
                            color = EmeraldGlow,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ከረጢት", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit Stock", tint = MutedText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            // Quick Raw Materials logs
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030604)),
                border = BorderStroke(0.8.dp, Color(0xFF13281E))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("የጥሬ ዕቃ እና ማስተርባች ፈጣን መጨመሪያ/መቀነሻ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.changeRawMaterial(100) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11261B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ጥሬ እቃ +100 ኪ.ግ", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.changeRawMaterial(-100) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261111)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ጥሬ እቃ -100 ኪ.ግ", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.changeMasterbatch(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2311)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ማስተርባች +10 ኪ.ግ", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.changeMasterbatch(-10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261111)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ማስተርባች -10 ኪ.ግ", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Product stock adjustments Dialog
    if (selectedProductForStockUpdate != null) {
        val p = selectedProductForStockUpdate!!
        AlertDialog(
            onDismissRequest = { selectedProductForStockUpdate = null },
            title = { Text("የክምችት መጠን ማስተካከያ (Stock Adjustment)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("ለምርት፡ ${p.name}", color = MutedText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = stockInputText,
                        onValueChange = { stockInputText = it },
                        label = { Text("የአሁኑ የከረጢት ብዛት") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val stockVal = stockInputText.toIntOrNull() ?: 0
                        viewModel.updateProductStock(p.id, stockVal)
                        selectedProductForStockUpdate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGlow)
                ) {
                    Text("አስቀምጥ (Save)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedProductForStockUpdate = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("ተው (Cancel)")
                }
            },
            containerColor = Color(0xFF0C100D),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("አዲስ ምርት መመዝገቢያ", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = { newProdName = it },
                        label = { Text("የምርቱ ስም") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                    OutlinedTextField(
                        value = newProdSize,
                        onValueChange = { newProdSize = it },
                        label = { Text("መጠን (ምሳሌ፡ 60x80)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                    OutlinedTextField(
                        value = newProdWeight,
                        onValueChange = { newProdWeight = it },
                        label = { Text("ክብደት በአንድ ከረጢት (ኪ.ግ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                    OutlinedTextField(
                        value = newProdStock,
                        onValueChange = { newProdStock = it },
                        label = { Text("የመጀመሪያ ክምችት (ብዛት)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val weight = newProdWeight.toDoubleOrNull() ?: 25.0
                        val stock = newProdStock.toIntOrNull() ?: 0
                        if (newProdName.isNotBlank()) {
                            viewModel.addProduct(newProdName, newProdSize, weight, stock)
                        }
                        // reset inputs
                        newProdName = ""
                        newProdSize = "Medium"
                        newProdWeight = "25.0"
                        newProdStock = "0"
                        showAddProductDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGlow)
                ) {
                    Text("ይመዝገብ (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("ተው (Cancel)")
                }
            },
            containerColor = Color(0xFF0C100D),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ==========================================
// SCREEN 3: STOCK WINDOW REDESIGNED TAB!!!
// ==========================================
@Composable
fun StockTab(
    viewModel: MainViewModel,
    products: List<Product>,
    rawMaterial: Int,
    masterbatch: Int
) {
    var selectedStockTab by remember { mutableStateOf("ProductStock") } // "RawMaterial", "Masterbatch", "ProductStock"

    Column(modifier = Modifier.fillMaxSize()) {
        // Redesigned sliding filter menu for sub-tabs with emerald shine line animation
        TabRow(
            selectedTabIndex = when (selectedStockTab) {
                "RawMaterial" -> 0
                "Masterbatch" -> 1
                else -> 2
            },
            containerColor = Color(0xFF040605),
            contentColor = EmeraldGlow,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[
                        when (selectedStockTab) {
                            "RawMaterial" -> 0
                            "Masterbatch" -> 1
                            else -> 2
                        }
                    ]),
                    color = EmeraldGlow
                )
            },
            modifier = Modifier.border(BorderStroke(0.5.dp, GrayBorder), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            Tab(
                selected = selectedStockTab == "RawMaterial",
                onClick = { selectedStockTab = "RawMaterial" },
                text = { Text("ጥሬ እቃ (Raw)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedStockTab == "Masterbatch",
                onClick = { selectedStockTab = "Masterbatch" },
                text = { Text("ማስተርባች (Color)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedStockTab == "ProductStock",
                onClick = { selectedStockTab = "ProductStock" },
                text = { Text("ምርት ክምችት", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (selectedStockTab) {
                "RawMaterial" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                            border = BorderStroke(0.8.dp, GrayBorder)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("ጥሬ ዕቃ ደረጃ (Raw Material Inventory)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "$rawMaterial ኪ.ግ",
                                    color = EmeraldGlow,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, "Status Ok", tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ደረጃው ምቹ ነው (Healthy level for production)", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                "Masterbatch" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                            border = BorderStroke(0.8.dp, GrayBorder)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("ማስተርባች ደረጃ (Color Masterbatch Inventory)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "$masterbatch ኪ.ግ",
                                    color = Color(0xFFFFD700),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, "Status Caution", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ደረጃው መካከለኛ ነው (Monitor stock)", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                "ProductStock" -> ProductStockGaugeView(products = products)
            }
        }
    }
}

@Composable
fun ProductStockGaugeView(products: List<Product>) {
    var listAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        listAnimated = true
    }

    val totalBags = products.sumOf { it.currentStock }
    // Critical is defined as < 5% of 200, i.e., < 10 bags
    val criticalCount = products.count { it.currentStock < 10 }
    val productCount = products.size

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_critical_stock")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 8.dp)
    ) {
        // Summary Card at top with count-up animation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF00FF88).copy(alpha = 0.1f),
                    spotColor = Color(0xFF00FF88).copy(alpha = 0.15f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
            border = BorderStroke(0.8.dp, Color(0xFF222222))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: Total Bags
                SummaryStockMetricItem(
                    title = "ጠቅላላ ከረጢት",
                    subTitle = "Total Bags",
                    value = "$totalBags",
                    color = Color(0xFF00FF88),
                    modifier = Modifier.weight(1f)
                )
                // Column 2: Critical Stocks
                SummaryStockMetricItem(
                    title = "ወሳኝ ክምችት",
                    subTitle = "Critical Stock",
                    value = "$criticalCount",
                    color = if (criticalCount > 0) Color(0xFFFF3B3B) else Color(0xFF8C9E94),
                    modifier = Modifier.weight(1f)
                )
                // Column 3: Product Types count
                SummaryStockMetricItem(
                    title = "የምርት አይነት",
                    subTitle = "Products",
                    value = "$productCount",
                    color = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Product stock list
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ምንም ምርት አልተመዘገበም...",
                    color = Color(0xFF8C9E94),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(products) { index, p ->
                    // Guarantee removing "avocado" and replacing empty/fallback with "ሸርጅን"
                    val cleanName = p.name
                        .replace("avocado", "", ignoreCase = true)
                        .replace("Avocado", "", ignoreCase = true)
                        .replace("አቮካዶ", "", ignoreCase = true)
                        .trim()
                        .let { if (it.isEmpty()) "ሸርጅን" else it }

                    val percent = (p.currentStock / 200.0).coerceIn(0.0, 1.0)
                    val isCritical = p.currentStock < 10 // Less than 5% of 200 (< 10 bags)

                    val levelColor = when {
                        percent > 0.40 -> Color(0xFF00FF88) // High
                        percent >= 0.15 -> Color(0xFFFFD700) // Medium/Gold
                        percent >= 0.05 -> Color(0xFFFF8C00) // Low/Orange
                        else -> Color(0xFFFF3B3B) // Critical/Red
                    }

                    val badgeText = when {
                        percent > 0.40 -> "✓ ምቹ ክምችት"
                        percent >= 0.15 -> "⚠ መካከለኛ"
                        else -> "🔴 ወሳኝ!"
                    }

                    // Staggered slide up animation calculation
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (listAnimated) 1f else 0f,
                        animationSpec = tween(400, delayMillis = index * 60),
                        label = "cardAlpha"
                    )
                    val cardSlide by animateFloatAsState(
                        targetValue = if (listAnimated) 0f else 40f,
                        animationSpec = tween(400, delayMillis = index * 60),
                        label = "cardSlide"
                    )

                    // Line filling from left animation on load
                    var isLineAnimated by remember { mutableStateOf(false) }
                    LaunchedEffect(p.currentStock) {
                        isLineAnimated = true
                    }
                    val animatedFillPercent by animateFloatAsState(
                        targetValue = if (isLineAnimated) percent.toFloat() else 0f,
                        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
                        label = "fillPercent"
                    )

                    // Glassmorphism Card with green left border glow
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = cardAlpha
                                translationY = cardSlide
                            }
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0xFF00FF88).copy(alpha = 0.12f),
                                spotColor = Color(0xFF00FF88).copy(alpha = 0.2f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0D0B).copy(alpha = 0.75f)),
                        border = BorderStroke(0.8.dp, Color(0xFF1F2E24).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Left border green glow
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF00FF88), Color(0xFF00FF88).copy(alpha = 0.4f))
                                        )
                                    )
                            )

                            // Main card layout content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cleanName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "መጠን፡ ${p.size} · ክብደት፡ ${p.bagWeightKg} ኪ.ግ",
                                            color = Color(0xFF8C9E94),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    // Bags Count (Stylized Monospace mimicking Orbitron)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "${p.currentStock}",
                                                color = levelColor,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.graphicsLayer {
                                                    if (isCritical) {
                                                        alpha = pulseAlpha
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ከረጢት",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Progress Bar representing 0 to 200 bags capacity
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color(0xFF151816), RoundedCornerShape(100.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedFillPercent)
                                            .fillMaxHeight()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(levelColor.copy(alpha = 0.6f), levelColor)
                                                ),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .graphicsLayer {
                                                if (isCritical) {
                                                    alpha = pulseAlpha
                                                }
                                            }
                                    )
                                }

                                // Status Badge below bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(levelColor.copy(alpha = 0.15f))
                                            .border(BorderStroke(0.5.dp, levelColor.copy(alpha = 0.4f)), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = levelColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.graphicsLayer {
                                                if (isCritical) {
                                                    alpha = pulseAlpha
                                                }
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
    }
}

@Composable
fun SummaryStockMetricItem(
    title: String,
    subTitle: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF020202).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(BorderStroke(0.5.dp, Color(0xFF1F1F1F)), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(subTitle, color = Color(0xFF8C9E94), fontSize = 8.sp, maxLines = 1)
        Spacer(modifier = Modifier.height(6.dp))
        AnimateCountUpText(
            valueString = value,
            color = color,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )
    }
}

// ==========================================
// SCREEN 4: WORKERS SCREEN WITH THIRD TAB!
// ==========================================
@Composable
fun WorkersTab(
    viewModel: MainViewModel,
    workers: List<Worker>,
    attendanceList: List<Attendance>,
    currentMonth: Int,
    currentYear: Int
) {
    var selectedWorkersTab by remember { mutableStateOf("Attendance") } // "Attendance", "Salary", "MonthlyCalendar"

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = when (selectedWorkersTab) {
                "Attendance" -> 0
                "Salary" -> 1
                else -> 2
            },
            containerColor = Color(0xFF040605),
            contentColor = EmeraldGlow,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[
                        when (selectedWorkersTab) {
                            "Attendance" -> 0
                            "Salary" -> 1
                            else -> 2
                        }
                    ]),
                    color = EmeraldGlow
                )
            },
            modifier = Modifier.border(BorderStroke(0.5.dp, GrayBorder), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            Tab(
                selected = selectedWorkersTab == "Attendance",
                onClick = { selectedWorkersTab = "Attendance" },
                text = { Text("አቴንዳንስ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedWorkersTab == "Salary",
                onClick = { selectedWorkersTab = "Salary" },
                text = { Text("ደመወዝ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedWorkersTab == "MonthlyCalendar",
                onClick = { selectedWorkersTab = "MonthlyCalendar" },
                text = { Text("የወር ቀን መቁጠሪያ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            when (selectedWorkersTab) {
                "Attendance" -> AttendanceTabContent(viewModel, workers, attendanceList)
                "Salary" -> SalaryTabContent(workers, attendanceList)
                "MonthlyCalendar" -> MonthlyCalendarTabContent(viewModel, workers, attendanceList, currentMonth, currentYear)
            }
        }
    }
}

// Subtab A: Today Attendance marker
@Composable
fun AttendanceTabContent(viewModel: MainViewModel, workers: List<Worker>, attendanceList: List<Attendance>) {
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var newWName by remember { mutableStateOf("") }
    var newWRole by remember { mutableStateOf("Operator") }
    var newWRate by remember { mutableStateOf("350") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("የዕለት ሰራተኞች መገኘት ምልክት ማድረጊያ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddWorkerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("አዲስ ሰራተኛ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(workers) { w ->
            // Find today (day 28) attendance for this worker
            val todayLog = attendanceList.find { it.workerId == w.id && it.day == 28 && it.month == 9 && it.year == 2018 }
            val status = todayLog?.status ?: "በስራ ላይ"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(0.6.dp, GrayBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(w.avatarColorString))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = w.name.take(1),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(w.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(w.role, color = MutedText, fontSize = 11.sp)
                        }
                    }

                    // Log control buttons for today's status
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.toggleAttendance(w.id, 28, 9, 2018, "በስራ ላይ") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == "በስራ ላይ") Color(0xFF1B4D2C) else Color(0xFF151816)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("✓ ስራ ላይ", fontSize = 10.sp, color = if (status == "በስራ ላይ") EmeraldGlow else Color.White)
                        }
                        Button(
                            onClick = { viewModel.toggleAttendance(w.id, 28, 9, 2018, "ቀርቷል") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == "ቀርቷል") Color(0xFF4D1B1B) else Color(0xFF151816)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("✗ ቀርቷል", fontSize = 10.sp, color = if (status == "ቀርቷል") Color.Red else Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showAddWorkerDialog) {
        AlertDialog(
            onDismissRequest = { showAddWorkerDialog = false },
            title = { Text("አዲስ ሰራተኛ መመዝገቢያ", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newWName,
                        onValueChange = { newWName = it },
                        label = { Text("የሰራተኛው ሙሉ ስም") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                    OutlinedTextField(
                        value = newWRole,
                        onValueChange = { newWRole = it },
                        label = { Text("የስራ ድርሻ (Role)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                    OutlinedTextField(
                        value = newWRate,
                        onValueChange = { newWRate = it },
                        label = { Text("ዕለታዊ የደመወዝ ታሪፍ (ብር)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldGlow,
                            unfocusedBorderColor = GrayBorder
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rate = newWRate.toIntOrNull() ?: 350
                        if (newWName.isNotBlank()) {
                            val colors = listOf("#00FF88", "#3498db", "#e67e22", "#9b59b6", "#f1c40f")
                            val chosenColor = colors.random()
                            viewModel.addWorker(newWName, newWRole, rate, chosenColor)
                        }
                        newWName = ""
                        newWRole = "Operator"
                        newWRate = "350"
                        showAddWorkerDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGlow)
                ) {
                    Text("አስቀምጥ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkerDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("ተው")
                }
            },
            containerColor = Color(0xFF0C100D),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Subtab B: Workers Salary Calculations
@Composable
fun SalaryTabContent(workers: List<Worker>, attendanceList: List<Attendance>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("የሰራተኞች ወራዊ የደመወዝ መዝገብ ሂሳብ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        items(workers) { w ->
            // Filter attendance for month 9 (Ginbot) 2018
            val monthLogs = attendanceList.filter { it.workerId == w.id && it.month == 9 && it.year == 2018 }
            val dutyDays = monthLogs.count { it.status == "በስራ ላይ" }
            val salaryTotal = dutyDays * w.dailyRate

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                border = BorderStroke(0.6.dp, GrayBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(w.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("በእያንዳንዱ ቀን፡ ${w.dailyRate} ብር", color = MutedText, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$salaryTotal ብር",
                            color = Color(0xFFFFD700),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$dutyDays ቀናት ሰርቷል",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}


// Subtab C: BRAND NEW MONTHLY CALENDAR TAB!!! ("የወር ቀን መቁጠሪያ")
@Composable
fun MonthlyCalendarTabContent(
    viewModel: MainViewModel,
    workers: List<Worker>,
    attendanceList: List<Attendance>,
    currentMonthId: Int,
    currentYearValue: Int
) {
    var animateOnLoad by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateOnLoad = true
    }

    val currentMonthName = ET_MONTHS.getOrNull(currentMonthId - 1) ?: "ግንቦት"

    // Multi-metrics count for calendar summary
    val filterMonthAttendance = attendanceList.filter { it.month == currentMonthId && it.year == currentYearValue }
    val totalDutyDays = filterMonthAttendance.count { it.status == "በስራ ላይ" }
    val totalAbsentDays = filterMonthAttendance.count { it.status == "ቀርቷል" }

    // Average attendance level calculation
    val averageRate = if (workers.isNotEmpty()) {
        val rates = workers.map { w ->
            val wl = filterMonthAttendance.filter { it.workerId == w.id }
            val duty = wl.count { it.status == "በስራ ላይ" }
            val total = wl.count { it.status == "በስራ ላይ" || it.status == "ቀርቷል" }
            if (total > 0) (duty.toDouble() / total) * 100 else 100.0
        }
        rates.average().toInt()
    } else 100

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. MONTH HEADER: Ethiopian month name and year with left/right navigation arrows
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFF00FF88).copy(alpha = 0.1f),
                        spotColor = Color(0xFF00FF88).copy(alpha = 0.15f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF040A06)),
                border = BorderStroke(0.8.dp, Color(0xFF1F2F24))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Modern top green shine line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(EmeraldGlow, EmeraldGlow.copy(alpha = 0.05f))
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateMonth(-1) },
                            modifier = Modifier.background(Color(0xFF0D1611), CircleShape)
                        ) {
                            Icon(Icons.Filled.ArrowBack, "Prev Month", tint = EmeraldGlow)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentMonthName $currentYearValue",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "የኢትዮጵያ ወር መቁጠሪያ (ET Month)",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateMonth(1) },
                            modifier = Modifier.background(Color(0xFF0D1611), CircleShape)
                        ) {
                            Icon(Icons.Filled.ArrowForward, "Next Month", tint = EmeraldGlow)
                        }
                    }
                }
            }
        }

        // 2. LEGEND ROW: Small colored squares showing categories
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = EmeraldGlow, label = "በስራ ላይ")
                LegendItem(color = Color(0xFFFF3B3B), label = "ቀርቷል")
                LegendItem(color = Color(0xFF3B82F6), label = "እሁድ")
                LegendItem(color = Color(0xFF1E2421), label = "ወደፊት")
            }
        }

        // 3. WORKER CALENDAR CARDS
        itemsIndexed(workers) { index0, w ->
            // Filter attendance states
            val workerLogs = filterMonthAttendance.filter { it.workerId == w.id }
            val dutyCount = workerLogs.count { it.status == "በስራ ላይ" }
            val absentCount = workerLogs.count { it.status == "ቀርቷል" }
            val hasAbsences = absentCount > 0

            val workerTotalLogged = workerLogs.count { it.status == "በስራ ላይ" || it.status == "ቀርቷል" }
            val workerAttendanceRate = if (workerTotalLogged > 0) {
                ((dutyCount.toDouble() / workerTotalLogged) * 100).toInt()
            } else 100

            // Staggered slide up animation
            val cardAlpha by animateFloatAsState(
                targetValue = if (animateOnLoad) 1f else 0f,
                animationSpec = tween(450, delayMillis = index0 * 50),
                label = "workerCardAlpha"
            )
            val cardSlide by animateFloatAsState(
                targetValue = if (animateOnLoad) 0f else 30f,
                animationSpec = tween(450, delayMillis = index0 * 50),
                label = "workerCardSlide"
            )

            // Dynamic border styling: RED if any absence this month, GREEN otherwise
            val leftBorderColor = if (hasAbsences) Color(0xFFFF3B3B) else EmeraldGlow

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                        translationY = cardSlide
                    }
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = leftBorderColor.copy(alpha = 0.12f),
                        spotColor = leftBorderColor.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B08).copy(alpha = 0.8f)),
                border = BorderStroke(0.8.dp, Color(0xFF1F2F24).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // Left glow border
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(leftBorderColor, leftBorderColor.copy(alpha = 0.3f))
                                )
                            )
                    )

                    // Worker card body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header info of worker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color(android.graphics.Color.parseColor(w.avatarColorString)).copy(
                                                alpha = 0.15f
                                            )
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                Color(android.graphics.Color.parseColor(w.avatarColorString))
                                            ), CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = w.name.take(2),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = w.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = w.role,
                                        color = MutedText,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Statistics rates row
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("በስራ", color = MutedText, fontSize = 8.sp)
                                    Text("${dutyCount}ቀን", color = EmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("absent", color = MutedText, fontSize = 8.sp)
                                    Text("${absentCount}ቀን", color = Color(0xFFFF3B3B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("መቶኛ", color = MutedText, fontSize = 8.sp)
                                    Text("$workerAttendanceRate%", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Grid 30 days of the Ethiopian Month
                        // Using a manual simple custom row layout to avoid Nesting lazy view within vertical lazy, ensuring high scroll safety & perfect rendering!
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0..4) { // 5 rows of 6 cells = 30 days
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 1..6) {
                                        val dayIndex = row * 6 + col
                                        val isSunday = dayIndex == 3 || dayIndex == 10 || dayIndex == 17 || dayIndex == 24
                                        val log = workerLogs.find { it.day == dayIndex }

                                        // Status routing
                                        val status = when {
                                            dayIndex > 27 -> "future" // Today is 28, so 29 and 30 are future
                                            log != null -> log.status
                                            isSunday -> "እሁድ"
                                            else -> "በስራ ላይ"
                                        }

                                        // Cell custom color scheme
                                        val cellColor = when (status) {
                                            "በስራ ላይ" -> Color(0xFF1B4D2C).copy(alpha = 0.5f)
                                            "ቀርቷል" -> Color(0xFF4D1B1B).copy(alpha = 0.5f)
                                            "እሁድ" -> Color(0xFF1B2C4D).copy(alpha = 0.5f)
                                            else -> Color(0xFF151816) // Future
                                        }

                                        val cellBorderColor = when (status) {
                                            "በስራ ላይ" -> EmeraldGlow.copy(alpha = 0.4f)
                                            "ቀርቷል" -> Color(0xFFFF3B3B).copy(alpha = 0.4f)
                                            "እሁድ" -> Color(0xFF3B82F6).copy(alpha = 0.4f)
                                            else -> Color(0xFF222222)
                                        }

                                        val cellText = when (status) {
                                            "በስራ ላይ" -> "✓"
                                            "ቀርቷል" -> "✗"
                                            "እሁድ" -> "☀"
                                            else -> "$dayIndex"
                                        }

                                        val cellTextColor = when (status) {
                                            "በስራ ላይ" -> EmeraldGlow
                                            "ቀርቷል" -> Color(0xFFFF3B3B)
                                            "እሁድ" -> Color(0xFF8AB4F8)
                                            else -> MutedText.copy(alpha = 0.4f)
                                        }

                                        // Highlighting day 28 (today's cell) with a green glow border!
                                        val isToday = dayIndex == 28
                                        val finalBorderStroke = if (isToday) {
                                            BorderStroke(1.2.dp, EmeraldGlow)
                                        } else {
                                            BorderStroke(0.5.dp, cellBorderColor)
                                        }

                                        // Small daily cell block representation
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(cellColor)
                                                .border(finalBorderStroke, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cellText,
                                                color = cellTextColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
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

        // 4. MONTHLY SUMMARY CARD: bottom statistics with animations
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFFFFD700).copy(alpha = 0.1f),
                        spotColor = Color(0xFFFFD700).copy(alpha = 0.15f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030303)),
                border = BorderStroke(0.8.dp, Color(0xFF1F1F1F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ወርሃዊ የአቴንዳንስ አጠቃላይ ድምር",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monthly Performance Aggregate Tracker",
                        color = MutedText,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStockMetricItem(
                            title = "ለስራ የተገኙ",
                            subTitle = "Duty Days",
                            value = "$totalDutyDays",
                            color = EmeraldGlow,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStockMetricItem(
                            title = "የቀሩ ቀናት",
                            subTitle = "Absent Days",
                            value = "$totalAbsentDays",
                            color = Color(0xFFFF3B3B),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStockMetricItem(
                            title = "አማካኝ መቶኛ",
                            subTitle = "Average Rate",
                            value = "$averageRate%",
                            color = Color(0xFFFFD700),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}


// ==========================================
// SCREEN 5: BINIYAM AI SCREEN
// ==========================================
@Composable
fun AiTab(viewModel: MainViewModel, messages: List<ChatMessage>) {
    var queryText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // AI Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
            border = BorderStroke(0.6.dp, GrayBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ቢኒያም AI ሪፖርት ረዳት", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("AI-Powered Recycle Analytics Assistant", color = MutedText, fontSize = 10.sp)
                }

                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.Refresh, "Clear chat", tint = Color.Red)
                }
            }
        }

        // Chat flow section
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isModel = msg.sender == "model"
                val align = if (isModel) Alignment.Start else Alignment.End
                val color = if (isModel) Color(0xFF0F1A14) else Color(0xFF1B4D31)
                val textCol = if (isModel) LightText else Color.White
                val borderCol = if (isModel) Color(0xFF152A1E) else EmeraldGlow.copy(alpha = 0.5f)

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isModel) 0.dp else 12.dp,
                            bottomEnd = if (isModel) 12.dp else 0.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = color),
                        border = BorderStroke(0.5.dp, borderCol),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .shadow(2.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = textCol,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Input bottom textfield bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                placeholder = { Text("ቢኒያም AI ን ይጠይቁ...", color = MutedText, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = EmeraldGlow,
                    unfocusedBorderColor = GrayBorder
                )
            )

            IconButton(
                onClick = {
                    if (queryText.isNotBlank()) {
                        viewModel.sendChatMessage(queryText)
                        queryText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(EmeraldGlow, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Send, "Send Message", tint = Color.Black)
            }
        }
    }
}
