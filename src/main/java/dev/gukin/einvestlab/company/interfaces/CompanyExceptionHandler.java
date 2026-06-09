package dev.gukin.einvestlab.company.interfaces;

import dev.gukin.einvestlab.company.domain.CompanyRegistrySourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CompanyExceptionHandler {

    @ExceptionHandler(CompanyRegistrySourceException.class)
    ProblemDetail handleRegistrySource(CompanyRegistrySourceException e) {
        log.warn("company registry source failed.", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problem.setProperty("code", "COMPANY_REGISTRY_SOURCE_ERROR");
        return problem;
    }
}
