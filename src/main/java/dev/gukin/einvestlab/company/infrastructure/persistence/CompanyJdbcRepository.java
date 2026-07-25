package dev.gukin.einvestlab.company.infrastructure.db;

import dev.gukin.einvestlab.company.domain.Company;
import dev.gukin.einvestlab.global.id.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CompanyJdbcRepository {

    private static final String UPSERT_COMPANIES_SQL = """
            INSERT INTO companies (
                id,
                corp_code,
                name,
                english_name,
                stock_code,
                registry_modified_date
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                english_name = VALUES(english_name),
                stock_code = VALUES(stock_code),
                registry_modified_date = VALUES(registry_modified_date)
            """;

    private final JdbcTemplate jdbc;

    public int upsertCompanies(List<Company> companies) {
        jdbc.batchUpdate(UPSERT_COMPANIES_SQL, companies, companies.size(), (statement, company) -> {
            statement.setBytes(1, Ids.toBytes(company.getId()));
            statement.setString(2, company.getCorpCode());
            statement.setString(3, company.getName());
            statement.setString(4, company.getEnglishName());
            statement.setString(5, company.getStockCode());
            statement.setDate(6, Date.valueOf(company.getRegistryModifiedDate()));
        });
        return companies.size();
    }
}
