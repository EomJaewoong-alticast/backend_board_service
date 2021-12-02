package com.msp.board_service.common

import com.msp.board_service.domain.Sequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CommonService(private val template: ReactiveMongoTemplate) {
    /**
     * 해당 sequence name에 맞는 sequence 생성
     *
     * 현재 sequence 값에 +1을 하여 준다
     */
    fun generateSequence(seqName: String): Mono<String> {
        return template.findAndModify(Query().addCriteria(Criteria.where("_id").`is`(seqName))
            ,Update().inc("seq", 1), FindAndModifyOptions.options().returnNew(true).upsert(true),
            Sequence::class.java).flatMap {
                Mono.just((it?.seq ?: 1).toString())
        }
    }
}