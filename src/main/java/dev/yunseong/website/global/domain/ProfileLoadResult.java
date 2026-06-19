package dev.yunseong.website.global.domain;

public record ProfileLoadResult(Profile profile, Status status) {

    public static ProfileLoadResult available(Profile profile) {
        return new ProfileLoadResult(profile, Status.AVAILABLE);
    }

    public static ProfileLoadResult missing() {
        return new ProfileLoadResult(null, Status.MISSING);
    }

    public static ProfileLoadResult invalid() {
        return new ProfileLoadResult(null, Status.INVALID);
    }

    public boolean available() {
        return status == Status.AVAILABLE;
    }

    public enum Status {
        AVAILABLE,
        MISSING,
        INVALID
    }
}
