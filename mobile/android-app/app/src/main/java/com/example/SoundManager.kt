package com.example

import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {
    private var toneGenerator: ToneGenerator? = null

    fun init() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
        }
    }

    fun playKeystroke() {
        // TONE_PROP_BEEP is a clean short beep
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 10)
    }

    fun playReceive() {
        // Different tone for receiving texts like a tick
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 15)
    }

    fun release() {
        // Wait briefly if tones are playing, or just release immediately
        toneGenerator?.release()
        toneGenerator = null
    }
}
