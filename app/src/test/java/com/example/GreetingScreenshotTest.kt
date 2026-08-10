package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GreetingScreenshotTest {

    @Test
    fun app_name_is_correct() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Basket Manager", context.getString(R.string.app_name))
    }
}
