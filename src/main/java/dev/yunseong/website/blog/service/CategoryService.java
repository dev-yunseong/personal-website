package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MemoRepository memoRepository;

    public List<String> getCategories() {
        Set<String> categories = new HashSet<>();
        memoRepository.findAll()
                .forEach(memo ->
                        categories.add(memo.getPath()));
        return categories.stream().sorted().toList();
    }
}
