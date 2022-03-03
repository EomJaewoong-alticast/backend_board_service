package com.msp.board_service.handler

import com.msp.board_service.domain.request.PostCreateRequest
import com.msp.board_service.domain.request.PostUpdateRequest
import com.msp.board_service.domain.response.Response
import com.msp.board_service.exception.CustomException
import com.msp.board_service.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Component
class PostHandler(val postService: PostService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    lateinit var reactiveTemplate: ReactiveMongoTemplate

    /**
     * 글 목록 조회 - 01
     */
    fun getPostList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getPostList")
        stopWatch.start("[PostHandler]getPostList start")
        logger.info("[PostHandler]getPostList start - req: ${req}, queryParams: ${req.queryParams()}")

        val offset = req.queryParam("offset").orElseGet{"10"}.toLong()  // offset : default 10
        val count = req.queryParam("count").orElseGet{"1"}.toLong()     // count : default 1
        val q = req.queryParam("q").orElseGet{""}                      // q : 검색 조건

        // offset, count 유효성 검증
        return this.postService.listRangeValidation(offset, count).flatMap {
            this.postService.getPostList(offset, count, q)
        }.flatMap {
            logger.debug("[PostHandler]getPostList - [SUCCESS]Response : $it")
            ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            logger.error("[PostHandler]getPostList - [FAILURE]message : ${it.message.toString()}," +
                    " queryParams : ${req.queryParams()}\n${it.printStackTrace()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[getPostList]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 글 생성 - 02
     */
    fun createPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]createPost")
        stopWatch.start("[PostHandler]createPost start")

        val currentTime = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond().toString()
        return req.bodyToMono(PostCreateRequest::class.java).doOnNext {
            logger.info("[PostHandler]createPost start - req: ${req}, body: $it")
            this.postService.createdPostValidation(it, currentTime) // 입력 값 validation
        }.flatMap {
            this.postService.createPost(it, currentTime)    // 글 생성
        }.flatMap {
            logger.debug("[PostHandler]createPost - [SUCCESS]Response : $it")
            ok().body(Mono.just(Response(200, "post created", it)))
        }.onErrorResume {
            logger.error("[PostHandler]createPost - [FAILURE]message : ${it.message.toString()}\n" +
                    "${it.printStackTrace()}", it)
            when (it) {
                is CustomException -> {
                    ServerResponse.badRequest()
                        .body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[createPost]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * postId로 글 조회 -03
     */
    fun getPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getPost")
        stopWatch.start("[PostHandler]getPost start")
        logger.info("[PostHandler]getPost start - req: ${req}, pathVariables: ${req.pathVariables()}")

        return this.postService.getPost(req.pathVariable("postId")).flatMap {
            logger.debug("[PostHandler]getPost - [SUCCESS]Response : $it")
            ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(    // 요청온 postId로 조회된 글이 없는 경우 예
            Mono.error<ServerResponse?>(CustomException.NoPost(req.pathVariable("postId"))).also {
                logger.info("[PostHandler]getPost - [SUCCESS]Response : No Post - ${req.pathVariables()}")
            }
        ).onErrorResume {
            logger.error("[PostHandler]getPost - [FAILURE]message : ${it.message.toString()}\n${it.printStackTrace()}, " +
                    "pathVariable : ${req.pathVariables()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[getPost]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 글 수정 - 04
     */
    fun updatePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]updatePost")
        stopWatch.start("[PostHandler]updatePost start")

        return req.bodyToMono(PostUpdateRequest::class.java).doOnNext {
            logger.info("[PostHandler]updatePost start - req: ${req}, body: $it")
            this.postService.updatedPostValidation(it)  // 유효성 검증
        }.flatMap {
            this.postService.updatePost(req.pathVariable("postId"), it)
        }.flatMap {
            logger.debug("[PostHandler]updatePost - [SUCCESS]Response: $it")
            ok().body(Mono.just(Response(200, "post updated", it)))
        }.onErrorResume {
            logger.error("[PostHandler]updatePost - [FAILURE]message : ${it.message.toString()}\n${it.printStackTrace()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[updatePost]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 글 삭제 - 05
     */
    fun deletePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]deletePost")
        stopWatch.start("[PostHandler]deletePost start")
        logger.info("[PostHandler]deletePost start - req: ${req}, pathVariables: ${req.pathVariables()}")

        return this.postService.deletePost(req.pathVariable("postId")).flatMap {
            logger.debug("[PostHandler]deletePost - [SUCCESS]Response : $it")
            ok().body(Mono.just(Response(200, "post deleted", it)))
        }.onErrorResume {
            logger.error("[PostHandler]deletePost _ [FAILURE]message : ${it.message.toString()}," +
                    " pathVariable : ${req.pathVariables()}\n${it.printStackTrace()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[deletePost]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 수정 이력 목록 조회 - 10
     */
    fun getTraceList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getTraceList")
        stopWatch.start("[PostHandler]getTraceList start")
        logger.info("[PostHandler]getTraceList start - req: ${req}, queryParams: ${req.queryParams()}")

        val offset = req.queryParam("offset").orElseGet {"10"}.toLong() // offset : default 10
        val count = req.queryParam("count").orElseGet{"1"}.toLong()     // count : default 1
        val startDate = req.queryParam("startDate").orElseGet{""}       // 검색 시작 시간 기준
        val endDate = req.queryParam("endDate").orElseGet{""}           // 검색 종료 시간 기준
        val q = req.queryParam("q").orElseGet{""}                       // 검색 조건

        // 검색 조건 유효성 검증
        return this.postService.traceListValidation(offset, count, startDate, endDate).flatMap {
            this.postService.getTraceList(req.pathVariable("postId"), offset, count, q, startDate, endDate)
        }.flatMap {
            logger.debug("[PostHandler]getTraceList - [SUCCESS]Response: $it")
            ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            logger.error("[PostHandler]getTraceList - [FAILURE]message : ${it.message.toString()}," +
                    " queryParams : ${req.queryParams()}\n" +
                    "${it.printStackTrace()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[getTraceList]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 수정 이력 조회 - 11
     */
    fun getTrace(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getTrace")
        stopWatch.start("[PostHandler]getTrace start")
        logger.info("[PostHandler]getTrace start - req: ${req}, pathVariables: ${req.pathVariables()}")

        return this.postService.getTrace(req.pathVariable("postId"), req.pathVariable("version")).flatMap {
            logger.debug("[PostHandler]getTrace - [SUCCESS]Response : $it")
            ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(
            Mono.error<ServerResponse?>(CustomException.NoTrace(req.pathVariable("postId"), req.pathVariable("version"))).also {
                logger.info("[PostHandler]getTrace - [SUCCESS]Response : No Trace - ${req.pathVariables()}")
            }
        ).onErrorResume {
            logger.error("[PostHandler]getTrace - [FAILURE]message : ${it.message.toString()}," +
                    " pathVariable : ${req.pathVariables()}\n${it.printStackTrace()}", it)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[getTrace]response time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * feign 통신 요청 - 12
     */
    fun getOtherList(req: ServerRequest): Mono<ServerResponse> {
        return ok()
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(this.postService.getOtherList(req.queryParam("page").orElseGet { "1" }.toLong(),
                req.queryParam("size").orElseGet { "1" }.toLong()))
    }
}