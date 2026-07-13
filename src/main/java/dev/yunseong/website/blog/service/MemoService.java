package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.annotation.FilterVisibleContent;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemoService {

    public static final String DELETED_MEMO_ROOT = Memo.DELETED_PREFIX;

    private static final DateTimeFormatter DELETED_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS");

    private final MemoRepository memoRepository;

    @FilterVisibleContent
    public Page<Memo> searchMemo(String query, Pageable pageable) {
        return memoRepository.findByQuery(query, pageable);
    }

    @FilterVisibleContent
    public Page<Memo> searchPublicMemo(String query, Pageable pageable) {
        return memoRepository.findPublicByQuery(query, pageable);
    }

    public long saveMemo(String title, String content) {
        Memo memo = new Memo(title, content);
        memoRepository.save(memo);
        return memo.getId();
    }

    public Memo upsertMemo(String name, String content) {
        return memoRepository.findByName(name)
                .map(memo -> {
                    memo.setContent(content);
                    return memo;
                })
                .orElseGet(() -> memoRepository.save(new Memo(name, content)));
    }

    @Transactional(readOnly = true)
    public Optional<Memo> findMemoWithoutVisibilityFilter(String name) {
        return memoRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Memo> getMemosByNamePrefix(String prefix, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("updatedAt").descending());
        return memoRepository.findAllByNameStartingWith(prefix, pageRequest).getContent();
    }

    @Transactional(readOnly = true)
    public List<Memo> getMemosByNamePrefixExcludingPrefix(String prefix, String excludedPrefix, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("updatedAt").descending());
        return memoRepository.findAllByNamePrefixExcludingPrefix(prefix, excludedPrefix, pageRequest).getContent();
    }

    public Memo moveMemo(String sourceName, String targetName) {
        Memo memo = memoRepository.findByName(sourceName)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
        memo.setName(targetName);
        return memo;
    }

    public Memo softDeleteMemo(long memoId) {
        Memo memo = getMemo(memoId);
        memo.setName(buildDeletedMemoName(memo));
        return memo;
    }

    private String buildDeletedMemoName(Memo memo) {
        String normalizedName = memo.getName() == null ? "memo-" + memo.getId() : memo.getName();
        String deletedName = normalizedName.replaceAll("^/+", "").replace('/', '_');
        String timestamp = LocalDateTime.now().format(DELETED_TIMESTAMP_FORMATTER);
        return DELETED_MEMO_ROOT + "/" + timestamp + "-" + deletedName;
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Memo getMemo(String name) {
        return memoRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Optional<Memo> findMemo(String name) {
        return memoRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Page<Memo> getMemos(Pageable pageable) {
        return memoRepository.findAll(pageable);
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Page<Memo> getPublicMemos(Pageable pageable) {
        return memoRepository.findPublic(pageable);
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public List<Memo> getRecentMemos(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("updatedAt").descending());
        return memoRepository.findRecentPublicMemos("/profile", pageRequest).getContent();
    }

    @Transactional(readOnly = true)
    public Page<Memo> getMemos(String category, Pageable pageable) {
        category = normalizeCategory(category);
        PageRequest pageRequest = getCategoryPageRequest(pageable);
        return memoRepository.findAllByPath(category, pageRequest);
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Page<Memo> getPublicMemos(String category, Pageable pageable) {
        category = normalizeCategory(category);
        PageRequest pageRequest = getCategoryPageRequest(pageable);
        return memoRepository.findPublicByPath(category, pageRequest);
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Memo getMemo(long memoId) {
        return memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Memo getPublicMemo(long memoId) {
        return memoRepository.findPublicById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    public void updateMemo(long memoId, String title, String content) {
        Memo memo = getMemo(memoId);
        memo.setName(title);
        memo.setContent(content);
    }

    private String normalizeCategory(String category) {
        if (!category.endsWith("/")) {
            return String.format("%s/", category);
        }
        return category;
    }

    private PageRequest getCategoryPageRequest(Pageable pageable) {
        return PageRequest
                .of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("created_at").descending());
    }
}
