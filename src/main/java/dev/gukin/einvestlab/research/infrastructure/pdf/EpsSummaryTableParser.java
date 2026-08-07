package dev.gukin.einvestlab.research.infrastructure.pdf;

import dev.gukin.einvestlab.research.domain.EpsExtraction;
import dev.gukin.einvestlab.research.domain.EpsFigure;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EpsSummaryTableParser {

    private static final Pattern EPS_LABEL = Pattern.compile("EPS(\\(.*\\))?");
    private static final Pattern YEAR = Pattern.compile("(20\\d{2})([EFA])?");
    private static final Pattern NUMBER = Pattern.compile("[-△(]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?\\)?");
    private static final Pattern PARENTHESIZED = Pattern.compile("\\(.*\\)");

    private static final int YEAR_HEADER_LOOKBACK = 20;
    private static final int DATA_ROW_LOOKAHEAD = 14;
    private static final int PAIR_TOLERANCE = 10;
    private static final int INDEX_CHECK_TOLERANCE = 20;
    private static final int MIN_FIGURES = 3;

    public EpsExtraction parse(String text) {
        List<List<Token>> lines = Token.linesOf(text);

        boolean anchorFound = false;
        for (int i = 0; i < lines.size(); i++) {
            List<Token> line = lines.get(i);
            int labelIndex = indexOfEpsLabel(line);
            if (labelIndex < 0) {
                continue;
            }
            List<Token> numbersAfterLabel = numbers(line.subList(labelIndex + 1, line.size()));
            List<EpsFigure> figures;
            if (!numbersAfterLabel.isEmpty()) {
                anchorFound = true;
                figures = parseVertical(lines, i, numbersAfterLabel);
            } else if (numbers(line).isEmpty()) {
                figures = parseHorizontal(lines, i);
            } else {
                continue;
            }
            if (figures.size() >= MIN_FIGURES) {
                return EpsExtraction.extracted(figures);
            }
        }
        return anchorFound ? EpsExtraction.failed() : EpsExtraction.noSummaryTable();
    }

    private List<EpsFigure> parseVertical(List<List<Token>> lines, int epsRowIndex, List<Token> numbers) {
        List<Token> years = findYearHeaderAbove(lines, epsRowIndex);
        return pairByColumn(years, numbers);
    }

    private List<Token> findYearHeaderAbove(List<List<Token>> lines, int epsRowIndex) {
        for (int i = epsRowIndex - 1; i >= Math.max(0, epsRowIndex - YEAR_HEADER_LOOKBACK); i--) {
            List<Token> years = years(lines.get(i));
            if (distinctFiscalYears(years) >= 2) {
                return years;
            }
        }
        return List.of();
    }

    private long distinctFiscalYears(List<Token> years) {
        return years.stream().map(this::fiscalYearOf).distinct().count();
    }

    private int fiscalYearOf(Token yearToken) {
        Matcher matcher = YEAR.matcher(yearToken.text());
        if (!matcher.matches()) {
            throw new IllegalStateException("연도 토큰이 아님: " + yearToken.text());
        }
        return Integer.parseInt(matcher.group(1));
    }

    private List<EpsFigure> pairByColumn(List<Token> years, List<Token> numbers) {
        record Pair(Token year, Token number, int distance) {
        }
        List<Pair> candidates = new ArrayList<>();
        for (Token year : years) {
            for (Token number : numbers) {
                if (year.distanceTo(number) <= PAIR_TOLERANCE) {
                    candidates.add(new Pair(year, number, year.distanceTo(number)));
                }
            }
        }
        candidates.sort(Comparator.comparingInt(Pair::distance)
                .thenComparingInt(pair -> pair.year().endColumn()));

        Set<Token> used = new HashSet<>();
        Set<Integer> usedFiscalYears = new HashSet<>();
        List<EpsFigure> figures = new ArrayList<>();
        for (Pair pair : candidates) {
            if (used.contains(pair.year()) || used.contains(pair.number())
                    || usedFiscalYears.contains(fiscalYearOf(pair.year()))) {
                continue;
            }
            used.add(pair.year());
            used.add(pair.number());
            usedFiscalYears.add(fiscalYearOf(pair.year()));
            figures.add(toFigure(pair.year(), pair.number()));
        }
        figures.sort(Comparator.comparingInt(EpsFigure::fiscalYear));
        return requireIntegerEps(figures);
    }

    private List<EpsFigure> requireIntegerEps(List<EpsFigure> figures) {
        boolean hasFraction = figures.stream()
                .anyMatch(figure -> figure.eps().stripTrailingZeros().scale() > 0);
        return hasFraction ? List.of() : figures;
    }

    private List<EpsFigure> parseHorizontal(List<List<Token>> lines, int headerIndex) {
        List<Token> labels = lines.get(headerIndex).stream()
                .filter(token -> !token.matches(PARENTHESIZED))
                .toList();
        int epsIndex = indexOfEpsLabel(labels);
        if (epsIndex < 0) {
            return List.of();
        }
        Token epsLabel = labels.get(epsIndex);

        List<EpsFigure> figures = new ArrayList<>();
        int limit = Math.min(lines.size(), headerIndex + 1 + DATA_ROW_LOOKAHEAD);
        for (int i = headerIndex + 1; i < limit; i++) {
            List<Token> row = lines.get(i);
            if (row.isEmpty() || !row.getFirst().matches(YEAR)) {
                continue;
            }
            Token value = pickValue(numbers(row.subList(1, row.size())), epsIndex, epsLabel);
            if (value != null) {
                figures.add(toFigure(row.getFirst(), value));
            }
        }
        return requireIntegerEps(figures);
    }

    private Token pickValue(List<Token> numbers, int epsIndex, Token epsLabel) {
        if (epsIndex < numbers.size()
                && numbers.get(epsIndex).distanceTo(epsLabel) <= INDEX_CHECK_TOLERANCE) {
            return numbers.get(epsIndex);
        }
        return numbers.stream()
                .filter(number -> number.distanceTo(epsLabel) <= PAIR_TOLERANCE)
                .min(Comparator.comparingInt(number -> number.distanceTo(epsLabel)))
                .orElse(null);
    }

    private int indexOfEpsLabel(List<Token> labels) {
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).matches(EPS_LABEL)) {
                return i;
            }
        }
        return -1;
    }

    private EpsFigure toFigure(Token yearToken, Token numberToken) {
        Matcher matcher = YEAR.matcher(yearToken.text());
        if (!matcher.matches()) {
            throw new IllegalStateException("연도 토큰이 아님: " + yearToken.text());
        }
        boolean estimated = "E".equals(matcher.group(2)) || "F".equals(matcher.group(2));
        return new EpsFigure(Integer.parseInt(matcher.group(1)), estimated, parseNumber(numberToken.text()));
    }

    private BigDecimal parseNumber(String raw) {
        boolean negative = raw.startsWith("-") || raw.startsWith("△")
                || (raw.startsWith("(") && raw.endsWith(")"));
        BigDecimal value = new BigDecimal(raw.replaceAll("[-△(),]", ""));
        return negative ? value.negate() : value;
    }

    private List<Token> years(List<Token> line) {
        return line.stream().filter(token -> token.matches(YEAR)).toList();
    }

    private List<Token> numbers(List<Token> line) {
        return line.stream().filter(token -> token.matches(NUMBER)).toList();
    }
}
