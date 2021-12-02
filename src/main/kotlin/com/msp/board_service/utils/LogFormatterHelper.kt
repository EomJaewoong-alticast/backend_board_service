package com.msp.board_service.utils

import com.msp.board_service.utils.LogFormatter.Companion.ServiceResult
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit

/**
 * log format definition
 */
class LogFormatterHelper {
    companion object {
        fun getSuccessLog(stopWatch: StopWatch, fixedMessage: String, serviceName: String, varMessage: String): String? {
            stopWatch.stop()
            val microSec = TimeUnit.NANOSECONDS.toMicros(stopWatch.nanoTime)
            return LogFormatter.getLog(fixedMessage, serviceName, ServiceResult.SUCCESS, microSec,getPrefix() + varMessage)
        }

        fun getFailLog(stopWatch: StopWatch, fixedMessage: String, serviceName: String, varMessage: String): String? {
            stopWatch.stop()
            val microSec = TimeUnit.NANOSECONDS.toMicros(stopWatch.nanoTime)
            return LogFormatter.getLog(fixedMessage, serviceName, ServiceResult.FAILURE, microSec, getPrefix() + varMessage)
        }

        fun getFailLog(stopWatch: StopWatch, errorCode: Long, fixedMessage: String, serviceName: String, varMessage: String): String? {
            stopWatch.stop()
            val microSec = TimeUnit.NANOSECONDS.toMicros(stopWatch.nanoTime)
            return LogFormatter.getLog(errorCode, fixedMessage, serviceName, ServiceResult.FAILURE, microSec, getPrefix() + varMessage)
        }

        fun getFailLog(fixedMessage: String, serviceName: String, varMessage: String): String? {
            return LogFormatter.getLog(fixedMessage, serviceName, ServiceResult.FAILURE, 0L, getPrefix() + varMessage)
        }

        private fun getPrefix(): String {
            val adminId = ""
            val remoteIp = ""
            return String.format("adminId=%s, remoteIp=%s, ", adminId, remoteIp)
        }
    }
}