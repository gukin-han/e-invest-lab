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

    private static final Pattern TOKEN = Pattern.compile("\\S+");
    private static final Pattern EPS_LABEL = Pattern.compile("EPS(\\(.*\\))?");
    private static final Pattern YEAR = Pattern.compile("(20\\d{2})([EFA])?");
    private static final Pattern NUMBER = Pattern.compile("[-△(]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?\\)?");
    private static final Pattern PARENTHESIZED = Pattern.compile("\\(.*\\)");

    private static final int YEAR_HEADER_LOOKBACK = 10;
    private static final int DATA_ROW_LOOKAHEAD = 14;
    private static final int COLUMN_TOLERANCE = 10;
    private static final int MIN_FIGURES = 2;

    public EpsExtraction parse(String text) {
        List<List<Token>> lines = text.lines().map(EpsSummaryTableParser::tokenize).toList();

        boolean anchorFound = false;
        for (int i = 0; i < lines.size(); i++) {
            List<Token> line = lines.get(i);
            if (line.isEmpty()) {
                continue;
            }
            if (matches(EPS_LABEL, line.getFirst()) && !numberTokens(line).isEmpty()) {
                anchorFound = true;
                List<EpsFigure> figures = parseVertical(lines, i);
                if (figures.size() >= MIN_FIGURES) {
                    return EpsExtraction.extracted(figures);
                }
            } else if (containsEpsLabel(line) && numberTokens(line).isEmpty()) {
                List<EpsFigure> figures = parseHorizontal(lines, i);
                if (figures.size() >= MIN_FIGURES) {
                    return EpsExtraction.extracted(figures);
                }
            }
        }
        return anchorFound ? EpsExtraction.failed() : EpsExtraction.noSummaryTable();
    }

    private List<EpsFigure> parseVertical(List<List<Token>> lines, int epsLineIndex) {
        List<Token> yearTokens = findYearHeaderAbove(lines, epsLineIndex);
        if (yearTokens.isEmpty()) {
            return List.of();
        }
        List<Token> numbers = numberTokens(lines.get(epsLineIndex));
        return pairByColumn(yearTokens, numbers);
    }

    private List<Token> findYearHeaderAbove(List<List<Token>> lines, int epsLineIndex) {
        for (int i = epsLineIndex - 1; i >= Math.max(0, epsLineIndex - YEAR_HEADER_LOOKBACK); i--) {
            List<Token> years = yearTokens(lines.get(i));
            if (years.size() >= 2) {
                return years;
            }
        }
        return List.of();
    }

    private List<EpsFigure> pairByColumn(List<Token> years, List<Token> numbers) {
        record Pair(Token year, Token number, int distance) {
        }
        List<Pair> candidates = new ArrayList<>();
        for (Token year : years) {
            for (Token number : numbers) {
                int distance = Math.abs(year.endColumn() - number.endColumn());
                if (distance <= COLUMN_TOLERANCE) {
                    candidates.add(new Pair(year, number, distance));
                }
            }
        }
        candidates.sort(Comparator.comparingInt(Pair::distance));

        Set<Token> usedYears = new HashSet<>();
        Set<Token> usedNumbers = new HashSet<>();
        List<EpsFigure> figures = new ArrayList<>();
        for (Pair pair : candidates) {
            if (!usedYears.add(pair.year()) || !usedNumbers.add(pair.number())) {
                continue;
            }
            figures.add(toFigure(pair.year(), pair.number()));
        }
        figures.sort(Comparator.comparingInt(EpsFigure::fiscalYear));
        return figures;
    }

    private List<EpsFigure> parseHorizontal(List<List<Token>> lines, int headerLineIndex) {
        List<Token> header = lines.get(headerLineIndex);
        List<Token> labels = header.stream()
                .filter(token -> !matches(PARENTHESIZED, token))
                .toList();
        int epsIndex = indexOfEpsLabel(labels);
        if (epsIndex < 0) {
            return List.of();
        }
        Token epsLabel = labels.get(epsIndex);

        List<EpsFigure> figures = new ArrayList<>();
        int limit = Math.min(lines.size(), headerLineIndex + 1 + DATA_ROW_LOOKAHEAD);
        for (int i = headerLineIndex + 1; i < limit; i++) {
            List<Token> row = lines.get(i);
            if (row.isEmpty() || !matches(YEAR, row.getFirst())) {
                continue;
            }
            Token year = row.getFirst();
            List<Token> numbers = numberTokens(row.subList(1, row.size()));
            Token value = pickByIndexOrColumn(numbers, epsIndex, epsLabel);
            if (value != null) {
                figures.add(toFigure(year, value));
            }
        }
        return figures;
    }

    private Token pickByIndexOrColumn(List<Token> numbers, int epsIndex, Token epsLabel) {
        if (numbers.size() > epsIndex && countsAlign(numbers, epsIndex, epsLabel)) {
            return numbers.get(epsIndex);
        }
        return numbers.stream()
                .filter(number -> Math.abs(number.endColumn() - epsLabel.endColumn()) <= COLUMN_TOLERANCE)
                .min(Comparator.comparingInt(number -> Math.abs(number.endColumn() - epsLabel.endColumn())))
                .orElse(null);
    }

    private boolean countsAlign(List<Token> numbers, int epsIndex, Token epsLabel) {
        Token byIndex = numbers.get(epsIndex);
        return Math.abs(byIndex.endColumn() - epsLabel.endColumn()) <= COLUMN_TOLERANCE * 2;
    }

    private int indexOfEpsLabel(List<Token> labels) {
        for (int i = 0; i < labels.size(); i++) {
            if (matches(EPS_LABEL, labels.get(i))) {
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
        int fiscalYear = Integer.parseInt(matcher.group(1));
        String suffix = matcher.group(2);
        boolean estimated = "E".equals(suffix) || "F".equals(suffix);
        return new EpsFigure(fiscalYear, estimated, parseNumber(numberToken.text()));
    }

    private BigDecimal parseNumber(String raw) {
        boolean negative = raw.startsWith("-") || raw.startsWith("△")
                || (raw.startsWith("(") && raw.endsWith(")"));
        BigDecimal value = new BigDecimal(raw.replaceAll("[-△(),]", ""));
        return negative ? value.negate() : value;
    }

    private boolean containsEpsLabel(List<Token> line) {
        return line.stream().anyMatch(token -> matches(EPS_LABEL, token));
    }

    private List<Token> yearTokens(List<Token> line) {
        return line.stream().filter(token -> matches(YEAR, token)).toList();
    }

    private List<Token> numberTokens(List<Token> line) {
        return line.stream().filter(token -> matches(NUMBER, token)).toList();
    }

    private static boolean matches(Pattern pattern, Token token) {
        return pattern.matcher(token.text()).matches();
    }

    private static List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(line);
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(), matcher.end()));
        }
        return tokens;
    }

    private record Token(String text, int endColumn) {
    }
}
