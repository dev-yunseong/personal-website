package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.domain.Profile;
import dev.yunseong.website.global.domain.ProfileLoadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private MemoService memoService;

    @Mock
    private ProfileMemoParser profileMemoParser;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfile_LoadsCanonicalProfileMemo() {
        Memo memo = new Memo("/profile", "name: Yunseong");
        Profile profile = new Profile(
                "Yunseong",
                null,
                null,
                "About",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(memoService.findMemo("/profile")).thenReturn(Optional.of(memo));
        when(profileMemoParser.parse(memo.getContent())).thenReturn(profile);

        ProfileLoadResult result = profileService.getProfile();

        assertEquals(profile, result.profile());
        assertEquals(ProfileLoadResult.Status.AVAILABLE, result.status());
        verify(memoService).findMemo("/profile");
    }

    @Test
    void getProfile_ReturnsEmptyWhenMemoIsMissing() {
        when(memoService.findMemo("/profile")).thenReturn(Optional.empty());

        ProfileLoadResult result = profileService.getProfile();

        assertNull(result.profile());
        assertEquals(ProfileLoadResult.Status.MISSING, result.status());
    }

    @Test
    void getProfile_ReturnsEmptyWhenYamlIsInvalid() {
        Memo memo = new Memo("/profile", "invalid");
        when(memoService.findMemo("/profile")).thenReturn(Optional.of(memo));
        when(profileMemoParser.parse(memo.getContent())).thenThrow(new IllegalArgumentException("Invalid YAML"));

        ProfileLoadResult result = profileService.getProfile();

        assertNull(result.profile());
        assertEquals(ProfileLoadResult.Status.INVALID, result.status());
    }
}
