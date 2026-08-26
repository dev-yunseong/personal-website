package dev.yunseong.website.manage.domain;

/**
 * The outcome of one bot classification: the decision, the evidence total that
 * produced it, and the names of the signals that fired.
 *
 * @param bot     whether the request is recorded as bot traffic
 * @param score   summed evidence weight; {@link BotDetector#CERTAIN_BOT_SCORE}
 *                when the client declared itself
 * @param signals comma-separated signal names, empty when nothing fired
 */
public record BotVerdict(boolean bot, int score, String signals) {
}
