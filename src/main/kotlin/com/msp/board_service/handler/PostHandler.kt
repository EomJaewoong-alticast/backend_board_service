package com.msp.board_service.handler

import com.msp.board_service.domain.request.PostCreateRequest
import com.msp.board_service.domain.request.PostUpdateRequest
import com.msp.board_service.domain.response.Response
import com.msp.board_service.exception.CustomException
import com.msp.board_service.service.PostService
import com.msp.board_service.utils.LogFormatterHelper
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono

@Component
class PostHandler(val postService: PostService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 글 목록 조회 - 01
     * @param offset, count, q
     * @return PostListResponse
     */
    fun getPostList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return this.postService.getPostList(
            req.queryParam("offset").orElse("10").toLong(), // offset : default 10
            req.queryParam("count").orElse("1").toLong(),   // count : default 1
            req.queryParam("q").orElse("")                  // 검색 조건
        ).flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "getPostList",
                "board-service",
                "offset: ${req.queryParam("offset")}, count: ${req.queryParam("count")}, q: ${req.queryParam("q")}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "getPostList",
                "board-service",
                "${it.message.toString()} - query : ${req.queryParams()}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * 글 생성 - 02
     * @param PostCreateRequest
     * @return postIdResponse
     */
    fun createPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return req.bodyToMono(PostCreateRequest::class.java).flatMap {
            this.postService.createPost(it)
        }.flatMap {
            val msg = LogFormatterHelper.getSuccessLog(
                stopWatch,
                "createPost",
                "board-service",
                "body: ${req.bodyToMono(PostCreateRequest::class.java)}"
            )
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "post created", it)))
        }.onErrorResume {
            val msg = LogFormatterHelper.getFailLog(
                stopWatch,
                "createPost",
                "board-service",
                "${it.message.toString()} - body: ${req.bodyToMono(PostCreateRequest::class.java)}"
            )
            logger.error(msg)
            when (it) {
                is CustomException -> {
                    ServerResponse.badRequest()
                        .body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * postId로 글 조회 -03
     * @param postId
     * @return Post
     */
    fun getPost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return this.postService.getPost(req.pathVariable("postId")).flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "getPost",
                "board-service",
                "path: ${req.pathVariables()}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(    // 요청온 postId로 조회된 글이 없는 경우 예
            Mono.error(CustomException.NoPost(req.pathVariable("postId")))
        ).onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "getPost",
                "board-service",
                "${it.message.toString()} - path: ${req.pathVariables()}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * 글 수정 - 04
     * @param postId, PostUpdateRequest
     * @return postIdResponse
     */
    fun updatePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return req.bodyToMono(PostUpdateRequest::class.java).flatMap {
            this.postService.updatePost(req.pathVariable("postId"), it)
        }.flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "updatePost",
                "board-service",
                "path: ${req.pathVariables()}, body: ${req.bodyToMono(PostUpdateRequest::class.java)}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "post updated", it)))
        }.onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "updatePost",
                "board-service",
                "${it.message.toString()} - path: ${req.pathVariables()}, body: ${req.bodyToMono(PostUpdateRequest::class.java)}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * 글 삭제 - 05
     * @param postId
     */
    fun deletePost(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return this.postService.deletePost(req.pathVariable("postId")).flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "deletePost",
                "board-service",
                "path: ${req.pathVariables()}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "post deleted", it)))
        }.onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "deletePost",
                "board-service",
                "${it.message.toString()} - path: ${req.pathVariables()}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * 수정 이력 목록 조회 - 10
     * @param postId
     * @return TraceListResponse
     */
    fun getTraceList(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return this.postService.getTraceList(
            req.pathVariable("postId"),                           // 글 ID
            req.queryParam("offset").orElse("10").toLong(), // offset : default 10
            req.queryParam("count").orElse("1").toLong(),   // count : default 1
            req.queryParam("q").orElse(""),                 // 검색 조건
            req.queryParam("startDate").orElse(""),         // 검색 시작 시간 기준
            req.queryParam("endDate").orElse("")            // 검색 종료 시간 기준
        ).flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "deletePost",
                "board-service",
                "path: ${req.pathVariables()}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "OK", it)))
        }.onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "getTraceList",
                "board-service",
                "${it.message.toString()} - path: ${req.pathVariables()}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }

    /**
     * 수정 이력 조회 - 11
     * @param postId, version
     * @return Trace
     */
    fun getTrace(req: ServerRequest): Mono<ServerResponse> {
        // 시간 체크 시작
        val stopWatch = StopWatch()
        stopWatch.start()

        return this.postService.getTrace(req.pathVariable("postId"), req.pathVariable("version")).flatMap {
            val msg = LogFormatterHelper.getSuccessLog(stopWatch,
                "getTrace",
                "board-service",
                "path: ${req.pathVariables()}")
            logger.debug(msg)
            ok().body(Mono.just(Response(200, "OK", it)))
        }.switchIfEmpty(
            Mono.error(CustomException.NoTrace(req.pathVariable("postId"), req.pathVariable("version")))
        ).onErrorResume {
            val msg = LogFormatterHelper.getFailLog(stopWatch,
                "getTrace",
                "board-service",
                "${it.message.toString()} - path: ${req.pathVariables()}")
            logger.error(msg)
            when(it) {
                is CustomException -> {
                    ServerResponse.badRequest().body(Mono.just(Response(it.errorCode, it.message.toString(), null)))
                }
                else -> ok().body(Mono.just(Response(500, it.message.toString(), null)))
            }
        }
    }
}