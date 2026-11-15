package io.github.rasagyavatsal.homeoremedica.ui

import java.time.LocalTime
import kotlin.random.Random

// The empty chat's greetings, by the part of the day on the device's clock, as in the web app's src/web/greetings.ts.

private val Morning = listOf(
    "Good morning. What's the case today?",
    "Morning. Who are we seeing first?",
    "Good morning. Let's find the remedy.",
    "A fresh morning, a fresh case.",
)

private val Afternoon = listOf(
    "Good afternoon. What's the case?",
    "Afternoon. Tell me about the patient.",
    "Good afternoon. Which symptoms are we working with?",
    "Afternoon. Let's take the case.",
)

private val Evening = listOf(
    "Good evening. What's the case?",
    "Evening. Ready when you are.",
    "Good evening. Tell me about the patient.",
    "Evening. Let's find the remedy.",
)

private val Night = listOf(
    "Working late? Let's find the remedy.",
    "Still up. What's the case?",
    "A quiet night for a careful case.",
    "Late hours. Tell me about the patient.",
)

private val AnyTime = listOf(
    "Hello. What's the case today?",
    "Ready when you are.",
    "Let's find the remedy.",
    "Describe the symptoms, and we'll begin.",
    "Like cures like. Let's begin.",
)

/** The greetings that suit [hour] (0 to 23): morning from 5, afternoon from 12, evening from 17, night from 22. */
fun greetingsAt(hour: Int): List<String> = when (hour) {
    in 5..11 -> Morning
    in 12..16 -> Afternoon
    in 17..21 -> Evening
    else -> Night
} + AnyTime

/** One greeting for [now], picked at random from those that suit its hour. */
fun greeting(now: LocalTime = LocalTime.now(), random: Random = Random): String = greetingsAt(now.hour).random(random)
