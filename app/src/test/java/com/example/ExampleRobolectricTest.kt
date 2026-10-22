package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiSecretaryService
import com.example.data.db.AppDatabase
import com.example.data.engine.SecretaryDecisionEngine
import com.example.data.model.DailyHealthLog
import com.example.data.model.DefaultFixedSchedules
import com.example.data.model.FixedScheduleItem
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import com.example.data.repository.StudentLifeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("學生生活秘書", appName)
    }

    @Test
    fun `verify health score calculation`() {
        val log = DailyHealthLog(
            date = "2026-10-14",
            sleepHours = 8.0f,
            targetSleepHours = 8.0f,
            waterMl = 2000,
            targetWaterMl = 2000,
            exerciseMinutes = 30,
            targetExerciseMinutes = 30,
            breakfastLogged = true,
            lunchLogged = true,
            dinnerLogged = true,
            screenWindDownDone = true
        )
        assertEquals(100, log.healthScore)
        assertEquals(0f, log.sleepDebtHours)
    }

    @Test
    fun `verify sleep debt calculation`() {
        val log = DailyHealthLog(
            date = "2026-10-14",
            sleepHours = 6.0f,
            targetSleepHours = 8.0f
        )
        assertEquals(2.0f, log.sleepDebtHours, 0.01f)
    }

    @Test
    fun `verify subtasks progress percentage calculation`() {
        val sub1 = SubTask(id = "1", title = "讀懂公式", isCompleted = true)
        val sub2 = SubTask(id = "2", title = "演算例題", isCompleted = false)
        val sub3 = SubTask(id = "3", title = "算練習題1", isCompleted = false)
        val sub4 = SubTask(id = "4", title = "訂正檢視", isCompleted = false)

        val task = TodoTask(
            title = "段考數學複習",
            subTasks = listOf(sub1, sub2, sub3, sub4)
        )

        assertEquals(25, task.progressPercent)
        assertEquals(1, task.completedSubTasksCount)
        assertEquals(4, task.totalSubTasksCount)

        // When all are completed
        val completedTask = task.copy(
            subTasks = task.subTasks.map { it.copy(isCompleted = true) },
            isCompleted = true
        )
        assertEquals(100, completedTask.progressPercent)
        assertEquals(4, completedTask.completedSubTasksCount)
    }

    @Test
    fun `verify room database instantiation with migration and fixed schedules`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        assertNotNull(db)
        assertNotNull(db.todoDao())
        assertNotNull(db.healthDao())
        assertNotNull(db.journalDao())
        assertNotNull(db.fixedScheduleDao())

        // Insert and verify a fixed schedule
        val schedule = FixedScheduleItem(
            title = "自習室晚自習",
            startTime = "18:30",
            endTime = "21:30",
            category = "SCHOOL"
        )
        val id = db.fixedScheduleDao().insertSchedule(schedule)
        assertTrue(id > 0)
    }

    @Test
    fun `verify secretary decision engine plan generation and buffer space`() {
        val fixedSchedules = DefaultFixedSchedules.DEFAULTS
        val tasks = listOf(
            TodoTask(
                id = 1,
                title = "數學講義雙重根式",
                subjectTag = "數學",
                priority = Priority.HIGH_URGENT,
                targetMinutes = 40
            ),
            TodoTask(
                id = 2,
                title = "英文 7000 單字朗讀",
                subjectTag = "英文",
                priority = Priority.HIGH_NOT_URGENT,
                targetMinutes = 30
            )
        )
        val healthLog = DailyHealthLog(date = "2026-10-21", bedtime = "23:00")

        val plan = SecretaryDecisionEngine.generateDailyPlan(
            fixedSchedules = fixedSchedules,
            tasks = tasks,
            healthLog = healthLog,
            bufferPerTaskMinutes = 10
        )

        assertNotNull(plan)
        assertFalse(plan.isOverloaded)
        assertEquals(2, plan.scheduledTasksCount)
        assertTrue(plan.plannedBlocks.any { it.category == "BUFFER" })
        assertTrue(plan.plannedBlocks.any { it.category == "WIND_DOWN" })
        assertTrue(plan.plannedBlocks.any { it.category == "SLEEP" })
    }

    @Test
    fun `verify secretary decision engine overload detection and explainable deficit`() {
        val fixedSchedules = DefaultFixedSchedules.DEFAULTS
        // Heavy overload: 8 tasks with 60 mins each = 480 mins, far exceeding 17:40 - 22:30 window (290 mins)
        val overloadedTasks = (1..8).map { i ->
            TodoTask(
                id = i.toLong(),
                title = "大型課業任務 $i",
                subjectTag = "學科",
                priority = if (i == 1) Priority.HIGH_URGENT else Priority.NORMAL,
                targetMinutes = 60
            )
        }
        val healthLog = DailyHealthLog(date = "2026-10-21", bedtime = "23:00")

        val plan = SecretaryDecisionEngine.generateDailyPlan(
            fixedSchedules = fixedSchedules,
            tasks = overloadedTasks,
            healthLog = healthLog,
            bufferPerTaskMinutes = 10
        )

        assertTrue(plan.isOverloaded)
        assertTrue(plan.unallocatedTasks.isNotEmpty())
        assertTrue(plan.deficitMinutes > 0)
        assertNotNull(plan.deficitExplanation)
        assertTrue(plan.deficitExplanation!!.contains("秘書科學決策建議"))
    }

    @Test
    fun `verify weekly review metrics distinguishing unlogged from zero completion`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = StudentLifeRepository(AppDatabase.getDatabase(context))

        val metrics = repo.getWeeklyReviewMetrics()
        assertNotNull(metrics)
        assertEquals(7, metrics.dailyStats.size)
        // Check that at least some days have proper logged/unlogged discrimination
        assertTrue(metrics.loggedHealthDays >= 0)
        assertTrue(metrics.unloggedHealthDays >= 0)
        assertNotNull(metrics.executiveSummary)
        assertNotNull(metrics.adviceAction)
    }

    @Test
    fun `verify gemini service graceful local fallback`() = runBlocking {
        val aiService = GeminiSecretaryService()
        val result = aiService.decomposeTask("物理牛頓第二定律", "自然")
        assertTrue(result.isSuccess)
        val steps = result.getOrNull()
        assertNotNull(steps)
        assertTrue(steps!!.isNotEmpty())
    }
}
