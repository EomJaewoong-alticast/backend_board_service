package com.msp.board_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableEurekaClient				// EurekaClient
@EnableFeignClients				// FeignClient
class BoardServiceApplication

fun main(args: Array<String>) {
	runApplication<BoardServiceApplication>(*args)
}
