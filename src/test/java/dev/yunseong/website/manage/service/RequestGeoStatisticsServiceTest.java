package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.GeoLocation;
import dev.yunseong.website.manage.repository.RequestGeoStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestGeoStatisticsServiceTest {

    @Mock
    private RequestGeoStatisticsRepository requestGeoStatisticsRepository;

    @Mock
    private GeoIpLocationResolver geoIpLocationResolver;

    @InjectMocks
    private RequestGeoStatisticsService requestGeoStatisticsService;

    @Test
    void getCountriesForLastDays_ExcludesBotsByDefaultAndPagesByTen() {
        Page<CountryStat> mockPage = new PageImpl<>(List.of(new CountryStat("KR", 42L)));
        when(requestGeoStatisticsRepository.findCountryCounts(
                any(LocalDateTime.class), eq(false), eq(PageRequest.of(0, 10))))
                .thenReturn(mockPage);

        Page<CountryStat> result = requestGeoStatisticsService.getCountriesForLastDays(7, false, 0);

        assertEquals("KR", result.getContent().get(0).countryCode());
        assertEquals(42L, result.getContent().get(0).requestCount());
    }

    @Test
    void getUnresolvedRequestsForLastDays_DelegatesToRepository() {
        when(requestGeoStatisticsRepository.countWithoutCountry(any(LocalDateTime.class), eq(true))).thenReturn(7L);

        assertEquals(7L, requestGeoStatisticsService.getUnresolvedRequestsForLastDays(30, true));
    }

    @Test
    void getCountriesForLastDays_WithZeroDays_UsesAllTimeBoundary() {
        when(requestGeoStatisticsRepository.findCountryCounts(
                any(LocalDateTime.class), eq(false), eq(PageRequest.of(0, 10))))
                .thenReturn(Page.empty());

        requestGeoStatisticsService.getCountriesForLastDays(0, false, 0);

        ArgumentCaptor<LocalDateTime> startDate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(requestGeoStatisticsRepository).findCountryCounts(
                startDate.capture(), eq(false), eq(PageRequest.of(0, 10)));
        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), startDate.getValue());
    }

    @Test
    void backfillLocations_WithoutGeoDatabase_DoesNothing() {
        when(geoIpLocationResolver.isAvailable()).thenReturn(false);

        assertEquals(0, requestGeoStatisticsService.backfillLocations());
        verifyNoInteractions(requestGeoStatisticsRepository);
    }

    @Test
    void backfillLocations_WalksChunksAndSkipsUnresolvableIps() {
        when(geoIpLocationResolver.isAvailable()).thenReturn(true);
        when(requestGeoStatisticsRepository.findDistinctIps(PageRequest.of(0, 500)))
                .thenReturn(List.of("1.1.1.1", "10.0.0.1"));
        when(requestGeoStatisticsRepository.findDistinctIps(PageRequest.of(1, 500)))
                .thenReturn(List.of());
        GeoLocation melbourne = new GeoLocation("AU", 2158177L, "Melbourne", -37.814, 144.9633, 20);
        when(geoIpLocationResolver.resolve("1.1.1.1")).thenReturn(melbourne);
        when(geoIpLocationResolver.resolve("10.0.0.1")).thenReturn(null);
        when(requestGeoStatisticsRepository.assignLocation(
                "1.1.1.1", "AU", 2158177L, "Melbourne", -37.814, 144.9633, 20)).thenReturn(3);

        assertEquals(3, requestGeoStatisticsService.backfillLocations());

        verify(requestGeoStatisticsRepository, times(2)).findDistinctIps(any(PageRequest.class));
        verify(requestGeoStatisticsRepository, never()).assignLocation(
                eq("10.0.0.1"), anyString(), any(), any(), any(), any(), any());
    }
}
