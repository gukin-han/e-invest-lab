package dev.gukin.einvestlab.market.application;

import dev.gukin.einvestlab.market.domain.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ShareCountChangeRebuildUseCase {

    private final DailyStockPriceRepository repository;

    public int rebuild(Instant baseTime) {
        return repository.rebuildShareCountChanges(baseTime);
    }
}
