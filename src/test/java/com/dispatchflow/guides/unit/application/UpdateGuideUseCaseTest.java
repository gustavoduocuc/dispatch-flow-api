package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.UpdateGuideUseCase;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.application.dto.UpdateGuideCommand;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.unit.application.support.GuideApplicationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateGuideUseCaseTest {

    private static final Instant CREATE_TIME = Instant.parse("2026-06-02T10:00:00Z");
    private static final Instant UPDATE_TIME = Instant.parse("2026-06-02T11:00:00Z");

    private InMemoryGuideRepository repository;
    private GuideApplicationTestSupport.InMemoryObjectStorage objectStorage;
    private CreateGuideUseCase createGuideUseCase;
    private UpdateGuideUseCase updateGuideUseCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        objectStorage = GuideApplicationTestSupport.inMemoryObjectStorage();
        createGuideUseCase = GuideApplicationTestSupport.createGuideUseCase(
                repository, Clock.fixed(CREATE_TIME, ZoneOffset.UTC), objectStorage);
        updateGuideUseCase = GuideApplicationTestSupport.updateGuideUseCase(
                repository, Clock.fixed(UPDATE_TIME, ZoneOffset.UTC), objectStorage);
    }

    @Test
    void updatesGuideAndRegeneratesPdfWithUploadedToS3Status() {
        GuideResponse created = createGuideUseCase.execute(sampleCreateCommand());
        UpdateGuideCommand updateCommand = new UpdateGuideCommand(
                "Transportes Norte",
                "Juan Pérez",
                "Av. Libertador 99, Santiago",
                "Calle Estado 100, Santiago",
                "Despacho actualizado",
                LocalDate.of(2026, 6, 3),
                "nuevo.responsable@empresa.cl");

        GuideResponse updated = updateGuideUseCase.execute(created.id(), updateCommand);

        assertEquals("Transportes Norte", updated.carrierName());
        assertEquals("UPLOADED_TO_S3", updated.status());
        assertEquals(UPDATE_TIME, updated.updatedAt());
        assertEquals("/efs/guides/2026-06-03/transportes-norte/guide-" + created.id() + ".pdf", updated.efsPath());
        assertEquals("guides/2026-06-03/transportes-norte/guide-" + created.id() + ".pdf", updated.s3Key());
        assertTrue(objectStorage.contains(updated.s3Key()));
    }

    private CreateGuideCommand sampleCreateCommand() {
        return new CreateGuideCommand(
                "Transportes Rápidos",
                "María González",
                "Av. Providencia 1234, Santiago",
                "Calle Huérfanos 567, Santiago",
                null,
                LocalDate.of(2026, 6, 2),
                "responsable@empresa.cl");
    }
}
