package dev.gukin.einvestlab.company.infra.http;

import dev.gukin.einvestlab.company.domain.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DART 회사 등록부 실제 응답 회귀 스모크 테스트")
class DartCompanyRegistryFixtureSmokeTest {

    private final DartCompanyRegistryReader reader = new DartCompanyRegistryReader();

    @Test
    @DisplayName("실제 등록부 zip 응답을 끝까지 읽고 알려진 상장 회사를 찾는다")
    void shouldReadCapturedCorpCodeZipFixture() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<Company> samsung = new AtomicReference<>();

        reader.read(corpCodeZipFixture(), company -> {
            count.incrementAndGet();
            if ("00126380".equals(company.getCorpCode())) {
                samsung.set(company);
            }
        });

        assertThat(count.get()).isGreaterThan(100_000);
        assertThat(samsung.get()).isNotNull();
        assertThat(samsung.get())
                .extracting(Company::getName, Company::getStockCode)
                .containsExactly("삼성전자", "005930");
        assertThat(samsung.get().getRegistryModifiedDate()).isNotNull();
    }

    private InputStream corpCodeZipFixture() {
        InputStream input = getClass().getResourceAsStream("/fixtures/company/CORPCODE.zip");
        assertThat(input).isNotNull();
        return input;
    }
}
