package dev.gukin.einvestlab.company.infra.http;

/**
 * DART 호출/응답 처리 중 발생한 인프라 예외.
 * 회사 도메인 한정 — DART 는 회사 등록부에서만 쓰므로 global 이 아닌 도메인 안에 둔다.
 */
public class DartClientException extends RuntimeException {

    public DartClientException(String message) {
        super(message);
    }

    public DartClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
