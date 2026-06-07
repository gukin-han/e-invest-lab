package dev.gukin.einvestlab.company.infra;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.infra.db.CompanyJpaRepository;
import dev.gukin.einvestlab.global.id.Ids;
import dev.gukin.einvestlab.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("회사 등록부 영속화 통합 테스트")
class CompanyPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CompanyJpaRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("회사를 등록하고 다시 찾을 때")
    class WhenSavingAndLookingUp {

        @Test
        @DisplayName("DART 고유번호로 다시 조회되고 발급한 식별자가 보존된다")
        void shouldRoundTripCompanyByCorpCode() {
            UUID id = Ids.generate();
            Company company = Company.builder()
                    .id(id)
                    .corpCode("00126380")
                    .name("삼성전자")
                    .englishName("SAMSUNG ELECTRONICS CO,.LTD")
                    .stockCode("005930")
                    .registryModifiedDate(LocalDate.of(2025, 12, 1))
                    .build();

            repository.save(company);

            Optional<Company> found = repository.findByCorpCode("00126380");
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("비상장 회사도 등록할 수 있다 — 종목코드 없음 허용")
        void shouldPersistUnlistedCompany() {
            Company company = Company.builder()
                    .id(Ids.generate())
                    .corpCode("00434003")
                    .name("다코")
                    .englishName("Daco corporation")
                    .stockCode(null)
                    .registryModifiedDate(LocalDate.of(2017, 6, 30))
                    .build();

            repository.save(company);

            assertThat(repository.findByCorpCode("00434003")).isPresent();
        }
    }

    @Nested
    @DisplayName("같은 DART 고유번호로 두 번 등록할 때")
    class WhenSavingDuplicateCorpCode {

        @Test
        @DisplayName("두 번째 등록은 중복으로 거부된다")
        void shouldRejectDuplicateCorpCode() {
            saveSample("00126380");

            assertThatThrownBy(() -> saveSample("00126380"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private void saveSample(String corpCode) {
            repository.saveAndFlush(Company.builder()
                    .id(Ids.generate())
                    .corpCode(corpCode)
                    .name("삼성전자")
                    .registryModifiedDate(LocalDate.of(2025, 12, 1))
                    .build());
        }
    }
}
