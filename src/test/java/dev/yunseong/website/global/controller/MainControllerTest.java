package dev.yunseong.website.global.controller;

import dev.yunseong.website.global.domain.Profile;
import dev.yunseong.website.global.domain.ProfileLoadResult;
import dev.yunseong.website.global.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private Model model;

    @InjectMocks
    private MainController mainController;

    @Test
    void index_LoadsAvailableProfile() {
        // Given
        Profile profile = profile();
        when(profileService.getProfile()).thenReturn(ProfileLoadResult.available(profile));

        // When
        String result = mainController.index(model);

        // Then
        assertEquals("index", result);
        verify(model).addAttribute("profile", profile);
        verify(model).addAttribute("profileStatus", ProfileLoadResult.Status.AVAILABLE);
    }

    @Test
    void index_ExposesMissingProfileState() {
        // Given
        when(profileService.getProfile()).thenReturn(ProfileLoadResult.missing());

        // When
        String result = mainController.index(model);

        // Then
        assertEquals("index", result);
        verify(model).addAttribute("profile", null);
        verify(model).addAttribute("profileStatus", ProfileLoadResult.Status.MISSING);
    }

    @Test
    void index_ExposesInvalidProfileState() {
        when(profileService.getProfile()).thenReturn(ProfileLoadResult.invalid());

        String result = mainController.index(model);

        assertEquals("index", result);
        verify(model).addAttribute("profile", null);
        verify(model).addAttribute("profileStatus", ProfileLoadResult.Status.INVALID);
    }

    private Profile profile() {
        return new Profile(
                "Yunseong Jeong",
                "Software Engineer",
                "Busan, KR",
                "About",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
