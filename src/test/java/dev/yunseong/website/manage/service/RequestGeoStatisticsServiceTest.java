package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.repository.RequestGeoStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private GeoIpCountryResolver geoIpCountryResolver;

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
    void backfillCountryCodes_WithoutGeoDatabase_DoesNothing() {
        when(geoIpCountryResolver.isAvailable()).thenReturn(false);

        assertEquals(0, requestGeoStatisticsService.backfillCountryCodes());
        verifyNoInteractions(requestGeoStatisticsRepository);
    }

    @Test
    void backfillCountryCodes_WalksChunksAndSkipsUnresolvableIps() {
        when(geoIpCountryResolver.isAvailable()).thenReturn(true);
        when(requestGeoStatisticsRepository.findDistinctIps(PageRequest.of(0, 500)))
                .thenReturn(List.of("1.1.1.1", "10.0.0.1"));
        when(requestGeoStatisticsRepository.findDistinctIps(PageRequest.of(1, 500)))
                .thenReturn(List.of());
        when(geoIpCountryResolver.resolveCountryCode("1.1.1.1")).thenReturn("AU");
        when(geoIpCountryResolver.resolveCountryCode("10.0.0.1")).thenReturn(null);
        when(requestGeoStatisticsRepository.assignCountryCode("1.1.1.1", "AU")).thenReturn(3);

        assertEquals(3, requestGeoStatisticsService.backfillCountryCodes());

        verify(requestGeoStatisticsRepository, times(2)).findDistinctIps(any(PageRequest.class));
        verify(requestGeoStatisticsRepository, never()).assignCountryCode(eq("10.0.0.1"), anyString());
    }
}
