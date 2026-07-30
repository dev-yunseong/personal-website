package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.NotFoundStat;
import dev.yunseong.website.manage.service.NotFoundReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class NotFoundReportApiController {

    private final NotFoundReportService notFoundReportService;

    @GetMapping("/not-found")
    public ResponseEntity<Map<String, Object>> notFound(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "count_desc") String sort,
            @RequestParam(defaultValue = "false") boolean includeBots,
            @RequestParam(defaultValue = "0") int page) {
        Page<NotFoundStat> result = notFoundReportService.getNotFoundReportForLastDays(days, sort, includeBots, page);
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "number", result.getNumber(),
                "first", result.isFirst(),
                "last", result.isLast()
        ));
    }
}
