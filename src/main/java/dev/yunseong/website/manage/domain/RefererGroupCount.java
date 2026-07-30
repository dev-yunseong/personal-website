package dev.yunseong.website.manage.domain;

/** One aggregated referer row: how many requests carried this referer, split by bot flag. */
public record RefererGroupCount(String referer, boolean bot, long requestCount) {
}
