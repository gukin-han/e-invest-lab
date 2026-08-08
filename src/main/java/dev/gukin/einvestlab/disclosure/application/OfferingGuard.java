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
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MULTI_PIE_TOLERANCE = new BigDecimal("15");
    private static final String TEXT_SEGMENT_DELIMITERS = "[(),·・/~]";

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
            EvidenceResult evidence = checkEvidence(draft, haystack);
            if (!evidence.missing().isEmpty()) {
                issues.add("원문 부재(환각 의심): " + evidence.missing());
                return OfferingGuardVerdict.failed(issues);
            }
            if (!evidence.partial().isEmpty()) {
                issues.add("부분 근거(의역 수용): " + evidence.partial());
                corrected = true;
            }
        }

        ShareSumCheck shareSums = checkShareSums(kept);
        if (!shareSums.violations().isEmpty()) {
            issues.add("비중 합 초과: " + shareSums.violations());
            return OfferingGuardVerdict.failed(issues);
        }
        if (!shareSums.multiPies().isEmpty()) {
            issues.add("복수 파이 수용(구분 차원 결손): " + shareSums.multiPies());
            corrected = true;
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

    private EvidenceResult checkEvidence(OfferingDraft draft, String haystack) {
        List<String> missing = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        for (String product : draft.products()) {
            classifyText("product=" + product, product, haystack, missing, partial);
        }
        for (String customer : draft.customers()) {
            classifyText("customer=" + customer, customer, haystack, missing, partial);
        }
        if (draft.revenueAmount() != null && amountCandidates(draft.revenueAmount()).stream()
                .noneMatch(haystack::contains)) {
            missing.add("amount=" + draft.revenueAmount());
        }
        if (draft.revenueShare() != null && shareCandidates(draft.revenueShare()).stream()
                .noneMatch(haystack::contains)) {
            missing.add("share=" + draft.revenueShare());
        }
        return new EvidenceResult(missing, partial);
    }

    private void classifyText(String label, String value, String haystack,
                              List<String> missing, List<String> partial) {
        if (haystack.contains(normalize(value))) {
            return;
        }
        for (String segment : value.split(TEXT_SEGMENT_DELIMITERS)) {
            String normalized = normalize(segment);
            if (normalized.length() >= 2 && haystack.contains(normalized)) {
                partial.add(label);
                return;
            }
        }
        missing.add(label);
    }

    private List<String> amountCandidates(BigDecimal amount) {
        List<String> candidates = new ArrayList<>();
        BigDecimal stripped = amount.stripTrailingZeros();
        candidates.add(stripped.toPlainString());
        if (stripped.signum() < 0) {
            candidates.addAll(negativeNotations(stripped.abs().toPlainString()));
        }
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
        List<String> candidates = new ArrayList<>(List.of(
                share.stripTrailingZeros().toPlainString(),
                share.setScale(1, RoundingMode.HALF_UP).toPlainString()));
        if (share.signum() < 0) {
            candidates.addAll(negativeNotations(share.abs().stripTrailingZeros().toPlainString()));
            candidates.addAll(negativeNotations(share.abs().setScale(1, RoundingMode.HALF_UP).toPlainString()));
        }
        return candidates;
    }

    private List<String> negativeNotations(String absolute) {
        return List.of("(" + absolute + ")", "△" + absolute, "▲" + absolute);
    }

    private ShareSumCheck checkShareSums(List<OfferingDraft> drafts) {
        Map<String, BigDecimal> sums = new HashMap<>();
        for (OfferingDraft draft : drafts) {
            if (draft.revenueShare() == null) {
                continue;
            }
            String key = draft.fiscalYear() + "/" + draft.businessPart() + "/"
                    + draft.entityName() + "/" + draft.revenueBasis();
            sums.merge(key, draft.revenueShare(), BigDecimal::add);
        }
        List<String> violations = new ArrayList<>();
        List<String> multiPies = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sums.entrySet()) {
            BigDecimal sum = entry.getValue();
            if (sum.compareTo(SHARE_SUM_LIMIT) <= 0) {
                continue;
            }
            if (nearMultipleOfHundred(sum)) {
                multiPies.add(entry.getKey() + "=" + sum);
            } else {
                violations.add(entry.getKey() + "=" + sum);
            }
        }
        return new ShareSumCheck(violations, multiPies);
    }

    private boolean nearMultipleOfHundred(BigDecimal sum) {
        BigDecimal nearest = sum.divide(HUNDRED, 0, RoundingMode.HALF_UP).multiply(HUNDRED);
        return sum.subtract(nearest).abs().compareTo(MULTI_PIE_TOLERANCE) <= 0;
    }

    private record EvidenceResult(List<String> missing, List<String> partial) {
    }

    private record ShareSumCheck(List<String> violations, List<String> multiPies) {
    }

    private static String normalize(String text) {
        return text.replaceAll("[\\s,]", "");
    }
}
