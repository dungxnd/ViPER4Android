package com.dxnd.viper4android.utils

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DecodedWav(
    val samples: FloatArray,
    val frameCount: Int,
    val channels: Int,
    val sampleRate: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedWav) return false
        return samples.contentEquals(other.samples) &&
            frameCount == other.frameCount &&
            channels == other.channels &&
            sampleRate == other.sampleRate
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + frameCount
        result = 31 * result + channels
        result = 31 * result + sampleRate
        return result
    }
}

object WavDecoder {
    private const val FORMAT_PCM = 1
    private const val FORMAT_IEEE_FLOAT = 3

    fun decode(stream: InputStream): DecodedWav {
        val all = stream.readBytes()
        return decode(all)
    }

    fun decode(bytes: ByteArray): DecodedWav {
        if (bytes.size < 44) {
            throw IllegalArgumentException("WAV too small: ${bytes.size} bytes (need >= 44 for header)")
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        if (buf.int != 0x46464952) { // RIFF
            throw IllegalArgumentException("Not a RIFF file")
        }
        buf.int
        if (buf.int != 0x45564157) { // WAVE
            throw IllegalArgumentException("Not a WAVE file")
        }

        var formatCode = -1
        var channels = -1
        var sampleRate = -1
        var bitsPerSample = -1
        var dataOffset = -1
        var dataSize = -1

        while (buf.remaining() >= 8) {
            val chunkId = buf.int
            val chunkSize = buf.int
            when (chunkId) {
                0x20746D66 -> { // fmt
                    if (chunkSize < 16) {
                        throw IllegalArgumentException("fmt chunk too small: $chunkSize")
                    }
                    formatCode = buf.short.toInt() and 0xFFFF
                    channels = buf.short.toInt() and 0xFFFF
                    sampleRate = buf.int
                    buf.int // byte rate
                    buf.short // block align
                    bitsPerSample = buf.short.toInt() and 0xFFFF
                    val consumed = 16
                    if (chunkSize > consumed) {
                        buf.position(buf.position() + chunkSize - consumed)
                    }
                }

                0x61746164 -> { // data
                    dataOffset = buf.position()
                    dataSize = chunkSize
                    buf.position(buf.position() + chunkSize)
                }

                else -> {
                    buf.position(buf.position() + chunkSize)
                }
            }
            if (chunkSize and 1 == 1 && buf.remaining() > 0) {
                buf.position(buf.position() + 1)
            }
            if (formatCode != -1 && dataOffset != -1) break
        }

        if (formatCode == -1) throw IllegalArgumentException("WAV missing fmt chunk")
        if (dataOffset == -1) throw IllegalArgumentException("WAV missing data chunk")
        if (channels !in 1..2) {
            throw IllegalArgumentException("Unsupported channel count: $channels (expected 1 or 2)")
        }
        if (formatCode != FORMAT_PCM && formatCode != FORMAT_IEEE_FLOAT) {
            throw IllegalArgumentException(
                "Unsupported WAV format code: $formatCode (expected 1=PCM or 3=IEEE float)",
            )
        }
        if (formatCode == FORMAT_PCM && bitsPerSample !in setOf(16, 24, 32)) {
            throw IllegalArgumentException(
                "Unsupported PCM bit depth: $bitsPerSample (expected 16, 24, or 32)",
            )
        }
        if (formatCode == FORMAT_IEEE_FLOAT && bitsPerSample != 32) {
            throw IllegalArgumentException(
                "Unsupported float bit depth: $bitsPerSample (expected 32)",
            )
        }

        val bytesPerSample = bitsPerSample / 8
        val totalSamples = dataSize / bytesPerSample
        val frameCount = totalSamples / channels
        if (frameCount <= 0) {
            throw IllegalArgumentException("WAV has no frames")
        }

        val samples = FloatArray(totalSamples)
        buf.position(dataOffset)
        when {
            formatCode == FORMAT_IEEE_FLOAT -> {
                for (i in 0 until totalSamples) {
                    samples[i] = buf.float
                }
            }

            bitsPerSample == 16 -> {
                val scale = 1.0f / 32768.0f
                for (i in 0 until totalSamples) {
                    samples[i] = buf.short.toInt() * scale
                }
            }

            bitsPerSample == 24 -> {
                val scale = 1.0f / (1L shl 23).toFloat()
                for (i in 0 until totalSamples) {
                    val b0 = bytes[dataOffset + i * 3].toInt() and 0xFF
                    val b1 = bytes[dataOffset + i * 3 + 1].toInt() and 0xFF
                    val b2 = bytes[dataOffset + i * 3 + 2].toInt()
                    val sample = (b2 shl 16) or (b1 shl 8) or b0
                    samples[i] = sample * scale
                }
            }

            bitsPerSample == 32 -> {
                val scale = 1.0f / (1L shl 31).toFloat()
                for (i in 0 until totalSamples) {
                    samples[i] = buf.int * scale
                }
            }
        }

        return DecodedWav(
            samples = samples,
            frameCount = frameCount,
            channels = channels,
            sampleRate = sampleRate,
        )
    }
}
