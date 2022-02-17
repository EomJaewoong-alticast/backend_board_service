package com.msp.board_service.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msp.board_service.common.CommonService
import com.msp.board_service.domain.Post
import com.msp.board_service.domain.Trace
import com.msp.board_service.domain.request.PostCreateRequest
import com.msp.board_service.domain.request.PostUpdateRequest
import com.msp.board_service.domain.response.PostIdResponse
import com.msp.board_service.domain.response.PostListResponse
import com.msp.board_service.domain.response.PostResponse
import com.msp.board_service.domain.response.TraceListResponse
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
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import reactor.core.publisher.Mono
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.validation.Validation
import kotlin.math.roundToInt

@Service
class PostService{
    private val logger = LoggerFactory.getLogger(this::class.java)  // logger
    private val mapper = jacksonObjectMapper()                      // json 직렬화를 위한 jackson Object Mapper

    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)  // Deserialize시 필드 제외 옵션
    }

    companion object {
        const val SEQUENCE_NAME_POST = "posts_sequence"
        const val SEQUENCE_NAME_TRACE = "traces_sequence"
    }

    @Autowired
    lateinit var redisTemplate: ReactiveRedisTemplate<String, String>

    @Autowired
    lateinit var postRepository: PostRepository

    @Autowired
    lateinit var traceRepository: TraceRepository

    @Autowired
    lateinit var commonService: CommonService

    /**
     * 게시글 목록 조회
     */
    fun getPostList(offset: Long, count: Long, q: String): Mono<PostListResponse> {
        logger.info("[PostService]getPostList start")

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
        val skip = Aggregation.skip((count-1)*offset)
        val cnt = Aggregation.limit(offset)
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
            "updatedAt",
            "showedAt"
        ))

        // Aggregations
        val countAgg = Aggregation.newAggregation(countAggOps)
        val listAgg = Aggregation.newAggregation(listAggOps)

        logger.info("query - aggregation: $listAgg")

        val stopWatch = StopWatch("[PostService]getPostList")
        stopWatch.start("Post Counting")

        return postRepository.findPostCount(countAgg).zipWhen{
            stopWatch.stop()
            logger.info("[PostService]getPostList - findPostCount Complete [count: ${it}]")
            stopWatch.start("Find Post list")
            postRepository.findPostList(listAgg).doOnNext {
                // 날짜들을 Timestamp -> 'yyyy-MM-dd HH:mm:ss' 형식으로 변환
                it.createdAt = timestampToLocaltime(it.createdAt)
                it.updatedAt = timestampToLocaltime(it.updatedAt)
                it.showedAt = timestampToLocaltime(it.showedAt)
            }.collectList()
        }.flatMap {
            logger.info("[PostService]getPostList - findPostList Complete")
            Mono.just(PostListResponse(it.t1, it.t2))
        }.doFinally{
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 글 생성
     */
    fun createPost(createdPost: PostCreateRequest, currentTime: String): Mono<PostIdResponse> {
        logger.info("[PostService]createPost start")
        val stopWatch = StopWatch("[PostService]createPost")
        stopWatch.start("Post Creating")

        // PostId sequence 생성 후 insert
        return this.commonService.generateSequence(SEQUENCE_NAME_POST).flatMap {
            logger.info("[PostService]createPost - generateSequence Complete [sequence: ${it}]")
            val post = createdPost.run {
                Post(
                    postId = it,
                    category = category,
                    title = title,
                    content = content,
                    author = author,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    showedAt = showedAt ?: currentTime,
                    delYn = false
                )
            }
            postRepository.insertPost(post).zipWhen {
                redisTemplate.opsForValue().set("post:${it.postId}", mapper.writeValueAsString(it))
            }.zipWhen {
                redisTemplate.expire("post:${it.t1.postId}", Duration.ofHours(1))
            }
        }.flatMap {
            logger.info("[PostService]createPost - insertPost Complete")
            Mono.just(PostIdResponse(it.t1.t1.postId))
        }.doFinally {
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * postId 기반 글 조회
     */
    fun getPost(postId: String): Mono<PostResponse> {
        logger.info("[PostService]getPost start")
        val stopWatch = StopWatch("[PostService]getPost")
        stopWatch.start("Post getting")

        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))

        return redisTemplate.opsForValue().get("post:${postId}").switchIfEmpty(
            postRepository.findPost(query).zipWhen {
                redisTemplate.opsForValue().set("post:${postId}", mapper.writeValueAsString(it)).flatMap {
                    redisTemplate.expire("post:${postId}", Duration.ofMinutes(30))
                }
            }.flatMap {
                Mono.just(mapper.writeValueAsString(it.t1))
            }
        ).flatMap {
            Mono.just(mapper.readValue(it, PostResponse::class.java).also{
                it.createdAt = timestampToLocaltime(it.createdAt)
                it.updatedAt = timestampToLocaltime(it.updatedAt)
                it.showedAt = timestampToLocaltime(it.showedAt)

                logger.info("[PostService]getPost - findPost Complete [postId: ${it.postId}]")
            })
        }.doFinally {
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * 글 업데이트
     */
    fun updatePost(postId: String, updatedPost: PostUpdateRequest): Mono<PostIdResponse> {
        logger.info("[PostService]updatePost start")

        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))
        val update = Update()

        val stopWatch = StopWatch("[PostService]updatePost")

        updatedPost.title?.let { update.set("title", updatedPost.title) }
        updatedPost.content?.let { update.set("content", updatedPost.content) }
        update.set("author", updatedPost.author)
        updatedPost.delYn?.let { update.set("delYn", updatedPost.delYn) }
        update.set("updatedAt", Timestamp.valueOf(LocalDateTime.now()).time.toString())

        stopWatch.start("updating post")
        return this.postRepository.updatePost(query, update).switchIfEmpty(
            // 바뀐 내용이 없다고 판단된 경우
            Mono.error<Post?>(CustomException.NotModified(postId)).also {
                logger.info("[PostService]updatePost - Nothing has changed")
            }
        ).zipWhen {
            stopWatch.stop()
            stopWatch.start("generating Sequence for trace")
            this.commonService.generateSequence(SEQUENCE_NAME_TRACE.plus("_").plus(postId))
        }.flatMap {
            logger.info("[PostService]updatePost - updatePost and generateSequence Complete " +
                    "[postId: ${it.t1.postId}, sequence: ${it.t2}]")
            stopWatch.stop()
            stopWatch.start("insert Trace")
            this.traceRepository.insertTrace(it.run {
                Trace(
                    version = t2,
                    title = t1.title,
                    content = t1.content,
                    editor = t1.author,
                    editedAt = t1.updatedAt,
                    postId = t1.postId
                )
            })
        }.zipWhen {
            redisTemplate.delete("post:${postId}")
        }.flatMap {
            Mono.just(PostIdResponse(it.t1.postId))
        }.doFinally {
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * postId 기반 글 삭제
     */
    fun deletePost(postId: String): Mono<PostIdResponse> {
        logger.info("[PostService]deletePost start")

        val query = Query().addCriteria(Criteria.where("postId").`is`(postId))
        val update: Update = Update().set("delYn", true)

        val stopWatch = StopWatch("[PostService]deletePost")
        stopWatch.start("Post delete")
        return postRepository.deletePost(query, update).zipWhen {
            redisTemplate.opsForValue().delete("post:${postId}")
        }.flatMap {
            if(it.t1.matchedCount > 0){    // post가 존재
                if(it.t1.modifiedCount > 0) {   // 지워지지 않은 post
                    redisTemplate.opsForValue().delete("post:${postId}").flatMap{
                        logger.info("[PostService]deletePost - deletePost Complete [postId: ${postId}]")
                        Mono.just(PostIdResponse(postId))
                    }
                } else {
                    logger.info("[PostService]deletePost - deletePost failure: AlreadyDeleted [postId: ${postId}]")
                    Mono.error(CustomException.AlreadyDeleted(postId))
                }
            } else {
                logger.info("[PostService]deletePost - deletePost failure: NoPost [postId: ${postId}]")
                Mono.error(CustomException.NoPost(postId))
            }
        }.doFinally {
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * postId 기반 수정이력 목록 조회
     */
    fun getTraceList(postId: String, offset: Long, count: Long, q: String, startDate: String, endDate: String): Mono<Any> {
        logger.info("[PostService]getTraceList start")

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
        val skip = Aggregation.skip((count - 1) * offset)
        val cnt = Aggregation.limit(offset)
        countAggOps.add(skip)
        countAggOps.add(cnt)
        listAggOps.add(skip)
        listAggOps.add(cnt)

        val key = StringBuilder()
            .append("trace:${postId}:${offset}:${count}")

        if(q.isNotBlank())          key.append(":${q}")
        if(startDate.isNotBlank()) {    // 시작일
            key.append(":${startDate}")

            val startMatch = Aggregation.match(Criteria.where("editedAt").gte(startDate))
            countAggOps.add(startMatch)
            listAggOps.add(startMatch)
        }

        if(endDate.isNotBlank()) {      // 종료일
            key.append(":${endDate}")

            val endMatch = Aggregation.match(Criteria.where("editedAt").lte(endDate))
            countAggOps.add(endMatch)
            listAggOps.add(endMatch)
        }

        // Aggregations
        val countAgg = Aggregation.newAggregation(countAggOps)
        val listAgg = Aggregation.newAggregation(listAggOps)

        val stopWatch = StopWatch("[PostService]getTraceList")
        stopWatch.start("find Post")
        return postRepository.findPostCount(Query()
            .addCriteria(Criteria.where("postId").`is`(postId))).flatMap {
            if(it == 0L)   // 해당 글이 없는 경우
                Mono.error<Any?>(CustomException.NoPost(postId)).also{
                    logger.info("[PostService]getTraceList - No Post [postId: ${postId}]")
                }
            else {
                stopWatch.stop()
                stopWatch.start("trace list count")

                redisTemplate.hasKey(key.toString()).flatMap { result ->
                    if(result) {
                        redisTemplate.opsForValue().get(key.toString()).flatMap { redisValue ->
                            Mono.just(mapper.readValue(redisValue, TraceListResponse::class.java))
                        }
                    }

                    else {
                        traceRepository.findTraceCount(countAgg).zipWhen {
                            stopWatch.stop()
                            stopWatch.start("find Trace list")
                            traceRepository.findTraceList(listAgg).doOnNext {
                                it.editedAt = timestampToLocaltime(it.editedAt)
                            }.collectList()
                        }.flatMap { chunk ->
                            logger.info("[PostService]getTraceList - findTraceCount and findTraceList Complete [count: ${chunk.t1}]")
                            val response = TraceListResponse(postId, chunk.t1, chunk.t2)
                            redisTemplate.opsForValue().set(key.toString(), mapper.writeValueAsString(response)).flatMap {
                                redisTemplate.expire(key.toString(), Duration.ofMinutes(10)).flatMap {
                                    Mono.just(response)
                                }
                            }
                        }
                    }
                }
            }.doFinally{
                stopWatch.stop()
                logger.info("total time: ${stopWatch.totalTimeMillis} ms")
                stopWatch.taskInfo.forEach { task ->
                    logger.info("name: ${task.taskName}, %: ${((task.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                            "elapsed: ${task.timeMillis} ms")
                }
            }
        }
    }

    /**
     * postId, version 기반 수정이력 조회
     */
    fun getTrace(postId: String, version: String): Mono<Trace> {
        logger.info("[PostService]getTrace start")
        val stopWatch = StopWatch("[PostService]getTrace")
        stopWatch.start("find Trace")

        val query = Query().addCriteria(Criteria
            .where("postId").`is`(postId)
            .and("version").`is`(version))
        return traceRepository.findTrace(query).doOnNext {
            it.editedAt = timestampToLocaltime(it.editedAt)
            logger.info("[PostService]getTrace - findTrace Complete [postId: ${postId}, version: $version")
        }.doFinally {
            stopWatch.stop()
            logger.info("total time: ${stopWatch.totalTimeMillis} ms")
            stopWatch.taskInfo.forEach {
                logger.info("name: ${it.taskName}, %: ${((it.timeNanos.toDouble()/stopWatch.totalTimeNanos)*100).roundToInt()}, " +
                        "elapsed: ${it.timeMillis} ms")
            }
        }
    }

    /**
     * q 조건식 해석
     */
    fun setQCriteria(q: String): Criteria? {
        val criteria = Criteria()

        if(q.isNotBlank()) {
            val qList = q.split(",")    // 하나의 식 단위로 분해
            val params = ArrayList<String>()
            var pos = -1
            val whereCriteria = ArrayList<Criteria>()

            // 검색 조건 조합을 식으로 분리
            qList.forEach {
                // 온전한 조건 식
                val regex1 = """.+%(eq|ne|lt|gt|ge|in|nin|like)\?[ㄱ-ㅎㅏ-ㅣ가-힣0-9a-zA-Z,]+"""
                    .trimMargin().toRegex()
                // 조건이 없는 평범한 문자열(value)
                val regex2 = """[ㄱ-ㅎㅏ-ㅣ가-힣0-9a-zA-Z]+""".trimMargin().toRegex()
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
                val param = it.split("%")
                if(param.size == 2) {
                    var paramName = param[0]
                    val paramValue = param[1].split("?")    // 조건과 값 분리
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
     * Lang 선택시 해당 값을 뽑아 줌
     */
    fun extractValueFromLang(lang: String, item: ArrayList<HashMap<String, String>>): Any {
        return when(lang) {
            "ko" -> item.first { it["lang"].equals("ko") }["value"]!!
            "en" -> item.first { it["lang"].equals("en") }["value"]!!
            else -> item
        }
    }

    /**
     * Timestamp -> LocalDataTime(yyyy-mm-dd HH:mm:ss)형태로 변경
     */
    fun timestampToLocaltime(timestamp: String): String =
        LocalDateTime.ofEpochSecond(timestamp.toLong(), 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))


    /**
     * 글 리스트 범위 validation
     */
    fun listRangeValidation(offset: Long, count: Long): Mono<Any> {
        lateinit var result: Mono<Any>

        // offset, count 범위를 벗어나는 경우
        if(offset <= 0) {
            result = Mono.error(CustomException.OffsetInvalid())
        } else if(count <= 0) {
            result = Mono.error(CustomException.CountInvalid())
        } else {
            result = Mono.just(true)
        }

        return result
    }

    /**
     * 글 생성 요청 validation
     */
    fun createdPostValidation(createdPost: PostCreateRequest, currentTime: String){
        val validate = Validation.buildDefaultValidatorFactory().validator.validate(createdPost)
        if(validate.isNotEmpty())
            throw CustomException.InValidValue(
                validate.first().invalidValue,
                validate.first().propertyPath,
                validate.first().message
            )

        // 노출 시간이 현재보다 과거인지 체크
        createdPost.showedAt?.let {
            if(createdPost.showedAt!! < currentTime)
                throw CustomException.IncorrectExposureTime()
        }
    }

    /**
     * 글 수정 요청 validation
     */
    fun updatedPostValidation(updatedPost: PostUpdateRequest) {
        val validate = Validation.buildDefaultValidatorFactory().validator.validate(updatedPost)
        // 변경사항이 없거나 작성자 누락, 삭제 여부 포맷이 맞지 않는 경우
        if(validate.isNotEmpty())
            throw CustomException.InValidValue(
                validate.first().invalidValue,
                validate.first().propertyPath,
                validate.first().message
            )
    }

    /**
     * 수정 이력 조건 validation
     */
    fun traceListValidation(offset: Long, count: Long,
                                     startDate: String, endDate: String): Mono<Any> {
        val currentTime = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC).toEpochSecond().toString()
        lateinit var result: Mono<Any>

        // offset, count 범위를 벗어나는 경우 OR 값이 누락된 경우
        if(offset <= 0) {
            result = Mono.error(CustomException.OffsetInvalid())
        } else if(count <= 0) {
            result = Mono.error(CustomException.CountInvalid())
        } else if(startDate.isNotBlank() && startDate >= currentTime) {    // 검색 시작일이 현재보다 미래일 경우
            result = Mono.error(CustomException.InvalidDate())
        } else if(endDate.isNotBlank() && startDate >= endDate) {  // 검색 종료일이 시작일보다 과거일 경우
            result = Mono.error(CustomException.InvalidDate())
        } else  result = Mono.just(true)

        return result
    }
}