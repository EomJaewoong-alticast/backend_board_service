package com.msp.board_service.handler

import com.msp.board_service.domain.request.PostCreateRequest
import com.msp.board_service.domain.request.PostUpdateRequest
import com.msp.board_service.domain.response.Response
import com.msp.board_service.exception.CustomException
import com.msp.board_service.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

@Component
class PostHandler(val postService: PostService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 글 목록 조회 - 01
     * @params offset, count, q
     * @return PostListResponse
     */
    fun getPostList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getPostList")
        stopWatch.start("[PostHandler]getPostList start")
        logger.info("[PostHandler]getPostList start")

        return this.postService.listRangeValidation(
            // offset, count 유효성 검증
            req.queryParam("offset").orElse(""), req.queryParam("count").orElse("")
        ).flatMap {
            this.postService.getPostList(
                req.queryParam("offset").orElse("10").toLong(), // offset : default 10
                req.queryParam("count").orElse("1").toLong(),   // count : default 1
                req.queryParam("q").orElse("")                  // 검색 조건
            )
        }.flatMap {
            logger.debug("SUCCESS - [PostHandler]getPostList _ queryParams : ${req.queryParams()}")
            ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            logger.error("FAILURE - [PostHandler]getPostList _ message : ${it.message.toString()} queryParams : ${req.queryParams()}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]getPostList end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * 글 생성 - 02
     * @params PostCreateRequest
     * @return postIdResponse
     */
    fun createPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]createPost")
        stopWatch.start("[PostHandler]createPost start")
        logger.info("[PostHandler]getPostList start")

        val currentTime = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        return req.bodyToMono(PostCreateRequest::class.java).doOnNext {
            this.postService.createdPostValidation(it, currentTime) // 입력 값 validation
        }.flatMap {
            this.postService.createPost(it, currentTime)    // 글 생성
        }.flatMap {
            logger.debug("SUCCESS - [PostHandler]createPost _ body : ${req.bodyToMono(PostCreateRequest::class.java)}")
            ok().body(Mono.just(Response(200, "post created", it)))
        }.onErrorResume {
            logger.error("FAILURE - [PostHandler]createPost _ message : ${it.message.toString()} body : ${req.bodyToMono(PostCreateRequest::class.java)}")
            when (it) {
                is CustomException -> {
                    ServerResponse.badRequest()
                        .body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]createPost end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * postId로 글 조회 -03
     * @param postId
     * @return Post
     */
    fun getPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getPost")
        stopWatch.start("[PostHandler]getPost start")
        logger.info("[PostHandler]getPost start")

        return this.postService.getPost(req.pathVariable("postId")).flatMap {
            logger.debug("SUCCESS - [PostHandler]getPost _ pathVariable : ${req.pathVariables()}")
            ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(    // 요청온 postId로 조회된 글이 없는 경우 예
            Mono.error(CustomException.NoPost(req.pathVariable("postId")))
        ).onErrorResume {
            logger.error("FAILURE - [PostHandler]getPost _ message : ${it.message.toString()} pathVariable : ${req.pathVariables()}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]getPost end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * 글 수정 - 04
     * @param postId, PostUpdateRequest
     * @return postIdResponse
     */
    fun updatePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]updatePost")
        stopWatch.start("[PostHandler]updatePost start")
        logger.info("[PostHandler]updatePost start")

        return req.bodyToMono(PostUpdateRequest::class.java).doOnNext {
            this.postService.updatedPostValidation(it)  // 유효성 검증
        }.flatMap {
            this.postService.updatePost(req.pathVariable("postId"), it)
        }.flatMap {
            logger.debug("SUCCESS - [PostHandler]updatePost _ body : ${req.bodyToMono(PostUpdateRequest::class.java)}")
            ok().body(Mono.just(Response(200, "post updated", it)))
        }.onErrorResume {
            logger.error("FAILURE - [PostHandler]updatePost _ message : ${it.message.toString()} body : ${req.bodyToMono(PostUpdateRequest::class.java)}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]updatePost end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * 글 삭제 - 05
     * @param postId
     */
    fun deletePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]deletePost")
        stopWatch.start("[PostHandler]deletePost start")
        logger.info("[PostHandler]deletePost start")

        return this.postService.deletePost(req.pathVariable("postId")).flatMap {
            logger.debug("SUCCESS - [PostHandler]deletePost _ pathVariable : ${req.pathVariables()}")
            ok().body(Mono.just(Response(200, "post deleted", it)))
        }.onErrorResume {
            logger.error("FAILURE - [PostHandler]deletePost _ message : ${it.message.toString()} pathVariable : ${req.pathVariables()}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]deletePost end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * 수정 이력 목록 조회 - 10
     * @param postId
     * @return TraceListResponse
     */
    fun getTraceList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getTraceList")
        stopWatch.start("[PostHandler]getTraceList start")
        logger.info("[PostHandler]getTraceList start")

        val currentTime = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        return this.postService.traceListValidation(    // 검색 조건 유효성 검증
            req.queryParam("offset").orElse(""),    // offset : default 10
            req.queryParam("count").orElse(""),     // count : default 1
            req.queryParam("startDate").orElse(""), // 검색 시작 시간 기준
            req.queryParam("endDate").orElse(""),   // 검색 종료 시간 기준
            currentTime
        ).flatMap {
            this.postService.getTraceList(
                req.pathVariable("postId"),                           // 글 ID
                req.queryParam("offset").orElse("10").toLong(), // offset : default 10
                req.queryParam("count").orElse("1").toLong(),   // count : default 1
                req.queryParam("q").orElse(""),                 // 검색 조건
                req.queryParam("startDate").orElse(""),         // 검색 시작 시간 기준
                req.queryParam("endDate").orElse("")            // 검색 종료 시간 기준
            )
        }.flatMap {
            logger.debug("SUCCESS - [PostHandler]getTraceList _ queryParams : ${req.queryParams()}")
            ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            logger.error("FAILURE - [PostHandler]getTraceList _ message : ${it.message.toString()} queryParams : ${req.queryParams()}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]getTraceList end | ${stopWatch.shortSummary()}")
        }
    }

    /**
     * 수정 이력 조회 - 11
     * @param postId, version
     * @return Trace
     */
    fun getTrace(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch("[PostHandler]getTrace")
        stopWatch.start("[PostHandler]getTrace start")
        logger.info("[PostHandler]getTrace start")

        return this.postService.getTrace(req.pathVariable("postId"), req.pathVariable("version")).flatMap {
            logger.debug("SUCCESS - [PostHandler]getTrace _ pathVariable : ${req.pathVariables()}")
            ok()
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(
            Mono.error(CustomException.NoTrace(req.pathVariable("postId"), req.pathVariable("version")))
        ).onErrorResume {
            logger.error("FAILURE - [PostHandler]getTrace _ message : ${it.message.toString()} pathVariable : ${req.pathVariables()}")
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("[PostHandler]getTrace end | ${stopWatch.shortSummary()}")
        }
    }
}