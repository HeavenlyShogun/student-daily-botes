package com.example.data.engine

import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.Priority
import com.example.data.model.TodoTask

enum class MaslowLevel(
    val tierNumber: Int,
    val title: String,
    val subtitle: String,
    val colorHex: Long
) {
    SELF_ACTUALIZATION(
        tierNumber = 5,
        title = "自我實現需求",
        subtitle = "頂層：志願突破、大考長遠目標與深度反思",
        colorHex = 0xFF7C3AED // Royal Purple
    ),
    ESTEEM_MASTERY(
        tierNumber = 4,
        title = "尊重與成就需求",
        subtitle = "第4層：學科精熟、專注累積與克服拖延成就感",
        colorHex = 0xFF2563EB // Deep Blue
    ),
    BELONGING_LOVE(
        tierNumber = 3,
        title = "社交與歸屬需求",
        subtitle = "第3層：感恩正向連結、同儕共學與情緒支持",
        colorHex = 0xFF059669 // Emerald Green
    ),
    SAFETY_STABILITY(
        tierNumber = 2,
        title = "安全與秩序需求",
        subtitle = "第2層：作息可控感、不超載計畫與睡前數位排毒",
        colorHex = 0xFFD97706 // Amber Gold
    ),
    PHYSIOLOGICAL_SURVIVAL(
        tierNumber = 1,
        title = "生理生存需求",
        subtitle = "底層基石：充足睡眠、水分、均衡三餐與伸展",
        colorHex = 0xFFDC2626 // Warm Coral Red
    )
}

data class MaslowTierStatus(
    val level: MaslowLevel,
    val scorePercent: Int, // 0..100
    val statusLabel: String, // "穩固" / "成長中" / "待補強"
    val summaryText: String,
    val actionableAdvice: String,
    val relatedTasks: List<TodoTask>
)

data class MaslowPyramidAssessment(
    val tiersTopToBottom: List<MaslowTierStatus>, // Tier 5 down to Tier 1
    val overallFoundationScore: Int,
    val bottleneckTier: MaslowTierStatus,
    val secretaryInsight: String
)

data class EisenhowerQuadrantData(
    val priority: Priority,
    val quadrantCode: String, // "Q1", "Q2", "Q3", "Q4"
    val title: String,
    val strategySubtitle: String,
    val actionGuide: String,
    val colorHex: Long,
    val tasks: List<TodoTask>,
    val totalTargetMinutes: Int,
    val completedCount: Int
)

object MaslowPyramidEngine {

    /**
     * 將待辦任務對應至馬斯洛需求階層，讓學生理解每項任務背後的生命意義
     */
    fun classifyTaskMaslowLevel(task: TodoTask): MaslowLevel {
        val text = "${task.subjectTag} ${task.title} ${task.description}"
        return when {
            task.subjectTag == "生活" && (text.contains("睡") || text.contains("水") || text.contains("餐") || text.contains("伸展") || text.contains("運動") || text.contains("休息")) ->
                MaslowLevel.PHYSIOLOGICAL_SURVIVAL
            task.subjectTag == "生活" ->
                MaslowLevel.SAFETY_STABILITY
            text.contains("社團") || text.contains("同學") || text.contains("討論") || text.contains("家人") ->
                MaslowLevel.BELONGING_LOVE
            task.subjectTag == "大考複習" || task.priority == Priority.HIGH_NOT_URGENT ->
                MaslowLevel.SELF_ACTUALIZATION
            else ->
                MaslowLevel.ESTEEM_MASTERY
        }
    }

    /**
     * 綜合計算馬斯洛五層需求金字塔的即時達成度與科學診斷建議
     */
    fun evaluateMaslowPyramid(
        healthLog: DailyHealthLog?,
        tasks: List<TodoTask>,
        journalEntry: DailyJournalEntry?,
        isPlanOverloaded: Boolean
    ): MaslowPyramidAssessment {
        val log = healthLog ?: DailyHealthLog(date = "")
        val groupedTasks = tasks.groupBy { classifyTaskMaslowLevel(it) }

        // 1. 底層：生理生存需求 (睡眠 + 飲水 + 三餐 + 運動)
        val sleepScore = ((log.sleepHours / log.targetSleepHours.coerceAtLeast(1f)).coerceIn(0f, 1f) * 35).toInt()
        val waterScore = ((log.waterMl.toFloat() / log.targetWaterMl.coerceAtLeast(1)).coerceIn(0f, 1f) * 25).toInt()
        val mealCount = listOf(log.breakfastLogged, log.lunchLogged, log.dinnerLogged).count { it }
        val mealScore = mealCount * 8 // up to 24
        val exerciseScore = ((log.exerciseMinutes.toFloat() / log.targetExerciseMinutes.coerceAtLeast(1)).coerceIn(0f, 1f) * 16).toInt()
        val physioPercent = (sleepScore + waterScore + mealScore + exerciseScore).coerceIn(10, 100)

        val physioTier = MaslowTierStatus(
            level = MaslowLevel.PHYSIOLOGICAL_SURVIVAL,
            scorePercent = physioPercent,
            statusLabel = statusLabelFor(physioPercent),
            summaryText = "睡眠 ${log.sleepHours}h · 飲水 ${log.waterMl}ml · 三餐打卡 $mealCount/3 · 運動 ${log.exerciseMinutes}分",
            actionableAdvice = if (physioPercent < 65)
                "生理底層是所有記憶與專注的地基！建議立即補充一杯 250ml 溫水，並透過「飲食自我菜單」規律完成三餐。"
            else
                "生理生存基石相當穩固，大腦海馬迴與前額葉擁有充足的血氧與能量供應！",
            relatedTasks = groupedTasks[MaslowLevel.PHYSIOLOGICAL_SURVIVAL].orEmpty()
        )

        // 2. 第2層：安全與秩序需求 (不超載排程 + 無大量逾期 + 睡前排毒 + 黃金就寢)
        val overdueCount = tasks.count { !it.isCompleted && it.postponeCount >= 2 }
        var safetyPercent = 45
        if (!isPlanOverloaded) safetyPercent += 25
        if (overdueCount == 0) safetyPercent += 15
        if (log.screenWindDownDone) safetyPercent += 15
        safetyPercent = safetyPercent.coerceIn(15, 100)

        val safetyTier = MaslowTierStatus(
            level = MaslowLevel.SAFETY_STABILITY,
            scorePercent = safetyPercent,
            statusLabel = statusLabelFor(safetyPercent),
            summaryText = if (isPlanOverloaded) "今日排程略顯超載 · 卡關任務 $overdueCount 項" else "今日排程留有緩衝 · 秩序感良好",
            actionableAdvice = if (isPlanOverloaded || overdueCount > 0)
                "當待辦超載或反覆延期時，大腦杏仁核會產生焦慮並破壞安全感。建議用「四象限規劃表」將非緊急事項移出今日清單。"
            else
                "生活節奏井然有序！睡前 30 分鐘記得打卡「數位排毒」，守護夜間安全感。",
            relatedTasks = groupedTasks[MaslowLevel.SAFETY_STABILITY].orEmpty()
        )

        // 3. 第3層：社交與歸屬需求 (感恩三件好事 + 情緒穩定度)
        val goodThingsCount = listOfNotNull(
            journalEntry?.goodThing1?.takeIf { it.isNotBlank() },
            journalEntry?.goodThing2?.takeIf { it.isNotBlank() },
            journalEntry?.goodThing3?.takeIf { it.isNotBlank() }
        ).size
        val belongingPercent = when {
            goodThingsCount >= 3 -> 95
            goodThingsCount == 2 -> 80
            goodThingsCount == 1 -> 65
            else -> 40
        }

        val belongingTier = MaslowTierStatus(
            level = MaslowLevel.BELONGING_LOVE,
            scorePercent = belongingPercent,
            statusLabel = statusLabelFor(belongingPercent),
            summaryText = "今日正向感恩紀錄：$goodThingsCount / 3 件好事",
            actionableAdvice = if (goodThingsCount < 2)
                "高中課業壓力大時容易感到孤軍奮戰。花 2 分鐘在「反思手帳」記下同學、家人或自己的一件小確幸，能顯著提升催產素與心理韌性。"
            else
                "正向感恩連結充沛！溫暖的情緒支持能有效緩衝大考壓力。",
            relatedTasks = groupedTasks[MaslowLevel.BELONGING_LOVE].orEmpty()
        )

        // 4. 第4層：尊重與成就需求 (待辦完成率 + 微步推進 + 專注累積)
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.isCompleted }
        val totalFocusMins = tasks.sumOf { it.completedMinutes }
        val completionRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0.5f
        val esteemPercent = ((completionRatio * 65f) + (totalFocusMins.coerceAtMost(60) * 0.58f)).toInt().coerceIn(20, 100)

        val esteemTier = MaslowTierStatus(
            level = MaslowLevel.ESTEEM_MASTERY,
            scorePercent = esteemPercent,
            statusLabel = statusLabelFor(esteemPercent),
            summaryText = "已完成 $completedTasks/${totalTasks} 項任務 · 累計專注 ${totalFocusMins} 分鐘",
            actionableAdvice = if (esteemPercent < 65)
                "自信心來自「說到做到的小勝利」。挑一項第一象限任務，啟動「5分鐘微步計時」，馬上累積今日成就感！"
            else
                "執行力表現亮眼！每一次打勾都在強化大腦多巴胺正向迴路。",
            relatedTasks = groupedTasks[MaslowLevel.ESTEEM_MASTERY].orEmpty()
        )

        // 5. 第5層：自我實現需求 (Q2 重要不緊急深耕 + 深度自我反思)
        val q2Tasks = tasks.filter { it.priority == Priority.HIGH_NOT_URGENT || it.subjectTag == "大考複習" }
        val q2Completed = q2Tasks.count { it.isCompleted || it.completedMinutes > 0 || it.currentMicroStepIndex > 0 }
        val hasDeepReflection = journalEntry?.selfReflection?.isNotBlank() == true
        var selfActualPercent = 35
        if (q2Tasks.isNotEmpty()) selfActualPercent += 20
        if (q2Completed > 0) selfActualPercent += 25
        if (hasDeepReflection) selfActualPercent += 20
        selfActualPercent = selfActualPercent.coerceIn(25, 100)

        val selfActualTier = MaslowTierStatus(
            level = MaslowLevel.SELF_ACTUALIZATION,
            scorePercent = selfActualPercent,
            statusLabel = statusLabelFor(selfActualPercent),
            summaryText = "長遠深耕任務 ${q2Tasks.size} 項（已推進 $q2Completed 項）· 深度反思 ${if (hasDeepReflection) "已撰寫" else "待記錄"}",
            actionableAdvice = if (q2Tasks.isEmpty())
                "自我實現不能只靠應付眼前的急事！建議在「四象限規劃表」的第二象限（重要不緊急）排入一項大考核心觀念或理想志願探索。"
            else
                "正在穩步邁向理想自我！底層生理健康與中層自律秩序正全力支撐你的頂層突破。",
            relatedTasks = groupedTasks[MaslowLevel.SELF_ACTUALIZATION].orEmpty()
        )

        val tiersTopToBottom = listOf(
            selfActualTier,
            esteemTier,
            belongingTier,
            safetyTier,
            physioTier
        )

        // 找出由底層向上第一個低於 65% 的瓶頸層（馬斯洛階層理論：底層優先滿足）
        val tiersBottomToTop = tiersTopToBottom.reversed()
        val bottleneck = tiersBottomToTop.firstOrNull { it.scorePercent < 65 }
            ?: tiersBottomToTop.minByOrNull { it.scorePercent }
            ?: physioTier

        val overallScore = tiersTopToBottom.map { it.scorePercent }.average().toInt()
        val insight = "依據馬斯洛需求階層理論，您目前的關鍵優先強化層為【${bottleneck.level.title} (${bottleneck.scorePercent}%)】：${bottleneck.actionableAdvice}"

        return MaslowPyramidAssessment(
            tiersTopToBottom = tiersTopToBottom,
            overallFoundationScore = overallScore,
            bottleneckTier = bottleneck,
            secretaryInsight = insight
        )
    }

    /**
     * 產生艾森豪「緊急與重要四象限規劃表」結構化統計
     */
    fun buildEisenhowerMatrix(tasks: List<TodoTask>): List<EisenhowerQuadrantData> {
        val q1Tasks = tasks.filter { it.priority == Priority.HIGH_URGENT }
        val q2Tasks = tasks.filter { it.priority == Priority.HIGH_NOT_URGENT }
        val q3Tasks = tasks.filter { it.priority == Priority.NORMAL }
        val q4Tasks = tasks.filter { it.priority == Priority.LOW }

        return listOf(
            EisenhowerQuadrantData(
                priority = Priority.HIGH_URGENT,
                quadrantCode = "Q1 第一象限",
                title = "重要且緊急 · 立即執行",
                strategySubtitle = "Do First：明日小考、今日截止作業",
                actionGuide = "優先排入今日黃金專注時段，搭配 5 分鐘微步立刻啟動，消除焦慮源。",
                colorHex = Priority.HIGH_URGENT.badgeColorHex,
                tasks = q1Tasks,
                totalTargetMinutes = q1Tasks.filter { !it.isCompleted }.sumOf { it.targetMinutes },
                completedCount = q1Tasks.count { it.isCompleted }
            ),
            EisenhowerQuadrantData(
                priority = Priority.HIGH_NOT_URGENT,
                quadrantCode = "Q2 第二象限",
                title = "重要不緊急 · 計畫深耕",
                strategySubtitle = "Schedule：大考複習、錯題整理、強科養成",
                actionGuide = "學霸脫穎而出的黃金象限！每天保留至少 25~45 分鐘深耕，避免未來退化為 Q1 火燒眉毛。",
                colorHex = Priority.HIGH_NOT_URGENT.badgeColorHex,
                tasks = q2Tasks,
                totalTargetMinutes = q2Tasks.filter { !it.isCompleted }.sumOf { it.targetMinutes },
                completedCount = q2Tasks.count { it.isCompleted }
            ),
            EisenhowerQuadrantData(
                priority = Priority.NORMAL,
                quadrantCode = "Q3 第三象限",
                title = "緊急不重要 · 速戰速決",
                strategySubtitle = "Quick Batch：一般待辦、繳交回條、例行庶務",
                actionGuide = "切忌耗費黃金大腦時段！建議利用課間或零碎 10~15 分鐘集中快速處理完畢。",
                colorHex = Priority.NORMAL.badgeColorHex,
                tasks = q3Tasks,
                totalTargetMinutes = q3Tasks.filter { !it.isCompleted }.sumOf { it.targetMinutes },
                completedCount = q3Tasks.count { it.isCompleted }
            ),
            EisenhowerQuadrantData(
                priority = Priority.LOW,
                quadrantCode = "Q4 第四象限",
                title = "不緊急不重要 · 減量調節",
                strategySubtitle = "Limit / Rest：瑣事備忘、完成目標後的適度放鬆",
                actionGuide = "若今日時間不足應優先刪減或順延；切勿在未完成 Q1/Q2 前拿來逃避拖延。",
                colorHex = Priority.LOW.badgeColorHex,
                tasks = q4Tasks,
                totalTargetMinutes = q4Tasks.filter { !it.isCompleted }.sumOf { it.targetMinutes },
                completedCount = q4Tasks.count { it.isCompleted }
            )
        )
    }

    private fun statusLabelFor(score: Int): String = when {
        score >= 80 -> "穩固充沛"
        score >= 60 -> "穩定成長"
        else -> "優先補強"
    }
}
