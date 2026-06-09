package dev.gukin.einvestlab.company.interfaces;

import dev.gukin.einvestlab.company.domain.CompanyRegistrySourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CompanyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CompanyExceptionHandler.class);

    @ExceptionHandler(CompanyRegistrySourceException.class)
    ProblemDetail handleRegistrySource(CompanyRegistrySourceException e) {
        log.warn("company registry source failed.", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problem.setProperty("code", "COMPANY_REGISTRY_SOURCE_ERROR");
        return problem;
    }
}
