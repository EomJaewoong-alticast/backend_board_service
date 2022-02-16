package com.msp.board_service.router

import com.msp.board_service.handler.PostHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

/**
 * PostRouter
 */
@Component
class PostRouter() {
    @Bean
    fun postRoutes(postHandler: PostHandler) = router {
        accept(MediaType.APPLICATION_JSON).nest {
            "v1.0".nest {
                GET("/posts", postHandler::getPostList)             // 게시글 목록 조회
                POST("/posts", postHandler::createPost)             // 게시글 등록
                GET("/posts/{postId}", postHandler::getPost)        // 게시글 조회
                PATCH("/posts/{postId}", postHandler::updatePost)   // 게시글 수정
                DELETE("/posts/{postId}", postHandler::deletePost)  // 게시글 삭제

                GET("posts/{postId}/traces", postHandler::getTraceList)         // 수정 이력 목록 조회
                GET("posts/{postId}/traces/{version}", postHandler::getTrace)   // 수정 이력 조회
            }
        }
    }
}