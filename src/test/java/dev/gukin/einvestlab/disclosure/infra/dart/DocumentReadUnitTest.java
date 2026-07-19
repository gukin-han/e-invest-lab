package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DART 원문 zip 본문 선택 단위 테스트")
class DocumentReadUnitTest {

    private final DocumentReader reader = new DocumentReader();

    @Test
    @DisplayName("첨부를 건너뛰고 이름이 정확히 {접수번호}.xml 인 본문 엔트리를 UTF-8 로 읽는다")
    void shouldReadBodyEntryAmongAttachments() {
        InputStream zipBody = zip(Map.of(
                "20260310002820_00001.xml", "<attachment>감사보고서</attachment>",
                "20260310002820.xml", "<document>II. 사업의 내용</document>",
                "20260310002820_00002.xml", "<attachment>연결감사보고서</attachment>"
        ));

        String body = reader.readBody("20260310002820", zipBody);

        assertThat(body).isEqualTo("<document>II. 사업의 내용</document>");
    }

    @Test
    @DisplayName("본문 엔트리가 없으면 예외를 던진다")
    void shouldThrowWhenBodyEntryMissing() {
        InputStream zipBody = zip(Map.of(
                "20260310002820_00001.xml", "<attachment>감사보고서</attachment>"
        ));

        assertThatThrownBy(() -> reader.readBody("20260310002820", zipBody))
                .isInstanceOf(DisclosureSourceException.class)
                .hasMessageContaining("20260310002820.xml");
    }

    private InputStream zip(Map<String, String> entries) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> entry : new LinkedHashMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (Exception e) {
            throw new IllegalStateException("테스트 zip 생성 실패", e);
        }
        return new ByteArrayInputStream(output.toByteArray());
    }
}
