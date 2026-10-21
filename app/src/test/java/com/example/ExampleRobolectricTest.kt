package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiSecretaryService
import com.example.data.db.AppDatabase
import com.example.data.model.DailyHealthLog
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun `verify room database instantiation with migration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        assertNotNull(db)
        assertNotNull(db.todoDao())
        assertNotNull(db.healthDao())
        assertNotNull(db.journalDao())
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
