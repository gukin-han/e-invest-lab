package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import dev.gukin.einvestlab.disclosure.domain.BusinessContentSlicer;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BusinessContentSlicerAdapter implements BusinessContentSlicer {

    private static final Pattern TITLE = Pattern.compile("<TITLE[^>]*>([^<]{1,120})</TITLE>");
    private static final Pattern TOP_TITLE = Pattern.compile("^\\s*\\d+\\.\\s*(?:\\((제조서비스업|금융업)\\))?");
    private static final String FINANCIAL_OPERATIONS = "영업의 현황";
    private static final String OVERVIEW = "사업의 개요";
    private static final List<String> GENERAL_TARGETS = List.of(OVERVIEW, "주요 제품", "주요 서비스", "매출");
    private static final List<String> FINANCIAL_TARGETS = List.of(OVERVIEW, FINANCIAL_OPERATIONS);
    private static final int FINANCIAL_OPERATIONS_TEXT_LIMIT = 4_000;
    private static final String TRUNCATION_MARK = "…(이하 생략)";

    @Override
    public String slice(String businessContent) {
        List<Heading> headings = findTopHeadings(businessContent);
        if (headings.isEmpty()) {
            throw new DisclosureSourceException("사업의 내용에 번호 붙은 하위 항목 제목 없음");
        }
        StringBuilder out = new StringBuilder();
        for (Part part : groupByPart(headings)) {
            appendPart(out, part, businessContent);
        }
        if (out.isEmpty()) {
            throw new DisclosureSourceException("LLM 입력으로 선별된 하위 항목 없음");
        }
        return out.toString().strip();
    }

    private List<Heading> findTopHeadings(String businessContent) {
        List<Heading> headings = new ArrayList<>();
        Matcher matcher = TITLE.matcher(businessContent);
        while (matcher.find()) {
            String title = matcher.group(1).replaceAll("\\s+", " ").strip();
            Matcher top = TOP_TITLE.matcher(title);
            if (top.find()) {
                headings.add(new Heading(matcher.start(), top.group(1), title));
            }
        }
        for (int i = 0; i < headings.size(); i++) {
            headings.get(i).sectionEnd = i + 1 < headings.size()
                    ? headings.get(i + 1).start
                    : businessContent.length();
        }
        return headings;
    }

    private List<Part> groupByPart(List<Heading> headings) {
        List<Part> parts = new ArrayList<>();
        Part current = null;
        for (Heading heading : headings) {
            if (current == null || !java.util.Objects.equals(current.name, heading.part)) {
                current = new Part(heading.part);
                parts.add(current);
            }
            current.headings.add(heading);
        }
        return parts;
    }

    private void appendPart(StringBuilder out, Part part, String businessContent) {
        if (part.headings.stream().noneMatch(h -> h.title.contains(OVERVIEW))) {
            throw new DisclosureSourceException(
                    "사업의 개요 항목 없음" + (part.name != null ? " (" + part.name + ")" : ""));
        }
        boolean financial = part.headings.stream().anyMatch(h -> h.title.contains(FINANCIAL_OPERATIONS));
        List<String> targets = financial ? FINANCIAL_TARGETS : GENERAL_TARGETS;
        for (Heading heading : part.headings) {
            if (targets.stream().noneMatch(heading.title::contains)) {
                continue;
            }
            String text = toText(businessContent.substring(heading.start, heading.sectionEnd));
            if (financial && heading.title.contains(FINANCIAL_OPERATIONS)
                    && text.length() > FINANCIAL_OPERATIONS_TEXT_LIMIT) {
                text = text.substring(0, FINANCIAL_OPERATIONS_TEXT_LIMIT) + "\n" + TRUNCATION_MARK;
            }
            if (part.name != null) {
                out.append("[").append(part.name).append("]\n");
            }
            out.append(text).append("\n\n");
        }
    }

    private String toText(String xml) {
        String text = xml;
        text = text.replaceAll("<(TR|TU)[^>]*>", "\n");
        text = text.replaceAll("<(TD|TH|TE)[^>]*>", " | ");
        text = text.replaceAll("<P[^>]*>", "\n");
        text = text.replaceAll("<[^>]+>", " ");
        text = unescapeEntities(text);
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n\\s*\\n+", "\n");
        return text.strip();
    }

    private String unescapeEntities(String text) {
        return text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }

    private static final class Heading {
        private final int start;
        private final String part;
        private final String title;
        private int sectionEnd;

        private Heading(int start, String part, String title) {
            this.start = start;
            this.part = part;
            this.title = title;
        }
    }

    private static final class Part {
        private final String name;
        private final List<Heading> headings = new ArrayList<>();

        private Part(String name) {
            this.name = name;
        }
    }
}
