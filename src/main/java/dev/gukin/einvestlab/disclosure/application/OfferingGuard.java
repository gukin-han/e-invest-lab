package dev.gukin.einvestlab.disclosure.application;

import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OfferingGuard {

    private static final Set<String> TOTAL_LABELS = Set.of("합계", "총계", "소계", "총합계", "계", "단순합계");
    private static final Map<String, String> BASIS_NORMALIZATION = Map.of(
            "매출", "매출액",
            "매출실적", "매출액");
    private static final BigDecimal SHARE_SUM_LIMIT = new BigDecimal("115");

    public OfferingGuardVerdict verify(List<OfferingDraft> drafts, String sourceContent) {
        if (drafts.isEmpty()) {
            return OfferingGuardVerdict.failed(List.of("빈 결과"));
        }

        List<String> issues = new ArrayList<>();
        boolean corrected = false;
        List<OfferingDraft> kept = new ArrayList<>();
        for (OfferingDraft draft : drafts) {
            if (isTotalRow(draft)) {
                issues.add("합계류 행 제거: segment=" + draft.segment() + " products=" + draft.products());
                corrected = true;
                continue;
            }
            OfferingDraft normalized = normalizeBasis(draft);
            if (!normalized.equals(draft)) {
                issues.add("basis 정규화: " + draft.revenueBasis() + " → " + normalized.revenueBasis());
                corrected = true;
            }
            kept.add(normalized);
        }
        if (kept.isEmpty()) {
            issues.add("합계 제거 후 빈 결과");
            return OfferingGuardVerdict.failed(issues);
        }

        String haystack = normalize(sourceContent);
        for (OfferingDraft draft : kept) {
            List<String> missing = missingEvidence(draft, haystack);
            if (!missing.isEmpty()) {
                issues.add("원문 부재(환각 의심): " + missing);
                return OfferingGuardVerdict.failed(issues);
            }
        }

        List<String> shareViolations = shareSumViolations(kept);
        if (!shareViolations.isEmpty()) {
            issues.add("비중 합 초과: " + shareViolations);
            return OfferingGuardVerdict.failed(issues);
        }

        OfferingExtractionStatus status = corrected
                ? OfferingExtractionStatus.CORRECTED
                : OfferingExtractionStatus.EXTRACTED;
        return new OfferingGuardVerdict(status, List.copyOf(kept), List.copyOf(issues));
    }

    private boolean isTotalRow(OfferingDraft draft) {
        if (draft.segment() != null && TOTAL_LABELS.contains(draft.segment().strip())) {
            return true;
        }
        if (draft.qualifier() != null && TOTAL_LABELS.contains(draft.qualifier().strip())) {
            return true;
        }
        return draft.products().stream().anyMatch(product -> TOTAL_LABELS.contains(product.strip()));
    }

    private OfferingDraft normalizeBasis(OfferingDraft draft) {
        if (draft.revenueBasis() == null) {
            return draft;
        }
        String normalized = BASIS_NORMALIZATION.get(draft.revenueBasis().strip());
        return normalized == null ? draft : draft.withRevenueBasis(normalized);
    }

    private List<String> missingEvidence(OfferingDraft draft, String haystack) {
        List<String> missing = new ArrayList<>();
        for (String product : draft.products()) {
            if (!haystack.contains(normalize(product))) {
                missing.add("product=" + product);
            }
        }
        for (String customer : draft.customers()) {
            if (!haystack.contains(normalize(customer))) {
                missing.add("customer=" + customer);
            }
        }
        if (draft.revenueAmount() != null && amountCandidates(draft.revenueAmount()).stream()
                .noneMatch(haystack::contains)) {
            missing.add("amount=" + draft.revenueAmount());
        }
        if (draft.revenueShare() != null && shareCandidates(draft.revenueShare()).stream()
                .noneMatch(haystack::contains)) {
            missing.add("share=" + draft.revenueShare());
        }
        return missing;
    }

    private List<String> amountCandidates(BigDecimal amount) {
        List<String> candidates = new ArrayList<>();
        BigDecimal stripped = amount.stripTrailingZeros();
        candidates.add(stripped.toPlainString());
        if (stripped.scale() <= 0) {
            long value = stripped.longValueExact();
            if (value >= 10_000) {
                long trillions = value / 10_000;
                long remainder = value % 10_000;
                candidates.add(remainder == 0 ? trillions + "조" : trillions + "조" + remainder);
            }
        }
        return candidates;
    }

    private List<String> shareCandidates(BigDecimal share) {
        return List.of(
                share.stripTrailingZeros().toPlainString(),
                share.setScale(1, RoundingMode.HALF_UP).toPlainString());
    }

    private List<String> shareSumViolations(List<OfferingDraft> drafts) {
        Map<String, BigDecimal> sums = new HashMap<>();
        for (OfferingDraft draft : drafts) {
            if (draft.revenueShare() == null) {
                continue;
            }
            String key = draft.fiscalYear() + "/" + draft.businessPart() + "/"
                    + draft.entityName() + "/" + draft.revenueBasis();
            sums.merge(key, draft.revenueShare(), BigDecimal::add);
        }
        return sums.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(SHARE_SUM_LIMIT) > 0)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
    }

    private static String normalize(String text) {
        return text.replaceAll("[\\s,]", "");
    }
}
