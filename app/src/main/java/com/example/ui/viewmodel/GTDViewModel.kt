package com.example.ui.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiManager
import com.example.data.api.GTDParserService
import com.example.data.api.ParsedGTDResult
import com.example.data.api.ParsedTask
import com.example.data.api.ParsedProject
import com.example.data.api.ParsedCalendarEvent
import com.example.data.model.GTDProject
import com.example.data.model.GTDTag
import com.example.data.model.GTDTask
import com.example.data.repository.GTDRepository
import com.example.util.addTaskToDeviceCalendar
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GTDViewModel(private val repository: GTDRepository) : ViewModel() {

    private val geminiManager = GeminiManager()

    // Database Flows
    val allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allActiveTasks = repository.allActiveTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags = repository.allTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedProjectFilter = MutableStateFlow<Int?>(null)
    val selectedProjectFilter = _selectedProjectFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter = _selectedTagFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("INBOX") // INBOX, NEXT, WAITING, SOMEDAY, SCHEDULED
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    // Filtered tasks
    val filteredTasks = combine(
        allTasks,
        _searchQuery,
        _selectedProjectFilter,
        _selectedTagFilter,
        _selectedStatusFilter
    ) { tasks, query, project, tag, status ->
        tasks.filter { task ->
            val matchesStatus = if (status == "ALL") true else task.gtdStatus == status
            val matchesProject = if (project == null) true else task.projectId == project
            val matchesTag = if (tag == null) true else task.tags.split(",").map { it.trim() }.contains(tag)
            val matchesQuery = if (query.isEmpty()) true else {
                task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)
            }
            matchesStatus && matchesProject && matchesTag && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated GPS Location State
    // Tehran Milad Tower default coordinates as a default
    private val _currentLatitude = MutableStateFlow(35.7448)
    val currentLatitude = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(51.3753)
    val currentLongitude = _currentLongitude.asStateFlow()

    // Triggered location alert if we get near a task
    private val _activeLocationAlertTask = MutableStateFlow<GTDTask?>(null)
    val activeLocationAlertTask = _activeLocationAlertTask.asStateFlow()

    // AI Advice State
    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // Conversational Chatbot Inbox States
    private val _chatbotMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            role = "assistant",
            text = "سلام! به اینباکس هوشمند و چت‌بات جی‌تی‌دی خوش آمدید. 🧠✨\n\nمن مشاور و پردازشگر هوشمند شما هستم. هر چه در ذهن دارید (شامل شرح حال شخصی، سوالات متداول، لیست خریدهای نامرتب، اهداف، آرزوها، دغدغه‌ها، کارهای محول شده یا حتی احساسات و نیاز به همدلی و مشاوره) را با کلمات ساده برای من بنویسید.\n\nمن پیام شما را خلاصه، دسته‌بندی و شفاف‌سازی کرده و به شما پیشنهاد ایجاد پروژه‌ها، عادات یا تسک‌ها با بهترین جمله‌بندی را می‌دهم و دکمه‌های ثبت مستقیم را برایتان فراهم می‌کنم."
        )
    ))
    val chatbotMessages = _chatbotMessages.asStateFlow()

    private val _isChatbotLoading = MutableStateFlow(false)
    val isChatbotLoading = _isChatbotLoading.asStateFlow()

    // Prepopulate some starting mock GTD data if database is empty
    init {
        viewModelScope.launch {
            allProjects.first { true } // wait for first emission
            val projects = repository.allProjects.first()
            if (projects.isEmpty()) {
                // Add default projects
                repository.insertProject(GTDProject(name = "کار و تجارت", description = "فعالیت‌های کاری، ددلاین‌ها و امور بیزنسی", colorHex = "#EF5350"))
                repository.insertProject(GTDProject(name = "زندگی شخصی", description = "خرید خانه، ورزش و اهداف فردی", colorHex = "#66BB6A"))
                repository.insertProject(GTDProject(name = "یادگیری و آموزش", description = "کتاب‌ها، مهارت‌ها و دوره‌ها", colorHex = "#29B6F6"))

                // Add default tags
                repository.insertTag(GTDTag(name = "فوری", colorHex = "#E53935"))
                repository.insertTag(GTDTag(name = "سیستم", colorHex = "#1E88E5"))
                repository.insertTag(GTDTag(name = "تلفنی", colorHex = "#43A047"))
                repository.insertTag(GTDTag(name = "خریدنی", colorHex = "#FDD835"))

                // Add sample Inbox task
                repository.insertTask(GTDTask(
                    title = "خرید اشتراک برنامه ریزی شخصی جی‌تی‌دی",
                    description = "باید بررسی کنم چه پلن‌هایی وجود داره و آیا خروجی فایل به درستی کار می‌کنه یا خیر.",
                    gtdStatus = "INBOX"
                ))

                // Add sample Next Action with location
                repository.insertTask(GTDTask(
                    title = "خرید اقلام هفتگی خانه",
                    description = "سیب، نان سنگک و ماست برومند از مغازه",
                    gtdStatus = "NEXT",
                    tags = "خریدنی",
                    locationName = "هایپرمارکت محله",
                    latitude = 35.7445,
                    longitude = 51.3750 // extremely close to Milad Tower coordinate (35.7448, 51.3753)
                ))
            }
        }
    }

    // Filter Controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setProjectFilter(projectId: Int?) {
        _selectedProjectFilter.value = projectId
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    // Tasks CRUD
    fun addTask(
        title: String,
        description: String,
        status: String,
        projectId: Int?,
        tags: String,
        dueDate: Long?,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        waitingFor: String?,
        context: Context?,
        syncToCalendar: Boolean
    ) {
        viewModelScope.launch {
            var calendarEventId: String? = null
            if (syncToCalendar && dueDate != null && context != null) {
                val eventId = withContext(Dispatchers.IO) {
                    addTaskToDeviceCalendar(
                        context,
                        title = title,
                        description = description,
                        dueDateMs = dueDate
                    )
                }
                if (eventId != null) {
                    calendarEventId = eventId.toString()
                }
            }

            val task = GTDTask(
                title = title,
                description = description,
                gtdStatus = status,
                projectId = projectId,
                tags = tags,
                dueDate = dueDate,
                locationName = if (locationName.isNullOrBlank()) null else locationName,
                latitude = latitude,
                longitude = longitude,
                waitingFor = if (waitingFor.isNullOrBlank()) null else waitingFor,
                googleCalendarEventId = calendarEventId
            )
            repository.insertTask(task)
            
            // Re-evaluate proximity alert for newly added tasks
            checkProximityToTasks()
        }
    }

    fun updateTaskStatus(task: GTDTask, newStatus: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(gtdStatus = newStatus))
        }
    }

    fun toggleTaskCompletion(task: GTDTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: GTDTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Projects CRUD
    fun addProject(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertProject(GTDProject(name = name, description = description, colorHex = colorHex))
        }
    }

    // Tags CRUD
    fun addTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertTag(GTDTag(name = name, colorHex = colorHex))
        }
    }

    // GPS Location Simulation
    fun updateSimulatedLocation(lat: Double, lng: Double) {
        _currentLatitude.value = lat
        _currentLongitude.value = lng
        checkProximityToTasks()
    }

    private fun checkProximityToTasks() {
        val tasks = allActiveTasks.value
        val lat = _currentLatitude.value
        val lng = _currentLongitude.value

        val closeTask = tasks.firstOrNull { task ->
            if (task.latitude != null && task.longitude != null) {
                val results = FloatArray(1)
                Location.distanceBetween(lat, lng, task.latitude, task.longitude, results)
                val distanceInMeters = results[0]
                distanceInMeters < 500 // Within 500 meters
            } else {
                false
            }
        }
        _activeLocationAlertTask.value = closeTask
    }

    fun dismissLocationAlert() {
        _activeLocationAlertTask.value = null
    }

    // AI Counseling Features
    fun askAICounsel(prompt: String, isInboxProcess: Boolean = false) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAdvice.value = null

            val systemInstruction = if (isInboxProcess) {
                "شما یک مشاور حرفه‌ای مدیریت زمان با استفاده از متدولوژی GTD (برنامه‌ریزی متمرکز بر تخلیه ذهن) هستید. هدف شما کمک به کاربر برای گرفتن سریع‌ترین تصمیم‌ها در رابطه با کارهای موقت صندوق ورودی (Inbox) است. به زبان فارسی شیوا پاسخ دهید."
            } else {
                "شما یک دستیار هوشمند و مشاور برنامه‌ریزی شخصی با متدولوژی GTD (Getting Things Done) هستید. به کاربر کمک کنید زمان خود را مدیریت کند، کارهایش را اولویت‌بندی کند و مسیر بهینه‌ای برای اهداف هفتگی خود به زبان فارسی روان و دلگرم‌کننده ترسیم کند."
            }

            // Enrich the prompt with the current task lists context to give an extremely tailored AI feedback!
            val tasksContext = allActiveTasks.value.joinToString("\n") { task ->
                "- ${task.title} (وضعیت: ${task.gtdStatus}, تگ‌ها: ${task.tags})"
            }

            val fullPrompt = """
                لیست کارهای فعال کاربر جهت هم‌بندی و درک بهتر:
                $tasksContext
                
                سوال/درخواست کاربر:
                $prompt
            """.trimIndent()

            val response = geminiManager.getAICounsel(fullPrompt, systemInstruction)
            _aiAdvice.value = response
            _isAiLoading.value = false
        }
    }

    fun clearAICounsel() {
        _aiAdvice.value = null
    }

    // Conversational Chatbot Inbox Methods
    fun sendChatbotMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(role = "user", text = text)
        _chatbotMessages.value = _chatbotMessages.value + userMsg
        
        viewModelScope.launch {
            _isChatbotLoading.value = true
            
            val systemInstruction = """
                شما یک دستیار هوشمند، دلسوز و مشاور متخصص مدیریت زمان به روش GTD (برنامه‌ریزی و تخلیه ذهن) هستید. 
                کاربر هر گونه متنی شامل شرح حال، ایده، لیست کارهای نامرتب، پروژه، قرار ملاقات، اهداف، آرزوها، دغدغه‌های ذهنی، درخواست مشاوره یا نیاز به همدلی را به شما می‌دهد.

                وظایف شما:
                ۱. تحلیل عمیق و ارائه یک خلاصه و جمع‌بندی بسیار دلسوزانه و حرفه‌ای از نیازها و دغدغه‌های ذهنی کاربر (شرح حال، اهداف، آرزوها، دغدغه‌ها، افراد و...).
                ۲. بازنویسی، اصلاح ادبی، ارتقای جمله بندی و انتخاب کلمات بهینه‌تر برای کارهای پراکنده کاربر تا انجام آن‌ها آسان‌تر و انگیزاننده‌تر شود.
                ۳. پیشنهاد کارهای مشخص، پروژه‌ها، عادات جدید یا کارهای آینده (Someday/Maybe) به صورت ساختاریافته.

                مهم: شما باید پیشنهادات ساختاریافته خود را در انتهای پاسخ با فرمت ویژه زیر مشخص کنید تا اپلیکیشن بتواند دکمه‌های اقدام مستقیم و افزودن خودکار برای کاربر ایجاد کند. حتماً مقادیر انگلیسی مربوط به مشخصات فنی را جلو تگ‌ها بنویسید.

                فرمت تگ‌های پیشنهادات به صورت دقیق:
                [RECOMMENDATION_START]
                Type: TASK
                Title: نام بازنویسی شده و بهینه کار به فارسی
                Description: توضیحات کار به فارسی
                Status: NEXT
                Tags: تگ‌ها (کلمات کلیدی مثل شخصی، کاری، خریدنی)
                [RECOMMENDATION_END]

                فرمت پروژه پیشنهادی:
                [RECOMMENDATION_START]
                Type: PROJECT
                Title: عنوان بهینه و انگیزاننده پروژه
                Description: شرح خروجی مطلوب پروژه
                [RECOMMENDATION_END]

                فرمت کار آینده یا آرزو/هدف:
                [RECOMMENDATION_START]
                Type: TASK
                Title: عنوان هدف یا کار آینده
                Description: توضیحات مربوطه
                Status: SOMEDAY
                Tags: اهداف
                [RECOMMENDATION_END]

                فرمت واگذاری به دیگری یا همکار:
                [RECOMMENDATION_START]
                Type: TASK
                Title: پیگیری کار محوله
                Description: کار محول شده به دیگری
                Status: WAITING
                WaitingFor: نام شخص مسئول
                [RECOMMENDATION_END]

                پاسخ کلی شما ابتدا باید شامل همدلی گرم، خلاصه جذاب و توصیه‌های دقیق فارسی باشد و سپس بخش‌های ساختاریافته فوق را بیاورید. به زبان فارسی بنویسید.
            """.trimIndent()

            val tasksContext = allActiveTasks.value.joinToString("\n") { task ->
                "- ${task.title} (وضعیت: ${task.gtdStatus}, تگ‌ها: ${task.tags})"
            }

            val fullPrompt = """
                لیست کارهای فعلی کاربر جهت هم‌بندی بهتر سیستم:
                $tasksContext
                
                پیام جدید کاربر جهت تحلیل و استخراج کارهای پیشنهادی و پردازش:
                $text
            """.trimIndent()

            var responseText = ""
            try {
                responseText = geminiManager.getAICounsel(fullPrompt, systemInstruction)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Ensure we have a valid response, otherwise use fallback
            if (responseText.isBlank() || responseText.contains("خطا در برقراری ارتباط") || responseText.contains("حالت دمو فعال است")) {
                val builder = java.lang.StringBuilder()
                builder.append("**🧠 پردازشگر هوشمند جی‌تی‌دی (حالت آفلاین):**\n\n")
                builder.append("درخواست یا شرح حال شما را بررسی کردم. برای هدایت بهتر و خلوت شدن ذهن، خلاصه و گام‌های عملی زیر پیشنهاد می‌شود:\n\n")
                builder.append("**📌 خلاصه خواسته‌ها و جمع‌بندی نیازهای شما:**\n")
                builder.append("شما به دنبال سازماندهی کارها، مکتوب کردن ایده‌های خام و دریافت راهکار برای اقدامات بعدی هستید. این کار به وضوح ذهنی شما کمک شایانی خواهد کرد.\n\n")
                builder.append("**🎯 پیشنهادات سازماندهی مستقیم:**\n")
                builder.append("جمله‌بندی‌های کارهای پراکنده شما اصلاح شده است تا انجام آن‌ها آسان‌تر شود. از دکمه‌های زیر برای ثبت مستقیم استفاده کنید:\n\n")
                
                val fallbackActions = generateDynamicFallbackActions(text)
                fallbackActions.forEach { action ->
                    builder.append("[RECOMMENDATION_START]\n")
                    builder.append("Type: ${action.type}\n")
                    builder.append("Title: ${action.title}\n")
                    builder.append("Description: ${action.description}\n")
                    builder.append("Status: ${action.status}\n")
                    builder.append("Tags: ${action.tags}\n")
                    if (action.waitingFor != null) {
                        builder.append("WaitingFor: ${action.waitingFor}\n")
                    }
                    builder.append("[RECOMMENDATION_END]\n")
                }
                responseText = builder.toString()
            }

            // If response doesn't contain tags, append them dynamically to guarantee buttons
            if (!responseText.contains("[RECOMMENDATION_START]")) {
                val extraActions = generateDynamicFallbackActions(text)
                val builder = java.lang.StringBuilder(responseText)
                builder.append("\n\n**📌 اقدامات استخراج‌شده پیشنهادی:**\n")
                extraActions.forEach { action ->
                    builder.append("[RECOMMENDATION_START]\n")
                    builder.append("Type: ${action.type}\n")
                    builder.append("Title: ${action.title}\n")
                    builder.append("Description: ${action.description}\n")
                    builder.append("Status: ${action.status}\n")
                    builder.append("Tags: ${action.tags}\n")
                    if (action.waitingFor != null) {
                        builder.append("WaitingFor: ${action.waitingFor}\n")
                    }
                    builder.append("[RECOMMENDATION_END]\n")
                }
                responseText = builder.toString()
            }

            // Parse out recommendations
            val parsed = parseRecommendations(responseText)
            val assistantMsg = ChatMessage(
                role = "assistant",
                text = parsed.first,
                actions = parsed.second
            )
            _chatbotMessages.value = _chatbotMessages.value + assistantMsg
            _isChatbotLoading.value = false
        }
    }

    fun clearChatbot() {
        _chatbotMessages.value = listOf(
            ChatMessage(
                role = "assistant",
                text = "صندوق ورودی چت‌بات بازنشانی شد. چه چیزی در ذهن دارید؟ 🧠"
            )
        )
    }

    fun commitChatAction(action: ChatAction, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (action.type == "PROJECT") {
                    repository.insertProject(GTDProject(
                        name = action.title,
                        description = action.description,
                        colorHex = "#29B6F6"
                    ))
                    onComplete("پروژه «${action.title}» با موفقیت ساخته شد!")
                } else {
                    val task = GTDTask(
                        title = action.title,
                        description = action.description,
                        gtdStatus = action.status,
                        tags = action.tags,
                        waitingFor = action.waitingFor
                    )
                    repository.insertTask(task)
                    val section = when (action.status) {
                        "NEXT" -> "اقدامات بعدی"
                        "SOMEDAY" -> "کارهای آینده"
                        "WAITING" -> "کارهای واگذار شده (در انتظار)"
                        "SCHEDULED" -> "کارهای زمان‌بندی شده"
                        else -> "صندوق ورودی"
                    }
                    onComplete("کار «${action.title}» به لیست $section افزوده شد!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete("خطا در ثبت اطلاعات: ${e.localizedMessage}")
            }
        }
    }

    private fun parseRecommendations(responseText: String): Pair<String, List<ChatAction>> {
        var mainText = responseText
        val actions = mutableListOf<ChatAction>()
        
        val recommendationRegex = Regex("\\[RECOMMENDATION_START\\]([\\s\\S]*?)\\[RECOMMENDATION_END\\]")
        val matches = recommendationRegex.findAll(responseText)
        
        for (match in matches) {
            val block = match.groups[1]?.value ?: ""
            var type = "TASK"
            var title = ""
            var description = ""
            var status = "NEXT"
            var tags = ""
            var projectName: String? = null
            var waitingFor: String? = null
            
            block.split("\n").forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    when (key) {
                        "type" -> type = value.uppercase()
                        "title", "name" -> title = value
                        "description" -> description = value
                        "status" -> status = value.uppercase()
                        "tags" -> tags = value
                        "projectname" -> projectName = value
                        "waitingfor", "delegate" -> waitingFor = value
                    }
                }
            }
            
            if (title.isNotEmpty()) {
                actions.add(ChatAction(
                    type = type,
                    title = title,
                    description = description,
                    status = status,
                    tags = tags,
                    projectName = projectName,
                    waitingFor = waitingFor
                ))
            }
        }
        
        mainText = mainText.replace(recommendationRegex, "")
        mainText = mainText.replace("[RECOMMENDATION_START]", "").replace("[RECOMMENDATION_END]", "")
        
        return Pair(mainText.trim(), actions)
    }

    private fun generateDynamicFallbackActions(prompt: String): List<ChatAction> {
        val actions = mutableListOf<ChatAction>()
        val p = prompt.lowercase()
        if (p.contains("ورزش") || p.contains("تمرین") || p.contains("سلامت") || p.contains("بدن")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "شروع نرمش روزانه سبک",
                description = "ورزش صبحگاهی به مدت ۱۵ دقیقه جهت بالابردن سطح انرژی",
                status = "NEXT",
                tags = "سلامتی، شخصی"
            ))
        }
        if (p.contains("خرید") || p.contains("تهیه")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "تهیه لیست خرید هفتگی منزل",
                description = "بررسی کمد آشپزخانه و نوشتن مایحتاج",
                status = "NEXT",
                tags = "خریدنی"
            ))
        }
        if (p.contains("کار") || p.contains("پروژه") || p.contains("شغل") || p.contains("شرکت")) {
            actions.add(ChatAction(
                type = "PROJECT",
                title = "پروژه بهبود برنامه‌ریزی کاری جدید",
                description = "دسته‌بندی ددلاین‌ها و امور مربوطه"
            ))
            actions.add(ChatAction(
                type = "TASK",
                title = "بررسی و شفاف‌سازی تسک‌های باز کاری",
                description = "زمان‌بندی گام‌های فیزیکی بعدی برای شروع کار",
                status = "NEXT",
                tags = "کاری"
            ))
        }
        if (p.contains("کتاب") || p.contains("مطالعه") || p.contains("یادگیری") || p.contains("آموزش")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "مطالعه کتاب مدیریت زمان جی‌تی‌دی",
                description = "روزانه حداقل ۱۰ صفحه برای بهبود مهارت برنامه‌ریزی",
                status = "NEXT",
                tags = "یادگیری"
            ))
        }
        if (p.contains("آرزو") || p.contains("علاقه") || p.contains("اهداف") || p.contains("هدف")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "نگارش اهداف بلندمدت و آرزوها",
                description = "تبدیل رویاهای شخصی به اهداف زمان‌دار و مکتوب",
                status = "SOMEDAY",
                tags = "اهداف"
            ))
        }
        if (p.contains("جلسه") || p.contains("ملاقات") || p.contains("قرار") || p.contains("دیدار")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "هماهنگی و تعیین دستور کار جلسه",
                description = "آماده‌سازی نکات کلیدی پیش از شروع جلسه",
                status = "NEXT",
                tags = "کاری"
            ))
        }
        if (p.contains("علی") || p.contains("رضا") || p.contains("محمد") || p.contains("مریم") || p.contains("زهرا") || p.contains("سپردن") || p.contains("واگذار")) {
            actions.add(ChatAction(
                type = "TASK",
                title = "پیگیری کار واگذار شده به همکار",
                description = "بررسی پیشرفت کار و هماهنگی نهایی",
                status = "WAITING",
                waitingFor = "پیگیری"
            ))
        }
        
        if (actions.isEmpty()) {
            actions.add(ChatAction(
                type = "TASK",
                title = "ثبت ایده جدید در صندوق ورودی",
                description = "پردازش و تبدیل به اقدام بعدی در روزهای آتی",
                status = "INBOX",
                tags = "ایده"
            ))
            actions.add(ChatAction(
                type = "TASK",
                title = "بازبینی هفتگی جی‌تی‌دی (Weekly Review)",
                description = "مرور کل کارهای فعال برای خلوت کردن کامل ذهن",
                status = "NEXT",
                tags = "سیستم"
            ))
        }
        return actions
    }

    // Natural Language Parser State
    val parserService = GTDParserService()
    
    private val _parsedResult = MutableStateFlow<ParsedGTDResult?>(null)
    val parsedResult = _parsedResult.asStateFlow()
    
    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    fun parseNaturalLanguage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isParsing.value = true
            try {
                val currentLocalTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val result = parserService.parseInput(text, currentLocalTime)
                _parsedResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isParsing.value = false
            }
        }
    }
    
    fun clearParsedResult() {
        _parsedResult.value = null
    }

    fun insertParsedTask(parsedTask: ParsedTask, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertTask(
                GTDTask(
                    title = parsedTask.title,
                    description = parsedTask.description,
                    gtdStatus = parsedTask.gtdStatus,
                    tags = parsedTask.tags,
                    waitingFor = parsedTask.waitingFor
                )
            )
            onComplete()
        }
    }

    fun insertParsedProject(parsedProject: ParsedProject, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertProject(
                GTDProject(
                    name = parsedProject.name,
                    description = parsedProject.description,
                    colorHex = parsedProject.colorHex
                )
            )
            onComplete()
        }
    }

    fun insertParsedCalendarEvent(context: Context, parsedEvent: ParsedCalendarEvent, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val dueDateMs = parsedEvent.dueDateMs ?: System.currentTimeMillis()
            var calendarEventId: String? = null
            
            val eventId = withContext(Dispatchers.IO) {
                addTaskToDeviceCalendar(
                    context,
                    title = parsedEvent.title,
                    description = parsedEvent.description,
                    dueDateMs = dueDateMs
                )
            }
            if (eventId != null) {
                calendarEventId = eventId.toString()
            }
            
            repository.insertTask(
                GTDTask(
                    title = parsedEvent.title,
                    description = parsedEvent.description,
                    gtdStatus = "SCHEDULED",
                    dueDate = dueDateMs,
                    googleCalendarEventId = calendarEventId,
                    tags = "تقویم"
                )
            )
            onComplete(if (calendarEventId != null) "با موفقیت به تقویم دستگاه و کارهای زمان‌بندی‌شده افزوده شد!" else "به کارهای زمان‌بندی‌شده افزوده شد!")
        }
    }

    fun quickSaveRawInput(context: Context, text: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentLocalTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val result = parserService.parseInput(text, currentLocalTime)
                val firstTask = result.tasks.firstOrNull()
                if (firstTask != null) {
                    var calId: String? = null
                    val firstEvent = result.calendarEvents.firstOrNull()
                    val calculatedDueDate = firstEvent?.dueDateMs
                    if (calculatedDueDate != null) {
                        val eventId = withContext(Dispatchers.IO) {
                            addTaskToDeviceCalendar(
                                context,
                                title = firstTask.title,
                                description = firstTask.description,
                                dueDateMs = calculatedDueDate
                            )
                        }
                        if (eventId != null) {
                            calId = eventId.toString()
                        }
                    }
                    
                    repository.insertTask(
                        GTDTask(
                            title = firstTask.title,
                            description = firstTask.description,
                            gtdStatus = firstTask.gtdStatus,
                            tags = firstTask.tags,
                            dueDate = calculatedDueDate,
                            waitingFor = firstTask.waitingFor,
                            googleCalendarEventId = calId
                        )
                    )
                    
                    var detailMsg = "ثبت شد: ${firstTask.title}"
                    if (firstTask.gtdStatus != "INBOX") {
                        val statusFa = when (firstTask.gtdStatus) {
                            "NEXT" -> "اقدام بعدی"
                            "WAITING" -> "در انتظار (${firstTask.waitingFor ?: ""})"
                            "SCHEDULED" -> "زمان‌بندی‌شده"
                            "SOMEDAY" -> "کارهای آینده"
                            else -> "صندوق ورودی"
                        }
                        detailMsg += " | وضعیت: $statusFa"
                    }
                    if (firstTask.tags.isNotBlank()) {
                        detailMsg += " | برچسب: ${firstTask.tags}"
                    }
                    onComplete(detailMsg)
                } else {
                    repository.insertTask(
                        GTDTask(
                            title = text,
                            description = "ثبت سریع ذهن‌روبی",
                            gtdStatus = "INBOX"
                        )
                    )
                    onComplete("با عنوان «$text» در صندوق ورودی ثبت شد.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete("خطا در ثبت سریع با هوش مصنوعی.")
            }
        }
    }

    fun commitAllParsedResults(context: Context, onComplete: (String) -> Unit) {
        val result = _parsedResult.value ?: return
        viewModelScope.launch {
            var tasksAdded = 0
            var projectsAdded = 0
            var calendarAdded = 0
            
            result.tasks.forEach { task ->
                repository.insertTask(
                    GTDTask(
                        title = task.title,
                        description = task.description,
                        gtdStatus = task.gtdStatus,
                        tags = task.tags,
                        waitingFor = task.waitingFor
                    )
                )
                tasksAdded++
            }
            
            result.projects.forEach { project ->
                repository.insertProject(
                    GTDProject(
                        name = project.name,
                        description = project.description,
                        colorHex = project.colorHex
                    )
                )
                projectsAdded++
            }
            
            result.calendarEvents.forEach { event ->
                val dueDateMs = event.dueDateMs ?: System.currentTimeMillis()
                var calendarEventId: String? = null
                val eventId = withContext(Dispatchers.IO) {
                    addTaskToDeviceCalendar(
                        context,
                        title = event.title,
                        description = event.description,
                        dueDateMs = dueDateMs
                    )
                }
                if (eventId != null) {
                    calendarEventId = eventId.toString()
                }
                repository.insertTask(
                    GTDTask(
                        title = event.title,
                        description = event.description,
                        gtdStatus = "SCHEDULED",
                        dueDate = dueDateMs,
                        googleCalendarEventId = calendarEventId,
                        tags = "تقویم"
                    )
                )
                calendarAdded++
            }
            
            _parsedResult.value = null // clear after import
            onComplete("با موفقیت وارد شد: $tasksAdded کار، $projectsAdded پروژه و $calendarAdded رویداد تقویم.")
        }
    }

    // JSON Export / Backup Feature
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        try {
            val tasks = allTasks.value
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, GTDTask::class.java)
            val adapter = moshi.adapter<List<GTDTask>>(listType)
            adapter.toJson(tasks) ?: "[]"
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    // JSON Import / Sync Feature
    fun importDataFromJson(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val listType = Types.newParameterizedType(List::class.java, GTDTask::class.java)
                val adapter = moshi.adapter<List<GTDTask>>(listType)
                val importedTasks = withContext(Dispatchers.IO) {
                    adapter.fromJson(jsonStr)
                }
                if (importedTasks != null) {
                    for (task in importedTasks) {
                        repository.insertTask(task.copy(id = 0)) // insert as new clean entities
                    }
                    onSuccess()
                } else {
                    onError("فرمت فایل نامعتبر است.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("خطا در پردازش فایل بک‌آپ: ${e.localizedMessage}")
            }
        }
    }
}

class GTDViewModelFactory(private val repository: GTDRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GTDViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GTDViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ChatAction(
    val type: String, // TASK, PROJECT
    val title: String,
    val description: String = "",
    val status: String = "NEXT",
    val tags: String = "",
    val projectName: String? = null,
    val waitingFor: String? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant"
    val text: String,
    val actions: List<ChatAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
