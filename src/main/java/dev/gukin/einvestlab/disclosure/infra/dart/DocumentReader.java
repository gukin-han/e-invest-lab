package dev.gukin.einvestlab.disclosure.infra.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DocumentReader {

    public String readBody(String filingNumber, InputStream zipBody) {
        String bodyEntryName = filingNumber + ".xml";
        try (ZipInputStream zip = new ZipInputStream(zipBody)) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (bodyEntryName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new DisclosureSourceException("DART 원문 zip 읽기 실패", e);
        }
        throw new DisclosureSourceException("DART 원문 zip에 본문 엔트리 없음: " + bodyEntryName);
    }
}
