package com.aryan.mysleep

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var chronometer: Chronometer
    private lateinit var tvSleepStage: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnSettings: Button
    private lateinit var tvAverageSleep: TextView
    private lateinit var tvSleepGoal: TextView
    private lateinit var tvLastSleep: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private var startTime: Long = 0L
    private var isTracking = false
    private var sleepGoalMillis: Long = 8 * 60 * 60 * 1000L // Default 8 hours

    companion object {
        private const val PREFS_NAME = "sleep_tracker_prefs"
        private const val KEY_SLEEP_START_TIME = "sleep_start_time"
        private const val KEY_IS_TRACKING = "is_tracking"
        private const val KEY_LAST_SLEEP_DURATION = "last_sleep_duration"
        private const val KEY_SLEEP_GOAL_MILLIS = "sleep_goal_millis"
        private const val KEY_SLEEP_SESSIONS = "sleep_sessions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI components
        chronometer = findViewById(R.id.chronometer)
        tvSleepStage = findViewById(R.id.tvSleepStage)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnSettings = findViewById(R.id.btnSettings)
        tvAverageSleep = findViewById(R.id.tvAverageSleep)
        tvSleepGoal = findViewById(R.id.tvSleepGoal)
        tvLastSleep = findViewById(R.id.tvLastSleep)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadSleepGoal()
        updateSleepStatistics()

        // Set up chronometer tick listener for sleep stage updates
        chronometer.onChronometerTickListener = Chronometer.OnChronometerTickListener {
            updateSleepStage()
        }

        setupListeners()
        restoreTrackingState()
    }

    private fun setupListeners() {
        btnStart.setOnClickListener { startSleepTracking() }
        btnStop.setOnClickListener { stopSleepTracking() }
        btnReset.setOnClickListener { resetSleepTracking() }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun startSleepTracking() {
        if (!isTracking) {
            startTime = SystemClock.elapsedRealtime()
            chronometer.base = startTime
            chronometer.start()
            isTracking = true
            btnStart.isEnabled = false
            btnStop.isEnabled = true
            Toast.makeText(this, "Sleep tracking started!", Toast.LENGTH_SHORT).show()
            sharedPreferences.edit()
                .putLong(KEY_SLEEP_START_TIME, startTime)
                .putBoolean(KEY_IS_TRACKING, true)
                .apply()
        } else {
            Toast.makeText(this, "Sleep tracking is already running.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSleepTracking() {
        if (isTracking) {
            chronometer.stop()
            val totalSleepDuration = SystemClock.elapsedRealtime() - chronometer.base
            isTracking = false
            btnStart.isEnabled = true
            btnStop.isEnabled = false
            Toast.makeText(this, "Sleep tracking stopped!", Toast.LENGTH_SHORT).show()
            saveSleepSession(totalSleepDuration)
            updateSleepStatistics()
            resetChronometerAndStage()
        } else {
            Toast.makeText(this, "Sleep tracking not started.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetSleepTracking() {
        chronometer.stop()
        chronometer.base = SystemClock.elapsedRealtime()
        isTracking = false
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        tvSleepStage.text = "Sleep Stage: Awake"
        Toast.makeText(this, "Sleep tracking reset!", Toast.LENGTH_SHORT).show()
        sharedPreferences.edit()
            .remove(KEY_SLEEP_START_TIME)
            .remove(KEY_IS_TRACKING)
            .apply()
        // Optionally clear last sleep duration or other stats if desired on full reset
        updateSleepStatistics() // Refresh statistics after reset
    }

    private fun updateSleepStage() {
        val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
        val elapsedMinutes = elapsedMillis / (1000 * 60)

        val (text, color) = when {
            elapsedMinutes < 30 -> "Light Sleep" to "#81C784" // Greenish
            elapsedMinutes < 90 -> "Deep Sleep" to "#64B5F6"  // Blueish
            else -> "REM Sleep" to "#BA68C8"                 // Purplish
        }

        tvSleepStage.text = "Sleep Stage: $text"
        tvSleepStage.setTextColor(android.graphics.Color.parseColor(color))
    }

    private fun saveSleepSession(duration: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(KEY_LAST_SLEEP_DURATION, duration)

        // Save to a set of sleep sessions for average calculation
        val sessionsJson = sharedPreferences.getString(KEY_SLEEP_SESSIONS, "[]")
        val sessions = parseSleepSessions(sessionsJson)
        sessions.add(SleepSession(System.currentTimeMillis(), duration))
        editor.putString(KEY_SLEEP_SESSIONS, serializeSleepSessions(sessions))
        editor.apply()
    }

    private fun updateSleepStatistics() {
        val lastSleepDuration = sharedPreferences.getLong(KEY_LAST_SLEEP_DURATION, 0L)
        if (lastSleepDuration > 0) {
            tvLastSleep.text = "Last Sleep Duration: ${formatDuration(lastSleepDuration)}"
        } else {
            tvLastSleep.text = "Last Sleep Duration: N/A"
        }

        val sessionsJson = sharedPreferences.getString(KEY_SLEEP_SESSIONS, "[]")
        val sessions = parseSleepSessions(sessionsJson)

        if (sessions.isNotEmpty()) {
            val totalDuration = sessions.sumOf { it.duration }
            val averageDuration = totalDuration / sessions.size
            tvAverageSleep.text = "Average Sleep Duration: ${formatDuration(averageDuration)}"
        } else {
            tvAverageSleep.text = "Average Sleep Duration: N/A"
        }

        tvSleepGoal.text = "Sleep Goal: ${formatDuration(sleepGoalMillis)}"

        // Compare last sleep with goal
        if (lastSleepDuration > 0 && sleepGoalMillis > 0) {
            val difference = lastSleepDuration - sleepGoalMillis
            val hoursDifference = (difference.toDouble() / (1000 * 60 * 60)).roundToInt()
            val toastMessage = when {
                hoursDifference > 0 -> "You slept ${hoursDifference} hour(s) more than your goal."
                hoursDifference < 0 -> "You slept ${-hoursDifference} hour(s) less than your goal."
                else -> "You met your sleep goal!"
            }
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSettingsDialog() {
        val currentGoalHours = (sleepGoalMillis / (1000 * 60 * 60)).toInt()
        val currentGoalMinutes = ((sleepGoalMillis / (1000 * 60)) % 60).toInt()

        val timePickerDialog = TimePickerDialog(this,
            {
                    _, hourOfDay, minute ->
                sleepGoalMillis = (hourOfDay * 60 * 60 * 1000L) + (minute * 60 * 1000L)
                sharedPreferences.edit().putLong(KEY_SLEEP_GOAL_MILLIS, sleepGoalMillis).apply()
                updateSleepStatistics()
                Toast.makeText(this, "Sleep goal updated to ${hourOfDay}h ${minute}m", Toast.LENGTH_SHORT).show()
            },
            currentGoalHours, currentGoalMinutes, true)
        timePickerDialog.setTitle("Set Sleep Goal")
        timePickerDialog.show()
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun loadSleepGoal() {
        sleepGoalMillis = sharedPreferences.getLong(KEY_SLEEP_GOAL_MILLIS, 8 * 60 * 60 * 1000L) // Default 8 hours
    }

    private fun restoreTrackingState() {
        isTracking = sharedPreferences.getBoolean(KEY_IS_TRACKING, false)
        if (isTracking) {
            startTime = sharedPreferences.getLong(KEY_SLEEP_START_TIME, 0L)
            if (startTime != 0L) {
                chronometer.base = startTime
                chronometer.start()
                btnStart.isEnabled = false
                btnStop.isEnabled = true
            } else {
                // Inconsistent state, reset tracking
                resetSleepTracking()
            }
        } else {
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun resetChronometerAndStage() {
        chronometer.base = SystemClock.elapsedRealtime()
        tvSleepStage.text = "Sleep Stage: Awake"
    }

    // Data class for storing sleep session info
    data class SleepSession(val timestamp: Long, val duration: Long)

    //region SharedPreferences helper functions to store list of SleepSession
    private fun serializeSleepSessions(sessions: MutableList<SleepSession>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("[")
        sessions.forEachIndexed { index, session ->
            stringBuilder.append("{\"timestamp\":")
            stringBuilder.append(session.timestamp)
            stringBuilder.append(",\"duration\":")
            stringBuilder.append(session.duration)
            stringBuilder.append("}")
            if (index < sessions.size - 1) {
                stringBuilder.append(",")
            }
        }
        stringBuilder.append("]")
        return stringBuilder.toString()
    }

    private fun parseSleepSessions(jsonString: String?): MutableList<SleepSession> {
        val sessions = mutableListOf<SleepSession>()
        if (jsonString.isNullOrEmpty() || jsonString == "[]") {
            return sessions
        }
        // Basic parsing assuming well-formed JSON array of objects like [{\"timestamp\":123,\"duration\":456}]
        val entries = jsonString.substring(1, jsonString.length - 1).split("},{")
        entries.forEach {
            val cleanEntry = it.replace("{", "").replace("}", "")
            val parts = cleanEntry.split(",")
            var timestamp: Long = 0
            var duration: Long = 0
            parts.forEach {
                if (it.contains("timestamp")) {
                    timestamp = it.split(":")[1].toLongOrNull() ?: 0L
                } else if (it.contains("duration")) {
                    duration = it.split(":")[1].toLongOrNull() ?: 0L
                }
            }
            if (timestamp != 0L && duration != 0L) {
                sessions.add(SleepSession(timestamp, duration))
            }
        }
        return sessions
    }
    //endregion
}
