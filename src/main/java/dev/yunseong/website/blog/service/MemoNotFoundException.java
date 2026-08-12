package dev.yunseong.website.blog.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a memo lookup finds nothing, or finds one the caller may not see.
 *
 * <p>The {@code @ResponseStatus} is the whole point: without it Spring maps the plain
 * {@link IllegalArgumentException} these lookups used to throw onto a 500, so a mistyped URL
 * and a genuine outage were indistinguishable to visitors, crawlers and logs alike.
 *
 * <p>It extends {@code IllegalArgumentException} rather than replacing it because callers and
 * tests already treat a missing memo as one. Narrowing the type keeps that contract intact
 * while letting the HTTP layer tell the two situations apart.
 *
 * <p>A private memo reports the same 404 as a missing one on purpose — answering 403 would
 * confirm the memo exists.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MemoNotFoundException extends IllegalArgumentException {

    public MemoNotFoundException() {
        super("Memo Not Found");
    }
}
