package com.msp.board_service.repository

import com.mongodb.client.result.UpdateResult
import com.msp.board_service.domain.Post
import com.msp.board_service.domain.response.ListInPostResponse
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

//TODO: DB logging

@Repository
class PostRepository(private val template: ReactiveMongoTemplate) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val COLLECTION_NM = "posts"
    }

    /**
     * 게시글 카운트 조회 - Query
     */
    fun findPostCount(query: Query): Mono<Long> =
        template.count(query, COLLECTION_NM)

    /**
     * 게시글 카운트 조회 - Aggregation
     */
    fun findPostCount(agg: Aggregation): Mono<Long> =
        template.aggregate(agg, COLLECTION_NM, ListInPostResponse::class.java).count()

    /**
     * 게시글 목록 조회 - Aggregation
     */
    fun findPostList(agg: Aggregation): Flux<ListInPostResponse> =
        template.aggregate(agg, COLLECTION_NM, ListInPostResponse::class.java)

    /**
     * 게시글 삽입
     */
    fun insertPost(post: Post): Mono<Post> =
        template.insert(post, COLLECTION_NM)

    /**
     * 게시글 조회 - Query
     */
    fun findPost(query: Query): Mono<Post> =
        template.findOne<Post>(query, COLLECTION_NM)

    /**
     * 게시글 수정 - Query, update
     */
    fun updatePost(query: Query, update: Update): Mono<Post> =
        template.findAndModify(query, update, Post::class.java)

    /**
     * 게시글 삭제 - Query, update
     */
    fun deletePost(query: Query, update: Update):Mono<UpdateResult> =
        template.updateFirst(query, update, COLLECTION_NM)
}