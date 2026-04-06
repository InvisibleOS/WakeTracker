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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shivansh.waketracker.data.WakeStatus
import com.shivansh.waketracker.ui.theme.WakeTrackerTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
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

        sharedPrefs = getSharedPreferences("WakeTrackerPrefs", Context.MODE_PRIVATE)

        setContent {
            WakeTrackerTheme {
                // Request Notification Permission on Startup for Android 13+
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
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
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                    },
                    bottomBar = {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                shadowElevation = 12.dp,
                                modifier = Modifier.height(64.dp).wrapContentWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        when (currentTab) {
                            AppTab.TRACKER -> WakeTrackerScreen(viewModel, sharedPrefs)
                            AppTab.SETTINGS -> SettingsScreen(sharedPrefs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "")
    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")

    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeTrackerScreen(viewModel: WakeViewModel, sharedPrefs: SharedPreferences) {
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
    val consistencyColor by animateColorAsState(targetValue = lerp(Color(0xFFF44336), Color(0xFF4CAF50), animatedFraction), animationSpec = tween(600), label = "")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
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
                Text(text = "${(animatedFraction * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = consistencyColor)
                Text(text = "Consistency", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedView == ConsistencyView.MONTHLY,
                onClick = { selectedView = ConsistencyView.MONTHLY },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(text = "Monthly", fontWeight = FontWeight.Bold) }
            SegmentedButton(
                selected = selectedView == ConsistencyView.YEARLY,
                onClick = { selectedView = ConsistencyView.YEARLY },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(text = "Yearly", fontWeight = FontWeight.Bold) }
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
                    DotItem(status, onClick = { if (!date.isAfter(today)) { selectedDate = date; showBottomSheet = true } })
                }
            }
        }
    }

    if (showBottomSheet && selectedDate != null) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            val log = logs.find { it.dateStr == selectedDate.toString() }
            val status = if (selectedDate == today && log == null) (if (currentTime.isAfter(LocalTime.of(sharedPrefs.getInt("target_hour", 8), 0))) WakeStatus.MISSED else WakeStatus.FUTURE) else log?.status ?: WakeStatus.MISSED

            Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(100.dp).clip(CookieShape()).background(if (status == WakeStatus.ON_TIME) Color(0xFF1B5E20) else if (status == WakeStatus.FUTURE) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFB71C1C)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = selectedDate!!.dayOfMonth.toString(), style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = selectedDate!!.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(imageVector = if (status == WakeStatus.ON_TIME) Icons.Default.CheckCircle else if (status == WakeStatus.FUTURE) Icons.Default.Schedule else Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(40.dp), tint = if (status == WakeStatus.ON_TIME) Color(0xFF4CAF50) else if (status == WakeStatus.FUTURE) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f) else Color(0xFFF44336))
                        Text(text = if (status == WakeStatus.ON_TIME) "You woke up!" else if (status == WakeStatus.FUTURE) "Awaiting Scan" else "Not scanned", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        if (log != null || status == WakeStatus.FUTURE) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {
                                val text = if (log != null) "Scan Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.actualScanTimeMs ?: 0L))}" else "Target: ${LocalTime.of(sharedPrefs.getInt("target_hour", 8), 0).format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))}"
                                Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
fun SettingsScreen(sharedPrefs: SharedPreferences) {
    var showTimePicker by remember { mutableStateOf(false) }
    var targetHour by remember { mutableIntStateOf(sharedPrefs.getInt("target_hour", 8)) }
    var targetMinute by remember { mutableIntStateOf(sharedPrefs.getInt("target_minute", 0)) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(32.dp))

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
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = targetHour, initialMinute = targetMinute)
        AlertDialog(onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { targetHour = state.hour; targetMinute = state.minute; sharedPrefs.edit().putInt("target_hour", targetHour).putInt("target_minute", targetMinute).apply(); showTimePicker = false }) { Text(text = "Confirm") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(text = "Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
fun DotItem(status: WakeStatus, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1f, label = "scale")

    val (color, isSolid) = when (status) {
        WakeStatus.ON_TIME -> Color(0xFF4CAF50) to true
        WakeStatus.LATE, WakeStatus.MISSED -> Color(0xFFF44336) to true
        WakeStatus.FUTURE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) to false
    }

    Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Surface(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxSize().padding(2.dp).scale(scale),
            shape = CircleShape,
            color = if (isSolid) color else Color.Transparent,
            border = if (isSolid) null else androidx.compose.foundation.BorderStroke(2.dp, color)
        ) {}
    }
}

class CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // 1. Create the official M3 Expressive Star/Cookie
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.8f,
            rounding = CornerRounding(size.minDimension * 0.1f)
        )

        // 2. Convert to standard Android Path
        val androidPath = android.graphics.Path()
        polygon.toPath(androidPath)

        // 3. Scale and Center it to fit the Compose Box
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)

        // 4. Convert safely to a Compose Path
        return Outline.Generic(androidPath.asComposePath())
    }
}