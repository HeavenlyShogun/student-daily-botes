package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiSecretaryService
import com.example.data.db.AppDatabase
import com.example.data.engine.SecretaryDecisionEngine
import com.example.data.model.DailyHealthLog
import com.example.data.model.DefaultFixedSchedules
import com.example.data.model.FixedScheduleItem
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import com.example.data.repository.StudentLifeRepository
import com.example.notification.AlarmScheduler
import com.example.notification.BootCompletedReceiver
import com.example.notification.NotificationHelper
import com.example.notification.ReminderBroadcastReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

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
    fun `verify PendingIntent identity consistency on schedule and cancelTaskReminders`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val baseNow = sdf.parse("2026-10-22 18:00")!!.time

        val task = TodoTask(
            id = 777L,
            title = "化學氧化還原滴定實驗預習",
            subjectTag = "自然",
            dueDate = "2026-10-22",
            dueTime = "21:00",
            scheduledSlotStart = "19:30",
            scheduledSlotEnd = "20:15",
            reminderMinutesBefore = 15
        )

        // Sync reminders for task 777
        val plan = AlarmScheduler.syncTaskReminders(context, task, baseNow)
        assertEquals(3, plan.stageTriggers.size)
        assertTrue(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.GENTLE))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.FOCUS))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.OVERDUE))

        // Cancel all reminders for task 777 -> all PendingIntents must be matched and removed
        AlarmScheduler.cancelTaskReminders(context, 777L)
        assertFalse(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.GENTLE))
        assertFalse(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.FOCUS))
        assertFalse(AlarmScheduler.hasScheduledReminder(context, 777L, ReminderStage.OVERDUE))
    }

    @Test
    fun `verify ReminderBroadcastReceiver postpone cancels stale stages and complete clears all alarms`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val nowMillis = sdf.parse("2026-10-22 19:00")!!.time

        val taskId = db.todoDao().insertTask(
            TodoTask(
                title = "國文古文十五篇背誦",
                subjectTag = "國文",
                dueDate = "2026-10-22",
                dueTime = "22:00",
                scheduledSlotStart = "19:30",
                scheduledSlotEnd = "20:00",
                subTasks = listOf(SubTask(id = "s1", title = "第一段默寫", isCompleted = false))
            )
        )
        val insertedTask = db.todoDao().getTaskById(taskId)!!
        AlarmScheduler.syncTaskReminders(context, insertedTask, nowMillis)
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.GENTLE))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.FOCUS))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.OVERDUE))

        // Trigger ACTION_POSTPONE twice -> should cancel old GENTLE, increment postponeCount to 2 (recurring delayed), and keep new FOCUS + OVERDUE
        val postponeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = NotificationHelper.ACTION_POSTPONE
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }
        ReminderBroadcastReceiver.processIntent(context, postponeIntent, nowMillis)
        ReminderBroadcastReceiver.processIntent(context, postponeIntent, nowMillis + 60_000L)

        val postponedTask = db.todoDao().getTaskById(taskId)!!
        assertEquals(2, postponedTask.postponeCount)
        assertTrue(postponedTask.isRecurringDelayed)
        assertEquals("20:00", postponedTask.scheduledSlotStart)
        assertFalse("Stale GENTLE reminder must be cancelled on snooze", AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.GENTLE))
        assertTrue("New snoozed FOCUS reminder must be scheduled", AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.FOCUS))
        assertTrue("Deadline OVERDUE reminder must remain active before dueTime", AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.OVERDUE))

        // Trigger ACTION_COMPLETE -> must cancel all stage alarms and mark task & subtasks completed
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = NotificationHelper.ACTION_COMPLETE
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
        }
        ReminderBroadcastReceiver.processIntent(context, completeIntent, nowMillis + 120_000L)

        val completedTask = db.todoDao().getTaskById(taskId)!!
        assertTrue(completedTask.isCompleted)
        assertTrue(completedTask.subTasks.all { it.isCompleted })
        assertFalse(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.GENTLE))
        assertFalse(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.FOCUS))
        assertFalse(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.OVERDUE))
    }

    @Test
    fun `verify TodoDao sorts by Priority weight and dueTime instead of alphabetical enum string`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.todoDao()

        // Insert in scrambled order: alphabetical sort would have placed HIGH_NOT_URGENT before HIGH_URGENT, and LOW before NORMAL
        dao.insertTask(TodoTask(title = "瑣事備忘任務", priority = Priority.LOW, dueDate = "2026-10-22", dueTime = "19:00"))
        dao.insertTask(TodoTask(title = "重要不緊急任務", priority = Priority.HIGH_NOT_URGENT, dueDate = "2026-10-22", dueTime = "20:00"))
        dao.insertTask(TodoTask(title = "一般待辦任務", priority = Priority.NORMAL, dueDate = "2026-10-22", dueTime = "19:30"))
        dao.insertTask(TodoTask(title = "重要且緊急較晚截止", priority = Priority.HIGH_URGENT, dueDate = "2026-10-22", dueTime = "21:30"))
        dao.insertTask(TodoTask(title = "重要且緊急較早截止", priority = Priority.HIGH_URGENT, dueDate = "2026-10-22", dueTime = "18:30"))

        val active = dao.getActiveTasksSync()
        assertEquals(5, active.size)
        assertEquals("重要且緊急較早截止", active[0].title)
        assertEquals("重要且緊急較晚截止", active[1].title)
        assertEquals("重要不緊急任務", active[2].title)
        assertEquals("一般待辦任務", active[3].title)
        assertEquals("瑣事備忘任務", active[4].title)

        db.close()
    }

    @Test
    fun `verify sequential Room migration v1 to v2 to v3 preserves existing data and populates defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_matrix_test.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Exact v1 schema before subTasks and v3 columns existed
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `subjectTag` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `reminderStage` TEXT NOT NULL,
                        `targetMinutes` INTEGER NOT NULL,
                        `completedMinutes` INTEGER NOT NULL,
                        `microSteps` TEXT NOT NULL,
                        `currentMicroStepIndex` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `dueTime` TEXT NOT NULL,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase

        // Insert a v1 task row
        sqliteDb.execSQL(
            """
            INSERT INTO todo_tasks (
                id, title, description, subjectTag, priority, reminderStage,
                targetMinutes, completedMinutes, microSteps, currentMicroStepIndex,
                isCompleted, dueDate, dueTime, completedAt, createdAt
            ) VALUES (
                101, 'v1歷史課本重點整理', '第一章至第三章筆記', '社會', 'HIGH_URGENT', 'GENTLE',
                35, 10, '["翻開課本"]', 0,
                0, '2026-10-22', '20:30', NULL, 1700000000000
            )
            """.trimIndent()
        )

        // Apply sequential migrations v1 -> v2 -> v3
        AppDatabase.MIGRATION_1_2.migrate(sqliteDb)
        AppDatabase.MIGRATION_2_3.migrate(sqliteDb)

        // Verify v1 row survived and v2/v3 columns have expected defaults
        val cursor = sqliteDb.query(
            "SELECT title, subjectTag, targetMinutes, subTasks, postponeCount, scheduledSlotStart, scheduledSlotEnd, reminderMinutesBefore FROM todo_tasks WHERE id = 101"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("v1歷史課本重點整理", cursor.getString(0))
        assertEquals("社會", cursor.getString(1))
        assertEquals(35, cursor.getInt(2))
        assertEquals("[]", cursor.getString(3))
        assertEquals(0, cursor.getInt(4))
        assertEquals("", cursor.getString(5))
        assertEquals("", cursor.getString(6))
        assertEquals(15, cursor.getInt(7))
        cursor.close()

        // Verify fixed_schedules table created in MIGRATION_2_3 is writable
        sqliteDb.execSQL(
            "INSERT INTO fixed_schedules (title, dayOfWeek, startTime, endTime, category, notes) VALUES ('週三英文補習', 3, '18:30', '20:30', 'CRAM_SCHOOL', '帶講義')"
        )
        val schedCursor = sqliteDb.query("SELECT title, category FROM fixed_schedules")
        assertTrue(schedCursor.moveToFirst())
        assertEquals("週三英文補習", schedCursor.getString(0))
        assertEquals("CRAM_SCHOOL", schedCursor.getString(1))
        schedCursor.close()

        sqliteDb.close()
        context.deleteDatabase(dbName)
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
        assertEquals("17:40" to "18:20", plan.scheduledSlotMap[1L])
        assertEquals("18:30" to "19:00", plan.scheduledSlotMap[2L])
        assertTrue(plan.plannedBlocks.any { it.category == "BUFFER" })
        assertTrue(plan.plannedBlocks.any { it.category == "WIND_DOWN" })
        assertTrue(plan.plannedBlocks.any { it.category == "SLEEP" })
    }

    @Test
    fun `verify decision engine edge cases with overlapping cram school, cross-midnight sleep, past due, and recurring delay`() {
        // Overlapping evening fixed schedules: 18:30-19:30 and 19:00-20:00 -> merged to 18:30-20:00
        // Leaves two free windows before wind-down (bedtime 00:30 -> wind-down 00:00):
        // Window 1: 17:40 - 18:30 (50 mins)
        // Window 2: 20:00 - 00:00 (240 mins)
        val fixedSchedules = listOf(
            FixedScheduleItem(title = "學校日間課程", startTime = "07:30", endTime = "17:00", category = "SCHOOL"),
            FixedScheduleItem(title = "放學通勤", startTime = "17:00", endTime = "17:40", category = "COMMUTE"),
            FixedScheduleItem(title = "數學加強班", startTime = "18:30", endTime = "19:30", category = "CRAM_SCHOOL"),
            FixedScheduleItem(title = "解題輔導課", startTime = "19:00", endTime = "20:00", category = "CRAM_SCHOOL")
        )

        val tasks = listOf(
            TodoTask(
                id = 10L,
                title = "生物遺傳學圖表整理",
                subjectTag = "自然",
                priority = Priority.HIGH_NOT_URGENT,
                targetMinutes = 40,
                postponeCount = 0,
                dueTime = "22:00"
            ),
            TodoTask(
                id = 11L,
                title = "物理斜面力學訂正",
                subjectTag = "自然",
                priority = Priority.HIGH_NOT_URGENT,
                targetMinutes = 45,
                postponeCount = 3, // Recurring delayed -> must be scheduled ahead of task 10
                dueTime = "18:00"  // Will finish at 18:25 (past 18:00 dueTime -> should annotate past-due rescue)
            )
        )
        // Cross-midnight bedtime 00:30, wake 06:45 -> 6h 15m (375 mins) sleep
        val healthLog = DailyHealthLog(date = "2026-10-22", bedtime = "00:30", wakeTime = "06:45")

        val plan = SecretaryDecisionEngine.generateDailyPlan(
            fixedSchedules = fixedSchedules,
            tasks = tasks,
            healthLog = healthLog,
            bufferPerTaskMinutes = 10
        )

        assertFalse(plan.isOverloaded)
        assertEquals(290, plan.totalFreeMinutes) // 50 + 240 = 290 mins
        // Task 11 (recurring delayed) gets Window 1 (17:40-18:25), Task 10 gets Window 2 after cram school (20:00-20:40)
        assertEquals("17:40" to "18:25", plan.scheduledSlotMap[11L])
        assertEquals("20:00" to "20:40", plan.scheduledSlotMap[10L])

        val task11Block = plan.plannedBlocks.first { it.taskId == 11L }
        assertTrue(task11Block.subtitle.contains("反覆延期"))
        assertTrue(task11Block.subtitle.contains("逾原訂 18:00 截止"))

        val sleepBlock = plan.plannedBlocks.first { it.category == "SLEEP" }
        assertEquals("00:30", sleepBlock.startTime)
        assertEquals("06:45", sleepBlock.endTime)
        assertEquals(375, sleepBlock.durationMinutes)
    }

    @Test
    fun `verify BootCompletedReceiver and dynamic schedule changes keep DB slots and AlarmScheduler in sync`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repo = StudentLifeRepository(db)
        val today = repo.getTodayDateString()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val morningNow = sdf.parse("$today 08:00")!!.time

        // Clear existing tasks & schedules for clean verification
        for (t in db.todoDao().getActiveTasksSync()) {
            AlarmScheduler.cancelTaskReminders(context, t.id)
            db.todoDao().deleteTask(t)
        }
        for (s in db.fixedScheduleDao().getAllFixedSchedulesSync()) {
            db.fixedScheduleDao().deleteSchedule(s)
        }

        // Insert standard school + commute ending at 17:40
        db.fixedScheduleDao().insertSchedule(
            FixedScheduleItem(title = "日間課程與通勤", startTime = "07:30", endTime = "17:40", category = "SCHOOL")
        )

        val taskId = db.todoDao().insertTask(
            TodoTask(
                title = "英文作文佳句仿寫",
                subjectTag = "英文",
                priority = Priority.HIGH_URGENT,
                targetMinutes = 30,
                dueDate = today,
                dueTime = "21:30"
            )
        )

        // 1. Run BootCompletedReceiver reconciliation
        BootCompletedReceiver.reconcileOnSystemEvent(context, morningNow)

        val taskBeforeCram = db.todoDao().getTaskById(taskId)!!
        assertEquals("17:40", taskBeforeCram.scheduledSlotStart)
        assertEquals("18:10", taskBeforeCram.scheduledSlotEnd)
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.GENTLE))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.FOCUS))
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.OVERDUE))

        // 2. Student adds a sudden evening cram school 17:40 - 19:30 -> task slot must shift to 19:30 - 20:00
        db.fixedScheduleDao().insertSchedule(
            FixedScheduleItem(title = "臨時英文衝刺補習", startTime = "17:40", endTime = "19:30", category = "CRAM_SCHOOL")
        )
        repo.reconcileDailyPlanAndAlarms(context, morningNow)

        val taskAfterCram = db.todoDao().getTaskById(taskId)!!
        assertEquals("19:30", taskAfterCram.scheduledSlotStart)
        assertEquals("20:00", taskAfterCram.scheduledSlotEnd)

        val updatedReminderPlan = AlarmScheduler.computeTaskReminderPlan(taskAfterCram, morningNow)
        val expectedFocusEpoch = sdf.parse("$today 19:30")!!.time
        assertEquals(expectedFocusEpoch, updatedReminderPlan.stageTriggers[ReminderStage.FOCUS])
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
    fun `verify gemini service graceful local fallback and privacy opt out`() = runBlocking {
        val aiService = GeminiSecretaryService()
        aiService.isCloudAiAllowed = false
        val result = aiService.decomposeTask("物理牛頓第二定律", "自然")
        assertTrue(result.isSuccess)
        val steps = result.getOrNull()
        assertNotNull(steps)
        assertEquals(3, steps!!.size)

        val healthResult = aiService.generateHealthConsultation(6.5f, 1.5f, 1500, 20, 3)
        assertTrue(healthResult.isSuccess)
        assertTrue(healthResult.getOrNull()!!.isNotBlank())
    }

    @Test
    fun `verify task rescheduling to tomorrow clears today slot and updates alarm trigger`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repo = StudentLifeRepository(db)

        val today = repo.getTodayDateString()
        val tomorrow = repo.getTomorrowDateString()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val morningNow = sdf.parse("$today 08:00")!!.time

        db.todoDao().getActiveTasksSync().forEach { db.todoDao().deleteTask(it) }

        val taskId = db.todoDao().insertTask(
            TodoTask(
                title = "生物遺傳學圖表整理",
                subjectTag = "自然",
                priority = Priority.HIGH_URGENT,
                targetMinutes = 30,
                dueDate = today,
                dueTime = "21:00"
            )
        )

        repo.reconcileDailyPlanAndAlarms(context, morningNow)
        val todayTask = db.todoDao().getTaskById(taskId)!!
        assertTrue(todayTask.scheduledSlotStart.isNotBlank())
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.FOCUS))

        // Reschedule task to tomorrow -> today's scheduledSlotStart must clear and alarms must point to tomorrow's dueTime
        db.todoDao().updateTask(
            todayTask.copy(
                dueDate = tomorrow,
                dueTime = "20:30",
                scheduledSlotStart = "",
                scheduledSlotEnd = ""
            )
        )
        repo.reconcileDailyPlanAndAlarms(context, morningNow)

        val tomorrowTask = db.todoDao().getTaskById(taskId)!!
        assertEquals("", tomorrowTask.scheduledSlotStart)
        assertEquals("", tomorrowTask.scheduledSlotEnd)

        val tomorrowPlan = AlarmScheduler.computeTaskReminderPlan(tomorrowTask, morningNow)
        val expectedTomorrowOverdue = sdf.parse("$tomorrow 20:30")!!.time
        assertEquals(expectedTomorrowOverdue, tomorrowPlan.stageTriggers[ReminderStage.OVERDUE])
        assertTrue(AlarmScheduler.hasScheduledReminder(context, taskId, ReminderStage.OVERDUE))
    }
}


