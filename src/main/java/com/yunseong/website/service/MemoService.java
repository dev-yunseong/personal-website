package com.yunseong.website.service;

import com.yunseong.website.domain.Memo;
import com.yunseong.website.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;

    public long saveMemo(String title, String content) {
        Memo memo = new Memo(title, content);
        memoRepository.save(memo);
        return memo.getId();
    }

    public Memo getMemo(String name) {
        return memoRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Memo Not Found"));
    }

    public List<Memo> getMemos() {
        return memoRepository.findAll();
    }

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
