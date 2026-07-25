package dev.gukin.einvestlab.disclosure.infrastructure.dart;

import dev.gukin.einvestlab.disclosure.domain.DisclosureDocumentMissingException;
import dev.gukin.einvestlab.disclosure.domain.DisclosureSourceException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DocumentReader {

    private static final String STATUS_FILE_MISSING = "<status>014</status>";

    public String readBody(String filingNumber, InputStream body) {
        byte[] bytes = readAll(body);
        if (isZip(bytes)) {
            return readBodyEntry(filingNumber, bytes);
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.contains(STATUS_FILE_MISSING)) {
            throw new DisclosureDocumentMissingException("DART 원문 파일 없음: " + filingNumber);
        }
        throw new DisclosureSourceException(
                "DART 원문 응답이 zip 이 아님: " + text.substring(0, Math.min(200, text.length())));
    }

    private byte[] readAll(InputStream body) {
        try {
            return body.readAllBytes();
        } catch (IOException e) {
            throw new DisclosureSourceException("DART 원문 응답 읽기 실패", e);
        }
    }

    private boolean isZip(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private String readBodyEntry(String filingNumber, byte[] zipBytes) {
        String bodyEntryName = filingNumber + ".xml";
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
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
