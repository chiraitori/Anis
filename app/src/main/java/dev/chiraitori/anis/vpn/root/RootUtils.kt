package dev.chiraitori.anis.vpn.root

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object RootUtils {
    private const val TAG = "RootUtils"

    /**
     * Checks whether root access is available and granted.
     */
    fun isRootAvailable(): Boolean {
        return try {
            val result = executeCommand("id")
            result.isSuccess && result.stdout.any { it.contains("uid=0") }
        } catch (e: Exception) {
            Log.w(TAG, "Root check failed: ${e.message}")
            false
        }
    }

    /**
     * Executes a command with 'su'.
     */
    fun executeCommand(command: String): ShellResult {
        return executeCommands(listOf(command))
    }

    /**
     * Executes a batch of commands in a single root shell session.
     */
    fun executeCommands(commands: List<String>): ShellResult {
        val stdoutList = mutableListOf<String>()
        val stderrList = mutableListOf<String>()

        return try {
            val process = ProcessBuilder("su").start()
            val os = DataOutputStream(process.outputStream)

            for (cmd in commands) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                line?.let { stdoutList.add(it) }
            }

            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            while (stderrReader.readLine().also { line = it } != null) {
                line?.let { stderrList.add(it) }
            }

            val exitCode = process.waitFor()
            ShellResult(exitCode, stdoutList, stderrList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root commands", e)
            ShellResult(-1, emptyList(), listOf(e.message ?: "Unknown error"))
        }
    }
}
