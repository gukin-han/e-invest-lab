package dev.gukin.einvestlab.company.infrastructure.dart;

import dev.gukin.einvestlab.company.domain.CompanyRegistryEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DART 회사 등록부 실제 응답 회귀 스모크 테스트")
class CompanyRegistryFixtureSmokeTest {

    private final CompanyRegistryReader reader = new CompanyRegistryReader();

    @Test
    @DisplayName("실제 등록부 zip 응답을 끝까지 읽고 알려진 상장 회사를 찾는다")
    void shouldReadCapturedCorpCodeZipFixture() {
        AtomicInteger count = new AtomicInteger();
        AtomicReference<CompanyRegistryEntry> samsung = new AtomicReference<>();

        reader.read(corpCodeZipFixture(), entry -> {
            count.incrementAndGet();
            if ("00126380".equals(entry.corpCode())) {
                samsung.set(entry);
            }
        });

        assertThat(count.get()).isGreaterThan(100_000);
        assertThat(samsung.get()).isNotNull();
        assertThat(samsung.get())
                .extracting(CompanyRegistryEntry::name, CompanyRegistryEntry::stockCode)
                .containsExactly("삼성전자", "005930");
        assertThat(samsung.get().registryModifiedDate()).isNotNull();
    }

    private InputStream corpCodeZipFixture() {
        InputStream input = getClass().getResourceAsStream("/fixtures/company/CORPCODE.zip");
        assertThat(input).isNotNull();
        return input;
    }
}
