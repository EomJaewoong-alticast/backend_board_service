package com.msp.board_service.utils

import com.msp.board_service.exception.CustomException
import org.springframework.data.mongodb.core.query.Criteria
import java.util.*

class MakeWhereCriteria {

    companion object {
        /**
         * 지정된 조건을 바탕으로 criteria 생성
         *
         * 한 단위의 조건식을 분해하여 조건을 완성
         *
         * @param param, expm valuem valueType
         * @return Criteria
         */
        fun makeWhereCriteria(param: String, exp: String, value: String, valueType: String = "string"): Criteria {
            val default = Criteria.where("postId").exists(false)

            return when(exp) {
                "eq" -> {           // 일치한다
                    when(valueType){
                        "string" -> {
                            if(value == "null") {
                                Criteria.where(param).`in`(null, "", "null")
                            } else {
                                Criteria.where(param).`is`(value)
                            }
                        }
                        "int" -> {
                            if(value == "null") {
                                Criteria.where(param).`is`(null)
                            } else {
                                Criteria.where(param).`is`(value.toInt())
                            }
                        }
                        "long" -> {
                            if(value == "null") {
                                Criteria.where(param).`is`(null)
                            } else {
                                Criteria.where(param).`is`(value.toLong())
                            }
                        }
                        "float" -> {
                            if(value == "null") {
                                Criteria.where(param).`is`(null)
                            } else {
                                Criteria.where(param).`is`(value.toFloat())
                            }
                        }
                        "double" -> {
                            if(value == "null") {
                                Criteria.where(param).`is`(null)
                            } else {
                                Criteria.where(param).`is`(value.toDouble())
                            }
                        }
                        "boolean" -> {
                            when(value) {
                                "true" -> Criteria.where(param).`is`(true)
                                "false" -> Criteria.where(param).`is`(false)
                                else -> default
                            }
                        }
                        else -> default
                    }
                }
                "ne" -> {   // 일치하지 않는다
                    when(valueType) {
                        "string" -> {
                            if(value == "null") {
                                Criteria.where(param).`nin`(null, "", "null")
                            } else {
                                Criteria.where(param).ne(value)
                            }
                        }
                        "int" -> {
                            if(value == null) {
                                Criteria.where(param).ne(null)
                            } else {
                                Criteria.where(param).ne(value.toInt())
                            }
                        }
                        "long" -> {
                            if(value == null) {
                                Criteria.where(param).ne(null)
                            } else {
                                Criteria.where(param).ne(value.toLong())
                            }
                        }
                        "float" -> {
                            if(value == null) {
                                Criteria.where(param).ne(null)
                            } else {
                                Criteria.where(param).ne(value.toFloat())
                            }
                        }
                        "double" -> {
                            if(value == null) {
                                Criteria.where(param).ne(null)
                            } else {
                                Criteria.where(param).ne(value.toDouble())
                            }
                        }
                        "boolean" -> {
                            when(value) {
                                "true" -> Criteria.where(param).ne(true)
                                "false" -> Criteria.where(param).ne(false)
                                else -> default
                            }
                        }
                        else -> default
                    }
                }
                "lt" -> {   // 값보다 작은것
                    when(valueType) {
                        "string" -> {
                            Criteria.where(param).lt(value)
                        }
                        "int" -> {
                            Criteria.where(param).lt(value.toInt())
                        }
                        "long" -> {
                            Criteria.where(param).lt(value.toLong())
                        }
                        "float" -> {
                            Criteria.where(param).lt(value.toFloat())
                        }
                        "double" -> {
                            Criteria.where(param).lt(value.toDouble())
                        }
                        else -> default
                    }
                }
                "le" -> {   // 값보다 같거나 작은것
                    when(valueType) {
                        "string" -> {
                            Criteria.where(param).lte(value)
                        }
                        "int" -> {
                            Criteria.where(param).lte(value.toInt())
                        }
                        "long" -> {
                            Criteria.where(param).lte(value.toLong())
                        }
                        "float" -> {
                            Criteria.where(param).lte(value.toFloat())
                        }
                        "double" -> {
                            Criteria.where(param).lte(value.toDouble())
                        }
                        else -> default
                    }
                }
                "gt" -> {   // 값보다 큰 것
                    when(valueType) {
                        "string" -> {
                            Criteria.where(param).gt(value)
                        }
                        "int" -> {
                            Criteria.where(param).gt(value.toInt())
                        }
                        "long" -> {
                            Criteria.where(param).gt(value.toLong())
                        }
                        "float" -> {
                            Criteria.where(param).gt(value.toFloat())
                        }
                        "double" -> {
                            Criteria.where(param).gt(value.toDouble())
                        }
                        else -> default
                    }
                }
                "ge" -> {   // 값보다 같거나 큰 것
                    when(valueType) {
                        "string" -> {
                            Criteria.where(param).gte(value)
                        }
                        "int" -> {
                            Criteria.where(param).gte(value.toInt())
                        }
                        "long" -> {
                            Criteria.where(param).gte(value.toLong())
                        }
                        "float" -> {
                            Criteria.where(param).gte(value.toFloat())
                        }
                        "double" -> {
                            Criteria.where(param).gte(value.toDouble())
                        }
                        else -> default
                    }
                }
                "like" -> { // 값과 일부 일치함
                    when(valueType) {
                        "string" -> {
                            Criteria.where(param).regex(".*$value.*", "i")
                        }
                        else -> default
                    }
                }
                "in" -> {   // 값들이 포함된 것
                    // value가 여러개이므로 다시 자른다
                    when (valueType) {
                        "string" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                Criteria().orOperator(
                                    Criteria.where(param).`in`(values),
                                    Criteria.where(param).`in`(listOf(null, "", Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).`in`(values)
                            }
                        }
                        "int" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).`in`(valuesExclNull.map { it.toInt() }),
                                    Criteria.where(param).`in`(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).`in`(values.map { it.toInt() })
                            }
                        }
                        "long" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).`in`(valuesExclNull.map { it.toLong() }),
                                    Criteria.where(param).`in`(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).`in`(values.map { it.toLong() })
                            }
                        }
                        "float" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).`in`(valuesExclNull.map { it.toFloat() }),
                                    Criteria.where(param).`in`(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).`in`(values.map { it.toFloat() })
                            }
                        }
                        "double" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).`in`(valuesExclNull.map { it.toDouble() }),
                                    Criteria.where(param).`in`(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).`in`(values.map { it.toDouble() })
                            }
                        }
                        else -> default
                    }
                }
                "nin" -> {  // 값들이 포함되지 않은 것
                    when (valueType) {
                        "string" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                Criteria().orOperator(
                                    Criteria.where(param).nin(values),
                                    Criteria.where(param).nin(listOf(null, "", Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).nin(values)
                            }
                        }
                        "int" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).nin(valuesExclNull.map { it.toInt() }),
                                    Criteria.where(param).nin(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).nin(values.map { it.toInt() })
                            }
                        }
                        "long" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).nin(valuesExclNull.map { it.toLong() }),
                                    Criteria.where(param).nin(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).nin(values.map { it.toLong() })
                            }
                        }
                        "float" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).nin(valuesExclNull.map { it.toFloat() }),
                                    Criteria.where(param).nin(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).nin(values.map { it.toFloat() })
                            }
                        }
                        "double" -> {
                            val values = value.replace(" ", "").split(",")
                            if ("null" in values) {
                                val valuesExclNull = values.filterNot { it == "null" }
                                Criteria().orOperator(
                                    Criteria.where(param).nin(valuesExclNull.map { it.toDouble() }),
                                    Criteria.where(param).nin(listOf(null, Collections.EMPTY_LIST))
                                )
                            } else {
                                Criteria.where(param).nin(values.map { it.toDouble() })
                            }
                        }
                        else -> default
                    }
                }
                else -> default
            }
        }
    }
}