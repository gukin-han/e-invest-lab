package dev.gukin.einvestlab.research.infrastructure;

import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.research.domain.EpsEstimate;
import dev.gukin.einvestlab.research.domain.EpsEstimateRepository;
import dev.gukin.einvestlab.research.infrastructure.persistence.EpsEstimateJpaRepository;
import dev.gukin.einvestlab.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EPS 추정치 교체(재추출) 통합 테스트")
class EpsEstimateReplaceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EpsEstimateRepository repository;

    @Autowired
    private EpsEstimateJpaRepository jpa;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        jpa.deleteAllInBatch();
    }

    @Test
    @DisplayName("한 트랜잭션에서 같은 연도를 지우고 다시 넣어도 유니크 충돌 없이 교체된다")
    void shouldReplaceSameFiscalYearsInOneTransaction() {
        repository.saveAll(List.of(estimate(1L, 2026, "1000"), estimate(1L, 2028, "2000")));

        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            repository.deleteAllByReportIdx(1L);
            repository.saveAll(List.of(
                    estimate(1L, 2026, "1100"),
                    estimate(1L, 2027, "1200"),
                    estimate(1L, 2028, "2100")));
        });

        assertThat(jpa.findAll())
                .extracting(EpsEstimate::getFiscalYear, estimateRow -> estimateRow.getEps().stripTrailingZeros())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(2026, new BigDecimal("1100").stripTrailingZeros()),
                        org.assertj.core.groups.Tuple.tuple(2027, new BigDecimal("1200").stripTrailingZeros()),
                        org.assertj.core.groups.Tuple.tuple(2028, new BigDecimal("2100").stripTrailingZeros()));
    }

    private EpsEstimate estimate(long reportIdx, int fiscalYear, String eps) {
        return EpsEstimate.builder()
                .id(Ids.generate())
                .reportIdx(reportIdx)
                .fiscalYear(fiscalYear)
                .estimated(true)
                .eps(new BigDecimal(eps))
                .extractedAt(Instant.parse("2026-08-08T03:00:00Z"))
                .build();
    }
}
