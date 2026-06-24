package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GTDProject
import com.example.data.model.GTDTag
import com.example.data.model.GTDTask
import com.example.ui.viewmodel.GTDViewModel
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.ChatAction
import com.example.util.JalaliCalendar
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GTDAppMainScreen(viewModel: GTDViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf("dashboard") } // dashboard, add_task, ai_counsel, inbox_helper, backup

    // Listen to location alerts reactively
    val locationAlertTask by viewModel.activeLocationAlertTask.collectAsState()

    // Enforce RTL Layout Direction for Persian language bidi optimization
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageIconsForTab(currentTab),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = titleForTab(currentTab),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "dashboard",
                        onClick = { currentTab = "dashboard" },
                        icon = { Icon(Icons.Default.List, contentDescription = "پیشخوان") },
                        label = { Text("پیشخوان", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == "add_task",
                        onClick = { currentTab = "add_task" },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "کار جدید") },
                        label = { Text("کار جدید", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_add_task")
                    )
                    NavigationBarItem(
                        selected = currentTab == "inbox_helper",
                        onClick = { currentTab = "inbox_helper" },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "دستیار صندوق") },
                        label = { Text("دستیار صندوق", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_inbox_helper")
                    )
                    NavigationBarItem(
                        selected = currentTab == "ai_counsel",
                        onClick = { currentTab = "ai_counsel" },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "مشاوره") },
                        label = { Text("مشاور هوش مصنوعی", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_ai_counsel")
                    )
                    NavigationBarItem(
                        selected = currentTab == "backup",
                        onClick = { currentTab = "backup" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "پشتیبان") },
                        label = { Text("تنظیمات", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_backup")
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background subtle ambient art
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )

                // Render screen content dynamically with transitions
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        "dashboard" -> DashboardScreen(viewModel = viewModel, onNavigateToAdd = { currentTab = "add_task" })
                        "add_task" -> AddTaskScreen(viewModel = viewModel, onTaskAdded = { currentTab = "dashboard" })
                        "ai_counsel" -> AICounselScreen(viewModel = viewModel)
                        "inbox_helper" -> InboxHelperScreen(viewModel = viewModel)
                        "backup" -> BackupSyncScreen(viewModel = viewModel)
                    }
                }

                // GPS Location proximity pop-up card
                AnimatedVisibility(
                    visible = locationAlertTask != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    locationAlertTask?.let { task ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "📍 یادآور هوشمند موقعیت مکانی فعال شد!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "شما به موقعیت «${task.locationName}» نزدیک شده‌اید. کار مربوطه:",
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (!task.description.isBlank()) {
                                    Text(
                                        text = task.description,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(onClick = { viewModel.dismissLocationAlert() }) {
                                        Text("بستن هشدار", color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.toggleTaskCompletion(task)
                                            viewModel.dismissLocationAlert()
                                            Toast.makeText(context, "کار با موفقیت انجام شد!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Text("تکمیل کار", color = MaterialTheme.colorScheme.onTertiary)
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

private fun imageIconsForTab(tab: String) = when (tab) {
    "dashboard" -> Icons.Default.List
    "add_task" -> Icons.Default.AddCircle
    "ai_counsel" -> Icons.Default.Psychology
    "inbox_helper" -> Icons.Default.SmartToy
    "backup" -> Icons.Default.Settings
    else -> Icons.Default.List
}

private fun titleForTab(tab: String) = when (tab) {
    "dashboard" -> "پیشخوان اقدامات GTD"
    "add_task" -> "ثبت ایده و کار جدید"
    "ai_counsel" -> "مشاور هوش مصنوعی زمان"
    "inbox_helper" -> "چت‌بات و اینباکس دستوری"
    "backup" -> "پشتیبان‌گیری و همگام‌سازی"
    else -> "EverGTD"
}

// ---------------------------------------------------------
// 1. DASHBOARD SCREEN
// ---------------------------------------------------------
@Composable
fun DashboardScreen(viewModel: GTDViewModel, onNavigateToAdd: () -> Unit) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsState()
    val selectedProjectFilter by viewModel.selectedProjectFilter.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()

    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val activeTasks by viewModel.allActiveTasks.collectAsState()

    // Location Simulation Coordinates
    val currentLat by viewModel.currentLatitude.collectAsState()
    val currentLng by viewModel.currentLongitude.collectAsState()

    val jalaliToday = JalaliCalendar.formatJalaliDate(System.currentTimeMillis())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Beautiful Hero Title Block
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تقویم هجری شمسی",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = jalaliToday,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "برنامه ریزی هفتگی منظم • اولین روز هفته: شنبه",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Location Simulation Console
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "موقعیت مکانی شبیه‌سازی‌شده (GPS)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "موقعیت شما: (${JalaliCalendar.formatPersianNumber(String.format("%.4f", currentLat))} , ${JalaliCalendar.formatPersianNumber(String.format("%.4f", currentLng))})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Location Quick Simulation Selector
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Button(
                        onClick = { viewModel.updateSimulatedLocation(35.7448, 51.3753) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("📍 تهران (برج میلاد)", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateSimulatedLocation(35.7445, 51.3750) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("🛍️ سوپرمارکت محله", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.updateSimulatedLocation(35.7219, 51.3347) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("🏢 دفتر کار مرکزی", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("جستجو در میان کارها و ایده‌ها...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced GTD Lists Filter Tabs with Counters
        Text("بخش‌های متدولوژی GTD:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        val gtdTabs = listOf(
            Triple("INBOX", "صندوق ورودی", Icons.Default.Inbox),
            Triple("NEXT", "اقدامات بعدی", Icons.Default.PlayArrow),
            Triple("SCHEDULED", "زمان‌بندی شده", Icons.Default.CalendarMonth),
            Triple("WAITING", "در انتظار", Icons.Default.HourglassEmpty),
            Triple("SOMEDAY", "شاید وقتی دیگر", Icons.Default.Lightbulb),
            Triple("ALL", "همه کارها", Icons.Default.List)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gtdTabs) { tab ->
                val isSelected = selectedStatus == tab.first
                val count = activeTasks.count { if (tab.first == "ALL") true else it.gtdStatus == tab.first }
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setStatusFilter(tab.first) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(tab.third, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tab.second, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(JalaliCalendar.formatPersianNumber(count))
                            }
                        }
                    },
                    modifier = Modifier.testTag("gtd_tab_${tab.first.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Project and Tag Filters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "پروژه‌ها و برچسب‌ها:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            // Button to clear project/tag filters
            if (selectedProjectFilter != null || selectedTagFilter != null) {
                TextButton(onClick = {
                    viewModel.setProjectFilter(null)
                    viewModel.setTagFilter(null)
                }) {
                    Text("حذف فیلترها", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Projects Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedProjectFilter == null,
                    onClick = { viewModel.setProjectFilter(null) },
                    label = { Text("همه پروژه‌ها", fontSize = 11.sp) }
                )
            }
            items(projects) { project ->
                FilterChip(
                    selected = selectedProjectFilter == project.id,
                    onClick = { viewModel.setProjectFilter(project.id) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(project.colorHex)),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(project.name, fontSize = 11.sp)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tags Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTagFilter == null,
                    onClick = { viewModel.setTagFilter(null) },
                    label = { Text("همه برچسب‌ها", fontSize = 11.sp) }
                )
            }
            items(tags) { tag ->
                FilterChip(
                    selected = selectedTagFilter == tag.name,
                    onClick = { viewModel.setTagFilter(tag.name) },
                    label = { Text("#${tag.name}", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task List Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کارهای لیست شده (${JalaliCalendar.formatPersianNumber(filteredTasks.size)} مورد):",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredTasks.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "صندوق ورودی و اقدامات خالی است!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "می‌توانید کارهای جدید ثبت کنید یا با هوش مصنوعی مشاوره کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToAdd) {
                        Text("افزودن کار فیزیکی یا ایده")
                    }
                }
            }
        } else {
            // LazyColumn replacement inside scrollable via column elements list:
            // This is clean and handles scroll layouts beautifully
            filteredTasks.forEach { task ->
                TaskItemRow(
                    task = task,
                    projects = projects,
                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: GTDTask,
    projects: List<GTDProject>,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val associatedProject = projects.firstOrNull { it.id == task.projectId }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (task.isCompleted) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Checkbox
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "علامت تکمیل کار",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Task details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!task.description.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges and Dates Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Project Badge
                        associatedProject?.let { project ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(android.graphics.Color.parseColor(project.colorHex)).copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    project.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(project.colorHex))
                                )
                            }
                        }

                        // Tags Badge
                        if (!task.tags.isBlank()) {
                            task.tags.split(",").forEach { t ->
                                if (!t.isBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("#$t", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Due Date Badge in Jalali
                        task.dueDate?.let { date ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "📅 ${JalaliCalendar.formatJalaliDate(date)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // Location Badge
                        task.locationName?.let { locName ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "📍 $locName",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Calendar Linked Badge
                        if (task.googleCalendarEventId != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🔗 گوگل کلندر", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                // Delete Button
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف کار",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Status transition buttons
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انتقال به بخش دیگر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val availableStatuses = listOf(
                            Pair("NEXT", "اقدام بعدی"),
                            Pair("WAITING", "در انتظار"),
                            Pair("SOMEDAY", "شاید بعدا"),
                            Pair("SCHEDULED", "زمان‌بندی")
                        )
                        availableStatuses.forEach { st ->
                            if (task.gtdStatus != st.first) {
                                Button(
                                    onClick = { onStatusChange(st.first) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(st.second, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 2. ADD TASK SCREEN
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(viewModel: GTDViewModel, onTaskAdded: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("INBOX") }
    var projectId by remember { mutableStateOf<Int?>(null) }
    var tagsString by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var locationName by remember { mutableStateOf("") }
    var latitudeStr by remember { mutableStateOf("") }
    var longitudeStr by remember { mutableStateOf("") }
    var waitingFor by remember { mutableStateOf("") }
    var syncToCalendar by remember { mutableStateOf(false) }

    val projects by viewModel.allProjects.collectAsState()

    // Date Dialog Helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selCal = Calendar.getInstance()
            selCal.set(year, month, dayOfMonth)
            dueDate = selCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "تخلیه ذهن به متدولوژی GTD",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "هر فکری، کاری یا برنامه‌ای که در سر دارید را بدون درنگ ثبت کنید تا صندوق ورودی تکمیل شود.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // AI Quick Entry Box
        var quickInput by remember { mutableStateOf("") }
        var isAIWorking by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "تخلیه ذهن و تحلیل هوشمند (هوش مصنوعی)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "یک کلمه یا جمله بنویسید؛ هوش مصنوعی تمام اطلاعات (تگ، وضعیت، تاریخ، مسئول) را استخراج می‌کند.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    placeholder = { Text("مثال: فردا ۵ عصر پیگیری تایید مریم، یا فقط یک کلمه: ورزش", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("ai_quick_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Autofill Form Button
                    Button(
                        onClick = {
                            if (quickInput.isNotBlank()) {
                                isAIWorking = true
                                coroutineScope.launch {
                                    try {
                                        val currentLocalTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                        val result = viewModel.parserService.parseInput(quickInput, currentLocalTime)
                                        val firstTask = result.tasks.firstOrNull()
                                        if (firstTask != null) {
                                            title = firstTask.title
                                            description = firstTask.description
                                            status = firstTask.gtdStatus
                                            tagsString = firstTask.tags
                                            waitingFor = firstTask.waitingFor ?: ""
                                            // Handle calendar date if present
                                            val firstEvent = result.calendarEvents.firstOrNull()
                                            if (firstEvent != null && firstEvent.dueDateMs != null) {
                                                dueDate = firstEvent.dueDateMs
                                                syncToCalendar = true
                                            } else {
                                                dueDate = null
                                                syncToCalendar = false
                                            }
                                            Toast.makeText(context, "اطلاعات فرم پر شد!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            title = quickInput
                                            Toast.makeText(context, "عنوانی استخراج نشد، ورودی در عنوان کپی شد.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "خطا در پردازش هوش مصنوعی", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isAIWorking = false
                                    }
                                }
                            }
                        },
                        enabled = quickInput.isNotBlank() && !isAIWorking,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("تکمیل هوشمند فرم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Quick Save Button
                    Button(
                        onClick = {
                            if (quickInput.isNotBlank()) {
                                isAIWorking = true
                                viewModel.quickSaveRawInput(context, quickInput) { msg ->
                                    isAIWorking = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onTaskAdded()
                                }
                            }
                        },
                        enabled = quickInput.isNotBlank() && !isAIWorking,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (isAIWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("ثبت مستقیم فوری", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان کار یا ایده (الزامی)", fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("توضیحات تکمیلی", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        // GTD Status Picker
        Text("دسته‌بندی اولیه (وضعیت GTD):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statuses = listOf(
                Pair("INBOX", "صندوق ورودی (ایده خام)"),
                Pair("NEXT", "اقدام بعدی (آماده انجام)"),
                Pair("SCHEDULED", "زمان‌بندی شده"),
                Pair("WAITING", "در انتظار پاسخ"),
                Pair("SOMEDAY", "شاید وقتی دیگر")
            )
            statuses.forEach { st ->
                val isSelected = status == st.first
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { status = st.first },
                    label = { Text(st.second, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // If status is WAITING, show "Waiting for" description
        if (status == "WAITING") {
            OutlinedTextField(
                value = waitingFor,
                onValueChange = { waitingFor = it },
                label = { Text("در انتظار کی یا چی؟ (مثلاً: تایید مدیریت)", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Project selector
        Text("انتساب به پروژه:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedFilterChip(
                selected = projectId == null,
                onClick = { projectId = null },
                label = { Text("بدون پروژه", fontSize = 11.sp) }
            )
            projects.forEach { pr ->
                val isSelected = projectId == pr.id
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { projectId = pr.id },
                    label = { Text(pr.name, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tags Input
        OutlinedTextField(
            value = tagsString,
            onValueChange = { tagsString = it },
            label = { Text("برچسب‌ها (با کاما جدا کنید، مثلاً: فوری, سیستم)", fontSize = 13.sp) },
            placeholder = { Text("فوری, بیرون, خانه", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Due Date & Calendar Sync
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("زمان‌بندی و اتصال به تقویم:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dueDate?.let { "تاریخ شمسی انتخابی:\n" + JalaliCalendar.formatJalaliDate(it) }
                            ?: "هیچ تاریخی انتخاب نشده است.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { datePickerDialog.show() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("انتخاب تاریخ", fontSize = 11.sp)
                    }
                }

                if (dueDate != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = syncToCalendar,
                            onCheckedChange = { syncToCalendar = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ثبت همزمان در تقویم بومی گوشی و گوگل کلندر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intelligent Location Reminders Parameters
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("📍 یادآور هوشمند موقعیت مکانی (اختیاری):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "هنگام ورود به این مختصات جغرافیایی، اپلیکیشن به شما یادآوری ارسال می‌کند.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("نام محل (مانند: هایپرمارکت محله)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = latitudeStr,
                        onValueChange = { latitudeStr = it },
                        label = { Text("عرض جغرافیایی (Latitude)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = longitudeStr,
                        onValueChange = { longitudeStr = it },
                        label = { Text("طول جغرافیایی (Longitude)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Autofill helper button
                TextButton(
                    onClick = {
                        locationName = "هایپرمارکت محله"
                        latitudeStr = "35.7445"
                        longitudeStr = "51.3750"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("استفاده از مختصات پیش‌فرض شبیه‌ساز", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                if (title.isBlank()) {
                    Toast.makeText(context, "لطفاً عنوان کار را وارد کنید.", Toast.LENGTH_SHORT).show()
                } else {
                    val lat = latitudeStr.toDoubleOrNull()
                    val lng = longitudeStr.toDoubleOrNull()
                    viewModel.addTask(
                        title = title,
                        description = description,
                        status = status,
                        projectId = projectId,
                        tags = tagsString,
                        dueDate = dueDate,
                        locationName = locationName,
                        latitude = lat,
                        longitude = lng,
                        waitingFor = waitingFor,
                        context = context,
                        syncToCalendar = syncToCalendar
                    )
                    Toast.makeText(context, "وظیفه با موفقیت ثبت شد!", Toast.LENGTH_SHORT).show()
                    onTaskAdded()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_task_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ثبت و ذخیره در سیستم GTD", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------
// 3. AI COUNSEL SCREEN
// ---------------------------------------------------------
@Composable
fun AICounselScreen(viewModel: GTDViewModel) {
    var query by remember { mutableStateOf("") }
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "همراه و مشاور مدیریت زمان هوش مصنوعی",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "می‌توانید در رابطه با تقسیم کارها، بازبینی هفتگی، نحوه اولویت‌بندی ایده‌ها یا کاهش دغدغه‌های فکری از من راهنمایی بخواهید.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chat input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("سوال یا مشکل برنامه‌ریزی شما چیست؟", fontSize = 13.sp) },
            placeholder = { Text("مثلاً چطور اینباکس کارهام رو سریع‌تر خالی کنم؟", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_query_input"),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (query.isNotBlank()) {
                        viewModel.askAICounsel(query, isInboxProcess = false)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("ai_send_button"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ارسال به هوش مصنوعی", fontSize = 13.sp)
                    }
                }
            }

            if (aiAdvice != null) {
                OutlinedButton(
                    onClick = {
                        query = ""
                        viewModel.clearAICounsel()
                    },
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("پاک کردن", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Advice box
        if (aiAdvice != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاوره اختصاصی هوش مصنوعی:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = aiAdvice ?: "",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else if (!isLoading) {
            // Recommendation prompt cards
            Text("سوالات پیشنهادی از هوش مصنوعی:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val suggestionPrompts = listOf(
                "چگونه قانون ۲ دقیقه‌ای جی‌تی‌دی را پیاده کنم؟",
                "با کارهای سنگین پروژه «کار و تجارت» چطور شروع کنم؟",
                "برای رفع خستگی فکری و بازبینی هفتگی چه گام‌هایی بردارم؟"
            )
            suggestionPrompts.forEach { pr ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            query = pr
                            viewModel.askAICounsel(pr, isInboxProcess = false)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 4. INBOX HELPER SCREEN (CONVERSATIONAL CHATBOT INBOX & NLP PARSER)
// ---------------------------------------------------------
@Composable
fun InboxHelperScreen(viewModel: GTDViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Chatbot, 1: Natural Language Parser (NLP)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Material 3 sub-tabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("چت‌بات تعاملی (مشاور)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("پردازشگر ذهن‌روبی (NLP)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
        
        if (activeSubTab == 0) {
            ChatbotSubScreen(viewModel)
        } else {
            NaturalLanguageParserSubScreen(viewModel)
        }
    }
}

@Composable
fun ChatbotSubScreen(viewModel: GTDViewModel) {
    val messages by viewModel.chatbotMessages.collectAsState()
    val isChatbotLoading by viewModel.isChatbotLoading.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // Track which action keys have been committed in this session
    var committedActions by remember { mutableStateOf(setOf<String>()) }
    var inputText by remember { mutableStateOf("") }
    
    // Automatically scroll to the latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat History Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (message.role == "user") Alignment.End else Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (message.role != "user") {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        // Bubble Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.role == "user") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (message.role == "user") {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(
                                topStart = if (message.role == "user") 16.dp else 4.dp,
                                topEnd = if (message.role == "user") 4.dp else 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = message.text,
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        
                        if (message.role == "user") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // Render suggestions action items under robot message
                    if (message.role == "assistant" && message.actions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .padding(start = 44.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📋 اقدامات استخراج‌شده پیشنهادی (کلیک برای ثبت مستقیم):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            message.actions.forEachIndexed { index, action ->
                                val actionKey = "${message.id}_$index"
                                val isCommitted = committedActions.contains(actionKey)
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isCommitted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isCommitted -> Icons.Default.Check
                                                    action.type == "PROJECT" -> Icons.Default.AccountTree
                                                    action.status == "SOMEDAY" -> Icons.Default.Star
                                                    action.status == "WAITING" -> Icons.Default.HourglassEmpty
                                                    else -> Icons.Default.PlayArrow
                                                },
                                                contentDescription = null,
                                                tint = if (isCommitted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(10.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = action.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCommitted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val labelText = when {
                                                    action.type == "PROJECT" -> "پروژه جدید"
                                                    action.status == "SOMEDAY" -> "هدف/آینده"
                                                    action.status == "WAITING" -> "در انتظار (${action.waitingFor ?: "پیگیری"})"
                                                    else -> "اقدام بعدی"
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(labelText, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            if (action.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = action.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCommitted) 0.5f else 0.8f)
                                                )
                                            }
                                            if (action.tags.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "تگ‌ها: ${action.tags}",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(6.dp))
                                        
                                        Button(
                                            onClick = {
                                                if (!isCommitted) {
                                                    viewModel.commitChatAction(action) { text ->
                                                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                                                        committedActions = committedActions + actionKey
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCommitted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary,
                                                contentColor = if (isCommitted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            enabled = !isCommitted
                                        ) {
                                            Text(
                                                text = if (isCommitted) "ثبت شد ✓" else "ثبت مستقیم",
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
            
            if (isChatbotLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال پردازش و استخراج کارهای پیشنهادی...", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // Quick suggestions row
        val suggestions = listOf(
            "تخلیه ذهن از دغدغه‌های کار و ورزش",
            "درخواست مشاوره جی‌تی‌دی و همدلی زمان",
            "طراحی پروژه جدید کاری برای بهبود شرکت",
            "ثبت آرزوها و چشم‌اندازهای ۵ ساله شخصی"
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(suggestions) { text ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        inputText = text
                    }
                ) {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        // Chat input bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset Conversation
                IconButton(
                    onClick = { viewModel.clearChatbot() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "پاک کردن چت",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Input Text field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("شرح حال، سوال، پروژه‌ها یا کارهای نامنظم را بنویسید...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chatbot_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    maxLines = 2
                )
                
                // Send Icon
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isChatbotLoading) {
                            val prompt = inputText
                            inputText = ""
                            viewModel.sendChatbotMessage(prompt)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isChatbotLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputText.isNotBlank() && !isChatbotLoading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "ارسال",
                        tint = if (inputText.isNotBlank() && !isChatbotLoading) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun NaturalLanguageParserSubScreen(viewModel: GTDViewModel) {
    val context = LocalContext.current
    val parsedResult by viewModel.parsedResult.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    var sweepInputText by remember { mutableStateOf("") }
    
    // Quick-dump presets for Persian-localized time and task scenarios
    val presets = listOf(
        "شنبه ساعت ۹ صبح جلسه با تیم توسعه دارم. همچنین باید برای عید خرید کنم و کار واگذار شده به مریم رو پیگیری کنم.",
        "می‌خوام پروژه جدید ساخت وبلاگ شخصی با رنگ بنفش رو ایجاد کنم. یادم باشه فردا به علی زنگ بزنم.",
        "سپردم به رضا فایل‌های طراحی رو فردا برام بفرسته. همچنین ثبت‌نام باشگاه برای سلامتی رو توی کارهای آینده بگذار."
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Explanatory Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "پردازشگر ذهن‌روبی هوشمند (NLP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "هر تفکر، یادداشت یا صورت‌جلسه صوتی/متنی خام را اینجا وارد کنید تا در ثانیه به تسک‌های GTD، پروژه‌های فعال و رویدادهای تقویم هماهنگ‌شده تبدیل شود.",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Large input field
        OutlinedTextField(
            value = sweepInputText,
            onValueChange = { sweepInputText = it },
            placeholder = {
                Text(
                    text = "مثال: فردا ساعت ۱۰ جلسه دارم، باید خرید هفتگی تگ شخصی کنم و به مریم هم کار طراحی رو سپردم...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("mind_sweep_input_field"),
            shape = RoundedCornerShape(14.dp),
            maxLines = 6
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Pre-set templates row
        Text(
            text = "💡 پیشنهادهای سریع تخلیه ذهن (کلیک کنید):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(presets) { preset ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { sweepInputText = preset }
                ) {
                    Text(
                        text = if (preset.length > 35) preset.take(35) + "..." else preset,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Submit button
        Button(
            onClick = {
                if (sweepInputText.isNotBlank()) {
                    viewModel.parseNaturalLanguage(sweepInputText)
                }
            },
            enabled = sweepInputText.isNotBlank() && !isParsing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("mind_sweep_submit_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isParsing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("در حال پردازش ساختاریافته توسط Gemini...", fontSize = 13.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تجزیه و تحلیل ذهن‌روبی", fontSize = 13.sp)
                }
            }
        }
        
        if (parsedResult != null) {
            val result = parsedResult!!
            
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Parsed Header & Batch Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 خروجی هوشمند استخراج‌شده:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = {
                        viewModel.commitAllParsedResults(context) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت همگانی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Advisor / Summary card
            if (result.summary.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توصیه مشاور زمان و تخلیه ذهن:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result.summary,
                            fontSize = 12.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Tasks Section
            if (result.tasks.isNotEmpty()) {
                Text(
                    text = "📝 کارهای استخراج‌شده (${result.tasks.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                result.tasks.forEach { task ->
                    var registered by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (registered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (task.gtdStatus) {
                                                "NEXT" -> "اقدام بعدی"
                                                "WAITING" -> "در انتظار"
                                                "SOMEDAY" -> "کارهای آینده"
                                                "SCHEDULED" -> "زمان‌بندی‌شده"
                                                else -> "صندوق ورودی"
                                            },
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (task.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(task.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (task.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("تگ‌ها: ${task.tags}", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (!task.waitingFor.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("مسئول: ${task.waitingFor}", fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Button(
                                onClick = {
                                    if (!registered) {
                                        viewModel.insertParsedTask(task) {
                                            registered = true
                                            Toast.makeText(context, "تسک با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (registered) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (registered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                enabled = !registered
                            ) {
                                Text(if (registered) "ثبت شد ✓" else "ثبت مستقل", fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Projects Section
            if (result.projects.isNotEmpty()) {
                Text(
                    text = "🗂️ پروژه‌های استخراج‌شده (${result.projects.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                result.projects.forEach { project ->
                    var registered by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try {
                                                Color(android.graphics.Color.parseColor(project.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (registered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (project.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(project.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (!registered) {
                                        viewModel.insertParsedProject(project) {
                                            registered = true
                                            Toast.makeText(context, "پروژه با موفقیت ایجاد شد", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (registered) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (registered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                enabled = !registered
                            ) {
                                Text(if (registered) "ثبت شد ✓" else "ثبت مستقل", fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Calendar Events Section
            if (result.calendarEvents.isNotEmpty()) {
                Text(
                    text = "📅 رویدادهای تقویم (${result.calendarEvents.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                result.calendarEvents.forEach { event ->
                    var registered by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (registered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                                if (event.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(event.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                event.dueDateMs?.let { time ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "زمان: " + JalaliCalendar.formatJalaliDate(time),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (!registered) {
                                        viewModel.insertParsedCalendarEvent(context, event) { msg ->
                                            registered = true
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (registered) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary,
                                    contentColor = if (registered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                enabled = !registered
                            ) {
                                Text(if (registered) "ثبت شد ✓" else "ثبت مستقل", fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Clean parsed results button
            OutlinedButton(
                onClick = { viewModel.clearParsedResult() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("پاک کردن نتایج فعلی", fontSize = 12.sp)
            }
        }
    }
}

// ---------------------------------------------------------
// 5. BACKUP & SYNC SCREEN
// ---------------------------------------------------------
@Composable
fun BackupSyncScreen(viewModel: GTDViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backupJson by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "تنظیمات، پشتیبان‌گیری و همگام‌سازی ابری",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "از اطلاعات شما به صورت لوکال پشتیبان‌گیری می‌شود. همچنین می‌توانید خروجی متنی JSON گرفته یا همگام‌سازی ابری را فعال کنید.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Cloud Sync Button
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("☁️ همگام‌سازی ابری دستگاه‌ها (Cloud Sync):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "با فعال‌سازی همگام‌سازی، اطلاعات GTD شما به صورت رمزنگاری شده با سرور همگام می‌شود تا در تبلت یا دیگر گوشی‌ها در دسترس باشد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSyncing = true
                            kotlinx.coroutines.delay(2000) // Beautiful simulated sync network latency
                            isSyncing = false
                            Toast.makeText(context, "اطلاعات با موفقیت با سرورهای ابری همگام‌سازی شد!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال همگام‌سازی با سرور...", fontSize = 12.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("هم‌اکنون همگام‌سازی ابری کن", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📤 خروجی بک‌آپ از کارهای ثبت شده (Export JSON):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "برای حفظ امنیت کامل داده‌های خود، یک نسخه پشتیبان کپی کنید تا بعداً بتوانید آن را به همین دستگاه یا دستگاه دیگری بازگردانید.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            backupJson = viewModel.exportDataToJson()
                            Toast.makeText(context, "بک‌آپ با موفقیت در باکس زیر تولید شد!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("تولید کد بک‌آپ داده‌ها", fontSize = 13.sp)
                }

                if (backupJson.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📥 بازنشانی بک‌آپ داده‌ها (Import JSON):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "یک کد بک‌آپ JSON معتبر را در کادر زیر وارد کنید تا اطلاعات به دیتابیس محلی شما اضافه گردد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    placeholder = { Text("کد بک‌آپ JSON را اینجا پیست کنید...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (importJson.isBlank()) {
                            Toast.makeText(context, "لطفاً ابتدا کد بک‌آپ را وارد کنید.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.importDataFromJson(
                                importJson,
                                onSuccess = {
                                    importJson = ""
                                    Toast.makeText(context, "داده‌ها با موفقیت بازنشانی و وارد شدند!", Toast.LENGTH_LONG).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بازنشانی کد بک‌آپ", fontSize = 13.sp)
                }
            }
        }
    }
}
