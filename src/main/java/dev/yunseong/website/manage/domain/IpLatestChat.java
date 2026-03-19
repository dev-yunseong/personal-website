package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

public record IpLatestChat(String ip, LocalDateTime latest) {}
