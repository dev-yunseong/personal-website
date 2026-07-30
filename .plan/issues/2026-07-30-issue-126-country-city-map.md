# 2026-07-30 — Country City Map

- Date: 2026-07-30
- GitHub Issue: #126
- Status: Implemented

## Goal

Let an administrator drill from the country table into one country's city-level
traffic, view aggregated city markers on a map, and inspect the matching request
rows without exposing raw IP addresses.

## Non-goals

- Precise user location, live tracking, or public analytics.
- A separate Country and City MaxMind database.
- Reverse geocoding, routing, clustering, or a reusable map framework.
- Reworking unrelated console tabs or the request persistence queue.

## Context / Constraints

- Replace `GeoLite2-Country.mmdb` with `GeoLite2-City.mmdb`; City provides the
  existing country code plus city and approximate location.
- Keep lookup local on the request path. No external GeoIP API.
- MaxMind coordinates are approximate area coordinates. Persist and display the
  accuracy radius, and never return raw IP from geo endpoints.
- Use MaxMind city GeoName ID as the city identity. Names are labels, not keys.
  City markers aggregate by GeoName ID, average their coordinates, and display
  the maximum accuracy radius represented by the aggregate.
- Keep the existing country list and filters. Drill down inside the Countries
  tab so console navigation and period state remain intact.
- Use Leaflet with OpenStreetMap tiles for the admin-only interactive map.
  Pin Leaflet 1.9.4 from the official unpkg snippet with SRI, display required attribution, and request
  only the visible viewport. City lists and request rows remain usable if map
  assets or tiles fail.
- `schema.sql` is manually applied and append-only. Deployment requires SQL
  before the new application image.
- Existing unrelated untracked files remain untouched.

## Approach (Checklist)

- [x] **Step 0: Recon**
  - Verify current `RequestStatistics` constructors/callers, geo aggregation,
    console fragment lifecycle, Docker distribution, and tests.
- [x] **Step 1: City persistence and lookup**
  - Add nullable `city_geoname_id BIGINT`, `city_name VARCHAR(255)`,
    `latitude DOUBLE PRECISION`, `longitude DOUBLE PRECISION`, and
    `accuracy_radius_km INTEGER` columns to `request_statistics`. Do not add a
    speculative index; existing country/date indexes cover current scale.
  - Replace the country-only resolver with one local City lookup returning an
    immutable location record. Reject a readable Country-edition mmdb as
    unavailable because it cannot satisfy City lookup.
  - Use MaxMind's English city label. Country-only/partial results remain valid:
    every non-null field is persisted independently.
  - Store country and city fields together when a request enters the queue.
  - Change Docker/config examples to download and open `GeoLite2-City.mmdb`.
- [x] **Step 2: Backfill and read APIs**
  - Reuse the distinct-IP chunk walk, including rows whose country is already
    present. Fill each null geo column independently and never overwrite an
    existing value. `rowsUpdated` counts rows where at least one null field
    gained a non-null value; partial country-only results are allowed.
  - Extend the existing geo repository/service/controller. Add only immutable
    city and request projection records.
  - `GET /api/admin/console/geo/countries/{countryCode}/cities` accepts `days`
    and `includeBots`; returns city aggregates ordered by request count then
    GeoName ID, plus unresolved-city request count. Each aggregate contains
    GeoName ID, label, average latitude/longitude, maximum accuracy radius,
    total requests, and bot requests.
  - `GET /api/admin/console/geo/countries/{countryCode}/requests` accepts
    `days`, `includeBots`, optional `cityId`, and zero-based `page`; returns 20
    rows per page ordered by `createdAt DESC, id DESC`. Rows contain URI,
    status, bot flag, city label, and timestamp only.
  - Accept `days` 1–90, nonnegative page, uppercase/normalize ISO alpha-2
    country codes, and positive city IDs. Invalid values return 400; unknown
    country/city returns an empty result.
- [x] **Step 3: Country drill-down map**
  - Make every country row an explicit keyboard-accessible button.
  - Render a country detail state inside the existing fragment with a back
    control, city aggregate list, request table, and Leaflet map.
  - Size `CircleMarker` radius by square root of request count. Marker click
    selects a city and filters the request table; a clear action restores all
    cities.
  - Preserve selected country and country-list page. Period/bot changes keep the
    country detail but clear selected city and request page; going back restores
    the country list page.
  - Show unresolved-city count and the approximate-location/accuracy notice.
  - Use marker radius `clamp(6, 4 + 3 * sqrt(requestCount), 28)`. Zero markers
    show the city list/empty notice without initializing map bounds; one marker
    uses a fixed city zoom.
- [x] **Step 4: Tests**
  - Add one resolver/persistence check.
  - Add repository integration coverage for aggregate/filter/backfill semantics.
  - Add controller coverage for bounds and serialized response privacy: no
    `ip`, `userAgent`, or `referer`.
  - Keep one fragment contract check and one browser flow covering country
    action, marker/city selection, pagination, keyboard use, asset/tile failure
    fallback, and narrow viewport.
  - Run focused geo tests, then `./gradlew test`.
- [x] **Step 5: Rollout / Rollback**
  - Document SQL-first deployment and City mmdb secret/download verification.
  - Verify desktop/mobile console rendering with representative city data.
  - Revert application if needed; additive nullable columns can remain unused.

## Validation

- **Commands to run:**
  - `./gradlew test --tests '*Geo*' --tests '*RequestStatisticsServiceTest'`
  - `./gradlew test`
  - Browser check of Countries list, country selection, city marker selection,
    pagination, period/bot reload, keyboard focus, and narrow viewport.
- **Expected output:**
  - All tests pass.
  - Geo API responses contain no `ip` or `userAgent`.
  - Country and city counts honor period and bot filters.
  - Missing/corrupt City DB still permits application startup.
  - Browser runtime was unavailable in this environment. Thymeleaf rendering,
    JavaScript syntax, responsive CSS review, focused tests, and full build were
    completed; interactive screenshot QA remains for PR review.

## Risks & Rollback

- **Risks:**
  - New application starts before manual columns exist.
  - A Country mmdb remains mounted at the configured City path, making City
    lookup unavailable.
  - City accuracy is approximate and some valid countries have no city result.
  - OpenStreetMap tiles are best-effort; lists must remain usable if tiles fail.
- **Rollback steps:**
  - Revert the application/asset/Docker commits and redeploy the previous image.
  - Leave additive nullable city columns in place.
  - Restore a Country mmdb only when running the reverted application.

## Open Questions

- None. Decisions recorded in #126: one City mmdb, Leaflet/OpenStreetMap,
  aggregated city markers, and no raw IP exposure.

## Rejected Feedback

- Add a composite city query index now: current data has roughly 4.2k distinct
  IPs and already has country/date indexes. Add only after a measured slow query.
- Preserve selected city across period/bot changes: stale selections add
  branches for little value; reset city and request page instead.
