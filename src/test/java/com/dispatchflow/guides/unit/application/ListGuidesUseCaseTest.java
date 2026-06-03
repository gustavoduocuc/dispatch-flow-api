package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.DeleteGuideUseCase;
import com.dispatchflow.guides.application.ListGuidesUseCase;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.unit.application.support.GuideApplicationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListGuidesUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-02T10:00:00Z"), ZoneOffset.UTC);

    private ListGuidesUseCase listGuidesUseCase;
    private CreateGuideUseCase createGuideUseCase;
    private DeleteGuideUseCase deleteGuideUseCase;

    @BeforeEach
    void setUp() {
        InMemoryGuideRepository repository = new InMemoryGuideRepository();
        createGuideUseCase = GuideApplicationTestSupport.createGuideUseCase(repository, FIXED_CLOCK);
        listGuidesUseCase = new ListGuidesUseCase(repository);
        deleteGuideUseCase = GuideApplicationTestSupport.deleteGuideUseCase(
                repository, FIXED_CLOCK, GuideApplicationTestSupport.inMemoryObjectStorage());
    }

    @Test
    void listsOnlyActiveGuides() {
        GuideResponse active = createGuideUseCase.execute(sampleCommand("Transportes Activos"));
        GuideResponse toDelete = createGuideUseCase.execute(sampleCommand("Transportes Eliminados"));
        deleteGuideUseCase.execute(toDelete.id());

        List<GuideResponse> guides = listGuidesUseCase.execute();

        assertEquals(1, guides.size());
        assertEquals(active.id(), guides.getFirst().id());
    }

    private CreateGuideCommand sampleCommand(String carrier) {
        return new CreateGuideCommand(
                carrier,
                "María González",
                "Av. Providencia 1234, Santiago",
                "Calle Huérfanos 567, Santiago",
                null,
                LocalDate.of(2026, 6, 2),
                "responsable@empresa.cl");
    }
}
