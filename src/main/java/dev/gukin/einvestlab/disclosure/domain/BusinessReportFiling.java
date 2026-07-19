package dev.gukin.einvestlab.disclosure.domain;

import java.time.LocalDate;

public record BusinessReportFiling(String corpCode, String filingNumber, LocalDate filedDate) {
}
