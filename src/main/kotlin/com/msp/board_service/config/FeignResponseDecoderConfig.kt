package com.msp.board_service.service

import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignResponseDecoderConfig {
    @Bean
    fun feignDecoder(): SpringDecoder {
        val messageConverters: ObjectFactory<HttpMessageConverters> = ObjectFactory<HttpMessageConverters> {
            val converters = HttpMessageConverters()
            converters
        }
        return SpringDecoder(messageConverters)
    }
}