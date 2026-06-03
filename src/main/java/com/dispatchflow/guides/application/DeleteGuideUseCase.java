package com.dispatchflow.guides.application;

import com.dispatchflow.guides.domain.entities.DispatchGuide;
import com.dispatchflow.guides.domain.repositories.GuideRepository;
import com.dispatchflow.guides.domain.valueobjects.GuideId;
import com.dispatchflow.shared.domain.DomainError;

import java.time.Clock;

public class DeleteGuideUseCase {

    private final GuideRepository guideRepository;
    private final Clock clock;

    public DeleteGuideUseCase(GuideRepository guideRepository, Clock clock) {
        this.guideRepository = guideRepository;
        this.clock = clock;
    }

    public void execute(String id) {
        DispatchGuide guide = guideRepository.findById(GuideId.create(id))
                .orElseThrow(() -> DomainError.notFound("Guide " + id + " not found"));

        if (guide.isDeleted()) {
            throw DomainError.notFound("Guide " + id + " not found");
        }

        guide.markDeleted(clock.instant());
        guideRepository.save(guide);
    }
}
