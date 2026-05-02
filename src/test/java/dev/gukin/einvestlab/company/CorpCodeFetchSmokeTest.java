package dev.gukin.einvestlab.company;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CorpCodeFetchSmokeTest {

    public static final int BATCH_SIZE = 1000;
    public static final int RESOLUTION = 500;
    String apiKey = System.getenv("DART_API_KEY");
    String baseUrl = System.getenv("DART_BASE_URL");
    HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldFetchCorpCodeWithValidDartApiKey() throws IOException, InterruptedException, XMLStreamException {
        URI uri = URI.create(baseUrl + "/corpCode.xml?crtfc_key=" + apiKey);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        List<CompanyRow> batch = new ArrayList<>(BATCH_SIZE);
        int totalCount = 0;
        int batchCount = 0;

        try (InputStream body = response.body();
             ZipInputStream zipStream = new ZipInputStream(body);
        ) {

            ZipEntry entry = zipStream.getNextEntry();

            if (entry == null || !entry.getName().endsWith(".xml")) {
                throw new IllegalStateException("expected xml file in zip but got " + entry);
            }

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(zipStream);

            try {
                String corpCode = null;
                String name = null;
                String englishName = null;
                String stockCode = null;
                String modifyDate = null;

                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamReader.START_ELEMENT) {
                        String tagName = reader.getLocalName();

                        switch (tagName) {
                            case "list" -> corpCode = name = englishName = stockCode = modifyDate = null;
                            case "corp_code" -> corpCode = reader.getElementText();
                            case "corp_name" -> name = reader.getElementText();
                            case "corp_eng_name" -> englishName = reader.getElementText();
                            case "stock_code" -> stockCode = reader.getElementText();
                            case "modify_date" -> modifyDate = reader.getElementText();
                        }
                    } else if (event == XMLStreamReader.END_ELEMENT) {
                        String tagName = reader.getLocalName();
                        if ("list".equals(tagName)) {
                            batch.add(new CompanyRow(corpCode, name, englishName, stockCode, modifyDate));
                            totalCount++;
                            batchCount++;
                            logProgress(totalCount);
                            if (batchCount == BATCH_SIZE) {
                                batch.clear();
                            }
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }

        System.out.println("총 처리: " + totalCount);
        assertThat(totalCount).isGreaterThan(50_000);

//        assertThat(result).hasSize(5);
//        assertThat(result).allSatisfy(row -> {
//            assertThat(row.corpCode()).hasSize(8); // DART corp_code는 8자리
//            assertThat(row.name()).isNotBlank();
//        });
    }

    public record CompanyRow(
        String corpCode,
        String name,
        String englishName,
        String stockCode,
        String modifyDate
    ) {
    }

    private void logProgress(int count) {
        if (count % RESOLUTION == 0) {
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long maxMB = rt.maxMemory() / 1024 / 1024;
            System.out.printf("processed=%d  heap=%dMB / %dMB%n", count, usedMB, maxMB);
        }
    }
}
