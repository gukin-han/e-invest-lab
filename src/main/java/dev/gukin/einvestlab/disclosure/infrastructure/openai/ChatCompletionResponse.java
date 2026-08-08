package dev.gukin.einvestlab.disclosure.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.gukin.einvestlab.disclosure.domain.OfferingExtractionException;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record ChatCompletionResponse(@JsonProperty("choices") List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(@JsonProperty("message") Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(@JsonProperty("content") String content) {
    }

    String firstContent() {
        if (choices == null || choices.isEmpty() || choices.getFirst().message() == null
                || choices.getFirst().message().content() == null) {
            throw new OfferingExtractionException("OpenAI 응답에 본문 없음");
        }
        return choices.getFirst().message().content();
    }
}
