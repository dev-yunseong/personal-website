package dev.yunseong.website.briefing.domain;

import dev.yunseong.website.blog.domain.Memo;

/**
 * One kind's briefing for one day, as the digest page needs it.
 *
 * <p>The kind is already inside the rendered markdown, but the template also needs it as
 * a value — for the section anchor and the jump list. Pairing it with the memo here keeps
 * the template from re-deriving it out of the memo name and duplicating the path
 * convention that {@link dev.yunseong.website.briefing.service.BriefingService} owns.
 */
public record BriefingEntry(String kind, Memo memo) {

    public String getKind() {
        return kind;
    }

    public Memo getMemo() {
        return memo;
    }

    /** Rendered body, inlined into the page's single markdown container. */
    public String getHtml() {
        return memo.getHtml();
    }
}
