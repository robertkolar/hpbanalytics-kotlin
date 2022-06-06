package com.highpowerbear.hpbanalytics.database

import com.highpowerbear.hpbanalytics.enums.Currency
import com.highpowerbear.hpbanalytics.enums.DataFilterOperator
import com.highpowerbear.hpbanalytics.enums.TradeStatus
import com.highpowerbear.hpbanalytics.enums.TradeType
import com.highpowerbear.hpbanalytics.model.DataFilterItem
import com.ib.client.Types.SecType
import org.springframework.data.jpa.domain.Specification
import java.text.MessageFormat
import java.time.LocalDateTime
import java.util.*
import java.util.function.Consumer
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

/**
 * Created by robertk on 4/19/2020.
 */
object DataFilters {

    fun executionSpecification(dataFilterItems: List<DataFilterItem>): Specification<Execution?> {
        return Specification { root: Root<Execution>, _: CriteriaQuery<*>?, builder: CriteriaBuilder ->
            build(
                root,
                builder,
                dataFilterItems
            )
        }
    }

    fun tradeSpecification(dataFilterItems: List<DataFilterItem>): Specification<Trade?> {
        return Specification { root: Root<Trade>, _: CriteriaQuery<*>?, builder: CriteriaBuilder ->
            build(
                root,
                builder,
                dataFilterItems
            )
        }
    }

    fun tradeSpecification(tradeType: TradeType?, secType: SecType?, currency: Currency?, underlying: String?, cutoffDate: LocalDateTime?): Specification<Trade?> {
        return Specification { root: Root<Trade?>, _: CriteriaQuery<*>?, builder: CriteriaBuilder ->
            val predicates: MutableList<Predicate> = ArrayList()
            if (tradeType != null) {
                predicates.add(builder.equal(root.get<Any>("type"), tradeType))
            }
            if (secType != null) {
                predicates.add(builder.equal(root.get<Any>("secType"), secType))
            }
            if (currency != null) {
                predicates.add(builder.equal(root.get<Any>("currency"), currency))
            }
            if (underlying != null) {
                predicates.add(builder.equal(root.get<Any>("underlying"), underlying))
            }
            if (cutoffDate != null) {
                predicates.add(
                    builder.or(
                        builder.greaterThanOrEqualTo(root.get("openDate"), cutoffDate),
                        builder.greaterThanOrEqualTo(root.get("closeDate"), cutoffDate),
                        builder.isNull(root.get<Any>("closeDate"))
                    )
                )
            }
            builder.and(*predicates.toTypedArray())
        }
    }

    private fun <R> build(root: Root<R>, builder: CriteriaBuilder, dataFilterItems: List<DataFilterItem>): Predicate {
        val outerAndPredicates = mutableListOf<Predicate>()
        val innerOrPredicates = mutableListOf<Predicate>()

        for (item in dataFilterItems) {
            val operator = DataFilterOperator.valueOf(item.operator!!.uppercase(Locale.getDefault()))
            val field = item.property
            when (operator) {
                DataFilterOperator.LIKE -> when (field) {
                    "symbol", "underlying" -> {
                        val likeStr = MessageFormat.format("%{0}%", item.value)
                        outerAndPredicates.add(builder.like(root.get(field), likeStr))
                    }
                }
                DataFilterOperator.EQ -> if ("multiplier" == field) {
                    outerAndPredicates.add(builder.equal(root.get<String>(field), builder.literal(item.doubleValue)))
                }
                DataFilterOperator.LT -> if ("multiplier" == field) {
                    innerOrPredicates.add(builder.lessThan(root.get(field), builder.literal<Double>(item.doubleValue)))
                }
                DataFilterOperator.GT -> if ("multiplier" == field) {
                    innerOrPredicates.add(builder.greaterThan(root.get(field), builder.literal<Double>(item.doubleValue)))
                }
                DataFilterOperator.IN -> when (field) {
                    "currency" -> {
                        val inPredicate = builder.`in`(root.get<Currency>(field))
                        item.getValues().forEach(Consumer { value: String? ->
                            inPredicate.value(
                                Currency.valueOf(
                                    value!!
                                )
                            )
                        })
                        outerAndPredicates.add(inPredicate)
                    }
                    "secType" -> {
                        val inPredicate = builder.`in`(root.get<SecType>(field))
                        item.getValues().forEach(Consumer { value: String? ->
                            inPredicate.value(
                                SecType.valueOf(
                                    value!!
                                )
                            )
                        })
                        outerAndPredicates.add(inPredicate)
                    }
                    "status" -> {
                        val inPredicate = builder.`in`(root.get<TradeStatus>(field))
                        item.getValues().forEach(Consumer { value: String? ->
                            inPredicate.value(
                                TradeStatus.valueOf(
                                    value!!
                                )
                            )
                        })
                        outerAndPredicates.add(inPredicate)
                    }
                }
            }
        }
        if (innerOrPredicates.isNotEmpty()) {
            outerAndPredicates.add(builder.or(*innerOrPredicates.toTypedArray()))
        }
        return builder.and(*outerAndPredicates.toTypedArray())
    }
}
