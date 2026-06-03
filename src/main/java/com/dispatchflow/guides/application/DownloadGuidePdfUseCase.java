package com.dispatchflow.guides.application;

import com.dispatchflow.guides.application.dto.GuidePdfDownload;
import com.dispatchflow.guides.application.ports.EfsStoragePort;
import com.dispatchflow.guides.domain.entities.DispatchGuide;
import com.dispatchflow.guides.domain.repositories.GuideRepository;
import com.dispatchflow.guides.domain.valueobjects.GuideId;
import com.dispatchflow.shared.domain.DomainError;

public class DownloadGuidePdfUseCase {

    private final GuideRepository guideRepository;
    private final EfsStoragePort efsStorage;

    public DownloadGuidePdfUseCase(GuideRepository guideRepository, EfsStoragePort efsStorage) {
        this.guideRepository = guideRepository;
        this.efsStorage = efsStorage;
    }

    public GuidePdfDownload execute(String id) {
        DispatchGuide guide = guideRepository.findById(GuideId.create(id))
                .orElseThrow(() -> DomainError.notFound("Guide " + id + " not found"));

        if (guide.isDeleted()) {
            throw DomainError.notFound("Guide " + id + " not found");
        }
        if (guide.getEfsPath() == null || guide.getEfsPath().isBlank()) {
            throw DomainError.notFound("PDF for guide " + id + " not found");
        }

        byte[] content = efsStorage.read(guide.getEfsPath());
        String fileName = "guide-" + guide.getId().value() + ".pdf";
        return new GuidePdfDownload(content, fileName);
    }
}
