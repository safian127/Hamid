package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiManager {
    suspend fun getAICounsel(prompt: String, systemInstruction: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getLocalFallbackResponse(prompt)
        }

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "خطا: متأسفانه پاسخی دریافت نشد. لطفاً مجدداً تلاش کنید."
        } catch (e: Exception) {
            e.printStackTrace()
            "خطا در برقراری ارتباط با هوش مصنوعی:\n${e.localizedMessage}\n\n[حالت دمو فعال است. پاسخ‌های محلی بارگذاری می‌شوند]\n\n${getLocalFallbackResponse(prompt)}"
        }
    }

    private fun getLocalFallbackResponse(prompt: String): String {
        // High quality Farsi simulation response based on input prompts
        val p = prompt.lowercase()
        return when {
            p.contains("inbox") || p.contains("صندوق ورودی") || p.contains("تصمیم") || p.contains("انتخاب") -> {
                """
                **🧠 مشاوره هوش مصنوعی برای صندوق ورودی (Inbox):**
                
                بر اساس متدولوژی GTD، صندوق ورودی شما مکانی برای ذخیره موقت ایده‌هاست. پیشنهاد من برای پردازش سریع‌تر این کارها:
                
                ۱. **قانون ۲ دقیقه**: وظایفی که کمتر از دو دقیقه زمان می‌برند را همین حالا انجام دهید (مانند تماس کوتاه یا چک کردن سریع ایمیل).
                ۲. **پروژه‌سازی**: اگر کاری نیاز به چند مرحله دارد، آن را به عنوان یک «پروژه» جدید ثبت کنید و اولین قدم فیزیکی را به «اقدامات بعدی» ببرید.
                ۳. **واگذاری یا تعویق**: کارهایی که به دیگران مربوط است را به وضعیت «در انتظار» ببرید و کارهایی که تاریخ خاصی دارند را «زمان‌بندی» کنید.
                
                پیشنهاد ویژه من برای کارهای فعلی شما: ابتدا روی شفاف‌سازی و نوشتن اولین گام عملی (Next Action) متمرکز شوید تا مانع ذهنی برطرف شود.
                """.trimIndent()
            }
            p.contains("زمان") || p.contains("اولویت") || p.contains("برنامه‌ریزی") || p.contains("prioritize") -> {
                """
                **📅 توصیه هوش مصنوعی برای مدیریت زمان و اولویت‌بندی:**
                
                برای داشتن یک برنامه‌ریزی بهینه بر اساس متدولوژی GTD و تقویم هجری شمسی:
                
                ۱. **تمرکز روزانه بر روی ۳ اولویت اصلی**: هر روز صبح، از بخش «اقدامات بعدی» فقط ۳ کار کلیدی و حیاتی را انتخاب کرده و مابقی را در پس‌زمینه نگه دارید.
                ۲. **دسته‌بندی متنی (Contexts)**: وظایف خود را بر اساس موقعیت یا ابزار فیلتر کنید (مثلاً تگ‌های #تلفن، #سیستم، #فروشگاه). این کار خستگی تصمیم‌گیری را تا ۴۰٪ کاهش می‌دهد.
                ۳. **بازبینی هفتگی**: هر جمعه یا شنبه صبح، کل سیستم خود را بررسی کنید. کارهای تکمیل‌شده را آرشیو کرده و صندوق ورودی را کاملاً خالی کنید.
                
                یادآوری: شنبه آغاز هفته است؛ پس انرژی خود را برای کارهای سنگین در شروع هفته ذخیره کنید!
                """.trimIndent()
            }
            else -> {
                """
                **💡 راهنمای هوش مصنوعی جی‌تی‌دی (EverGTD):**
                
                سلام! من دستیار هوش مصنوعی شما در مسیر افزایش بهره‌وری به روش GTD هستم. 
                
                - شما می‌توانید کارهای خود را بدون دغدغه در **صندوق ورودی** بنویسید تا ذهنتان آزاد شود.
                - برای کارهای پیچیده، **پروژه** بسازید و مراحل آن را خرد کنید.
                - با فعال کردن **یادآوری‌های هوشمند بر اساس موقعیت مکانی**، به محض رسیدن به محل کار یا فروشگاه، اعلانی برای کارهای مربوطه دریافت می‌کنید.
                - سیستم با **تقویم هجری شمسی** شما کاملاً هماهنگ است و آغاز هفته از **شنبه** تعیین شده تا منطبق با نیازهای بومی شما باشد.
                
                چه کمکی از دست من بر می‌آید؟ می‌توانید درباره اولویت‌بندی، کاهش استرس کاری، یا خالی کردن صندوق ورودی سوال بپرسید.
                """.trimIndent()
            }
        }
    }
}
