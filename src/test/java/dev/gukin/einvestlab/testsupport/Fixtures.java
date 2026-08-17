package dev.gukin.einvestlab.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Fixtures {

    private Fixtures() {
    }

    public static String read(String resourcePath) {
        try (InputStream input = Fixtures.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("픽스처 없음: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
