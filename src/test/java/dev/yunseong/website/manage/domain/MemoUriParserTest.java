package dev.yunseong.website.manage.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MemoUriParserTest {

    @Test
    void extractMemoId_PlainDetailUri_ReturnsId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42"));
    }

    @Test
    void extractMemoId_TrailingSlash_ReturnsId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42/"));
    }

    @Test
    void extractMemoId_QueryString_ReturnsId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42?page=1&size=10"));
    }

    @Test
    void extractMemoId_Fragment_ReturnsId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42#section"));
    }

    @Test
    void extractMemoId_PathParameter_ReturnsId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42;jsessionid=abc"));
    }

    @Test
    void extractMemoId_SubPath_ReturnsOwningMemoId() {
        assertEquals(42L, MemoUriParser.extractMemoId("/public/memos/42/files/7"));
    }

    @Test
    void extractMemoId_ListPath_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/public/memos"));
    }

    @Test
    void extractMemoId_ListPathWithTrailingSlash_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/public/memos/"));
    }

    @Test
    void extractMemoId_ListPathWithQueryString_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/public/memos?category=/dev&page=2"));
    }

    @Test
    void extractMemoId_NonNumericSegment_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/public/memos/latest"));
        assertNull(MemoUriParser.extractMemoId("/public/memos/42abc"));
        assertNull(MemoUriParser.extractMemoId("/public/memos/-1"));
        assertNull(MemoUriParser.extractMemoId("/public/memos/%2E%2E"));
    }

    @Test
    void extractMemoId_OtherPublicUri_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/"));
        assertNull(MemoUriParser.extractMemoId("/public/search"));
        assertNull(MemoUriParser.extractMemoId("/api/public/memos/42"));
    }

    @Test
    void extractMemoId_NullUri_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId(null));
    }

    @Test
    void extractMemoId_IdBeyondLongRange_ReturnsNull() {
        assertNull(MemoUriParser.extractMemoId("/public/memos/99999999999999999999"));
    }
}
