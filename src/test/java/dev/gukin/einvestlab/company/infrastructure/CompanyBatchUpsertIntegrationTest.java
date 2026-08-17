package dev.gukin.einvestlab.company.infrastructure;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRepository;
import dev.gukin.einvestlab.company.infrastructure.persistence.CompanyJpaRepository;
import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("회사 등록부 배치 반영 통합 테스트")
class CompanyBatchUpsertIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CompanyRepository repository;

    @Autowired
    private CompanyJpaRepository jpaRepository;

    @AfterEach
    void tearDown() {
        jpaRepository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("같은 DART 고유번호의 회사를 다시 반영할 때")
    class WhenUpsertingExistingCompany {

        @Test
        @DisplayName("새 행을 만들지 않고 기존 회사의 등록부 필드를 갱신한다")
        void shouldUpdateRegistryFieldsWithoutInsertingNewRow() {
            UUID originalId = Ids.generate();
            repository.upsertCompanies(List.of(company(
                    originalId,
                    "00126380",
                    "삼성전자",
                    "SAMSUNG ELECTRONICS CO,.LTD",
                    "005930",
                    LocalDate.of(2025, 12, 1)
            )));

            int upsertedCount = repository.upsertCompanies(List.of(company(
                    Ids.generate(),
                    "00126380",
                    "삼성전자변경",
                    "SAMSUNG ELECTRONICS",
                    null,
                    LocalDate.of(2026, 1, 15)
            )));

            Company found = repository.findByCorpCode("00126380").orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(jpaRepository.count()).isEqualTo(1);
                softly.assertThat(upsertedCount).isEqualTo(1);
                softly.assertThat(found)
                        .extracting(
                                Company::getId,
                                Company::getName,
                                Company::getEnglishName,
                                Company::getStockCode,
                                Company::getRegistryModifiedDate
                        )
                        .containsExactly(
                                originalId,
                                "삼성전자변경",
                                "SAMSUNG ELECTRONICS",
                                null,
                                LocalDate.of(2026, 1, 15)
                        );
            });
        }
    }

    private Company company(UUID id, String corpCode, String name, String englishName,
                            String stockCode, LocalDate registryModifiedDate) {
        return Company.builder()
                .id(id)
                .corpCode(corpCode)
                .name(name)
                .englishName(englishName)
                .stockCode(stockCode)
                .registryModifiedDate(registryModifiedDate)
                .build();
    }
}
