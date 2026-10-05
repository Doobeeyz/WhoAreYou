package com.example.whoareyou.tts

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>();
    private var focusRequest: AudioFocusRequest? = null
    private lateinit var audioManager: AudioManager

    private var activeUtterances = 0
    private var utteranceCounter = 0

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS_MANAGER", "TTS initialized");
            isInitialized = true
            for (text in pendingQueue){
                speaker(text)
            }
            pendingQueue.clear()
        } else {
            Log.d("TTS_MANAGER", "TTS initialization failed");
        }
    }

    init {
        tts = TextToSpeech(context, this)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest = buildFocusRequest()

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener(){
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                activeUtterances--
                if (activeUtterances <= 0){
                    focusRequest?.let{audioManager.abandonAudioFocusRequest(it)}
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                activeUtterances--
                if (activeUtterances <= 0){
                    focusRequest?.let{audioManager.abandonAudioFocusRequest(it)}
                }
            }
        })
    }

    fun speak(text: String){
        Log.d("TTS_MANAGER", "speak() called, isInitialized=$isInitialized")

        if (!isInitialized){
            pendingQueue.add(text)
        } else {
            speaker(text)
        }
    }

    private fun speaker(text: String){
        utteranceCounter++
        val id = utteranceCounter.toString()
        if (activeUtterances == 0){
            focusRequest?.let{audioManager.requestAudioFocus(it)}
        }
        activeUtterances++
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
    }

    private fun buildFocusRequest(): AudioFocusRequest{
        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
    }
}
