package com.highpowerbear.hpbanalytics.database;

import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.enums.DataFilterOperator;
import com.highpowerbear.hpbanalytics.enums.TradeStatus;
import com.highpowerbear.hpbanalytics.enums.TradeType;
import com.highpowerbear.hpbanalytics.model.DataFilterItem;
import com.ib.client.Types;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robertk on 4/19/2020.
 */
public class DataFilters {

    public static Example<Trade> tradeExample(TradeType tradeType, Types.SecType secType, Currency currency, String underlying) {
        return Example.of(new Trade()
                .setType(tradeType)
                .setSecType(secType)
                .setCurrency(currency)
                .setUnderlying(underlying));
    }

    public static Specification<Execution> executionSpecification(List<DataFilterItem> dataFilterItems) {
        return (root, query, builder) -> build(root, builder, dataFilterItems);
    }

    public static Specification<Trade> tradeSpecification(List<DataFilterItem> dataFilterItems) {
        return (root, query, builder) -> build(root, builder, dataFilterItems);
    }

    public static Specification<Trade> tradeSpecification(TradeType tradeType, Types.SecType secType, Currency currency, String underlying, LocalDateTime cutoffDate) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tradeType != null) {
                predicates.add(builder.equal(root.get("tradeType"), tradeType));
            }
            if (secType != null) {
                predicates.add(builder.equal(root.get("secType"), secType));
            }
            if (currency != null) {
                predicates.add(builder.equal(root.get("currency"), currency));
            }
            if (underlying != null) {
                predicates.add(builder.equal(root.get("underlying"), underlying));
            }
            predicates.add(builder.or(
                    builder.greaterThanOrEqualTo(root.get("openDate"), cutoffDate),
                    builder.greaterThanOrEqualTo(root.get("closeDate"), cutoffDate),
                    builder.isNull(root.get("closeDate"))));

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <R> Predicate build(Root<R> root, CriteriaBuilder builder, List<DataFilterItem> dataFilterItems) {
        List<Predicate> outerAndPredicates = new ArrayList<>();
        List<Predicate> innerOrPredicates = new ArrayList<>();

        for (DataFilterItem item : dataFilterItems) {
            DataFilterOperator operator = DataFilterOperator.valueOf(item.getOperator().toUpperCase());
            String field = item.getProperty();

            switch (operator) {
                case LIKE:
                    switch (field) {
                        case "symbol":
                        case "underlying":
                            String likeStr = MessageFormat.format("%{0}%", item.getValue());
                            outerAndPredicates.add(builder.like(root.get(field), likeStr));
                            break;
                    }
                    break;
                case EQ:
                    if ("multiplier".equals(field)) {
                        outerAndPredicates.add(builder.equal(root.get(field), builder.literal(item.getDoubleValue())));
                    }
                    break;
                case LT:
                    if ("multiplier".equals(field)) {
                        innerOrPredicates.add(builder.lessThan(root.get(field), builder.literal(item.getDoubleValue())));
                    }
                    break;
                case GT:
                    if ("multiplier".equals(field)) {
                        innerOrPredicates.add(builder.greaterThan(root.get(field), builder.literal(item.getDoubleValue())));
                    }
                    break;
                case IN:
                    switch (field) {
                        case "currency": {
                            CriteriaBuilder.In<Currency> inPredicate = builder.in(root.get(field));
                            item.getValues().forEach(value -> inPredicate.value(Currency.valueOf(value)));
                            outerAndPredicates.add(inPredicate);
                            break;
                        }
                        case "secType": {
                            CriteriaBuilder.In<Types.SecType> inPredicate = builder.in(root.get(field));
                            item.getValues().forEach(value -> inPredicate.value(Types.SecType.valueOf(value)));
                            outerAndPredicates.add(inPredicate);
                            break;
                        }
                        case "status": {
                            CriteriaBuilder.In<TradeStatus> inPredicate = builder.in(root.get(field));
                            item.getValues().forEach(value -> inPredicate.value(TradeStatus.valueOf(value)));
                            outerAndPredicates.add(inPredicate);
                            break;
                        }
                    }
                    break;
            }
        }
        if (!innerOrPredicates.isEmpty()) {
            outerAndPredicates.add(builder.or(innerOrPredicates.toArray(new Predicate[0])));
        }
        return builder.and(outerAndPredicates.toArray(new Predicate[0]));
    }
}
