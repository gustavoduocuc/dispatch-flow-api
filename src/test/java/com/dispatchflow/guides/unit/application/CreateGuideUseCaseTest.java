package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.GuidePdfEfsStorage;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.application.ports.EfsStoragePort;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.services.GuidePdfPathBuilder;
import com.dispatchflow.guides.domain.valueobjects.GuideId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateGuideUseCaseTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-02T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final byte[] PDF_BYTES = new byte[] {37, 80, 68, 70};

    private InMemoryGuideRepository repository;
    private CreateGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        GuidePdfEfsStorage guidePdfEfsStorage = new GuidePdfEfsStorage(
                new GuidePdfPathBuilder(),
                guide -> PDF_BYTES,
                new StubEfsStorage());
        useCase = new CreateGuideUseCase(repository, new GuideNumberGenerator(), guidePdfEfsStorage, FIXED_CLOCK);
    }

    @Test
    void createsGuideWithPdfGeneratedAndPersists() {
        CreateGuideCommand command = new CreateGuideCommand(
                "Transportes Rápidos",
                "María González",
                "Av. Providencia 1234, Santiago",
                "Calle Huérfanos 567, Santiago",
                "Electrónicos",
                LocalDate.of(2026, 6, 2),
                "responsable@empresa.cl");

        GuideResponse response = useCase.execute(command);

        assertNotNull(response.id());
        assertTrue(response.guideNumber().startsWith("GD-"));
        assertEquals("PDF_GENERATED", response.status());
        assertNotNull(response.efsPath());
        assertTrue(repository.findById(GuideId.create(response.id())).isPresent());
    }

    private static class StubEfsStorage implements EfsStoragePort {

        @Override
        public String write(String relativePath, byte[] content) {
            return "/efs/" + relativePath;
        }

        @Override
        public byte[] read(String absolutePath) {
            return PDF_BYTES;
        }
    }
}
