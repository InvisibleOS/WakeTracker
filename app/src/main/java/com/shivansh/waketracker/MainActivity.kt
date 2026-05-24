package com.shivansh.waketracker

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import com.shivansh.waketracker.data.BufferTimePreferences
import com.shivansh.waketracker.ui.NfcProvisioningSheet
import com.shivansh.waketracker.ui.WakeGreen
import com.shivansh.waketracker.ui.WakeRed
import com.shivansh.waketracker.ui.calculateWakeDotColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shivansh.waketracker.data.WakeStatus
import com.shivansh.waketracker.ui.theme.WakeTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

enum class AppTab { TRACKER, SETTINGS }
enum class ConsistencyView { MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: WakeViewModel by viewModels()
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedPrefs = getSharedPreferences("WakeTrackerPrefs", MODE_PRIVATE)
        // Reset provisioning flag in case it was left stale
        sharedPrefs.edit().putBoolean("is_provisioning", false).apply()

        setContent {
            WakeTrackerTheme {
                val bufferTimeMinutes by BufferTimePreferences
                    .getBufferTimeMinutes(LocalContext.current)
                    .collectAsState(initial = BufferTimePreferences.DEFAULT_BUFFER_MINUTES)
                // Request Notification Permission on Startup for Android 13+
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    viewModel.scheduleReminders(
                        context,
                        sharedPrefs.getInt("target_hour", 8),
                        sharedPrefs.getInt("target_minute", 0)
                    )
                }

                var currentTab by remember { mutableStateOf(AppTab.TRACKER) }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = if (currentTab == AppTab.TRACKER) "Tracker" else "Settings",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                    },
                    bottomBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NavBarItem(
                                        icon = Icons.Default.CalendarMonth,
                                        label = "Tracker",
                                        isSelected = currentTab == AppTab.TRACKER,
                                        onClick = { currentTab = AppTab.TRACKER }
                                    )
                                    NavBarItem(
                                        icon = Icons.Default.Settings,
                                        label = "Settings",
                                        isSelected = currentTab == AppTab.SETTINGS,
                                        onClick = { currentTab = AppTab.SETTINGS }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (currentTab) {
                            AppTab.TRACKER -> WakeTrackerScreen(viewModel, sharedPrefs, bufferTimeMinutes)
                            AppTab.SETTINGS -> SettingsScreen(sharedPrefs, viewModel, bufferTimeMinutes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, animationSpec = tween(250), label = "")
    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, animationSpec = tween(250), label = "")

    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = if (!isSelected) label else null, tint = contentColor, modifier = Modifier.size(24.dp))
        
        AnimatedVisibility(
            visible = isSelected,
            enter = expandHorizontally(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = shrinkHorizontally(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeTrackerScreen(viewModel: WakeViewModel, sharedPrefs: SharedPreferences, bufferTimeMinutes: Int) {
    val logs by viewModel.monthlyLogs.collectAsState()
    val pagerState = rememberPagerState(initialPage = LocalDate.now().monthValue - 1, pageCount = { 12 })
    val visibleMonth = remember(pagerState.currentPage) { YearMonth.of(LocalDate.now().year, pagerState.currentPage + 1) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf(ConsistencyView.MONTHLY) }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30000); currentTime = LocalTime.now() } }

    val today = LocalDate.now()
    val totalYearlyRecords = logs.size
    val onTimeYearly = logs.count { it.status == WakeStatus.ON_TIME }
    val yearlyFraction = if (totalYearlyRecords > 0) onTimeYearly.toFloat() / totalYearlyRecords else 0f
    val logsForCurrentPage = logs.filter { LocalDate.parse(it.dateStr).monthValue == visibleMonth.monthValue }
    val onTimeMonthly = logsForCurrentPage.count { it.status == WakeStatus.ON_TIME }
    val daysDivider = if (visibleMonth.monthValue == today.monthValue) today.dayOfMonth else visibleMonth.lengthOfMonth()
    val monthlyFraction = if (daysDivider > 0) onTimeMonthly.toFloat() / daysDivider else 0f

    val targetFraction = if (selectedView == ConsistencyView.YEARLY) yearlyFraction else monthlyFraction
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "")
    val consistencyColor by animateColorAsState(targetValue = lerp(WakeRed, WakeGreen, animatedFraction), animationSpec = tween(600), label = "")

    val bufferDurationMs = bufferTimeMinutes.toLong() * 60_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier.fillMaxSize(),
                color = consistencyColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${(animatedFraction * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = consistencyColor)
                Text(text = "Consistency", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isMonthly = selectedView == ConsistencyView.MONTHLY
            
            val monthlyInteractionSource = remember { MutableInteractionSource() }
            val monthlyIsPressed by monthlyInteractionSource.collectIsPressedAsState()
            val monthlyScale by animateFloatAsState(
                targetValue = if (monthlyIsPressed) 0.94f else 1f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow), label = ""
            )
            val monthlyWeight by animateFloatAsState(if (isMonthly) 1.25f else 1f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "")
            val monthlyColor by animateColorAsState(if (isMonthly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), animationSpec = tween(300), label = "")
            val monthlyTextColor by animateColorAsState(if (isMonthly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, animationSpec = tween(300), label = "")
            val monthlyEndCorner by androidx.compose.animation.core.animateDpAsState(if (isMonthly) 28.dp else 6.dp, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "")

            val yearlyInteractionSource = remember { MutableInteractionSource() }
            val yearlyIsPressed by yearlyInteractionSource.collectIsPressedAsState()
            val yearlyScale by animateFloatAsState(
                targetValue = if (yearlyIsPressed) 0.94f else 1f,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow), label = ""
            )
            val yearlyWeight by animateFloatAsState(if (!isMonthly) 1.25f else 1f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "")
            val yearlyColor by animateColorAsState(if (!isMonthly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), animationSpec = tween(300), label = "")
            val yearlyTextColor by animateColorAsState(if (!isMonthly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, animationSpec = tween(300), label = "")
            val yearlyStartCorner by androidx.compose.animation.core.animateDpAsState(if (!isMonthly) 28.dp else 6.dp, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "")

            Surface(
                modifier = Modifier
                    .weight(monthlyWeight)
                    .height(48.dp)
                    .scale(monthlyScale),
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = monthlyEndCorner, bottomEnd = monthlyEndCorner),
                color = monthlyColor,
                contentColor = monthlyTextColor,
                onClick = { selectedView = ConsistencyView.MONTHLY },
                interactionSource = monthlyInteractionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Monthly", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(yearlyWeight)
                    .height(48.dp)
                    .scale(yearlyScale),
                shape = RoundedCornerShape(topStart = yearlyStartCorner, bottomStart = yearlyStartCorner, topEnd = 28.dp, bottomEnd = 28.dp),
                color = yearlyColor,
                contentColor = yearlyTextColor,
                onClick = { selectedView = ConsistencyView.YEARLY },
                interactionSource = yearlyInteractionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Yearly", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "${visibleMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            pageSpacing = 24.dp,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
        ) { page ->
            val pageMonth = YearMonth.of(LocalDate.now().year, page + 1)
            val firstDayOffset = pageMonth.atDay(1).dayOfWeek.value - 1

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                items(7) { i -> Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) { Text(text = listOf("M", "T", "W", "T", "F", "S", "S")[i], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) } }
                items(firstDayOffset) { Spacer(modifier = Modifier.aspectRatio(1f)) }
                items(pageMonth.lengthOfMonth()) { i ->
                    val date = pageMonth.atDay(i + 1)
                    val log = logs.find { it.dateStr == date.toString() }
                    val status = if (date.isAfter(today)) WakeStatus.FUTURE else if (date == today) (log?.status ?: if (currentTime.isAfter(LocalTime.of(sharedPrefs.getInt("target_hour", 8), 0))) WakeStatus.MISSED else WakeStatus.FUTURE) else log?.status ?: WakeStatus.MISSED

                    // Compute gradient color for LATE dots; ON_TIME/MISSED/FUTURE use canonical colors
                    val dotColor = when {
                        status == WakeStatus.FUTURE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        log != null && log.targetTimeMs > 0L -> calculateWakeDotColor(
                            targetTimeMs = log.targetTimeMs,
                            actualScanTimeMs = log.actualScanTimeMs,
                            bufferDurationMs = bufferDurationMs
                        )
                        status == WakeStatus.ON_TIME -> WakeGreen
                        else -> WakeRed // MISSED or legacy log with targetTimeMs == 0
                    }

                    DotItem(status = status, dotColor = dotColor, onClick = { if (!date.isAfter(today)) { selectedDate = date; showBottomSheet = true } })
                }
            }
        }
    }

    if (showBottomSheet && selectedDate != null) {
        val log = logs.find { it.dateStr == selectedDate.toString() }
        val targetTime = LocalTime.of(sharedPrefs.getInt("target_hour", 8), 0)
        val status = if (selectedDate == today && log == null) {
            if (currentTime.isAfter(targetTime)) WakeStatus.MISSED else WakeStatus.FUTURE
        } else {
            log?.status ?: WakeStatus.MISSED
        }

        // Compute the gradient-aware color for the bottom sheet indicator
        val sheetIndicatorColor = when {
            status == WakeStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant
            log != null && log.targetTimeMs > 0L -> calculateWakeDotColor(
                targetTimeMs = log.targetTimeMs,
                actualScanTimeMs = log.actualScanTimeMs,
                bufferDurationMs = bufferDurationMs
            )
            status == WakeStatus.ON_TIME -> WakeGreen
            else -> WakeRed
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date Indicator
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CookieShape())
                        .background(sheetIndicatorColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedDate!!.dayOfMonth.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (status == WakeStatus.FUTURE) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDate!!.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = (if (status == WakeStatus.FUTURE) MaterialTheme.colorScheme.onSurfaceVariant else Color.White).copy(alpha = 0.8f)
                        )
                    }
                }

                // Status Card
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = when (status) {
                                WakeStatus.ON_TIME -> "You woke up!"
                                WakeStatus.LATE -> "You woke up late!"
                                else -> "Not scanned"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // Scan time pill inside the card
                        if (status == WakeStatus.ON_TIME || status == WakeStatus.LATE) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            ) {
                                val scanTime = log?.actualScanTimeMs ?: 0L
                                Text(
                                    text = "Scan Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(scanTime))}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(sharedPrefs: SharedPreferences, viewModel: WakeViewModel, bufferTimeMinutes: Int) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showNfcProvisioningSheet by remember { mutableStateOf(false) }
    var targetHour by remember { mutableIntStateOf(sharedPrefs.getInt("target_hour", 8)) }
    var targetMinute by remember { mutableIntStateOf(sharedPrefs.getInt("target_minute", 0)) }
    
    val nfcProvisioningViewModel: NfcProvisioningViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(onClick = { showTimePicker = true }, shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Target Wake Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Scan NFC before this time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Text(LocalTime.of(targetHour, targetMinute).format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // --- Buffer Time Selector ---
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Buffer Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Grace period before a late scan counts as missed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Icon(Icons.Default.Timer, contentDescription = "Buffer Time", tint = MaterialTheme.colorScheme.primary)
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BufferTimePreferences.PRESET_OPTIONS.forEachIndexed { index, minutes ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = BufferTimePreferences.PRESET_OPTIONS.size
                            ),
                            onClick = {
                                scope.launch {
                                    BufferTimePreferences.setBufferTimeMinutes(context, minutes)
                                }
                            },
                            selected = bufferTimeMinutes == minutes
                        ) {
                            Text(
                                text = if (minutes < 60) "${minutes}m" else "${minutes / 60}h",
                                fontWeight = if (bufferTimeMinutes == minutes) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Card(onClick = { 
            nfcProvisioningViewModel.reset() // Reset state when opening
            showNfcProvisioningSheet = true 
        }, shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Setup Wake Tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Configure an NFC tag for tracking", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Icon(Icons.Default.Nfc, contentDescription = "NFC Tags", tint = MaterialTheme.colorScheme.primary)
            }
        }

        OutlinedButton(
            onClick = { showClearConfirmDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear All Data", fontWeight = FontWeight.Bold)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This will permanently clear all your wake records. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(text = "Yes, Clear It") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text(text = "Cancel") }
            }
        )
    }

    if (showNfcProvisioningSheet) {
        NfcProvisioningSheet(
            viewModel = nfcProvisioningViewModel,
            onDismissRequest = { 
                nfcProvisioningViewModel.onDismissed()
                showNfcProvisioningSheet = false 
            }
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = targetHour, initialMinute = targetMinute)
        val context = LocalContext.current
        AlertDialog(onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { 
                targetHour = state.hour
                targetMinute = state.minute
                sharedPrefs.edit().putInt("target_hour", targetHour).putInt("target_minute", targetMinute).apply()
                viewModel.scheduleReminders(context, targetHour, targetMinute)
                showTimePicker = false 
            }) { Text(text = "Confirm") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(text = "Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
fun DotItem(status: WakeStatus, dotColor: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1f, label = "scale")

    val isSolid = status != WakeStatus.FUTURE

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = if (isSolid) dotColor else Color.Transparent,
            border = if (isSolid) null else androidx.compose.foundation.BorderStroke(2.dp, dotColor.copy(alpha = 0.4f))
        ) {}
    }
}

class CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.8f,
            rounding = CornerRounding(size.minDimension * 0.1f)
        )

        val androidPath = android.graphics.Path()
        polygon.toPath(androidPath)

        // Center and scale the path accurately
        val bounds = android.graphics.RectF()
        androidPath.computeBounds(bounds, true)
        
        val matrix = android.graphics.Matrix()
        // Move current center to origin
        matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
        // Scale to fit target size (with small margin to avoid clipping)
        val scale = (size.minDimension / maxOf(bounds.width(), bounds.height())) * 0.95f
        matrix.postScale(scale, scale)
        // Move to center of the requested box
        matrix.postTranslate(size.width / 2f, size.height / 2f)

        androidPath.transform(matrix)

        return Outline.Generic(androidPath.asComposePath())
    }
}