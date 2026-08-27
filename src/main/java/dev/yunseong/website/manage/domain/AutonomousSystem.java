package dev.yunseong.website.manage.domain;

/**
 * The network an IP address belongs to, as MaxMind records it. Both fields may
 * be absent for an address the database does not cover.
 *
 * <p>Pure data, like {@link GeoLocation}. Whether the organisation is a hosting
 * provider is a bot-classification rule, so that judgement lives in
 * {@link BotDetector} with every other rule rather than here.
 */
public record AutonomousSystem(Long number, String organisation) {
}
