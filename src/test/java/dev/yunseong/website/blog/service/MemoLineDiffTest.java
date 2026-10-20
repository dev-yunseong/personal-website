package dev.yunseong.website.blog.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MemoLineDiffTest {
    @Test
    void replacementKeepsContextAndLineNumbers() {
        assertThat(MemoLineDiff.compare("heading\nold\nend", "heading\nnew\nend"))
                .containsExactly(
                        new MemoLineDiff.Line("context", " ", 1, 1, "heading"),
                        new MemoLineDiff.Line("removed", "-", 2, null, "old"),
                        new MemoLineDiff.Line("added", "+", null, 2, "new"),
                        new MemoLineDiff.Line("context", " ", 3, 3, "end"));
    }

    @Test
    void tracksFinalNewlineAndEmptyDocument() {
        assertThat(MemoLineDiff.compare("a", "a\n")).last()
                .isEqualTo(new MemoLineDiff.Line("added", "+", null, 2, ""));
        assertThat(MemoLineDiff.compare("", "한글")).containsExactly(
                new MemoLineDiff.Line("added", "+", null, 1, "한글"));
    }

    @Test
    void largeDiffHasBoundedMemory() {
        assertThat(MemoLineDiff.compare("a\n".repeat(1100), "b\n".repeat(1100))).hasSize(2202);
    }
}
