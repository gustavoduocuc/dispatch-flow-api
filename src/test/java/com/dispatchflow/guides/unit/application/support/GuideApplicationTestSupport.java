package com.dispatchflow.guides.unit.application.support;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.GuidePdfEfsStorage;
import com.dispatchflow.guides.application.ports.EfsStoragePort;
import com.dispatchflow.guides.domain.repositories.GuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.services.GuidePdfPathBuilder;

import java.time.Clock;

public final class GuideApplicationTestSupport {

    public static final byte[] PDF_BYTES = new byte[] {37, 80, 68, 70};

    private GuideApplicationTestSupport() {
    }

    public static CreateGuideUseCase createGuideUseCase(GuideRepository repository, Clock clock) {
        return new CreateGuideUseCase(
                repository,
                new GuideNumberGenerator(),
                guidePdfEfsStorage(),
                clock);
    }

    public static GuidePdfEfsStorage guidePdfEfsStorage() {
        return new GuidePdfEfsStorage(
                new GuidePdfPathBuilder(),
                guide -> PDF_BYTES,
                stubEfsStorage());
    }

    public static EfsStoragePort stubEfsStorage() {
        return new EfsStoragePort() {
            @Override
            public String write(String relativePath, byte[] content) {
                return "/efs/" + relativePath;
            }

            @Override
            public byte[] read(String absolutePath) {
                return PDF_BYTES;
            }
        };
    }
}
