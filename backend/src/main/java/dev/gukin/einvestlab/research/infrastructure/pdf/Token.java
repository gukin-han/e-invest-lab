package dev.gukin.einvestlab.research.infrastructure.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Token(String text, int endColumn) {

    private static final Pattern TOKEN = Pattern.compile("\\S+");

    static List<List<Token>> linesOf(String text) {
        return text.lines().map(Token::tokenize).toList();
    }

    static List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(line);
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(), matcher.end()));
        }
        return tokens;
    }

    boolean matches(Pattern pattern) {
        return pattern.matcher(text).matches();
    }

    int distanceTo(Token other) {
        return Math.abs(endColumn - other.endColumn());
    }
}
