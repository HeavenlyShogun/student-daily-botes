package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "self_menu_items")
data class SelfMenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mealSlot: String, // "早餐", "午餐", "晚餐", "點心宵夜"
    val tags: List<String> = emptyList(), // e.g., ["高蛋白健腦", "低GI防昏睡", "便利商店可得"]
    val brainBenefitNote: String = "",
    val isFavorite: Boolean = false,
    val timesChosen: Int = 0
)

object DefaultSelfMenu {
    val PRESET_MEAL_SLOTS = listOf("早餐", "午餐", "晚餐", "點心宵夜")

    val PRESET_TAGS = listOf(
        "低GI防昏睡",
        "高蛋白健腦",
        "便利商店可得",
        "銅板省錢",
        "快速5分鐘",
        "考前清淡護胃",
        "暖胃舒壓",
        "護眼葉黃素"
    )

    val DEFAULTS = listOf(
        SelfMenuItem(
            name = "無糖高纖豆漿 ＋ 里肌起司全麥蛋餅",
            mealSlot = "早餐",
            tags = listOf("高蛋白健腦", "低GI防昏睡", "銅板省錢"),
            brainBenefitNote = "大豆卵磷脂搭配雞蛋膽鹼，穩定供應上午四節課的前額葉專注能量。",
            isFavorite = true
        ),
        SelfMenuItem(
            name = "烤地瓜 ＋ 茶葉蛋2顆 ＋ 無糖拿鐵/鮮奶",
            mealSlot = "早餐",
            tags = listOf("便利商店可得", "低GI防昏睡", "快速5分鐘", "考前清淡護胃"),
            brainBenefitNote = "原型慢速碳水化合物，避免早餐高糖抹醬造成第二節課血糖驟降犯睏。"
        ),
        SelfMenuItem(
            name = "希臘優格佐燕麥堅果與藍莓",
            mealSlot = "早餐",
            tags = listOf("快速5分鐘", "護眼葉黃素", "考前清淡護胃"),
            brainBenefitNote = "花青素與核桃 Omega-3 協同護眼補腦，適合考前腸胃容易緊張的早晨。"
        ),
        SelfMenuItem(
            name = "舒肥雞胸肉糙米便當 ＋ 燙深綠地瓜葉",
            mealSlot = "午餐",
            tags = listOf("高蛋白健腦", "低GI防昏睡", "護眼葉黃素"),
            brainBenefitNote = "白肉低脂好消化，糙米與深綠蔬菜富含維生素B群與葉黃素，預防下午第一節課昏沉。",
            isFavorite = true
        ),
        SelfMenuItem(
            name = "鮭魚毛豆烤飯糰 ＋ 和風海藻雞肉沙拉",
            mealSlot = "午餐",
            tags = listOf("便利商店可得", "低GI防昏睡", "快速5分鐘"),
            brainBenefitNote = "天氣悶熱沒胃口時的首選，攝取深海魚 DHA 且不造成腸胃血液過度集中。"
        ),
        SelfMenuItem(
            name = "滷牛腱/滷蛋白豆腐湯麵（少油多青菜）",
            mealSlot = "午餐",
            tags = listOf("高蛋白健腦", "銅板省錢", "暖胃舒壓"),
            brainBenefitNote = "富含鐵質與優質蛋白質，提高血液攜氧量，讓下午數理邏輯推導更清晰。"
        ),
        SelfMenuItem(
            name = "清蒸鯛魚豆腐味噌湯 ＋ 半碗五穀飯 ＋ 炒菠菜",
            mealSlot = "晚餐",
            tags = listOf("高蛋白健腦", "考前清淡護胃", "暖胃舒壓", "護眼葉黃素"),
            brainBenefitNote = "溫熱味噌湯舒緩晚自習前的焦慮，色胺酸有助於夜間 23:00 褪黑激素合成。",
            isFavorite = true
        ),
        SelfMenuItem(
            name = "蒜泥白肉瘦肉片 ＋ 番茄炒蛋 ＋ 糙米飯",
            mealSlot = "晚餐",
            tags = listOf("高蛋白健腦", "銅板省錢", "暖胃舒壓"),
            brainBenefitNote = "維生素B1消除整天用腦疲勞，茄紅素抗氧化，七分飽不干擾夜間深層睡眠。"
        ),
        SelfMenuItem(
            name = "關東煮（白蘿蔔、板豆腐、水煮蛋、海帶）",
            mealSlot = "晚餐",
            tags = listOf("便利商店可得", "考前清淡護胃", "暖胃舒壓", "銅板省錢"),
            brainBenefitNote = "補習班課間快速解決晚餐的最佳組合，低油溫熱、零負擔。"
        ),
        SelfMenuItem(
            name = "溫牛奶一杯 ＋ 小把無調味綜合堅果/香蕉半根",
            mealSlot = "點心宵夜",
            tags = listOf("快速5分鐘", "暖胃舒壓", "便利商店可得"),
            brainBenefitNote = "晚自習肚子餓的救星：鈣質、鎂與色胺酸幫助放鬆神經，取代炸物手搖飲。"
        )
    )
}
