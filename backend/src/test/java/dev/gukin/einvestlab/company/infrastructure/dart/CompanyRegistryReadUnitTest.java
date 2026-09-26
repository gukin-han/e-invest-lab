package dev.gukin.einvestlab.company.infrastructure.dart;

import dev.gukin.einvestlab.company.domain.CompanyRegistryEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("DART 회사 등록부 파싱 단위 테스트")
class CompanyRegistryReadUnitTest {

    private final CompanyRegistryReader reader = new CompanyRegistryReader();

    @Test
    @DisplayName("DART 에서 받은 등록부 필드를 등록부 항목으로 변환해 순서대로 전달한다")
    void shouldReadEntriesFromCorpCodeZip() {
        List<CompanyRegistryEntry> entries = new ArrayList<>();

        reader.read(corpCodeZip(), entries::add);

        assertThat(entries).hasSize(2);
        assertThat(entries)
                .extracting(
                        CompanyRegistryEntry::corpCode,
                        CompanyRegistryEntry::name,
                        CompanyRegistryEntry::englishName,
                        CompanyRegistryEntry::stockCode,
                        CompanyRegistryEntry::registryModifiedDate
                )
                .containsExactly(
                        tuple(
                                "00126380",
                                "삼성전자",
                                "SAMSUNG ELECTRONICS CO,.LTD",
                                "005930",
                                LocalDate.of(2025, 12, 1)
                        ),
                        tuple(
                                "00434003",
                                "다코",
                                "Daco corporation",
                                null,
                                LocalDate.of(2017, 6, 30)
                        )
                );
    }

    private InputStream corpCodeZip() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <result>
                    <list>
                        <corp_code>00126380</corp_code>
                        <corp_name>삼성전자</corp_name>
                        <corp_eng_name>SAMSUNG ELECTRONICS CO,.LTD</corp_eng_name>
                        <stock_code>005930</stock_code>
                        <modify_date>20251201</modify_date>
                    </list>
                    <list>
                        <corp_code>00434003</corp_code>
                        <corp_name>다코</corp_name>
                        <corp_eng_name>Daco corporation</corp_eng_name>
                        <stock_code> </stock_code>
                        <modify_date>20170630</modify_date>
                    </list>
                </result>
                """;
        return zip("CORPCODE.xml", xml);
    }

    private InputStream zip(String entryName, String content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (Exception e) {
            throw new IllegalStateException("테스트 zip 생성 실패", e);
        }
        return new ByteArrayInputStream(output.toByteArray());
    }
}
