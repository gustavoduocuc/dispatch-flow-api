package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.valueobjects.GuideId;
import com.dispatchflow.guides.domain.valueobjects.GuideStatus;
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

    private InMemoryGuideRepository repository;
    private CreateGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        useCase = new CreateGuideUseCase(repository, new GuideNumberGenerator(), FIXED_CLOCK);
    }

    @Test
    void createsAndPersistsGuideWithSystemGeneratedNumber() {
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
        assertEquals(GuideStatus.CREATED.name(), response.status());
        assertEquals(FIXED_INSTANT, response.createdAt());
        assertTrue(repository.findById(GuideId.create(response.id())).isPresent());
    }
}
