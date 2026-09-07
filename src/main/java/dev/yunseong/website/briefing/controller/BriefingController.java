package dev.yunseong.website.briefing.controller;

import dev.yunseong.website.briefing.domain.BriefingEntry;
import dev.yunseong.website.briefing.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * The public digest: one day's briefings, every body expanded, on one page.
 *
 * <p>Public even though the memos behind it are private. Keeping the storage private is
 * what stops briefings from landing between blog posts; keeping the reading public is what
 * makes a morning's briefing something you can send someone a link to.
 *
 * <p>Which means this controller renders memos the visibility filter would otherwise hide,
 * and the only thing keeping it honest is that
 * {@link BriefingService} answers exclusively from under
 * {@link BriefingService#BRIEFING_ROOT}.
 */
@Controller
@RequestMapping("/briefing")
@RequiredArgsConstructor
public class BriefingController {

    /** Enough back-links to cover a couple of weeks without turning into an archive index. */
    private static final int DATE_NAVIGATION_LIMIT = 14;

    private final BriefingService briefingService;

    @GetMapping
    public String briefing(@RequestParam(required = false) String date, Model model) {
        List<LocalDate> recentDates = briefingService.recentDates(DATE_NAVIGATION_LIMIT);
        LocalDate selectedDate = resolveDate(date, recentDates);

        List<BriefingEntry> entries = briefingService.findByDate(selectedDate).stream()
                .map(memo -> new BriefingEntry(briefingService.kindOf(memo).orElse(""), memo))
                .toList();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("entries", entries);
        model.addAttribute("recentDates", recentDates);

        return "briefing";
    }

    /**
     * An explicit {@code ?date=} wins; otherwise the newest day that actually has a
     * briefing, so the page opens on something to read rather than on an empty today when
     * the agent has not run yet. Today is the last resort, for a site with no briefings at
     * all.
     */
    private LocalDate resolveDate(String date, List<LocalDate> recentDates) {
        if (date == null || date.isBlank()) {
            return recentDates.isEmpty() ? LocalDate.now() : recentDates.get(0);
        }
        try {
            return BriefingService.parseRequiredDate(date);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be yyyy-MM-dd");
        }
    }
}
