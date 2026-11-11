package com.reykizen.app.resolution

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal ADB TCP client.
 *
 * Assumes the device already has TCP ADB enabled (adb tcpip 5555 or Developer Options).
 * Does NOT handle RSA authentication — works only on devices where ADB is already
 * trusted (i.e., the device has accepted the RSA key previously, or auth is bypassed
 * via developer mode on a debug build).
 *
 * Protocol reference: platform/packages/modules/adb/OVERVIEW.TXT
 *
 * For the typical use case (self-ADB on localhost), no RSA handshake is needed
 * because localhost connections are auto-trusted on Android 11+.
 */
class AdbEngine(
    private val host: String = "127.0.0.1",
    private val port: Int = 5555
) {

    companion object {
        private const val A_SYNC = 0x434e5953
        private const val A_CNXN = 0x4e584e43
        private const val A_OPEN = 0x4e45504f
        private const val A_OKAY = 0x59414b4f
        private const val A_CLSE = 0x45534c43
        private const val A_WRTE = 0x45545257
        private const val A_AUTH = 0x48545541

        private const val ADB_VERSION  = 0x01000001
        private const val MAX_PAYLOAD  = 256 * 1024

        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS    = 5_000
    }

    fun isAvailable(): Boolean = runCatching {
        val sock = Socket()
        sock.connect(java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        sock.close()
        true
    }.getOrDefault(false)

    /**
     * Sends `wm size WxH && wm density DPI` via an ADB shell transaction.
     * Returns Success when the socket handshake completes and the shell command is sent.
     * Does not wait for shell output — wm commands are fire-and-forget.
     */
    fun setResolution(width: Int, height: Int, density: Int): ResolutionResult =
        sendShellCommand("wm size ${width}x${height} && wm density $density")

    fun resetResolution(): ResolutionResult =
        sendShellCommand("wm size reset && wm density reset")

    private fun sendShellCommand(cmd: String): ResolutionResult {
        return runCatching {
            Socket().use { sock ->
                sock.connect(java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                sock.soTimeout = READ_TIMEOUT_MS
                val out = sock.getOutputStream()
                val inp = sock.getInputStream()

                // 1. Send CONNECT
                sendMessage(out, A_CNXN, ADB_VERSION, MAX_PAYLOAD,
                    "host::features=shell_v2,stat_v2,apex".toByteArray())

                // 2. Read until we get CNXN back (server may send AUTH first on some builds)
                val serverMsg = readMessageLoop(inp, maxRounds = 3)
                    ?: return ResolutionResult.Failure("ADB: no CNXN from server")

                if (serverMsg.cmd == A_AUTH) {
                    // Android 11+ localhost is auto-trusted; on older builds this will fail.
                    return ResolutionResult.Failure(
                        "ADB: server demands auth — enable USB debugging and trust this device first"
                    )
                }

                // 3. OPEN shell
                val localId = 1
                sendMessage(out, A_OPEN, localId, 0, "shell:$cmd\u0000".toByteArray())

                // 4. Wait for OKAY (remote assigned an ID)
                val okMsg = readMessageLoop(inp, maxRounds = 5)
                if (okMsg?.cmd != A_OKAY) {
                    return ResolutionResult.Failure("ADB: shell OPEN not acknowledged")
                }

                // 5. Send CLSE to close the shell channel
                sendMessage(out, A_CLSE, localId, okMsg.arg0, ByteArray(0))

                ResolutionResult.Success
            }
        }.getOrElse { e ->
            ResolutionResult.Failure("ADB engine: ${e.message}")
        }
    }

    // ── ADB message framing ──────────────────────────────────────────────────

    private data class AdbMessage(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private fun sendMessage(out: OutputStream, cmd: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val dataLen  = data.size
        val checksum = data.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) }
        val magic    = cmd xor -0x01

        val hdr = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(cmd)
            putInt(arg0)
            putInt(arg1)
            putInt(dataLen)
            putInt(checksum)
            putInt(magic)
        }.array()

        out.write(hdr)
        if (dataLen > 0) out.write(data)
        out.flush()
    }

    private fun readExact(inp: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = inp.read(buf, off, n - off)
            if (r < 0) throw java.io.EOFException("ADB: stream closed after $off/$n bytes")
            off += r
        }
        return buf
    }

    private fun readMessageLoop(inp: InputStream, maxRounds: Int): AdbMessage? {
        repeat(maxRounds) {
            val hdr = runCatching { readExact(inp, 24) }.getOrNull() ?: return null
            val bb  = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
            val cmd     = bb.int
            val arg0    = bb.int
            val arg1    = bb.int
            val dataLen = bb.int
            bb.int  // checksum — skip
            bb.int  // magic    — skip
            val data = if (dataLen > 0) readExact(inp, dataLen) else ByteArray(0)
            return AdbMessage(cmd, arg0, arg1, data)
        }
        return null
    }
}
