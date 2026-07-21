package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureDocumentMissingException;
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
    @DisplayName("zip 대신 파일 없음(status 014) XML 이 오면 문서 없음 예외를 던진다")
    void shouldThrowDocumentMissingOnStatus014() {
        String errorBody = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?><result><status>014</status><message>파일이 존재하지 않습니다.</message></result>
                """;

        assertThatThrownBy(() -> reader.readBody("20260619000667",
                new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(DisclosureDocumentMissingException.class)
                .hasMessageContaining("20260619000667");
    }

    @Test
    @DisplayName("zip 도 파일 없음도 아닌 응답은 일반 원천 예외를 던진다")
    void shouldThrowOnUnexpectedNonZipBody() {
        String errorBody = """
                <?xml version="1.0"?><result><status>020</status><message>요청 제한</message></result>
                """;

        assertThatThrownBy(() -> reader.readBody("20260310002820",
                new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(DisclosureSourceException.class)
                .isNotInstanceOf(DisclosureDocumentMissingException.class)
                .hasMessageContaining("zip 이 아님");
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
