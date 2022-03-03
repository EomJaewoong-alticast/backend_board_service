package com.msp.board_service.proxy

import com.msp.board_service.fallback.TaehwanServiceFallback
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name="board-service", fallbackFactory = TaehwanServiceFallback::class)
interface TaehwanServiceProxy {
    @GetMapping("/v1/board")
    fun getBoardOther(@RequestParam page: Long, @RequestParam size: Long): HashMap<String, Any>
}