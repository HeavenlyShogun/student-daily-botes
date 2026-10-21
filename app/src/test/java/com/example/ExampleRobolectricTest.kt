package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DailyHealthLog
import org.junit.Assert.assertEquals
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
}
