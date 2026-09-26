package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import dev.gukin.einvestlab.disclosure.domain.OfferingDraft;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAI 응답 해석 단위 테스트")
class OfferingResponseReadUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("chat.completions 봉투에서 본문을 꺼내 Offering 초안으로 해석한다")
    void shouldReadDraftsFromCompletionEnvelope() {
        String envelope = """
                {"id":"chatcmpl-1","choices":[{"index":0,"message":{"role":"assistant",
                "content":"{\\"offerings\\":[{\\"business_part\\":null,\\"segment\\":\\"DX\\",\\"qualifier\\":null,\\"products\\":[\\"TV\\",\\"모니터\\"],\\"revenue_amount\\":38542,\\"revenue_unit\\":\\"억원\\",\\"revenue_basis\\":\\"매출액\\",\\"revenue_share\\":42.8,\\"customers\\":[],\\"entity\\":null,\\"fiscal_year\\":2025}]}"}}]}
                """;

        ChatCompletionResponse response = objectMapper.readValue(envelope, ChatCompletionResponse.class);
        List<OfferingDraft> drafts =
                objectMapper.readValue(response.firstContent(), OfferingPayload.class).toDrafts();

        assertThat(drafts).hasSize(1);
        OfferingDraft draft = drafts.getFirst();
        assertThat(draft.segment()).isEqualTo("DX");
        assertThat(draft.products()).containsExactly("TV", "모니터");
        assertThat(draft.revenueAmount()).isEqualByComparingTo(new BigDecimal("38542"));
        assertThat(draft.revenueUnit()).isEqualTo("억원");
        assertThat(draft.revenueShare()).isEqualByComparingTo(new BigDecimal("42.8"));
        assertThat(draft.fiscalYear()).isEqualTo(2025);
        assertThat(draft.businessPart()).isNull();
        assertThat(draft.customers()).isEmpty();
    }

    @Test
    @DisplayName("본문 없는 봉투는 추출 예외를 던진다")
    void shouldRejectEnvelopeWithoutContent() {
        ChatCompletionResponse response = objectMapper.readValue(
                "{\"choices\":[]}", ChatCompletionResponse.class);

        assertThatThrownBy(response::firstContent)
                .isInstanceOf(OfferingExtractionException.class);
    }
}
