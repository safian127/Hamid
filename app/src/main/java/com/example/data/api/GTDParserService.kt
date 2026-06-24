package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class ParsedGTDResult(
    @Json(name = "tasks") val tasks: List<ParsedTask> = emptyList(),
    @Json(name = "projects") val projects: List<ParsedProject> = emptyList(),
    @Json(name = "calendarEvents") val calendarEvents: List<ParsedCalendarEvent> = emptyList(),
    @Json(name = "summary") val summary: String = ""
)

@JsonClass(generateAdapter = true)
data class ParsedTask(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "gtdStatus") val gtdStatus: String = "INBOX", // INBOX, NEXT, WAITING, SOMEDAY, SCHEDULED
    @Json(name = "tags") val tags: String = "",
    @Json(name = "waitingFor") val waitingFor: String? = null
)

@JsonClass(generateAdapter = true)
data class ParsedProject(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "colorHex") val colorHex: String = "#29B6F6"
)

@JsonClass(generateAdapter = true)
data class ParsedCalendarEvent(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "dueDateMs") val dueDateMs: Long? = null
)

class GTDParserService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val resultAdapter = moshi.adapter(ParsedGTDResult::class.java)

    /**
     * Parses the user's natural language input into a structured GTD result using Gemini API JSON Mode.
     * Falls back to a rich local parser when API is not configured or fails.
     */
    suspend fun parseInput(input: String, currentLocalTime: String): ParsedGTDResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackParsing(input)
        }

        val systemInstruction = """
            You are a professional GTD (Getting Things Done) time management assistant and natural language parser.
            The user will give you a raw mind sweep, a natural language statement, a personal desire, or an unorganized list of tasks.
            
            Your job is to parse it and return a SINGLE JSON object conforming EXACTLY to the following schema structure:
            {
              "tasks": [
                {
                  "title": "Clean, action-oriented, and Persian task title (e.g. 'تماس با علی برای جلسه')",
                  "description": "Elaborated Persian details of the task",
                  "gtdStatus": "NEXT" (for immediate actions), "WAITING" (if waiting on someone else), "SOMEDAY" (future dreams/wishes), "SCHEDULED" (if exact date/time specified), "INBOX" (fallback/raw thoughts),
                  "tags": "comma-separated Persian tags, e.g. 'کاری، شخصی، خریدنی'",
                  "waitingFor": "Person name to follow up with (only if gtdStatus is 'WAITING')"
                }
              ],
              "projects": [
                {
                  "name": "Persian outcome-oriented project name (e.g. 'طراحی سایت شرکتی جدید')",
                  "description": "Persian details of the successful project outcome",
                  "colorHex": "A hex color code starting with # representing project color (e.g. '#29B6F6', '#AB47BC', '#66BB6A')"
                }
              ],
              "calendarEvents": [
                {
                  "title": "Persian calendar event title",
                  "description": "Persian event description",
                  "dueDateMs": Epoch timestamp in milliseconds. (Calculate based on the provided current user time: $currentLocalTime. Do not generate random future dates unless requested)"
                }
              ],
              "summary": "A warm, empathetic, and encouraging Persian summary of what was extracted from their text and any time management tips."
            }
            
            Strict Guidelines:
            1. Output ONLY a valid JSON string. Do not wrap it in ```json``` or any markdown tags. It must start with '{' and end with '}'.
            2. Fully translate and format the title, description, project name, calendar title, and summary to fluent, high-quality Persian (Farsi).
            3. Make sure titles are highly motivating and start with an active action verb wherever possible.
            4. Parse the dates/times to epoch milliseconds (dueDateMs) correctly based on the current local time context ($currentLocalTime) if any specific timing is mentioned (e.g. 'فردا ساعت ۱۰', 'دوشنبه آینده', '۴ تیر'). If no date/time is mentioned, set dueDateMs to null or omit it.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Please parse this raw user text: $input")))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                val cleanJson = jsonText.trim()
                    .replace("^```json".toRegex(), "")
                    .replace("^```".toRegex(), "")
                    .replace("```$".toRegex(), "")
                    .trim()
                val parsedResult = resultAdapter.fromJson(cleanJson)
                if (parsedResult != null) {
                    return@withContext parsedResult
                }
            }
            return@withContext getLocalFallbackParsing(input)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getLocalFallbackParsing(input)
        }
    }

    private fun getLocalFallbackParsing(prompt: String): ParsedGTDResult {
        val p = prompt.lowercase()
        val tasks = mutableListOf<ParsedTask>()
        val projects = mutableListOf<ParsedProject>()
        val calendarEvents = mutableListOf<ParsedCalendarEvent>()
        
        val summary = "تحلیل هوشمند و تخلیه ذهن محلی (حالت دمو/آفلاین):\nما ورودی شما («$prompt») را پردازش کردیم. با توجه به محتوا، اقدامات، پروژه‌ها و زمان‌بندی‌های بهینه استخراج شده است تا ذهن شما کاملاً آزاد و آماده اقدام فیزیکی شود."
        
        if (p.contains("ورزش") || p.contains("تمرین") || p.contains("سلامت") || p.contains("بدن") || p.contains("دویدن")) {
            tasks.add(ParsedTask(
                title = "تمرین ورزشی روزانه",
                description = "نرمش سبک به مدت ۲۰ دقیقه برای بالا بردن سطح نشاط و انگیزه فیزیکی",
                gtdStatus = "NEXT",
                tags = "سلامتی، شخصی"
            ))
        }
        if (p.contains("خرید") || p.contains("تهیه")) {
            tasks.add(ParsedTask(
                title = "خرید مایحتاج منزل",
                description = "بررسی قفسه‌ها و تهیه لیست نهایی خریدهای ضروری هفته",
                gtdStatus = "NEXT",
                tags = "خریدنی"
            ))
        }
        if (p.contains("کار") || p.contains("پروژه") || p.contains("شرکت") || p.contains("دفتر")) {
            projects.add(ParsedProject(
                name = "پروژه ارتقای برنامه‌ریزی کاری جدید",
                description = "دسته‌بندی و تعیین دقیق ددلاین‌ها برای تسک‌های کاری باز شرکت",
                colorHex = "#29B6F6"
            ))
            tasks.add(ParsedTask(
                title = "شفاف‌سازی گام‌های بعدی پروژه‌های کاری",
                description = "مشخص کردن اولین اقدام فیزیکی ملموس برای شروع کار جدید",
                gtdStatus = "NEXT",
                tags = "کاری"
            ))
        }
        if (p.contains("فردا") || p.contains("شنبه") || p.contains("یکشنبه") || p.contains("دوشنبه") || p.contains("سه شنبه") || p.contains("چهارشنبه") || p.contains("پنجشنبه") || p.contains("جمعه") || p.contains("ساعت") || p.contains("تقویم") || p.contains("دیدار")) {
            calendarEvents.add(ParsedCalendarEvent(
                title = "قرار ملاقات و رویداد تقویم",
                description = "جلسه یا قرار زمان‌بندی شده بر اساس پیام ورودی شما",
                dueDateMs = System.currentTimeMillis() + 86400000 // default to tomorrow
            ))
        }
        if (p.contains("علی") || p.contains("رضا") || p.contains("محمد") || p.contains("مریم") || p.contains("زهرا") || p.contains("سپردم") || p.contains("واگذار")) {
            val person = when {
                p.contains("علی") -> "علی"
                p.contains("رضا") -> "رضا"
                p.contains("محمد") -> "محمد"
                p.contains("مریم") -> "مریم"
                p.contains("زهرا") -> "زهرا"
                else -> "همکار"
            }
            tasks.add(ParsedTask(
                title = "پیگیری امور محول شده به $person",
                description = "بررسی پیشرفت و هماهنگی برای گام بعدی کار سپرده شده",
                gtdStatus = "WAITING",
                tags = "کاری، پیگیری",
                waitingFor = person
            ))
        }
        if (p.contains("کتاب") || p.contains("مطالعه") || p.contains("یادگیری") || p.contains("آموزش")) {
            tasks.add(ParsedTask(
                title = "مطالعه کتاب مدیریت زمان به روش GTD",
                description = "کتاب صوتی یا فیزیکی جهت افزایش انگیزه سازماندهی کارها",
                gtdStatus = "NEXT",
                tags = "یادگیری"
            ))
        }
        if (p.contains("آرزو") || p.contains("اهداف") || p.contains("هدف") || p.contains("درست کردن") || p.contains("ساختن")) {
            tasks.add(ParsedTask(
                title = "نگارش چشم‌انداز بلندمدت و اهداف شخصی",
                description = "تبدیل آرزوها به اهداف مکتوب با وضعیت کارهای آینده",
                gtdStatus = "SOMEDAY",
                tags = "اهداف"
            ))
        }
        
        if (tasks.isEmpty() && projects.isEmpty() && calendarEvents.isEmpty()) {
            tasks.add(ParsedTask(
                title = if (prompt.length > 50) prompt.take(50) + "..." else prompt,
                description = prompt,
                gtdStatus = "INBOX",
                tags = "ایده"
            ))
        }
        
        return ParsedGTDResult(
            tasks = tasks,
            projects = projects,
            calendarEvents = calendarEvents,
            summary = summary
        )
    }
}
