package com.dispatchflow.guides.infrastructure.config;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.DeleteGuideUseCase;
import com.dispatchflow.guides.application.DownloadGuidePdfUseCase;
import com.dispatchflow.guides.application.GetGuideUseCase;
import com.dispatchflow.guides.application.GuidePdfEfsStorage;
import com.dispatchflow.guides.application.ListGuidesUseCase;
import com.dispatchflow.guides.application.SearchGuidesUseCase;
import com.dispatchflow.guides.application.UpdateGuideUseCase;
import com.dispatchflow.guides.application.ports.EfsStoragePort;
import com.dispatchflow.guides.application.ports.GuidePdfGeneratorPort;
import com.dispatchflow.guides.domain.repositories.GuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import com.dispatchflow.guides.domain.services.GuidePdfPathBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(EfsStorageProperties.class)
public class GuideBeanConfiguration {

    @Bean
    public GuideNumberGenerator guideNumberGenerator() {
        return new GuideNumberGenerator();
    }

    @Bean
    public GuidePdfPathBuilder guidePdfPathBuilder() {
        return new GuidePdfPathBuilder();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public GuidePdfEfsStorage guidePdfEfsStorage(
            GuidePdfPathBuilder guidePdfPathBuilder,
            GuidePdfGeneratorPort guidePdfGeneratorPort,
            EfsStoragePort efsStoragePort) {
        return new GuidePdfEfsStorage(guidePdfPathBuilder, guidePdfGeneratorPort, efsStoragePort);
    }

    @Bean
    public CreateGuideUseCase createGuideUseCase(
            GuideRepository guideRepository,
            GuideNumberGenerator guideNumberGenerator,
            GuidePdfEfsStorage guidePdfEfsStorage,
            Clock clock) {
        return new CreateGuideUseCase(guideRepository, guideNumberGenerator, guidePdfEfsStorage, clock);
    }

    @Bean
    public GetGuideUseCase getGuideUseCase(GuideRepository guideRepository) {
        return new GetGuideUseCase(guideRepository);
    }

    @Bean
    public ListGuidesUseCase listGuidesUseCase(GuideRepository guideRepository) {
        return new ListGuidesUseCase(guideRepository);
    }

    @Bean
    public UpdateGuideUseCase updateGuideUseCase(
            GuideRepository guideRepository,
            GuidePdfEfsStorage guidePdfEfsStorage,
            Clock clock) {
        return new UpdateGuideUseCase(guideRepository, guidePdfEfsStorage, clock);
    }

    @Bean
    public DeleteGuideUseCase deleteGuideUseCase(GuideRepository guideRepository, Clock clock) {
        return new DeleteGuideUseCase(guideRepository, clock);
    }

    @Bean
    public SearchGuidesUseCase searchGuidesUseCase(GuideRepository guideRepository) {
        return new SearchGuidesUseCase(guideRepository);
    }

    @Bean
    public DownloadGuidePdfUseCase downloadGuidePdfUseCase(
            GuideRepository guideRepository,
            EfsStoragePort efsStoragePort) {
        return new DownloadGuidePdfUseCase(guideRepository, efsStoragePort);
    }
}
