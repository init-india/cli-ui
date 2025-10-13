package com.smartcli.core.system

import java.io.BufferedReader
import java.io.InputStreamReader

class LinuxCommandExecutor {
    
    fun executeCommand(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val output = readStream(process.inputStream)
            val error = readStream(process.errorStream)
            val exitCode = process.waitFor()
            
            CommandResult(exitCode, output, error)
        } catch (e: Exception) {
            CommandResult(-1, "", "Error: ${e.message}")
        }
    }
    
    fun executeWithRoot(command: String): CommandResult {
        return executeCommand("su -c $command")
    }
    
    private fun readStream(inputStream: java.io.InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText().trim()
    }
    
    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    ) {
        fun isSuccess(): Boolean = exitCode == 0
    }
}
