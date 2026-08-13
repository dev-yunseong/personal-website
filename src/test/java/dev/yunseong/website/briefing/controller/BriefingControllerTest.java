package dev.yunseong.website.briefing.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.briefing.domain.BriefingEntry;
import dev.yunseong.website.briefing.service.BriefingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BriefingControllerTest {

    private static final LocalDate TODAY_WITH_BRIEFINGS = LocalDate.of(2026, 8, 12);

    @Mock
    private BriefingService briefingService;

    @InjectMocks
    private BriefingController briefingController;

    private final Model model = new ConcurrentModel();

    private static Memo briefing(long id, String kind, String date) {
        return new Memo(id, "/private/briefing/" + kind + "/" + date, "# 제목\n\n본문",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private List<BriefingEntry> entries() {
        return (List<BriefingEntry>) model.getAttribute("entries");
    }

    @Test
    void briefing_WithoutDate_FallsBackToTheNewestDayThatHasBriefings() {
        Memo news = briefing(1L, "news", "2026-08-12");
        when(briefingService.recentDates(anyInt()))
                .thenReturn(List.of(TODAY_WITH_BRIEFINGS, LocalDate.of(2026, 8, 11)));
        when(briefingService.findByDate(TODAY_WITH_BRIEFINGS)).thenReturn(List.of(news));
        when(briefingService.kindOf(news)).thenReturn(Optional.of("news"));

        String view = briefingController.briefing(null, model);

        assertThat(view).isEqualTo("briefing");
        assertThat(model.getAttribute("selectedDate")).isEqualTo(TODAY_WITH_BRIEFINGS);
        assertThat(entries()).extracting(BriefingEntry::kind).containsExactly("news");
    }

    @Test
    void briefing_WithNoBriefingsAtAll_FallsBackToTodayAndRendersEmpty() {
        when(briefingService.recentDates(anyInt())).thenReturn(List.of());
        when(briefingService.findByDate(LocalDate.now())).thenReturn(List.of());

        String view = briefingController.briefing(null, model);

        assertThat(view).isEqualTo("briefing");
        assertThat(model.getAttribute("selectedDate")).isEqualTo(LocalDate.now());
        assertThat(entries()).isEmpty();
    }

    @Test
    void briefing_WithExplicitDate_ShowsThatDay() {
        LocalDate requested = LocalDate.of(2026, 8, 11);
        Memo jobs = briefing(2L, "jobs", "2026-08-11");
        when(briefingService.recentDates(anyInt()))
                .thenReturn(List.of(TODAY_WITH_BRIEFINGS, requested));
        when(briefingService.findByDate(requested)).thenReturn(List.of(jobs));
        when(briefingService.kindOf(jobs)).thenReturn(Optional.of("jobs"));

        briefingController.briefing("2026-08-11", model);

        assertThat(model.getAttribute("selectedDate")).isEqualTo(requested);
        assertThat(entries()).extracting(BriefingEntry::kind).containsExactly("jobs");
        verify(briefingService, never()).findByDate(TODAY_WITH_BRIEFINGS);
    }

    @Test
    void briefing_ExposesEveryKindPublishedThatDay() {
        Memo jobs = briefing(2L, "jobs", "2026-08-12");
        Memo news = briefing(1L, "news", "2026-08-12");
        when(briefingService.recentDates(anyInt())).thenReturn(List.of(TODAY_WITH_BRIEFINGS));
        when(briefingService.findByDate(TODAY_WITH_BRIEFINGS)).thenReturn(List.of(jobs, news));
        when(briefingService.kindOf(jobs)).thenReturn(Optional.of("jobs"));
        when(briefingService.kindOf(news)).thenReturn(Optional.of("news"));

        briefingController.briefing(null, model);

        assertThat(entries()).extracting(BriefingEntry::kind).containsExactly("jobs", "news");
        assertThat(entries()).extracting(BriefingEntry::memo).containsExactly(jobs, news);
    }

    @Test
    void briefing_ExposesRecentDatesForNavigation() {
        List<LocalDate> dates = List.of(TODAY_WITH_BRIEFINGS, LocalDate.of(2026, 8, 11));
        when(briefingService.recentDates(anyInt())).thenReturn(dates);
        when(briefingService.findByDate(TODAY_WITH_BRIEFINGS)).thenReturn(List.of());

        briefingController.briefing(null, model);

        assertThat(model.getAttribute("recentDates")).isEqualTo(dates);
    }

    @Test
    void briefing_RejectsAMalformedDateRatherThanSilentlyFallingBack() {
        lenient().when(briefingService.recentDates(anyInt())).thenReturn(List.of());

        assertThatThrownBy(() -> briefingController.briefing("yesterday", model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(briefingService, never()).findByDate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void briefing_TreatsABlankDateAsAbsent() {
        when(briefingService.recentDates(anyInt())).thenReturn(List.of(TODAY_WITH_BRIEFINGS));
        when(briefingService.findByDate(TODAY_WITH_BRIEFINGS)).thenReturn(List.of());

        briefingController.briefing("  ", model);

        assertThat(model.getAttribute("selectedDate")).isEqualTo(TODAY_WITH_BRIEFINGS);
    }

    /**
     * The page renders these memos without the visibility filter, so it must only ever ask
     * the briefing service — never a memo lookup of its own.
     */
    @Test
    void briefing_AsksOnlyTheBriefingServiceForContent() {
        when(briefingService.recentDates(anyInt())).thenReturn(List.of(TODAY_WITH_BRIEFINGS));
        when(briefingService.findByDate(TODAY_WITH_BRIEFINGS)).thenReturn(List.of());

        briefingController.briefing(null, model);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(briefingService).findByDate(dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(TODAY_WITH_BRIEFINGS);
    }
}
