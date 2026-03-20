package com.example.sudokukidsandroid

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FADE_THRESHOLD_MS = 2000L
private const val FADE_DURATION_MS = 400L
private const val FADE_STEPS = 20

class InstrumentPlayer(context: Context) {

    private val soundPool = SoundPool.Builder().setMaxStreams(2).build()
    private val scope = MainScope()

    private val soundIds = IntArray(9)
    private val durations = LongArray(9)

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
        val retriever = MediaMetadataRetriever()
        rawResIds.forEachIndexed { i, resId ->
            soundIds[i] = soundPool.load(context, resId, 1)
            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
            retriever.setDataSource(context, uri)
            durations[i] = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L
        }
        retriever.release()
    }

    fun play(instrumentIndex: Int) {
        if (instrumentIndex < 1 || instrumentIndex > soundIds.size) return
        val i = instrumentIndex - 1
        val id = soundIds[i]
        if (id == 0) return

        val streamId = soundPool.play(id, 1f, 1f, 0, 0, 1f)

        if (durations[i] > FADE_THRESHOLD_MS && streamId != 0) {
            scope.launch {
                delay(FADE_THRESHOLD_MS)
                val stepDelay = FADE_DURATION_MS / FADE_STEPS
                repeat(FADE_STEPS) { step ->
                    val vol = 1f - (step + 1).toFloat() / FADE_STEPS
                    soundPool.setVolume(streamId, vol, vol)
                    delay(stepDelay)
                }
                soundPool.stop(streamId)
            }
        }
    }

    fun release() {
        scope.cancel()
        soundPool.release()
    }
}
