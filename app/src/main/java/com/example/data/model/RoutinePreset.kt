package com.example.data.model

data class RoutinePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val wakeTime: String,
    val bedTime: String,
    val targetSleepHours: Float,
    val waterTargetMl: Int,
    val stretchIntervalMinutes: Int,
    val scheduleBlocks: List<ScheduleBlock>
)

data class ScheduleBlock(
    val timeRange: String,
    val title: String,
    val category: String, // SLEEP, STUDY, MEAL, EXERCISE, REST
    val tip: String
)

object DefaultPresets {
    val REGULAR_SCHOOL_DAY = RoutinePreset(
        id = "regular_school",
        title = "日常上學節律 (自律充實)",
        subtitle = "維持大腦生物鐘穩定，兼顧學業與體力",
        description = "根據青少年生理時鐘設計：早上7點起床，晚上11點前數位排毒，確保8小時黃金生長與記憶固化期。",
        wakeTime = "06:45",
        bedTime = "23:00",
        targetSleepHours = 7.75f,
        waterTargetMl = 2000,
        stretchIntervalMinutes = 45,
        scheduleBlocks = listOf(
            ScheduleBlock("06:45 - 07:15", "晨起日照與高蛋白早餐", "MEAL", "自然光照抑制褪黑激素，快速喚醒大腦思維"),
            ScheduleBlock("07:30 - 12:00", "學校專注課堂", "STUDY", "每堂課間起立喝水或遠眺綠色植物3分鐘"),
            ScheduleBlock("12:00 - 13:00", "均衡午餐與20分鐘微充電午休", "REST", "短暫小憩20分鐘，能提升下午記憶力與邏輯力"),
            ScheduleBlock("13:00 - 17:00", "下午課堂與課間伸展", "STUDY", "保持坐姿端正，預防頸椎壓迫"),
            ScheduleBlock("17:15 - 17:45", "運動微出汗 (操場快走/球類)", "EXERCISE", "釋放腦源性神經營養因子(BDNF)，減緩課業焦慮"),
            ScheduleBlock("18:00 - 19:00", "清淡晚餐與身心轉換", "MEAL", "避免高糖暴飲暴食導致昏睡"),
            ScheduleBlock("19:00 - 21:30", "黃金自習與待辦清單清空", "STUDY", "使用番茄鐘每45分鐘切換科目，避免大腦疲乏"),
            ScheduleBlock("21:30 - 22:30", "整理書包與正向手帳反思", "REST", "寫下今日3件好事，平靜神經系統"),
            ScheduleBlock("22:30 - 23:00", "關閉螢幕、數位排毒儀式", "REST", "遠離藍光刺激，為深層睡眠做準備")
        )
    )

    val EXAM_SPRINT = RoutinePreset(
        id = "exam_sprint",
        title = "段考衝刺節律 (高效防崩潰)",
        subtitle = "高強度複習更需要精準休息，絕不盲目熬夜",
        description = "神經科學證明：熬夜會直接破壞海馬迴記憶提取。此模組加強微型課間休息與高強度記憶時段，確保考場思維清晰。",
        wakeTime = "06:30",
        bedTime = "23:15",
        targetSleepHours = 7.25f,
        waterTargetMl = 2200,
        stretchIntervalMinutes = 40,
        scheduleBlocks = listOf(
            ScheduleBlock("06:30 - 07:00", "晨讀高難度概念/公式回憶", "STUDY", "利用晨間皮質醇高峰記憶關鍵考點"),
            ScheduleBlock("07:00 - 07:30", "健腦早餐 (雞蛋+燕麥+堅果)", "MEAL", "平穩血糖供應大腦持續計算能源"),
            ScheduleBlock("08:00 - 17:00", "學校模擬測驗與錯題訂正", "STUDY", "錯題分析勝過盲目刷題"),
            ScheduleBlock("17:30 - 18:00", "有氧快走或跳繩15分鐘", "EXERCISE", "刺激海馬迴，迅速掃除下午疲勞"),
            ScheduleBlock("18:30 - 21:30", "高效率分塊複習 (40+10法則)", "STUDY", "將大題庫切為微步目標，嚴防拖延"),
            ScheduleBlock("21:30 - 22:30", "回顧今日核心錯題手帳", "REST", "溫故知新，建立自我信心"),
            ScheduleBlock("22:30 - 23:15", "深度腹式呼吸與準時就寢", "SLEEP", "睡眠是大腦洗去代謝廢物、固化記憶的唯一方式")
        )
    )

    val WEEKEND_RESET = RoutinePreset(
        id = "weekend_reset",
        title = "週末能量調適 (活力重置)",
        subtitle = "適度補眠防時差，兼顧複習與戶外放鬆",
        description = "避免報復性熬夜與睡過頭造成的「社交時差 (Social Jetlag)」，起床時間不比平日延後超過1小時。",
        wakeTime = "07:30",
        bedTime = "23:00",
        targetSleepHours = 8.5f,
        waterTargetMl = 2000,
        stretchIntervalMinutes = 50,
        scheduleBlocks = listOf(
            ScheduleBlock("07:30 - 08:30", "自然醒晨光散步與悠閒早餐", "MEAL", "吸收陽光調節退黑激素"),
            ScheduleBlock("09:00 - 11:30", "一週課業弱點專項攻克", "STUDY", "整塊時間攻克本週最難的單元"),
            ScheduleBlock("12:00 - 14:00", "營養午餐與放鬆小憩", "REST", "讓緊繃神經獲得舒展"),
            ScheduleBlock("14:30 - 17:00", "戶外運動或興趣培養", "EXERCISE", "身心全面放鬆，預防學習倦怠"),
            ScheduleBlock("19:00 - 20:30", "下週生活與學習日程排定", "STUDY", "預先規劃下週目標，消除週日晚間焦慮"),
            ScheduleBlock("20:30 - 22:00", "正向筆記、三件好事與家人朋友對話", "REST", "記錄一週進步，培養正向心態"),
            ScheduleBlock("22:30 - 23:00", "安神熱水澡與準備就寢", "SLEEP", "準時就寢，以充沛精力迎接新的一週")
        )
    )

    val ALL = listOf(REGULAR_SCHOOL_DAY, EXAM_SPRINT, WEEKEND_RESET)
}
