package dev.yunseong.website.blog.domain;

import java.time.LocalDateTime;

/** Public target metadata; id is null until a draft-only memo is published. */
public record MemoNavigationEntry(Long id, String name, LocalDateTime updatedAt, boolean hasDraft) {}
