package dev.gukin.einvestlab.company.infra.http;

import dev.gukin.einvestlab.company.domain.CompanyRegistrySourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DART 회사 등록부 응답 형식 오류 단위 테스트")
class DartCompanyRegistryReadFailureUnitTest {

    private final DartCompanyRegistryReader reader = new DartCompanyRegistryReader();

    @Test
    @DisplayName("zip 이 아닌 응답은 등록부로 읽을 수 없다")
    void shouldRejectNonZipBody() {
        InputStream body = new ByteArrayInputStream("{\"status\":\"013\"}".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> reader.read(body, company -> {
        })).isInstanceOf(CompanyRegistrySourceException.class);
    }

    @Test
    @DisplayName("zip 안에 XML 항목이 없으면 등록부로 읽을 수 없다")
    void shouldRejectZipWithoutXmlEntry() {
        InputStream body = zip("README.txt", "not xml");

        assertThatThrownBy(() -> reader.read(body, company -> {
        })).isInstanceOf(CompanyRegistrySourceException.class)
                .hasMessageContaining(".xml 항목이 없음");
    }

    @Test
    @DisplayName("깨진 XML 은 등록부로 읽을 수 없다")
    void shouldRejectBrokenXml() {
        InputStream body = zip("CORPCODE.xml", "<result><list><corp_code>00126380</corp_code>");

        assertThatThrownBy(() -> reader.read(body, company -> {
        })).isInstanceOf(CompanyRegistrySourceException.class)
                .hasMessageContaining("스트림 파싱 실패");
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
