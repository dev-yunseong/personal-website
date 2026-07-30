package dev.yunseong.website.manage.domain;

import java.util.List;

/**
 * Traffic distribution for one period.
 *
 * @param timeZone         zone the day-of-week and hour buckets are expressed
 *                         in; {@code created_at} is a zone-less timestamp
 *                         written and read as wall-clock time in the server
 *                         zone, so the reading is only meaningful next to this
 *                         label
 * @param hourOfWeekCounts 7 rows of 24 counts, row 0 = Monday, column 0 = 00:00
 * @param devices          device classes ordered by request count, descending
 * @param browsers         browsers ordered by request count, descending
 * @param operatingSystems operating systems ordered by request count, descending
 */
public record TrafficDistribution(String timeZone,
                                  List<List<Long>> hourOfWeekCounts,
                                  List<TrafficSlice> devices,
                                  List<TrafficSlice> browsers,
                                  List<TrafficSlice> operatingSystems) {
}
