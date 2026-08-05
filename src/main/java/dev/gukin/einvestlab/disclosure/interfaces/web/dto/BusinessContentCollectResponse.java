package dev.gukin.einvestlab.disclosure.interfaces.web.dto;

import dev.gukin.einvestlab.disclosure.application.BusinessContentCollectResult;

public record BusinessContentCollectResponse(String result) {

    public static BusinessContentCollectResponse from(BusinessContentCollectResult result) {
        return new BusinessContentCollectResponse(result.name());
    }
}
