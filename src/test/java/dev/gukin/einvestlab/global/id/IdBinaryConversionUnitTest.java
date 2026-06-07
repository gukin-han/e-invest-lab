package dev.gukin.einvestlab.global.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("식별자 바이너리 변환 단위 테스트")
class IdBinaryConversionUnitTest {

    @Test
    @DisplayName("16바이트 저장 값으로 바꾼 뒤 원래 식별자를 그대로 복원한다")
    void shouldRestoreOriginalIdAfterBinaryRoundTrip() {
        UUID id = Ids.generate();

        byte[] bytes = Ids.toBytes(id);
        UUID restored = Ids.fromBytes(bytes);

        assertThat(bytes).hasSize(16);
        assertThat(restored).isEqualTo(id);
    }
}
