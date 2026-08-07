package dev.gukin.einvestlab.research.interfaces.web;

import dev.gukin.einvestlab.global.web.ApiResponse;
import dev.gukin.einvestlab.research.application.EpsStatisticsQuery;
import dev.gukin.einvestlab.research.interfaces.web.dto.EpsConsensusResponse;
import dev.gukin.einvestlab.research.interfaces.web.dto.EpsRevisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EpsStatisticsController {

    private final EpsStatisticsQuery statisticsQuery;
    private final Clock clock;

    @GetMapping("/api/stocks/{stockCode}/eps-consensus")
    public ApiResponse<EpsConsensusResponse> consensus(@PathVariable String stockCode) {
        return ApiResponse.of(EpsConsensusResponse.from(statisticsQuery.consensus(stockCode, clock.instant())));
    }

    @GetMapping("/api/stocks/{stockCode}/eps-revisions")
    public ApiResponse<List<EpsRevisionResponse>> revisions(@PathVariable String stockCode,
                                                            @RequestParam int fiscalYear) {
        return ApiResponse.of(statisticsQuery.revisions(stockCode, fiscalYear).stream()
                .map(EpsRevisionResponse::from)
                .toList());
    }
}
