package com.msp.board_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient

@SpringBootApplication
@EnableEurekaClient				// EurekaClient
class BoardServiceApplication

fun main(args: Array<String>) {
	runApplication<BoardServiceApplication>(*args)
}
