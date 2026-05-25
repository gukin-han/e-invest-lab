package dev.gukin.einvestlab.company.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회사 마스터 도메인 생성")
class CompanyCreationUnitTest {

    @Nested
    @DisplayName("마스터 필드로 회사를 등록할 때")
    class WhenBuiltFromMasterFields {

        @Test
        @DisplayName("DART 에서 받은 마스터 필드 5개를 그대로 보존한다")
        void shouldHoldAllProvidedFields() {
            Company company = Company.builder()
                    .corpCode("00126380")
                    .name("삼성전자")
                    .englishName("SAMSUNG ELECTRONICS CO,.LTD")
                    .stockCode("005930")
                    .masterModifiedDate(LocalDate.of(2025, 12, 1))
                    .build();

            assertThat(company)
                    .extracting(
                            Company::getCorpCode,
                            Company::getName,
                            Company::getEnglishName,
                            Company::getStockCode,
                            Company::getMasterModifiedDate)
                    .containsExactly(
                            "00126380",
                            "삼성전자",
                            "SAMSUNG ELECTRONICS CO,.LTD",
                            "005930",
                            LocalDate.of(2025, 12, 1));
        }

        @Test
        @DisplayName("비상장 회사도 등록할 수 있다 — 종목코드 없음 허용")
        void shouldAcceptNullStockCodeWithUnlistedCompany() {
            Company company = Company.builder()
                    .corpCode("00434003")
                    .name("다코")
                    .englishName("Daco corporation")
                    .stockCode(null)
                    .masterModifiedDate(LocalDate.of(2017, 6, 30))
                    .build();

            assertThat(company.getStockCode()).isNull();
        }

        @Test
        @DisplayName("영문명이 없는 회사도 등록할 수 있다")
        void shouldAcceptNullEnglishName() {
            Company company = Company.builder()
                    .corpCode("00126380")
                    .name("삼성전자")
                    .englishName(null)
                    .stockCode("005930")
                    .masterModifiedDate(LocalDate.of(2025, 12, 1))
                    .build();

            assertThat(company.getEnglishName()).isNull();
        }
    }
}
