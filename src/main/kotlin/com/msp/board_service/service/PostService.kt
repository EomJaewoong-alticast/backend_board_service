package com.msp.board_service.service

import com.mongodb.client.result.UpdateResult
import com.msp.board_service.common.CommonService
import com.msp.board_service.domain.Post
import com.msp.board_service.domain.Trace
import com.msp.board_service.domain.request.PostCreateRequest
import com.msp.board_service.domain.request.PostUpdateRequest
import com.msp.board_service.domain.response.PostIdResponse
import com.msp.board_service.domain.response.PostListResponse
import com.msp.board_service.domain.response.TraceListResponse
import com.msp.board_service.domain.response.TraceVersionResponse
import com.msp.board_service.exception.CustomException
import com.msp.board_service.repository.PostRepository
import com.msp.board_service.repository.TraceRepository
import com.msp.board_service.utils.MakeWhereCriteria
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

interface PostServiceIn {
    fun getPostList(offset: Long, count: Long, q: String): Mono<Any>
    fun createPost(createdPost: PostCreateRequest): Mono<PostIdResponse>
    fun getPost(postId: String): Mono<Post>
    fun updatePost(postId: String, updatedPost: PostUpdateRequest): Mono<Any>
    fun deletePost(postId: String): Mono<Any>

    fun getTraceList(postId: String, offset: Long, count: Long, q: String, startDate: String, endDate: String): Mono<Any>
    fun getTrace(postId: String, version: String): Mono<Trace>
}

@Service
class PostService: PostServiceIn {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val SEQUENCE_NAME_POST = "posts_sequence"
        const val SEQUENCE_NAME_TRACE = "traces_sequence"
    }

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var traceRepository: TraceRepository

    @Autowired
    lateinit var commonService: CommonService

    /**
     * 게시글 목록 조회
     */
    override fun getPostList(offset: Long, count: Long, q: String): Mono<Any> {
        // offset, count 범위를 벗어나는 경우 OR 값이 누락된 경우
        if(offset <= 0) {
            return Mono.error(CustomException.OffsetInvalid())
        }
        if(count <= 0) {
            return Mono.error(CustomException.CountInvalid())
        }

        // Aggregation Operation 집합
        val countAggOps = ArrayList<AggregationOperation>()
        val listAggOps = ArrayList<AggregationOperation>()

        // Criteria
        val match = Aggregation.match(
            setQCriteria(q)?: return Mono.error(CustomException.QueryNotMatched(q))
        )
        countAggOps.add(match)
        listAggOps.add(match)

        // paging
        val skip = Aggregation.skip((count-1).toLong()*offset)
        val cnt = Aggregation.limit(offset.toLong())
        countAggOps.add(skip)
        countAggOps.add(cnt)
        listAggOps.add(skip)
        listAggOps.add(cnt)

        // project 설정
        listAggOps.add(Aggregation.project(
            "postId",
            "category",
            "title",
            "author",
            "createdAt",
            "showedAt"
        ))

        // Aggregations
        val countAgg = Aggregation.newAggregation(countAggOps)
        val listAgg = Aggregation.newAggregation(listAggOps)

        return postRepository.findPostCount(countAgg).flatMap { total ->
            postRepository.findPostList(listAgg).doOnNext {
                // 날짜들을 Timestamp -> 'yyyy-MM-dd HH:mm:ss' 형식으로 변환
                it.createdAt = TimestampToLocaltime(it.createdAt)
                it.updatedAt?.run {
                    it.updatedAt = TimestampToLocaltime(it.updatedAt!!)
                }
                it.showedAt = TimestampToLocaltime(it.showedAt)
            }.collectList().flatMap { posts ->
                Mono.just(PostListResponse(total, posts))
            }
        }
    }

    /**
     * 글 생성
     */
    override fun createPost(createdPost: PostCreateRequest): Mono<PostIdResponse> {
        // 입력할 값이 존재하는지 체크
        if(createdPost.category.isNullOrEmpty() || createdPost.title.isNullOrEmpty()
            || createdPost.content.isNullOrEmpty() || createdPost.author.isNullOrEmpty())
            return Mono.error(CustomException.InValidValue())

        // 노출 시간이 현재보다 과거인지 체크
        val currentTime = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        if(createdPost?.showedAt != null && createdPost.showedAt!! < currentTime) {
            return Mono.error(CustomException.IncorrectExposureTime())
        }

        // PostId sequence 생성 후 insert
        return this.commonService.generateSequence(SEQUENCE_NAME_POST).flatMap {
            val post = Post(
                postId = it,
                category = createdPost.category!!,
                title = createdPost.title!!,
                content = createdPost.content!!,
                author = createdPost.author!!,
                createdAt = currentTime,
                showedAt = createdPost?.showedAt ?: currentTime
            )
            postRepository.insertPost(post)
        }.flatMap {
            Mono.just(PostIdResponse(it.postId))
        }
    }

    /**
     * postId 기반 글 조회
     */
    override fun getPost(postId: String): Mono<Post> {
        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))
        return postRepository.findPost(query)
    }

    /**
     * 글 업데이트
     */
    override fun updatePost(postId: String, updatedPost: PostUpdateRequest): Mono<Any> {
        // 작성자가 없거나, 제목과 내용이 없는 경우(변경사항 x)
        if(updatedPost.author.isNullOrEmpty())
            return Mono.error(CustomException.InValidValue())

        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))
        val update: Update = Update()

        return postRepository.findPost(query).switchIfEmpty(
            Mono.error(CustomException.NoPost(postId))
        ).flatMap { post ->
            // 바뀐 내용이 없다고 판단된 경우
            if((updatedPost.title.isNullOrEmpty() && updatedPost.content.isNullOrEmpty()) ||
               (updatedPost.title.isNullOrEmpty() && updatedPost.content.equals(post.content)) ||
               (updatedPost.content.isNullOrEmpty() && updatedPost.title!!.containsAll(post.title)) ||
               (updatedPost.content.equals(post.content) && updatedPost.title!!.containsAll(post.title)))
                Mono.error(CustomException.NotModified(postId))

            else {
                this.commonService.generateSequence(SEQUENCE_NAME_TRACE.plus("_").plus(postId)).
                flatMap { seq ->
                    this.traceRepository.insertTrace(Trace(
                        version = seq,
                        title = post.title,
                        content = post.content,
                        editor = post.author,
                        editedAt = post?.updatedAt ?: post.createdAt,
                        postId = post.postId
                    )).flatMap {
                        updatedPost.title?.let { update.set("title", updatedPost.title) }
                        updatedPost.content?.let { update.set("content", updatedPost.content) }
                        update.set("author", updatedPost.author)
                        updatedPost.delYn?.let { update.set("delYn", updatedPost.delYn) }
                        update.set("updatedAt", Timestamp.valueOf(LocalDateTime.now()).time)
                        postRepository.updatePost(query, update)
                    }.flatMap {
                        Mono.just(TraceVersionResponse(postId, seq))
                    }
                }
            }
        }
    }

    /**
     * postId 기반 글 삭제
     */
    override fun deletePost(postId: String): Mono<Any> {
        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))
        val update: Update = Update()
        update.set("delYn", true)

        return postRepository.deletePost(query, update).flatMap {
            if(it.matchedCount > 0){    // post가 존재
                if(it.modifiedCount > 0)    // 지워지지 않은 post
                    Mono.just(PostIdResponse(postId))
                else Mono.error(CustomException.AlreadyDeleted(postId))
            }
            else Mono.error(CustomException.NoPost(postId))
        }
    }

    /**
     * postId 기반 수정이력 목록 조회
     */
    override fun getTraceList(postId: String, offset: Long, count: Long, q: String, startDate: String, endDate: String): Mono<Any> {
        // offset, count 범위를 벗어나는 경우 OR 값이 누락된 경우
        if(offset <= 0) {
            return Mono.error(CustomException.OffsetInvalid())
        }
        if(count <= 0) {
            return Mono.error(CustomException.CountInvalid())
        }

        val countAggOps = ArrayList<AggregationOperation>()
        val listAggOps = ArrayList<AggregationOperation>()

        // Criteria
        val matchId = Aggregation.match(Criteria.where("postId").`is`(postId))
        val match = Aggregation.match(
            setQCriteria(q)?: return Mono.error(CustomException.QueryNotMatched(q))
        )
        countAggOps.add(matchId)
        countAggOps.add(match)
        listAggOps.add(matchId)
        listAggOps.add(match)

        // paging
        val skip = Aggregation.skip((count - 1).toLong() * offset)
        val cnt = Aggregation.limit(offset.toLong())
        countAggOps.add(skip)
        countAggOps.add(cnt)
        listAggOps.add(skip)
        listAggOps.add(cnt)

        val currentTime = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        if(startDate.isNotBlank() && startDate.isNotEmpty()) { // 시작일 조건
            if(startDate >= currentTime)    // 검색 시작일이 현재보다 미래일 경우
                return Mono.error(CustomException.InvalidDate())

            val startMatch = Aggregation.match(Criteria.where("editedAt").gte(startDate))
            countAggOps.add(startMatch)
            listAggOps.add(startMatch)
        }
        if(endDate.isNotBlank() && endDate.isNotEmpty()) { // 종료일 조건
            if(startDate >= endDate)    // 검색 종료일이 시작일보다 과거일 경우
                return Mono.error(CustomException.InvalidDate())

            val endMatch = Aggregation.match(Criteria.where("editedAt").lte(endDate))
            countAggOps.add(endMatch)
            listAggOps.add(endMatch)
        }

        // Aggregations
        val countAgg = Aggregation.newAggregation(countAggOps)
        val listAgg = Aggregation.newAggregation(listAggOps)

        return postRepository.findPostCount(Query()
            .addCriteria(Criteria.where("postId").`is`(postId))).flatMap { cnt ->
            if(cnt == 0L)   // 해당 글이 없는 경우
                Mono.error(CustomException.NoPost(postId))

            else {
                traceRepository.findTraceCount(countAgg).flatMap { total ->
                    traceRepository.findTraceList(listAgg).doOnNext {
                        it.editedAt = TimestampToLocaltime(it.editedAt)
                    }.collectList().flatMap { traces ->
                        Mono.just(TraceListResponse(postId, total, traces))
                    }
                }
            }
        }
    }

    /**
     * postId, version 기반 수정이력 조회
     */
    override fun getTrace(postId: String, version: String): Mono<Trace> {
        logger.info("[PostService][getTrace]")
        val query = Query().addCriteria(Criteria
            .where("postId").`is`(postId)
            .and("version").`is`(version))
        return traceRepository.findTrace(query)
    }

    /**
     * q 조건식 해석
     */
    fun setQCriteria(q: String): Criteria? {
        var criteria = Criteria()

        if(!q.isNullOrEmpty()) {
            var qList = q.split(",")    // 하나의 식 단위로 분해
            var params = ArrayList<String>()
            var pos = -1
            var whereCriteria = ArrayList<Criteria>()

            // 검색 조건 조합을 식으로 분리
            qList.forEach {
                // 온전한 조건 식
                var regex1 = """.+%(eq|ne|lt|gt|ge|in|nin|like)\?[ㄱ-ㅎㅏ-ㅣ가-힣0-9a-zA-Z,]+"""
                    .trimMargin().toRegex()
                // 조건이 없는 평범한 문자열(value)
                var regex2 = """[ㄱ-ㅎㅏ-ㅣ가-힣0-9a-zA-Z]+""".trimMargin().toRegex()
                // 조건에 맞지 않는 식이 존재
                if(!(it.matches(regex1) || it.matches(regex2)))
                    return null

                if(it.indexOf("%") > 0) {   // % 구분 연산자 찾기
                    params.add(it)
                    pos++
                } else {
                    if(pos == -1)   return null
                    params[pos] = params[pos] + "," + it
                }
            }

            params.forEach {
                var param = it.split("%")
                if(param.size == 2) {
                    var paramName = param[0]
                    var paramValue = param[1].split("?")    // 조건과 값 분리
                    if(paramValue.size == 2) {
                        var valueType = "string"

                        // title 내의 value들을 대상으로  검색
                        if(StringUtils.equalsAnyIgnoreCase(paramName, "title")) {
                            paramName = "title.value"
                        }

                        // paramName과 하나라도 일치하는 String이 있으면 적용
                        if(StringUtils.equalsAnyIgnoreCase(paramName, "createdAt", "updatedAt"
                                , "showedAt")) {
                            valueType = "long"
                        } else if(StringUtils.equalsAnyIgnoreCase(paramName, "delYn")) {
                            valueType = "boolean"
                        }

                        whereCriteria.add(MakeWhereCriteria.makeWhereCriteria(
                            paramName,      // 필드명
                            paramValue[0],  // 조건
                            paramValue[1],  // value(s)
                            valueType
                        ))
                    }
                }
            }

            // * : spread operator
            criteria.andOperator(*whereCriteria.toTypedArray())
        }

        return criteria
    }

    /**
     * Timestamp -> LocalDataTime(yyyy-mm-dd HH:mm:ss)형태로 변경
     */
    fun TimestampToLocaltime(timestamp: String) =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.toLong()),
            TimeZone.getDefault().toZoneId()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()
}