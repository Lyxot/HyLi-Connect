package xyz.hyli.connect.hook.utils

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author Trinea
 * @date 2013-5-16
 * http://www.trinea.cn
 * 米窗声明：该类作者并非米窗，作者信息和源代码地址见上
 */
class HookShellUtils private constructor() {
    init {
        throw AssertionError()
    }

    /**
     * 运行结果
     *
     *  * [result] means result of command, 0 means normal,
     * else means error, same to excute in linux shell
     *  * [successMsg] means success message of command
     * result
     *  * [errorMsg] means error message of command result
     *
     *
     * @author [Trinea](http://www.trinea.cn)
     * 2013-5-16
     */
    class CommandResult(
        /** 运行结果  */
        var result: Int,
        /** 运行成功结果  */
        var successMsg: String?,
        /** 运行失败结果  */
        var errorMsg: String?
    ) {
        init {
            errorMsg = errorMsg
        }
    }

    companion object {
        const val COMMAND_SU = "su"
        const val COMMAND_SH = "sh"
        const val COMMAND_EXIT = "exit\n"
        const val COMMAND_LINE_END = "\n"

        /**
         * 执行shell命令，默认返回结果
         *
         * @param command
         * command
         * 运行是否需要root权限
         * @return
         * @see HookShellUtils.execCommand
         */
        fun execCommand(command: String, isRoot: Boolean): CommandResult {
            return execCommand(arrayOf(command), isRoot, true)
        }

        /**
         * execute shell commands
         *
         * @param commands
         * command array
         * 运行是否需要root权限
         * 是否需要返回运行结果
         * @return
         *  * if isNeedResultMsg is false, [successMsg]
         * is null and [errorMsg] is null.
         *  * if [result] is -1, there maybe some
         * excepiton.
         *
         */
        fun execCommand(
            commands: Array<String>?,
            isRoot: Boolean,
            isNeedResultMsg: Boolean
        ): CommandResult {
            var result = -1
            if (commands.isNullOrEmpty()) {
                return CommandResult(result, null, null)
            }
            var process: Process? = null
            var successResult: BufferedReader? = null
            var errorResult: BufferedReader? = null
            var successMsg: StringBuilder? = null
            var errorMsg: StringBuilder? = null
            var os: DataOutputStream? = null
            try {
                process = Runtime.getRuntime().exec(
                    if (isRoot) COMMAND_SU else COMMAND_SH
                )
                os = DataOutputStream(process.outputStream)
                for (command in commands) {
                    if (command == null) {
                        continue
                    }

                    // donnot use os.writeBytes(commmand), avoid chinese charset
                    // error
                    os.write(command.toByteArray())
                    os.writeBytes(COMMAND_LINE_END)
                    os.flush()
                }
                os.writeBytes(COMMAND_EXIT)
                os.flush()
                result = process.waitFor()
                // get command result
                if (isNeedResultMsg) {
                    successMsg = StringBuilder()
                    errorMsg = StringBuilder()
                    successResult = BufferedReader(
                        InputStreamReader(
                            process.inputStream
                        )
                    )
                    errorResult = BufferedReader(
                        InputStreamReader(
                            process.errorStream
                        )
                    )
                    var s: String?
                    while (successResult.readLine().also { s = it } != null) {
                        successMsg.append(s)
                    }
                    while (errorResult.readLine().also { s = it } != null) {
                        errorMsg.append(s)
                    }
                }
            } catch (e: Exception) {
                Log.e("hyli_connect", e.toString())
                e.printStackTrace()
            } finally {
                try {
                    os?.close()
                    successResult?.close()
                    errorResult?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                process?.destroy()
            }
            return CommandResult(
                result,
                successMsg?.toString(),
                errorMsg?.toString()
            )
        }
    }
}
