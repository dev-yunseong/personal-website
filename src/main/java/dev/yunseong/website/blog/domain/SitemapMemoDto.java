package dev.yunseong.website.blog.domain;

import java.time.LocalDateTime;

public record SitemapMemoDto(
        Long id,
        LocalDateTime updatedAt
) {
}
