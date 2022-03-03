package com.msp.board_service.fallback

import com.msp.board_service.proxy.TaehwanServiceProxy
import feign.hystrix.FallbackFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TaehwanServiceFallback: FallbackFactory<TaehwanServiceProxy> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun create(cause: Throwable) = object: TaehwanServiceProxy {
        override fun getBoardOther(page: Long, size: Long): HashMap<String, Any> {
            logger.error("getBoardOther : ${cause.message}")
            val fallbackMap = HashMap<String, Any>()
            fallbackMap["code"] = 501
            fallbackMap["message"] = cause.message.toString()
            return fallbackMap
        }
    }
}