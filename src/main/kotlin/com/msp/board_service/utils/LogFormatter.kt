package com.msp.board_service.utils

/**
 * Log format
 */
class LogFormatter {
    companion object {
        enum class ServiceResult {
            SUCCESS, FAILURE
        }

        /**
         * 로그를 생성하여 리턴한다
         * @param fixedMessage
         * @param varMessage
         * @return  로그 문자열
         */
        fun getLog(fixedMessage: String, varMessage: String): String? {
            return "$fixedMessage ~> $varMessage"
        }


        /**
         * 로그를 생성하여 리턴한다.
         * @param fixedMessage  fixed message
         * @param serviceName   service 이름
         * @param serviceResult service 결과 (성공, 실패)
         * @param microSec      걸린 시간 (microseconds)
         * @param varMessage    variable message
         * @return  로그 문자열
         */
        fun getLog(fixedMessage: String, serviceName: String, serviceResult: ServiceResult, microSec: Long, varMessage: String): String? {
            return getLog(fixedMessage, getLogMessage(serviceName, serviceResult, microSec) + " " + varMessage)
        }

        /**
         * 로그를 생성하여 리턴한다.
         * @param errorCode     에러코드
         * @param fixedMessage  fixed message
         * @param varMessage    variable message
         * @return  로그 문자열
         */
        fun getLog(errorCode: Long, fixedMessage: String, varMessage: String): String? {
            return String.format("[%08X] ", errorCode) + fixedMessage + " ~> " + varMessage
        }

        /**
         * 로그를 생성하여 리턴한다.
         * @param errorCode     에러코드
         * @param fixedMessage  fixed message
         * @param serviceName   service 이름
         * @param serviceResult service 결과 (성공, 실패)
         * @param microSec      profile에 걸린 시간 (microseconds)
         * @param varMessage    variable message
         * @return  로그 문자열
         */
        fun getLog(errorCode: Long, fixedMessage: String, profileName: String, serviceResult: ServiceResult, microSec: Long, varMessage: String): String? {
            return getLog(errorCode, fixedMessage, getLogMessage(profileName, serviceResult, microSec) + " " + varMessage)
        }

        private fun getLogMessage(serviceeName: String, serviceResult: ServiceResult, microSec: Long): String {
            var profile: String? = null
            var result: String? = null
            result = if (serviceResult == ServiceResult.SUCCESS) "SUCCESS" else "FAILURE"
            profile = "<" + serviceeName + " " + result + " " + String.format("%dmcs", microSec) + ">"
            return profile
        }

    }
}