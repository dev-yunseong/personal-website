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

@Service
@Transactional
@RequiredArgsConstructor
public class MemoService {

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

    @FilterVisibleContent
    @Transactional(readOnly = true)
    public Memo getMemo(String name) {
        return memoRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    @FilterVisibleContent
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
