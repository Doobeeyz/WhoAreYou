package com.example.whoareyou.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.KeyEvent

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()
    private var focusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeUtterances = mutableSetOf<String>()
    private var utteranceCounter = 0
    var onInterrupted: (() -> Unit)? = null

    private val mediaSession = MediaSession(context, "WhoAreYouTts").apply {
        setCallback(object : MediaSession.Callback() {
            // Перехватываем любую медиакнопку для остановки озвучивания. приходит как «следующий трек», а не как пауза.
            override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                val event = androidx.core.content.IntentCompat.getParcelableExtra(
                    mediaButtonIntent, android.content.Intent.EXTRA_KEY_EVENT, KeyEvent::class.java
                )
                Log.d("TTS_MANAGER", "media button: ${event?.let { KeyEvent.keyCodeToString(it.keyCode) }} action=${event?.action}")
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    interruptFromHeadset()
                }
                return true
            }
            override fun onPause() = interruptFromHeadset()
            override fun onStop() = interruptFromHeadset()
            override fun onPlay() = interruptFromHeadset()
            override fun onSkipToNext() = interruptFromHeadset()
            override fun onSkipToPrevious() = interruptFromHeadset()
        }, mainHandler)
    }

    override fun onInit(status: Int) {
        mainHandler.post {
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS_MANAGER", "TTS initialized")
                isInitialized = true
                for (text in pendingQueue) {
                    speaker(text)
                }
                pendingQueue.clear()
            } else {
                Log.d("TTS_MANAGER", "TTS initialization failed")
            }
        }
    }

    init {
        tts = TextToSpeech(context, this)
        focusRequest = buildFocusRequest()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { onUtteranceFinished(utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { onUtteranceFinished(utteranceId) }
            }

            // Приходит после tts.stop() для прерванных фраз onDone не вызывается
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post { onUtteranceFinished(utteranceId) }
            }
        })
    }

    fun speak(text: String) {
        mainHandler.post {
            Log.d("TTS_MANAGER", "speak() called, isInitialized=$isInitialized")
            if (!isInitialized) {
                pendingQueue.add(text)
            } else {
                speaker(text)
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            Log.d("TTS_MANAGER", "stopSpeaking()")
            pendingQueue.clear()
            activeUtterances.clear()
            tts?.stop()
            finishSpeaking()
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        mediaSession.isActive = false
        mediaSession.release()
    }

    private fun speaker(text: String) {
        utteranceCounter++
        val id = utteranceCounter.toString()
        if (activeUtterances.isEmpty()) {
            startSpeaking()
        }
        activeUtterances.add(id)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
    }

    private fun onUtteranceFinished(utteranceId: String?) {
        // Если id уже нет (его убрал stopSpeaking), это запоздавший колбэк — игнорируем,
        // чтобы он не сломал озвучку новых сообщений.
        if (utteranceId == null || !activeUtterances.remove(utteranceId)) return
        if (activeUtterances.isEmpty()) {
            finishSpeaking()
        }
    }

    private fun interruptFromHeadset() {
        if (activeUtterances.isEmpty()) return
        Log.d("TTS_MANAGER", "interrupted by headset button")
        stopSpeaking()
        onInterrupted?.invoke()
    }

    private fun startSpeaking() {
        focusRequest?.let { audioManager.requestAudioFocus(it) }
        mediaSession.setPlaybackState(buildState(PlaybackState.STATE_PLAYING))
        mediaSession.isActive = true
        playSilence()
    }

    // Речь проигрывает процесс движка TTS, а не наше приложение. Система отдаёт
    // медиакнопки сессии того приложения, которое последним само играло медиа-звук,
    // то есть музыкальному плееру. Короткая тишина из нашего процесса делает
    // последним играющим нас, и кнопки начинают приходить в нашу MediaSession.
    private fun playSilence() {
        try {
            val sampleRate = 8000
            val bytes = ByteArray(sampleRate / 5 * 2) // 200 мс, 16 бит, моно
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bytes.size)
                .build()
            track.write(bytes, 0, bytes.size)
            track.play()
            mainHandler.postDelayed({ track.release() }, 500L)
        } catch (e: Exception) {
            Log.d("TTS_MANAGER", "silence playback failed: $e")
        }
    }

    private fun finishSpeaking() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        mediaSession.setPlaybackState(buildState(PlaybackState.STATE_STOPPED))
        // Деактивируем сессию, чтобы после озвучки кнопка паузы снова управляла плеером
        mediaSession.isActive = false
    }

    private fun buildState(state: Int): PlaybackState =
        PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            .build()

    private fun buildFocusRequest(): AudioFocusRequest {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
    }
}