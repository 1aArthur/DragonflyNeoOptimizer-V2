package com.example.data.executor

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PrivilegeMode {
    SHIZUKU,
    ROOT,
    LIMITED
}

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val durationMs: Long
)

class PrivilegedExecutor(private val context: Context) {

    private val tag = "PrivilegedExecutor"

    // Whitelist of allowed parameters and tools to avoid command injection vulnerability
    private val allowedCommandPatterns = listOf(
        Regex("^settings put (system|global|secure) [a-zA-Z_0-9.]+ [a-zA-Z_0-9.-]+$"),
        Regex("^settings get (system|global|secure) [a-zA-Z_0-9.]+$"),
        Regex("^pm (trim-caches|list packages|clear) [a-zA-Z_0-9.]+$"),
        Regex("^am (force-stop|kill-all)$"),
        Regex("^getprop [a-zA-Z_0-9.]+$"),
        Regex("^dumpsys (meminfo|cpuinfo|battery)$"),
        Regex("^wm (size|density)$")
    )

    private val blockedPatterns = listOf(
        Regex("rm\\s+-rf"),
        Regex("mkfs"),
        Regex("reboot"),
        Regex("dd\\s+if"),
        Regex("chmod\\s+777"),
        Regex("shutdown"),
        Regex("wipe")
    )

    /**
     * Detects the highest privilege mode available on the device.
     */
    suspend fun detectPrivilegeMode(): PrivilegeMode = withContext(Dispatchers.IO) {
        if (checkRootAvailable()) {
            return@withContext PrivilegeMode.ROOT
        }
        if (checkShizukuActive()) {
            return@withContext PrivilegeMode.SHIZUKU
        }
        PrivilegeMode.LIMITED
    }

    private fun checkRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.destroy()
            !line.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkShizukuActive(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder() && 
                    rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuInstalledAndRunning(): Boolean {
        return try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes a system command safely depending on the active privilege mode.
     */
    suspend fun executeCommand(command: String, mode: PrivilegeMode): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // 1. Safety validation
        if (!isCommandSafe(command)) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext ExecutionResult(
                success = false,
                output = "",
                error = "Security Violation: Command is blocked or not in whitelist.",
                durationMs = duration
            )
        }

        try {
            val process = when (mode) {
                PrivilegeMode.ROOT -> Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                PrivilegeMode.SHIZUKU -> {
                    try {
                        val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                        val newProcessMethod = shizukuClass.getDeclaredMethod(
                            "newProcess",
                            Array<String>::class.java,
                            Array<String>::class.java,
                            String::class.java
                        )
                        newProcessMethod.isAccessible = true
                        newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
                    } catch (e: Exception) {
                        Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                    }
                }
                PrivilegeMode.LIMITED -> Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            }
            
            val outputBuilder = StringBuilder()
            val errorBuilder = StringBuilder()

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            var outLine: String?
            while (outReader.readLine().also { outLine = it } != null) {
                outputBuilder.append(outLine).append("\n")
            }

            var errLine: String?
            while (errReader.readLine().also { errLine = it } != null) {
                errorBuilder.append(errLine).append("\n")
            }

            val exitVal = process.waitFor()
            process.destroy()

            val duration = System.currentTimeMillis() - startTime
            val success = (exitVal == 0)

            ExecutionResult(
                success = success,
                output = outputBuilder.toString().trim(),
                error = if (success) null else errorBuilder.toString().trim().ifEmpty { "Exit code $exitVal" },
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(tag, "Command execution error: ${e.message}", e)
            ExecutionResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Unknown error",
                durationMs = duration
            )
        }
    }

    private fun isCommandSafe(command: String): Boolean {
        // Check blacklists first
        for (pattern in blockedPatterns) {
            if (pattern.containsMatchIn(command)) {
                return false
            }
        }
        
        // Match against whitelist patterns
        for (pattern in allowedCommandPatterns) {
            if (pattern.matches(command)) {
                return true
            }
        }
        
        // Relax checks slightly for safe query/info dumpsys/getprop commands
        if (command.startsWith("getprop") || command.startsWith("dumpsys") || command.startsWith("wm size") || command.startsWith("wm density")) {
            return true
        }

        return false
    }
}
