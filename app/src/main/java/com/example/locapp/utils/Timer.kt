package com.example.locapp.utils

import android.icu.util.Calendar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale

class Timer {
    private var startTime = 0L
    private var elapsedTime = 0L
    private var isRunning = false
    private val timeLiveData = MutableLiveData<String>()
    private var timerJob: Job? = null
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val rightNow = Calendar.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var startTimeDate = ""

    private fun updateElapsedTime() {
        timerJob = coroutineScope.launch {
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val totalElapsedTime = elapsedTime + (currentTime - startTime)
                val seconds = (totalElapsedTime / 1000) % 60
                val minutes = (totalElapsedTime / (1000 * 60)) % 60
                val hours = (totalElapsedTime / (1000 * 60 * 60)) % 24

                timeLiveData.postValue(String.format("%02d:%02d:%02d", hours, minutes, seconds))

                delay(1000)
            }
        }
    }

    fun start() {
        if (!isRunning) {
            startTimeDate = dateFormatter.format(rightNow.time)
            startTime = System.currentTimeMillis()
            isRunning = true
            updateElapsedTime()
        }
    }

    fun pause() {
        if (isRunning) {
            elapsedTime += System.currentTimeMillis() - startTime
            isRunning = false
            timerJob?.cancel()
        }
    }

    fun stop() {
        startTime = 0L
        elapsedTime = 0L
        isRunning = false
        timerJob?.cancel()
        timeLiveData.postValue("00:00:00")
    }

    fun getTimeLiveData(): LiveData<String> {
        return timeLiveData
    }

    fun getTotalTimeElapsed(): Long {
        return if (isRunning) {
            val currentTime = System.currentTimeMillis()
            elapsedTime + (currentTime - startTime)
        } else {
            elapsedTime
        }
    }

    fun getStartTime():String {
        return startTimeDate
    }

}

