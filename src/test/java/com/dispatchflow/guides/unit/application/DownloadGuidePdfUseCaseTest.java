package com.dispatchflow.guides.unit.application;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.DeleteGuideUseCase;
import com.dispatchflow.guides.application.DownloadGuidePdfUseCase;
import com.dispatchflow.guides.application.GuidePdfEfsStorage;
import com.dispatchflow.guides.application.dto.CreateGuideCommand;
import com.dispatchflow.guides.application.dto.GuidePdfDownload;
import com.dispatchflow.guides.application.dto.GuideResponse;
import com.dispatchflow.guides.application.ports.EfsStoragePort;
import com.dispatchflow.guides.domain.repositories.InMemoryGuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.services.GuidePdfPathBuilder;
import com.dispatchflow.shared.domain.DomainError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadGuidePdfUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-02T10:00:00Z"), ZoneOffset.UTC);
    private static final byte[] PDF_BYTES = new byte[] {37, 80, 68, 70};

    private InMemoryGuideRepository repository;
    private CreateGuideUseCase createGuideUseCase;
    private DownloadGuidePdfUseCase downloadGuidePdfUseCase;
    private StubEfsStorage efsStorage;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGuideRepository();
        efsStorage = new StubEfsStorage();
        GuidePdfEfsStorage guidePdfEfsStorage = new GuidePdfEfsStorage(
                new GuidePdfPathBuilder(),
                guide -> PDF_BYTES,
                efsStorage);
        createGuideUseCase = new CreateGuideUseCase(
                repository, new GuideNumberGenerator(), guidePdfEfsStorage, FIXED_CLOCK);
        downloadGuidePdfUseCase = new DownloadGuidePdfUseCase(repository, efsStorage);
    }

    @Test
    void downloadsPdfForExistingGuide() {
        GuideResponse created = createGuideUseCase.execute(sampleCommand());

        GuidePdfDownload download = downloadGuidePdfUseCase.execute(created.id());

        assertArrayEquals(PDF_BYTES, download.content());
        assertEquals("guide-" + created.id() + ".pdf", download.fileName());
    }

    @Test
    void throwsNotFoundWhenGuideDoesNotExist() {
        assertThrows(DomainError.class, () -> downloadGuidePdfUseCase.execute("missing-id"));
    }

    @Test
    void throwsNotFoundWhenGuideIsDeleted() {
        GuideResponse created = createGuideUseCase.execute(sampleCommand());
        new DeleteGuideUseCase(repository, FIXED_CLOCK).execute(created.id());

        assertThrows(DomainError.class, () -> downloadGuidePdfUseCase.execute(created.id()));
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
