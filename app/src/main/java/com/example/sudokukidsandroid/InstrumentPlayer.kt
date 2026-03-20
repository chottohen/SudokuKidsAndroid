package com.example.sudokukidsandroid

import android.content.Context
import android.media.SoundPool

class InstrumentPlayer(context: Context) {

    private val soundPool = SoundPool.Builder().setMaxStreams(2).build()

    // Indexed 0..8 → instruments 1..9 (matches MUSIC_4/MUSIC_9 order)
    private val soundIds = IntArray(9)

    private val rawResIds = listOf(
        R.raw.music_piano,       // 🎹 index 1
        R.raw.music_guitar,      // 🎸 index 2
        R.raw.music_drums,       // 🥁 index 3
        R.raw.music_trumpet,     // 🎺 index 4
        R.raw.music_violin,      // 🎻 index 5
        R.raw.music_saxophone,   // 🎷 index 6
        R.raw.music_accordion,   // 🪗 index 7
        R.raw.music_djembe,      // 🪘 index 8
        R.raw.music_flute        // 🪈 index 9
    )

    init {
        rawResIds.forEachIndexed { i, resId ->
            soundIds[i] = soundPool.load(context, resId, 1)
        }
    }

    fun play(instrumentIndex: Int) {
        if (instrumentIndex < 1 || instrumentIndex > soundIds.size) return
        val id = soundIds[instrumentIndex - 1]
        if (id != 0) soundPool.play(id, 1f, 1f, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
