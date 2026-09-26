package com.personalai.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val prefs = context.getSharedPreferences("jarvis_audio_prefs", Context.MODE_PRIVATE)
    var speechRate: Float = prefs.getFloat("tts_speech_rate", 1.05f)
        private set
    var pitch: Float = prefs.getFloat("tts_pitch", 0.95f)
        private set
    var autoSpeak: Boolean = prefs.getBoolean("tts_auto_speak", true)
        private set

    companion object {
        private const val TAG = "TTSHelper"
    }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "TTS: Language not supported or missing data")
                    } else {
                        isInitialized = true
                        engine.setPitch(pitch)
                        engine.setSpeechRate(speechRate)
                        Log.i(TAG, "TTS initialized successfully.")
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed.")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    fun speak(text: String, flush: Boolean = true, force: Boolean = false) {
        if (!force && !autoSpeak) {
            Log.d(TAG, "Skipping speech: Auto-speak is disabled.")
            return
        }
        if (!isInitialized || tts == null) {
            Log.w(TAG, "Cannot speak: TTS not initialized.")
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        tts?.speak(text, queueMode, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
        prefs.edit().putFloat("tts_speech_rate", rate).apply()
    }

    fun setPitch(newPitch: Float) {
        pitch = newPitch
        tts?.setPitch(newPitch)
        prefs.edit().putFloat("tts_pitch", newPitch).apply()
    }

    fun setAutoSpeak(enabled: Boolean) {
        autoSpeak = enabled
        prefs.edit().putBoolean("tts_auto_speak", enabled).apply()
    }

    fun testVoice(sampleText: String = "Jarvis audio systems online, sir.") {
        speak(sampleText, flush = true, force = true)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
