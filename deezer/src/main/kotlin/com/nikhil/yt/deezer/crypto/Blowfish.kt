package com.nikhil.yt.deezer.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Blowfish {
    private const val CHUNK_SIZE = 2048
    private val SECRET = "g4el58wc0zvf9na1".toByteArray()
    private val IV = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)

    fun deriveKey(trackId: String): ByteArray {
        val md5 = MessageDigest.getInstance("MD5").digest(trackId.toByteArray())
        val hex = md5.joinToString("") { "%02x".format(it) }
        val key = ByteArray(16)
        for (i in 0 until 16) {
            key[i] = (hex[i].code xor hex[i + 16].code xor SECRET[i].toInt()).toByte()
        }
        return key
    }

    fun decryptChunk(encrypted: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("Blowfish/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "Blowfish"), IvParameterSpec(IV))
        return cipher.doFinal(encrypted)
    }

    fun decryptAll(input: ByteArray, trackId: String): ByteArray {
        val key = deriveKey(trackId)
        val output = mutableListOf<Byte>()
        var offset = 0
        var chunkIndex = 0

        while (offset < input.size) {
            val remaining = input.size - offset
            val chunkSize = minOf(CHUNK_SIZE, remaining)
            val chunk = input.copyOfRange(offset, offset + chunkSize)

            if (chunkSize == CHUNK_SIZE && chunkIndex % 3 == 0) {
                output.addAll(decryptChunk(chunk, key).toList())
            } else {
                output.addAll(chunk.toList())
            }
            chunkIndex++
            offset += chunkSize
        }

        return output.toByteArray()
    }
}

class StripeDecryptor(trackId: String) {
    private val key = Blowfish.deriveKey(trackId)
    private var chunkIndex = 0
    private var buf = ByteArray(0)

    fun feed(data: ByteArray): ByteArray {
        val output = mutableListOf<Byte>()
        var offset = 0

        while (offset < data.size) {
            val need = 2048 - buf.size
            val take = minOf(need, data.size - offset)
            buf += data.copyOfRange(offset, offset + take)
            offset += take

            if (buf.size == 2048) {
                if (chunkIndex % 3 == 0) {
                    output.addAll(Blowfish.decryptChunk(buf, key).toList())
                } else {
                    output.addAll(buf.toList())
                }
                chunkIndex++
                buf = ByteArray(0)
            }
        }

        return output.toByteArray()
    }

    fun finish(): ByteArray {
        if (buf.isNotEmpty()) {
            val result = buf
            buf = ByteArray(0)
            return result
        }
        return ByteArray(0)
    }
}
