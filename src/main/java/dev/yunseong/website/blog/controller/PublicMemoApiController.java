package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/public/memos")
@RequiredArgsConstructor
public class PublicMemoApiController {

    private static final String MARKDOWN_UTF8_VALUE = "text/markdown;charset=UTF-8";
    private static final MediaType MARKDOWN_UTF8 =
            new MediaType("text", "markdown", StandardCharsets.UTF_8);

    private final MemoService memoService;

    @GetMapping
    public PublicMemoListResponse list(
            @RequestParam(required = false) String updatedAfter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int limit) {
        LocalDateTime updatedAfterTime = parseUpdatedAfter(updatedAfter);
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be at least 0");
        }
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        PageRequest pageRequest = PageRequest.of(
                page,
                limit,
                Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("id")));
        Page<Memo> memos = updatedAfterTime == null
                ? memoService.getPublicMemos(pageRequest)
                : memoService.getPublicMemosUpdatedAfter(updatedAfterTime, pageRequest);
        List<PublicMemoSummary> items = memos.getContent().stream()
                .map(PublicMemoSummary::from)
                .toList();
        return new PublicMemoListResponse(items, page, limit, memos.hasNext());
    }

    private LocalDateTime parseUpdatedAfter(String updatedAfter) {
        if (updatedAfter == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(updatedAfter);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "updatedAfter must be an ISO local date-time without an offset");
        }
    }

    @GetMapping(value = "/{memoId}/content", produces = MARKDOWN_UTF8_VALUE)
    public ResponseEntity<String> read(@PathVariable Long memoId) {
        Memo memo = memoService.getPublicMemo(memoId);
        return ResponseEntity.ok()
                .contentType(MARKDOWN_UTF8)
                .body(memo.getContent());
    }

    public record PublicMemoSummary(Long id, String name, LocalDateTime updatedAt) {
        private static PublicMemoSummary from(Memo memo) {
            return new PublicMemoSummary(memo.getId(), memo.getName(), memo.getUpdatedAt());
        }
    }

    public record PublicMemoListResponse(
            List<PublicMemoSummary> items,
            int page,
            int limit,
            boolean hasNext) {
    }
}
