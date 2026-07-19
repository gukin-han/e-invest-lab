package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BusinessContentExtractor {

    private static final Pattern SECTION_START = Pattern.compile("<TITLE[^>]*>\\s*II\\.\\s*사업의\\s*내용");
    private static final Pattern SECTION_END = Pattern.compile("<TITLE[^>]*>\\s*III\\.");

    private static final int MIN_LENGTH = 1_000;
    private static final int MAX_LENGTH = 5_000_000;

    public String extract(String documentXml) {
        Matcher start = SECTION_START.matcher(documentXml);
        if (!start.find()) {
            throw new DisclosureSourceException("사업의 내용 섹션 시작 경계 없음");
        }
        Matcher end = SECTION_END.matcher(documentXml);
        if (!end.find(start.end())) {
            throw new DisclosureSourceException("사업의 내용 섹션 끝 경계 없음");
        }
        String section = documentXml.substring(start.start(), end.start());
        verifyInvariants(section);
        return section;
    }

    private void verifyInvariants(String section) {
        if (section.length() < MIN_LENGTH) {
            throw new DisclosureSourceException("사업의 내용 섹션이 비정상적으로 짧음: " + section.length() + "자");
        }
        if (section.length() > MAX_LENGTH) {
            throw new DisclosureSourceException("사업의 내용 섹션이 비정상적으로 긺: " + section.length() + "자");
        }
        if (section.indexOf("<TITLE", 1) < 0) {
            throw new DisclosureSourceException("사업의 내용 섹션에 하위 항목 제목 없음");
        }
    }
}
