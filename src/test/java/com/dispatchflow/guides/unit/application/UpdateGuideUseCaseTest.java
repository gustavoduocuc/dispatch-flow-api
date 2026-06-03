package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.UpdateGuideUseCase;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.application.dto.UpdateGuideCommand;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.valueobjects.GuideStatus;
import com.dispatchflow.shared.domain.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateGuideUseCaseTest {

    private static final Instant CREATE_TIME = Instant.parse("2026-06-02T10:00:00Z");
    private static final Instant UPDATE_TIME = Instant.parse("2026-06-02T11:00:00Z");

    private InMemoryGuideRepository repository;
    private CreateGuideUseCase createGuideUseCase;
    private UpdateGuideUseCase updateGuideUseCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        createGuideUseCase = new CreateGuideUseCase(
                repository, new GuideNumberGenerator(), Clock.fixed(CREATE_TIME, ZoneOffset.UTC));
        updateGuideUseCase = new UpdateGuideUseCase(repository, Clock.fixed(UPDATE_TIME, ZoneOffset.UTC));
    }

    @Test
    void updatesGuideAndSetsUpdatedStatus() {
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
        assertEquals(GuideStatus.UPDATED.name(), updated.status());
        assertEquals(UPDATE_TIME, updated.updatedAt());
    }

    @Test
    void throwsNotFoundWhenGuideDoesNotExist() {
        assertThrows(DomainError.class, () -> updateGuideUseCase.execute(
                "missing-id",
                new UpdateGuideCommand(
                        "Transportista",
                        "Destinatario",
                        "Av. Providencia 1234, Santiago",
                        "Calle Huérfanos 567, Santiago",
                        null,
                        LocalDate.of(2026, 6, 2),
                        "responsable@empresa.cl")));
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
