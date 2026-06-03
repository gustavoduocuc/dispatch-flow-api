package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.DeleteGuideUseCase;
import com.dispatchflow.guides.application.GetGuideUseCase;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.shared.domain.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetGuideUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-02T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryGuideRepository repository;
    private GetGuideUseCase getGuideUseCase;
    private CreateGuideUseCase createGuideUseCase;
    private DeleteGuideUseCase deleteGuideUseCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        createGuideUseCase = new CreateGuideUseCase(repository, new GuideNumberGenerator(), FIXED_CLOCK);
        getGuideUseCase = new GetGuideUseCase(repository);
        deleteGuideUseCase = new DeleteGuideUseCase(repository, FIXED_CLOCK);
    }

    @Test
    void returnsGuideById() {
        GuideResponse created = createGuideUseCase.execute(sampleCommand());

        GuideResponse found = getGuideUseCase.execute(created.id());

        assertEquals(created.id(), found.id());
    }

    @Test
    void throwsNotFoundWhenGuideDoesNotExist() {
        DomainError error = assertThrows(DomainError.class, () -> getGuideUseCase.execute("missing-id"));

        assertEquals(DomainError.Type.NOT_FOUND, error.getType());
    }

    @Test
    void throwsNotFoundWhenGuideIsDeleted() {
        GuideResponse created = createGuideUseCase.execute(sampleCommand());
        deleteGuideUseCase.execute(created.id());

        assertThrows(DomainError.class, () -> getGuideUseCase.execute(created.id()));
    }

    private CreateGuideCommand sampleCommand() {
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
