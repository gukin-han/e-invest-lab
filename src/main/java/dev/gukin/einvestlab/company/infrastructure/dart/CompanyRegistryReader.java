package dev.gukin.einvestlab.company.infrastructure.dart;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.company.domain.CompanyRegistrySourceException;
import dev.gukin.einvestlab.global.id.Ids;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Component
public class CompanyRegistryReader {

    private static final DateTimeFormatter DART_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public void read(InputStream zipBody, Consumer<Company> handler) {
        try (ZipInputStream zipInput = new ZipInputStream(zipBody)) {
            ZipEntry entry = zipInput.getNextEntry();
            if (entry == null || !entry.getName().endsWith(".xml")) {
                throw new CompanyRegistrySourceException("corpCode zip 안에 .xml 항목이 없음: " + entry);
            }
            parseCompanies(zipInput, handler);
        } catch (ZipException e) {
            throw new CompanyRegistrySourceException("corpCode 응답이 zip 이 아님 (DART 에러 본문 가능성)", e);
        } catch (IOException | XMLStreamException e) {
            throw new CompanyRegistrySourceException("corpCode 스트림 파싱 실패", e);
        }
    }

    private void parseCompanies(InputStream xmlInput, Consumer<Company> handler) throws XMLStreamException {
        XMLStreamReader reader = createXmlReader(xmlInput);
        try {
            String corpCode = null;
            String name = null;
            String englishName = null;
            String stockCode = null;
            String modifyDate = null;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "list" -> corpCode = name = englishName = stockCode = modifyDate = null;
                        case "corp_code" -> corpCode = reader.getElementText();
                        case "corp_name" -> name = reader.getElementText();
                        case "corp_eng_name" -> englishName = reader.getElementText();
                        case "stock_code" -> stockCode = reader.getElementText();
                        case "modify_date" -> modifyDate = reader.getElementText();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "list".equals(reader.getLocalName())) {
                    handler.accept(Company.builder()
                            .id(Ids.generate())
                            .corpCode(corpCode)
                            .name(name)
                            .englishName(englishName)
                            .stockCode(normalizeStockCode(stockCode))
                            .registryModifiedDate(parseDate(modifyDate))
                            .build());
                }
            }
        } finally {
            reader.close();
        }
    }

    private static String normalizeStockCode(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return LocalDate.parse(raw, DART_DATE_FORMATTER);
    }

    private static XMLStreamReader createXmlReader(InputStream xmlInput) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return factory.createXMLStreamReader(xmlInput);
    }
}
