package com.example.whoareyou.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray
import android.content.Context
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.whoareyou.tts.TtsManager
import com.example.whoareyou.util.BluetoothHeadsetChecker
import com.example.whoareyou.util.TextCleaner

class MessengerNotificationListenerService : NotificationListenerService(){

    private val allowedPackages = setOf("com.whatsapp", "org.telegram.messenger")
    private val spokenMessages = mutableSetOf<Pair<Long, String>>()

    private lateinit var ttsManager: TtsManager
    private lateinit var bluetoothHeadsetChecker: BluetoothHeadsetChecker
    private lateinit var powerManager: PowerManager

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null
    private val pendingMessages = mutableListOf<Triple<Long, String, String>>()

    override fun onCreate() {
        super.onCreate()
        ttsManager = TtsManager(this)
        bluetoothHeadsetChecker = BluetoothHeadsetChecker(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private fun scheduleSpeak(){
        pendingRunnable?.let { handler.removeCallbacks(it)}
        val runnable = Runnable{
            val sorted = pendingMessages.sortedBy { it.first }
            var currentSender: String? = null
            val parts = mutableListOf<String>()
            for ((_, senderTitle, text) in sorted){
                if (currentSender != senderTitle){
                    parts.add("сообщение от $senderTitle")
                    currentSender = senderTitle
                }
                parts.add(text)
            }
            val phrase = parts.joinToString(". ")
            if (phrase.isNotEmpty()){
                ttsManager.speak(phrase)
            }
            pendingMessages.clear()

        }
        pendingRunnable = runnable
        handler.postDelayed(runnable, 800L)
    }

    @SuppressLint("MissingPermission")
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (powerManager.isInteractive) return
        if (sbn == null || sbn.packageName !in allowedPackages || !bluetoothHeadsetChecker.isHeadsetConnected()) return

        val messagesArray = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_MESSAGES)

        val progressMax = sbn.notification?.extras?.getInt(Notification.EXTRA_PROGRESS_MAX, -1) ?: -1
        if (progressMax > 0) return

        val title = sbn.notification?.extras?.getString(Notification.EXTRA_TITLE)
        val currentText = sbn.notification?.extras?.getString(Notification.EXTRA_TEXT)

        if (messagesArray != null){
            for (msg in getMessagesFromBundleArray(messagesArray)){
                val key = Pair(msg.timestamp, msg.text.toString())
                if (!spokenMessages.contains(key)){
                    Log.d("NOTIF_LISTENER", "${sbn.packageName} | $title | ${msg.text}")
                    spokenMessages.add(key)
                    pendingMessages.add(Triple(msg.timestamp, title?: "неизвестно", TextCleaner.cleanText(msg.text.toString())))
                }
            }
            if (pendingMessages.isNotEmpty()){
                scheduleSpeak()
            }
        } else {
            Log.d("NOTIF_LISTENER", sbn.packageName)
            Log.d("NOTIF_LISTENER", title ?: "")
            Log.d("NOTIF_LISTENER", "text: $currentText")

            pendingMessages.add(Triple(sbn.postTime, title ?: "неизвестно", TextCleaner.cleanText(currentText ?: "")))

            scheduleSpeak()
        }
    }
}
