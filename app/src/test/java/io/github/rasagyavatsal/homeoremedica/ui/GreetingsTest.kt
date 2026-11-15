package io.github.rasagyavatsal.homeoremedica.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import kotlin.random.Random

class GreetingsTest {
    @Test
    fun suitsThePartOfTheDay() {
        assertTrue("Good morning. What's the case today?" in greetingsAt(5))
        assertFalse("Good afternoon. What's the case?" in greetingsAt(11))
        assertTrue("Good afternoon. What's the case?" in greetingsAt(12))
        assertTrue("Good evening. What's the case?" in greetingsAt(17))
        assertTrue("Still up. What's the case?" in greetingsAt(22))
        assertTrue("Still up. What's the case?" in greetingsAt(4))
    }

    @Test
    fun canGreetAtAnyTimeOfDay() {
        for (hour in listOf(3, 9, 14, 19)) assertTrue("Ready when you are." in greetingsAt(hour))
    }

    @Test
    fun picksFromThoseThatSuitTheHour() {
        val morning = LocalTime.of(8, 0)
        repeat(20) { seed -> assertTrue(greeting(morning, Random(seed)) in greetingsAt(8)) }
    }
}
