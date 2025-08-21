package com.yunseong.website.repository;

import com.yunseong.website.domain.Memo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findByName(String name);

    Page<Memo> findAllByNameStartingWith(String name, Pageable pageable);
}
