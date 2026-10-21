package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class GeminiSecretaryService {

    companion object {
        private const val TAG = "GeminiSecretary"
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * 1. 課業任務智慧 5 分鐘微步拆解 (5-Minute Task Micro-stepping)
     */
    suspend fun decomposeTask(title: String, subject: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val prompt = """
            你是一位專業的高中生生活管理秘書與學習心理學教練。
            學生當前正面臨拖延或畏難情緒，請針對任務「【$subject】$title」，將其拆解為 3 到 4 個具體、門檻極低且能在 3~5 分鐘內立刻動手開始的「微步執行步驟」(Micro-steps)。
            原則：
            1. 第一步必須非常簡單（例如：拿出講義翻到第XX頁，深呼吸看第1題），以啟動心理學「蔡加尼克效應」。
            2. 請直接條列輸出 3~4 個微步，每行一個，不要輸出多餘的開頭問候或廢話。格式如：
            微步1：翻開講義找到單元核心例題 (3分鐘)
            微步2：遮住詳解，在草稿紙動筆推導前兩行 (5分鐘)
            微步3：紅筆標註卡關觀念並對照課本公式 (4分鐘)
        """.trimIndent()

        if (!isApiKeyConfigured) {
            // 提供高品質領域經驗的本地離線規則拆解，保證無連線或無金鑰時依然體驗極佳
            return@withContext Result.success(getLocalDecomposedSteps(title, subject))
        }

        try {
            val responseText = callGeminiApi(prompt)
            val steps = responseText.lines()
                .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                .filter { it.isNotBlank() && (it.contains("微步") || it.contains("步") || it.contains("1") || it.contains("2") || it.contains("3") || it.contains("4")) }
                .take(4)

            if (steps.isNotEmpty()) {
                Result.success(steps)
            } else {
                Result.success(getLocalDecomposedSteps(title, subject))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini task decomposition failed, falling back to local heuristics", e)
            Result.success(getLocalDecomposedSteps(title, subject))
        }
    }

    /**
     * 2. 綜合作息與健康診斷顧問 (Circadian & Life Health Advice)
     */
    suspend fun generateHealthConsultation(
        sleepHours: Float,
        sleepDebtHours: Float,
        waterMl: Int,
        exerciseMinutes: Int,
        pendingTasksCount: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            你是一位專門輔導高中生的自律健康與課業秘書。
            學生今日生理數據：
            - 實際睡眠時數：${sleepHours} 小時（睡眠赤字：${String.format("%.1f", sleepDebtHours)} 小時）
            - 飲水量：${waterMl} ml
            - 課間運動：${exerciseMinutes} 分鐘
            - 待完成學業待辦：${pendingTasksCount} 項

            請以溫暖、科學、具有建設性且不給予壓力的語氣，提供 100~150 字的今日生活與作息建議：
            1. 給予一項具體的睡眠或補水微調動作。
            2. 提醒如何運用 5 分鐘法則克服未完成的功課。
            3. 強調深層睡眠才能固化今天背下來的記憶。
        """.trimIndent()

        if (!isApiKeyConfigured) {
            return@withContext Result.success(getLocalHealthAdvice(sleepDebtHours, waterMl, exerciseMinutes))
        }

        try {
            val responseText = callGeminiApi(prompt)
            Result.success(responseText.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini health consultation failed", e)
            Result.success(getLocalHealthAdvice(sleepDebtHours, waterMl, exerciseMinutes))
        }
    }

    /**
     * 3. 每日正向手帳反思賦能 (Positive Journal Feedback)
     */
    suspend fun generateJournalFeedback(
        mood: String,
        goodThings: List<String>,
        selfReflection: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val combinedGoods = goodThings.filter { it.isNotBlank() }.joinToString("；")
        val prompt = """
            學生今日正向手帳紀錄：
            - 心情狀態：$mood
            - 今日 3 件好事：$combinedGoods
            - 今日自我對話與反思：$selfReflection

            請身為學伴秘書，給予一段 80~120 字的成長心態 (Growth Mindset) 賦能點評：
            1. 肯定他記錄的微小進步（微小的自律也是大成就）。
            2. 給予溫暖的心理學鼓勵，引導他平靜放鬆、安心就寢。
        """.trimIndent()

        if (!isApiKeyConfigured) {
            return@withContext Result.success(
                "✨ 秘書點評：今天能看見生活中的微小成就（$combinedGoods），這就是大腦建立正向神經迴路的最好證明！每一天穩定的微小努力，時間都會在考場與生活中複利回報給你。今晚請放下手機，享受高品質深層睡眠！"
            )
        }

        try {
            val responseText = callGeminiApi(prompt)
            Result.success(responseText.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini journal feedback failed", e)
            Result.success("✨ 秘書點評：每一次反思都是自我掌控感的提升。帶著今天的三件好事入睡，明天又是充滿活力的一天！")
        }
    }

    private suspend fun callGeminiApi(promptText: String): String = withTimeout(30_000L) {
        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", promptText)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = "$GEMINI_ENDPOINT?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Gemini API error code ${response.code}: $errorBody")
                throw IOException("Gemini API 回應錯誤 (${response.code})")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty response from Gemini")
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withTimeout parts.getJSONObject(0).optString("text", "")
                }
            }
            throw IOException("無法解析 Gemini API 回傳內容")
        }
    }

    private fun getLocalDecomposedSteps(title: String, subject: String): List<String> {
        return when (subject) {
            "數學" -> listOf(
                "微步1：翻開講義找到「$title」的核心範例一，仔細讀題 (3分鐘)",
                "微步2：遮住詳解，在筆記本上動筆寫下已知條件與第1步算式 (5分鐘)",
                "微步3：核對解答，用紅筆圈出關鍵公式與易錯觀念 (3分鐘)",
                "微步4：趁勝追擊，完成練習題第1小題 (5分鐘)"
            )
            "英文" -> listOf(
                "微步1：拿出單字書或講義，播放音檔跟讀前5個單字 (3分鐘)",
                "微步2：在筆記本中抄寫例句並圈出核心片語用法 (4分鐘)",
                "微步3：遮住中文釋義，快速自我快閃抽測記憶 (3分鐘)"
            )
            "自然" -> listOf(
                "微步1：拿出筆記，畫出受力圖或反應式示意圖 (4分鐘)",
                "微步2：檢視題目給定的已知物理量與公式匹配 (3分鐘)",
                "微步3：帶入數值演算，標註易陷阱單位 (5分鐘)"
            )
            else -> listOf(
                "微步1：桌面淨空，將手機翻面放到身後抽屜 (1分鐘)",
                "微步2：翻開「$title」對應頁面，只讀第一行或看前兩個觀念 (3分鐘)",
                "微步3：動筆寫下第一個步驟，藉由慣性流暢展開專注 (5分鐘)"
            )
        }
    }

    private fun getLocalHealthAdvice(sleepDebtHours: Float, waterMl: Int, exerciseMinutes: Int): String {
        return buildString {
            if (sleepDebtHours > 0.5f) {
                append("⚠️ 睡眠赤字達 ${String.format("%.1f", sleepDebtHours)} 小時！醫學實證熬夜會直接破壞大腦海馬迴記憶提取，今晚請務必在 23:00 前上床入睡。")
            } else {
                append("🌟 昨夜睡眠時數維持良好，大腦神經突觸已獲得充分修復，今天專注力將維持在高水準！")
            }
            if (waterMl < 1500) {
                append(" 目前飲水量偏低，記得在課間補滿水杯，大腦水分充足思維更清晰。")
            }
            if (exerciseMinutes < 20) {
                append(" 學習之餘記得進行 3 分鐘肩頸伸展，活絡血液循環。")
            }
        }
    }
}
