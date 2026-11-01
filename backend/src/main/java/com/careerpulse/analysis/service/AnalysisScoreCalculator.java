package com.careerpulse.analysis.service;

import com.careerpulse.analysis.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;

@Component
public class AnalysisScoreCalculator {

    public AnalysisScore calculate(List<AnalysisItem> items) {
        List<AnalysisItem> scoredItems = items.stream()
                .filter(item -> item.getRequirement().getComparisonKind()
                        != ComparisonKind.RESPONSIBILITY)
                .toList();
        int matched = count(scoredItems, item -> item.getJudgment().isMatched());
        int comparable = count(scoredItems, item -> item.getJudgment().isComparable());
        int unknown = scoredItems.size() - comparable;

        return new AnalysisScore(
                matched,
                comparable,
                unknown,
                rate(matched, comparable),
                categoryRate(scoredItems, RequirementCategory.REQUIRED),
                categoryRate(scoredItems, RequirementCategory.PREFERRED),
                kindRate(scoredItems, ComparisonKind.OBJECTIVE),
                rate(comparable, scoredItems.size())
        );
    }

    private BigDecimal categoryRate(
            List<AnalysisItem> items,
            RequirementCategory category
    ) {
        List<AnalysisItem> targets = items.stream()
                .filter(item -> item.getRequirement().getRequirementType() == category)
                .toList();
        return matchedRate(targets);
    }

    private BigDecimal kindRate(List<AnalysisItem> items, ComparisonKind kind) {
        List<AnalysisItem> targets = items.stream()
                .filter(item -> item.getRequirement().getComparisonKind() == kind)
                .toList();
        return matchedRate(targets);
    }

    private BigDecimal matchedRate(List<AnalysisItem> items) {
        int comparable = count(items, item -> item.getJudgment().isComparable());
        int matched = count(items, item -> item.getJudgment().isMatched());
        return rate(matched, comparable);
    }

    private int count(List<AnalysisItem> items, Predicate<AnalysisItem> predicate) {
        return Math.toIntExact(items.stream().filter(predicate).count());
    }

    static BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return null;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
