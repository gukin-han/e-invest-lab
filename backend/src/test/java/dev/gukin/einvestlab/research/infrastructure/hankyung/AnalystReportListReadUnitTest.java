package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.research.domain.AnalystReportListing;
import dev.gukin.einvestlab.testsupport.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("한경 컨센서스 목록 파싱 단위 테스트")
class AnalystReportListReadUnitTest {

    private final AnalystReportListReader reader = new AnalystReportListReader();

    @Test
    @DisplayName("실물 목록 HTML 에서 종목·리포트 메타를 행 단위로 읽는다")
    void shouldReadListingsFromRealFixture() {
        List<AnalystReportListing> listings = reader.readListings(fixture());

        assertThat(listings).isNotEmpty();
        assertThat(listings.getFirst()).isEqualTo(new AnalystReportListing(
                651490L,
                "016360",
                "삼성증권",
                "삼성증권(016360) 최대실적 지속 경신",
                "LS증권",
                "전배승",
                LocalDate.of(2026, 8, 5),
                115_000L,
                "Hold"
        ));
        assertThat(listings)
                .allSatisfy(listing -> {
                    assertThat(listing.stockCode()).matches("\\d{6}");
                    assertThat(listing.reportIdx()).isPositive();
                    assertThat(listing.broker()).isNotBlank();
                });
    }

    @Test
    @DisplayName("종목명(코드) 패턴이 없는 행은 건너뛴다")
    void shouldSkipRowsWithoutCompanyPattern() {
        String html = """
                <table><tbody>
                <tr>
                  <td class="first txt_number">2026-08-05</td>
                  <td><a href="/analysis/downpdf?report_idx=1">시장 코멘트: 금리 전망</a></td>
                  <td>-</td><td></td><td>홍길동</td><td>가나증권</td><td></td><td></td><td></td>
                </tr>
                <tr>
                  <td class="first txt_number">2026-08-05</td>
                  <td><a href="/analysis/downpdf?report_idx=2">가나전자(123456) 실적 리뷰</a></td>
                  <td>10,000</td><td>Buy</td><td>홍길동</td><td>가나증권</td><td></td><td></td><td></td>
                </tr>
                </tbody></table>
                """;

        List<AnalystReportListing> listings = reader.readListings(html);

        assertThat(listings).hasSize(1);
        assertThat(listings.getFirst().reportIdx()).isEqualTo(2L);
        assertThat(listings.getFirst().stockCode()).isEqualTo("123456");
    }

    @Test
    @DisplayName("목표주가가 없는 행은 null 로 읽는다")
    void shouldReadMissingTargetPriceAsNull() {
        String html = """
                <table><tbody>
                <tr>
                  <td class="first txt_number">2026-08-05</td>
                  <td><a href="/analysis/downpdf?report_idx=3">가나전자(123456) 리뷰</a></td>
                  <td></td><td></td><td></td><td>가나증권</td><td></td><td></td><td></td>
                </tr>
                </tbody></table>
                """;

        AnalystReportListing listing = reader.readListings(html).getFirst();

        assertThat(listing.targetPrice()).isNull();
        assertThat(listing.opinion()).isNull();
        assertThat(listing.authors()).isNull();
    }

    @Test
    @DisplayName("페이징 영역에서 마지막 페이지 번호를 읽는다 (실물 픽스처는 1페이지)")
    void shouldReadLastPageFromRealFixture() {
        assertThat(reader.readLastPage(fixture())).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 페이지 링크가 있으면 가장 큰 페이지 번호를 돌려준다")
    void shouldReadMaxPageAmongLinks() {
        String html = """
                <div class="paging">
                  <span>1</span>
                  <a href="/analysis/list?now_page=2">2</a>
                  <a href="/analysis/list?now_page=3">3</a>
                  <a href="/analysis/list?now_page=12" class="btn last">끝으로</a>
                </div>
                """;

        assertThat(reader.readLastPage(html)).isEqualTo(12);
    }

    private String fixture() {
        return Fixtures.read("/fixtures/research/hankyung-list.html");
    }
}
