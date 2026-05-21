package dev.gukin.einvestlab.company;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Builds synthetic corpCode-{N}x.zip by repeating the inner &lt;list&gt; payload N times.
 * Used to sweep input-file size while keeping the streaming pipeline identical.
 *
 * <p>corp_code values collide across copies; V3 relies on {@code ON DUPLICATE KEY UPDATE}
 * so the DB ends up with the original 117k rows. Heap footprint is what we're measuring.
 */
public class CorpCodeSyntheticZip {

    public static void main(String[] args) throws IOException {
        int multiplier = Integer.parseInt(args[0]);
        Path home = Path.of(System.getProperty("user.home"));
        Path original = home.resolve(".cache/e-invest-lab/corpCode.zip");
        Path output = home.resolve(".cache/e-invest-lab/corpCode-" + multiplier + "x.zip");

        if (Files.exists(output)) {
            System.out.printf("cached: %s (%.1fMB)%n", output, Files.size(output) / 1024.0 / 1024);
            return;
        }
        if (!Files.exists(original)) {
            throw new IllegalStateException("original zip not found, run a 1x experiment first: " + original);
        }

        long start = System.nanoTime();

        byte[] originalXml;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(original))) {
            zis.getNextEntry();
            originalXml = zis.readAllBytes();
        }

        String xml = new String(originalXml, StandardCharsets.UTF_8);
        int resultOpen = xml.indexOf("<result>");
        int resultClose = xml.lastIndexOf("</result>");
        if (resultOpen < 0 || resultClose < 0) {
            throw new IllegalStateException("unexpected XML structure");
        }
        byte[] prelude = xml.substring(0, resultOpen + "<result>".length())
            .getBytes(StandardCharsets.UTF_8);
        byte[] inner = xml.substring(resultOpen + "<result>".length(), resultClose)
            .getBytes(StandardCharsets.UTF_8);
        byte[] postlude = "</result>".getBytes(StandardCharsets.UTF_8);

        Files.createDirectories(output.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {
            zos.putNextEntry(new ZipEntry("CORPCODE.xml"));
            zos.write(prelude);
            for (int i = 0; i < multiplier; i++) {
                zos.write(inner);
            }
            zos.write(postlude);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("created %s: %.1fMB, %dms%n",
            output, Files.size(output) / 1024.0 / 1024, elapsedMs);
    }
}
