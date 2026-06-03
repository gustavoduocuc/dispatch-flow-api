package com.dispatchflow.guides.infrastructure.config;

import com.dispatchflow.guides.application.CreateGuideUseCase;
import com.dispatchflow.guides.application.DeleteGuideUseCase;
import com.dispatchflow.guides.application.GetGuideUseCase;
import com.dispatchflow.guides.application.ListGuidesUseCase;
import com.dispatchflow.guides.application.SearchGuidesUseCase;
import com.dispatchflow.guides.application.UpdateGuideUseCase;
import com.dispatchflow.guides.domain.repositories.GuideRepository;
import com.dispatchflow.guides.domain.services.GuideNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GuideBeanConfiguration {

    @Bean
    public GuideNumberGenerator guideNumberGenerator() {
        return new GuideNumberGenerator();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CreateGuideUseCase createGuideUseCase(
            GuideRepository guideRepository,
            GuideNumberGenerator guideNumberGenerator,
            Clock clock) {
        return new CreateGuideUseCase(guideRepository, guideNumberGenerator, clock);
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
    public UpdateGuideUseCase updateGuideUseCase(GuideRepository guideRepository, Clock clock) {
        return new UpdateGuideUseCase(guideRepository, clock);
    }

    @Bean
    public DeleteGuideUseCase deleteGuideUseCase(GuideRepository guideRepository, Clock clock) {
        return new DeleteGuideUseCase(guideRepository, clock);
    }

    @Bean
    public SearchGuidesUseCase searchGuidesUseCase(GuideRepository guideRepository) {
        return new SearchGuidesUseCase(guideRepository);
    }
}
