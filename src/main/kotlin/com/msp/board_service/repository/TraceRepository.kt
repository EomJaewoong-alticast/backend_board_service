package com.msp.board_service.repository

import com.msp.board_service.domain.Trace
import com.msp.board_service.domain.response.ListInTraceResponse
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class TraceRepository(private val template: ReactiveMongoTemplate) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val COLLECTION_NM = "traces"
    }

    /**
     * 수정 이력 카운트 - Aggregation
     */
    fun findTraceCount(agg: Aggregation): Mono<Long> {
        logger.info("[TraceRepository][findTraceCount]")
        return template.aggregate(agg, COLLECTION_NM, Any::class.java).count()
    }

    /**
     * 수정 이력 목록 조회 - Aggregation
     */
    fun findTraceList(agg: Aggregation): Flux<ListInTraceResponse> {
        logger.info("[TraceRepository][findTraceList]")
        return template.aggregate(agg, COLLECTION_NM, ListInTraceResponse::class.java)
    }

    /**
     * 수정 이력 조회 - Query
     */
    fun findTrace(query: Query): Mono<Trace> {
        logger.info("[TraceRepository][getTrace]")
        return template.findOne(query, COLLECTION_NM)
    }

    /**
     * 수정 이력 삽입
     */
    fun insertTrace(trace: Trace): Mono<Trace> {
        logger.info("[TraceRepository][insertTrace]")
        return template.insert(trace, COLLECTION_NM)
    }
}