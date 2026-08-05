package dev.gukin.einvestlab.research.domain;

public interface AnalystReportRepository {

    AnalystReport save(AnalystReport analystReport);

    boolean existsByReportIdx(long reportIdx);
}
