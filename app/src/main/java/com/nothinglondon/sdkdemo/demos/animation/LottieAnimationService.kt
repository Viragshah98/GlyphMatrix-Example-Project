package com.nothinglondon.sdkdemo.demos.animation

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class LottieAnimationService : GlyphMatrixService("Sharingan") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        backgroundScope.launch {
            val frames = loadFrames(context)
            if (frames.isEmpty()) {
                Log.e("Sharingan", "No frames loaded!")
                return@launch
            }
            Log.d("Sharingan", "Loaded ${frames.size} frames")
            while (isActive) {
                for ((pixels, duration) in frames) {
                    if (!isActive) break
                    val data = pixels
                    uiScope.launch {
                        glyphMatrixManager.setMatrixFrame(data)
                    }
                    delay(duration)
                }
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        backgroundScope.cancel()
    }

    private fun loadFrames(context: Context): List<Pair<IntArray, Long>> {
        val result = mutableListOf<Pair<IntArray, Long>>()
        try {
            val json = context.assets.open("animation.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val frames = root.getJSONArray("frames")
            for (i in 0 until frames.length()) {
                val frame = frames.getJSONObject(i)
                val duration = frame.optLong("d", 100)
                val pArray = frame.getJSONArray("p")
                val pixels = IntArray(pArray.length()) { idx ->
                    val raw = pArray.getInt(idx)
                    if (raw > 0) minOf((raw * 255 / 146), 255) else 0
                }
                result.add(Pair(pixels, duration))
            }
        } catch (e: Exception) {
            Log.e("Sharingan", "Error loading frames: ${e.message}")
        }
        return result
    }
}