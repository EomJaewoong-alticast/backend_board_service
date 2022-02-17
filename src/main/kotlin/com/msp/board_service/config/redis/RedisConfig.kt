package com.msp.board_service.config.redis

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import javax.inject.Inject


@Configuration
@EnableCaching
class RedisConfig : CachingConfigurerSupport() {
    @Inject
    lateinit var redisProperties: RedisProperties

    @Autowired
    lateinit var env: Environment

    @Bean
    @Primary
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory(redisProperties.host, redisProperties.port)
    }

    /**
     * RedisTemplate
     * - Redis 커맨드 수행을 위한 high level abstractions 제공
     * - 직렬화, Connection 관리
     * - CRUD를 위한 Data Operation 제공
     */
    @Bean
    @Primary
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()

        val builder = RedisSerializationContext.newSerializationContext<String, String>(serializer)
        val context = builder.value(serializer).build()

        return ReactiveRedisTemplate(factory, context)
    }
}