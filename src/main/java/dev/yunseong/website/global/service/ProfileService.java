package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.domain.Profile;
import dev.yunseong.website.global.domain.ProfileLoadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final String PROFILE_MEMO_NAME = "/profile";

    private final MemoService memoService;
    private final ProfileMemoParser profileMemoParser;

    @Transactional(readOnly = true)
    public ProfileLoadResult getProfile() {
        Optional<Memo> memo = memoService.findMemo(PROFILE_MEMO_NAME);
        if (memo.isEmpty()) {
            return ProfileLoadResult.missing();
        }

        try {
            return ProfileLoadResult.available(profileMemoParser.parse(memo.get().getContent()));
        } catch (IllegalArgumentException e) {
            log.warn("Unable to load profile memo: {}", e.getMessage());
            return ProfileLoadResult.invalid();
        }
    }

    public Profile validate(String content) {
        return profileMemoParser.parse(content);
    }
}
