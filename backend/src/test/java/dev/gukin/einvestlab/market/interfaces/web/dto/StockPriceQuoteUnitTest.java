package dev.gukin.einvestlab.market.interfaces.web.dto;

import dev.gukin.einvestlab.market.domain.DailyStockPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("일별 시세 응답 시·고·저 해석 단위 테스트")
class StockPriceQuoteUnitTest {

    @Test
    @DisplayName("거래가 있던 날은 시·고·저를 그대로 내보낸다")
    void shouldKeepQuotedPricesWithPriceDetail() {
        StockPriceResponse response = StockPriceResponse.from(
                price(199_000, 202_500, 193_500, 197_500, 24_158L));

        assertThat(response.openPrice()).isEqualTo(199_000);
        assertThat(response.highPrice()).isEqualTo(202_500);
        assertThat(response.lowPrice()).isEqualTo(193_500);
        assertThat(response.closePrice()).isEqualTo(197_500);
        assertThat(response.volume()).isEqualTo(24_158L);
    }

    @Test
    @DisplayName("거래정지로 시·고·저가 비어 있으면 종가로 눌러 변동 없는 하루로 내보낸다")
    void shouldFallBackToClosePriceWithHaltedDay() {
        StockPriceResponse response = StockPriceResponse.from(
                price(0, 0, 0, 197_500, 0L));

        assertThat(response.openPrice()).isEqualTo(197_500);
        assertThat(response.highPrice()).isEqualTo(197_500);
        assertThat(response.lowPrice()).isEqualTo(197_500);
        assertThat(response.closePrice()).isEqualTo(197_500);
        assertThat(response.volume()).isZero();
    }

    @Test
    @DisplayName("거래량은 있는데 시·고·저만 비어 있는 원천 결함도 종가로 눌러 내보낸다")
    void shouldFallBackToClosePriceWithVolumeButNoPriceDetail() {
        StockPriceResponse response = StockPriceResponse.from(
                price(0, 0, 0, 10_050, 25_000L));

        assertThat(response.openPrice()).isEqualTo(10_050);
        assertThat(response.highPrice()).isEqualTo(10_050);
        assertThat(response.lowPrice()).isEqualTo(10_050);
        assertThat(response.volume()).isEqualTo(25_000L);
    }

    private DailyStockPrice price(int open, int high, int low, int close, long volume) {
        return DailyStockPrice.builder()
                .stockCode("035510")
                .tradeDate(LocalDate.of(2022, 4, 11))
                .marketCategory("KOSPI")
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .closePrice(close)
                .volume(volume)
                .build();
    }
}
