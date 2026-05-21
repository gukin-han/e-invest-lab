package dev.gukin.einvestlab.company;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Testcontainers
public class CorpCodeFetchSmokeTestV3 {

    public static final int BATCH_SIZE = Integer.parseInt(
        System.getenv().getOrDefault("EXPERIMENT_BATCH_SIZE", "1000"));
    public static final int ITERATIONS = Integer.parseInt(
        System.getenv().getOrDefault("EXPERIMENT_ITERATIONS", "1"));
    public static final int INPUT_MULTIPLIER = Integer.parseInt(
        System.getenv().getOrDefault("EXPERIMENT_INPUT_MULTIPLIER", "1"));
    public static final int RESOLUTION = 5000;
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Path ZIP_CACHE_DIR =
        Path.of(System.getProperty("user.home"), ".cache/e-invest-lab");
    private static final Path ZIP_CACHE_1X = ZIP_CACHE_DIR.resolve("corpCode.zip");

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("e_invest_lab_exp")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);

    static JdbcTemplate jdbc;
    static PrintWriter csvWriter;
    static long startNs;

    String apiKey = System.getenv("DART_API_KEY");
    String baseUrl = System.getenv("DART_BASE_URL");
    HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void setUp() throws IOException {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(mysql.getJdbcUrl() + "?rewriteBatchedStatements=true");
        ds.setUsername(mysql.getUsername());
        ds.setPassword(mysql.getPassword());
        jdbc = new JdbcTemplate(ds);

        jdbc.execute("DROP TABLE IF EXISTS companies");
        jdbc.execute("""
            CREATE TABLE companies (
                corp_code CHAR(8) NOT NULL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                english_name VARCHAR(255),
                stock_code CHAR(6),
                master_modify_date DATE,
                stock_name VARCHAR(255),
                market_type VARCHAR(16),
                fiscal_year_end_month TINYINT,
                profile_modify_date DATE,
                profile_synced_at TIMESTAMP NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """);

        String label = System.getenv().getOrDefault("EXPERIMENT_HEAP_LABEL", "default");
        Path csvPath = Path.of("docs/adr/streaming-memory-experiment/v3-heap-" + label + ".csv");
        Files.createDirectories(csvPath.getParent());
        csvWriter = new PrintWriter(Files.newBufferedWriter(csvPath));
        csvWriter.println("count,iteration,heap_used_mb,heap_max_mb,elapsed_ms");
    }

    @AfterAll
    static void tearDown() {
        if (csvWriter != null) csvWriter.close();
    }

    static int currentIteration = 1;

    @Test
    void shouldStreamCorpCodeIntoDb() throws IOException, InterruptedException, XMLStreamException {
        int cumulativeCount = 0;
        startNs = System.nanoTime();

        for (int iter = 1; iter <= ITERATIONS; iter++) {
            currentIteration = iter;
            cumulativeCount = streamOnce(cumulativeCount);
            System.out.printf("--- iteration %d done, cumulative=%d ---%n", iter, cumulativeCount);
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        Integer rowsInDb = jdbc.queryForObject("SELECT COUNT(*) FROM companies", Integer.class);
        System.out.printf("총 처리: %d, DB 행: %d, 시간: %dms%n", cumulativeCount, rowsInDb, elapsedMs);
    }

    private int streamOnce(int initialCount) throws IOException, InterruptedException, XMLStreamException {
        List<CompanyRow> batch = new ArrayList<>(BATCH_SIZE);
        int cumulativeCount = initialCount;

        try (InputStream zipIn = openCorpCodeZip();
             ZipInputStream zipStream = new ZipInputStream(zipIn)) {

            ZipEntry entry = zipStream.getNextEntry();
            if (entry == null || !entry.getName().endsWith(".xml")) {
                throw new IllegalStateException("expected xml file in zip but got " + entry);
            }

            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(zipStream);

            try {
                String corpCode = null;
                String name = null;
                String englishName = null;
                String stockCode = null;
                String modifyDate = null;

                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamReader.START_ELEMENT) {
                        switch (reader.getLocalName()) {
                            case "list" -> corpCode = name = englishName = stockCode = modifyDate = null;
                            case "corp_code" -> corpCode = reader.getElementText();
                            case "corp_name" -> name = reader.getElementText();
                            case "corp_eng_name" -> englishName = reader.getElementText();
                            case "stock_code" -> stockCode = reader.getElementText();
                            case "modify_date" -> modifyDate = reader.getElementText();
                        }
                    } else if (event == XMLStreamReader.END_ELEMENT && "list".equals(reader.getLocalName())) {
                        batch.add(new CompanyRow(
                            corpCode, name, englishName,
                            normalizeStockCode(stockCode),
                            parseDate(modifyDate)
                        ));
                        cumulativeCount++;
                        if (batch.size() == BATCH_SIZE) {
                            flush(batch, cumulativeCount);
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    flush(batch, cumulativeCount);
                }
            } finally {
                reader.close();
            }
        }
        return cumulativeCount;
    }

    private InputStream openCorpCodeZip() throws IOException, InterruptedException {
        if (!Files.exists(ZIP_CACHE_1X)) {
            Files.createDirectories(ZIP_CACHE_DIR);
            URI uri = URI.create(baseUrl + "/corpCode.xml?crtfc_key=" + apiKey);
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<InputStream> res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = res.body()) {
                Files.copy(body, ZIP_CACHE_1X);
            }
        }
        Path zipPath = INPUT_MULTIPLIER == 1
            ? ZIP_CACHE_1X
            : ZIP_CACHE_DIR.resolve("corpCode-" + INPUT_MULTIPLIER + "x.zip");
        if (!Files.exists(zipPath)) {
            throw new IllegalStateException("synthetic zip missing: " + zipPath
                + " — run CorpCodeSyntheticZip with multiplier=" + INPUT_MULTIPLIER);
        }
        return Files.newInputStream(zipPath);
    }

    private void flush(List<CompanyRow> batch, int totalCount) {
        String sql = """
            INSERT INTO companies (corp_code, name, english_name, stock_code, master_modify_date)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              name = VALUES(name),
              english_name = VALUES(english_name),
              stock_code = VALUES(stock_code),
              master_modify_date = VALUES(master_modify_date)
            """;
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setString(1, row.corpCode());
            ps.setString(2, row.name());
            ps.setString(3, row.englishName());
            ps.setString(4, row.stockCode());
            ps.setObject(5, row.modifyDate());
        });
        batch.clear();

        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMB = rt.maxMemory() / 1024 / 1024;
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        csvWriter.printf("%d,%d,%d,%d,%d%n", totalCount, currentIteration, usedMB, maxMB, elapsedMs);
        if (totalCount % RESOLUTION == 0) {
            System.out.printf("processed=%d  heap=%dMB / %dMB%n", totalCount, usedMB, maxMB);
        }
    }

    private static String normalizeStockCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDate.parse(raw, DART_DATE);
    }

    public record CompanyRow(
        String corpCode,
        String name,
        String englishName,
        String stockCode,
        LocalDate modifyDate
    ) {
    }
}
