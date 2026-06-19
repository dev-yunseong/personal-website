package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;

    public Page<Memo> searchMemo(String query, Pageable pageable) {
        return memoRepository.findByQuery(query, pageable);
    }

    public long saveMemo(String title, String content) {
        Memo memo = new Memo(title, content);
        memoRepository.save(memo);
        return memo.getId();
    }

    @Transactional(readOnly = true)
    public Memo getMemo(String name) {
        return memoRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    @Transactional(readOnly = true)
    public Optional<Memo> findMemo(String name) {
        return memoRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Page<Memo> getMemos(Pageable pageable) {
        return memoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Memo> getMemos(String category, Pageable pageable) {
        if (!category.endsWith("/")) {
            category = String.format("%s/", category);
        }
        PageRequest pageRequest = PageRequest
                .of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("created_at").descending());
        return memoRepository.findAllByPath(category, pageRequest);
    }

    @Transactional(readOnly = true)
    public Memo getMemo(long memoId) {
        return memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    public void updateMemo(long memoId, String title, String content) {
        Memo memo = getMemo(memoId);
        memo.setName(title);
        memo.setContent(content);
    }
}
