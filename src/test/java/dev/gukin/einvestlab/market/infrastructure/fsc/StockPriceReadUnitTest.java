package dev.gukin.einvestlab.market.infrastructure.fsc;

import dev.gukin.einvestlab.market.domain.DailyStockPriceEntry;
import dev.gukin.einvestlab.market.domain.MarketSourceException;
import dev.gukin.einvestlab.support.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("금융위 주식시세 응답 해석 단위 테스트 (실물 픽스처)")
class StockPriceReadUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("실물 페이지 응답을 종목별 일별 시세로 해석한다")
    void shouldReadEntriesFromRealPage() {
        StockPriceResponse response = read(Fixtures.read("/fixtures/market/fsc-stock-price-page.json"));

        response.requireSuccess();
        assertThat(response.totalCount()).isEqualTo(2_872);
        assertThat(response.toEntries())
                .extracting(DailyStockPriceEntry::stockCode, DailyStockPriceEntry::tradeDate,
                        DailyStockPriceEntry::marketCategory, DailyStockPriceEntry::closePrice,
                        DailyStockPriceEntry::openPrice, DailyStockPriceEntry::volume,
                        DailyStockPriceEntry::listedShareCount, DailyStockPriceEntry::marketCap)
                .containsExactly(
                        tuple("900110", LocalDate.of(2026, 8, 6), "KOSDAQ", 1_126, 1_171, 16_498L,
                                18_437_131L, 20_760_209_506L),
                        tuple("900270", LocalDate.of(2026, 8, 6), "KOSDAQ", 420, 0, 0L,
                                25_561_441L, 10_735_805_220L),
                        tuple("900260", LocalDate.of(2026, 8, 6), "KOSDAQ", 1_689, 1_727, 3_273L,
                                46_029_706L, 77_744_173_434L)
                );
    }

    @Test
    @DisplayName("상장주식수·시가총액이 없는 항목은 해당 값만 null 로 해석한다")
    void shouldReadMissingShareCountAsNull() {
        StockPriceResponse response = read("""
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"totalCount":1,
                "items":{"item":[{"basDt":"20260806","srtnCd":"900110","mrktCtg":"KOSDAQ",
                "mkp":"1171","hipr":"1188","lopr":"1126","clpr":"1126","trqu":"16498","lstgStCnt":""}]}}}}
                """);

        DailyStockPriceEntry entry = response.toEntries().getFirst();
        assertThat(entry.listedShareCount()).isNull();
        assertThat(entry.marketCap()).isNull();
        assertThat(entry.closePrice()).isEqualTo(1_126);
    }

    @Test
    @DisplayName("휴일 응답(전체 0건)은 빈 목록으로 해석한다")
    void shouldReadHolidayResponseAsEmpty() {
        StockPriceResponse response = read(Fixtures.read("/fixtures/market/fsc-stock-price-empty.json"));

        response.requireSuccess();
        assertThat(response.totalCount()).isZero();
        assertThat(response.toEntries()).isEmpty();
    }

    @Test
    @DisplayName("정상 코드가 아니면 원천 예외를 던진다")
    void shouldRejectErrorResultCode() {
        StockPriceResponse response = read("""
                {"response":{"header":{"resultCode":"22","resultMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"},"body":null}}
                """);

        assertThatThrownBy(response::requireSuccess)
                .isInstanceOf(MarketSourceException.class)
                .hasMessageContaining("22");
    }

    @Test
    @DisplayName("서비스 키 오류 본문(다른 봉투 형식)은 원천 예외를 던진다")
    void shouldRejectServiceKeyErrorEnvelope() {
        StockPriceResponse response = read("""
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnReasonCode":"30"}}}
                """);

        assertThatThrownBy(response::requireSuccess)
                .isInstanceOf(MarketSourceException.class)
                .hasMessageContaining("형식 오류");
    }

    private StockPriceResponse read(String json) {
        return objectMapper.readValue(json, StockPriceResponse.class);
    }
}
