package dev.gukin.einvestlab.research.infrastructure.hankyung;

import dev.gukin.einvestlab.research.domain.AnalystReportListing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AnalystReportListReader {

    private static final Pattern COMPANY_TITLE = Pattern.compile("^(.+?)\\((\\d{6})\\).*$");
    private static final Pattern REPORT_IDX = Pattern.compile("report_idx=(\\d+)");
    private static final Pattern NOW_PAGE = Pattern.compile("now_page=(\\d+)");

    private static final int PUBLISHED_DATE_CELL = 0;
    private static final int TARGET_PRICE_CELL = 2;
    private static final int OPINION_CELL = 3;
    private static final int AUTHORS_CELL = 4;
    private static final int BROKER_CELL = 5;
    private static final int MIN_CELLS = 6;

    public List<AnalystReportListing> readListings(String html) {
        return Jsoup.parse(html).select("tbody tr").stream()
                .map(this::parseListing)
                .flatMap(Optional::stream)
                .toList();
    }

    public int readLastPage(String html) {
        Document document = Jsoup.parse(html);
        int lastPage = 1;
        for (Element element : document.select("div.paging a, div.paging span")) {
            lastPage = Math.max(lastPage, pageNumberOf(element));
        }
        return lastPage;
    }

    private Optional<AnalystReportListing> parseListing(Element row) {
        Elements cells = row.select("> td");
        if (cells.size() < MIN_CELLS) {
            return Optional.empty();
        }
        Element link = row.selectFirst("a[href*=report_idx=]");
        if (link == null) {
            return Optional.empty();
        }
        Matcher reportIdx = REPORT_IDX.matcher(link.attr("href"));
        String title = link.text().strip();
        Matcher company = COMPANY_TITLE.matcher(title);
        if (!reportIdx.find() || !company.matches()) {
            return Optional.empty();
        }
        return Optional.of(new AnalystReportListing(
                Long.parseLong(reportIdx.group(1)),
                company.group(2),
                company.group(1).strip(),
                title,
                cells.get(BROKER_CELL).text().strip(),
                blankToNull(cells.get(AUTHORS_CELL).text()),
                LocalDate.parse(cells.get(PUBLISHED_DATE_CELL).text().strip()),
                parsePrice(cells.get(TARGET_PRICE_CELL).text()),
                blankToNull(cells.get(OPINION_CELL).text())
        ));
    }

    private int pageNumberOf(Element element) {
        Matcher linkedPage = NOW_PAGE.matcher(element.attr("href"));
        if (linkedPage.find()) {
            return Integer.parseInt(linkedPage.group(1));
        }
        String text = element.text().strip();
        return text.matches("\\d+") ? Integer.parseInt(text) : 1;
    }

    private Long parsePrice(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Long.parseLong(digits);
    }

    private String blankToNull(String text) {
        String stripped = text.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
