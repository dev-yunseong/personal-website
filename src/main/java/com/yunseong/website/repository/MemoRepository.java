package com.yunseong.website.repository;

import com.yunseong.website.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findByName(String name);

    List<Memo> findAllByNameStartingWith(String name);
}
