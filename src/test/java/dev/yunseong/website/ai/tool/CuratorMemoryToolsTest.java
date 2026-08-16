package dev.yunseong.website.ai.tool;

import dev.yunseong.website.ai.domain.ToolEventPublisher;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import org.springframework.ai.chat.model.ToolContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuratorMemoryToolsTest {

    private static final ToolContext NO_TOOL_CONTEXT = new ToolContext(Map.of());

    @Mock
    private MemoService memoService;

    private CuratorMemoryTools curatorMemoryTools;

    @BeforeEach
    void setUp() {
        curatorMemoryTools = new CuratorMemoryTools(memoService, new ToolEventPublisher());
    }

    @Test
    void listCuratorMemories_ShouldReturnMemoryPaths() {
        // Given
        Memo memory = new Memo(1L, "/private/curator/preferences", "Use concise answers", null, null);
        when(memoService.getMemosByNamePrefixExcludingPrefix("/private/curator/", "/private/curator/deleted/", 25)).thenReturn(List.of(memory));

        // When
        List<String> result = curatorMemoryTools.listCuratorMemories(NO_TOOL_CONTEXT);

        // Then
        assertThat(result).containsExactly("/private/curator/preferences");
    }

    @Test
    void readCuratorMemory_ShouldReadOnlyUnderCuratorRoot() {
        // Given
        Memo memory = new Memo(1L, "/private/curator/preferences", "Use concise answers", null, null);
        when(memoService.findMemoWithoutVisibilityFilter("/private/curator/preferences")).thenReturn(Optional.of(memory));

        // When
        String result = curatorMemoryTools.readCuratorMemory("preferences", NO_TOOL_CONTEXT);

        // Then
        assertThat(result).isEqualTo("Use concise answers");
        verify(memoService).findMemoWithoutVisibilityFilter("/private/curator/preferences");
    }

    @Test
    void writeCuratorMemory_ShouldTrimContentAndConstrainPath() {
        // When
        String result = curatorMemoryTools.writeCuratorMemory("preferences", "  Use concise answers  ", NO_TOOL_CONTEXT);

        // Then
        assertThat(result).isEqualTo("Saved curator memory at /private/curator/preferences.");
        verify(memoService).upsertMemo("/private/curator/preferences", "Use concise answers");
    }

    @Test
    void deleteCuratorMemory_ShouldMoveToDeletedPathWithTimestamp() {
        // When
        String result = curatorMemoryTools.deleteCuratorMemory("preferences", NO_TOOL_CONTEXT);

        // Then
        assertThat(result).isEqualTo("Deleted curator memory at /private/curator/preferences.");
        verify(memoService).moveMemo(
                org.mockito.ArgumentMatchers.eq("/private/curator/preferences"),
                matches("/private/curator/deleted/\\d{8}T\\d{9}-preferences"));
    }

    @Test
    void readCuratorMemory_WhenDeletedPath_ShouldThrow() {
        assertThatThrownBy(() -> curatorMemoryTools.readCuratorMemory("/private/curator/deleted/20260713T175500000-preferences", NO_TOOL_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deleted curator memories are inaccessible");
    }

    @Test
    void writeCuratorMemory_WhenDeletedPath_ShouldThrow() {
        assertThatThrownBy(() -> curatorMemoryTools.writeCuratorMemory("/private/curator/deleted/20260713T175500000-preferences", "Nope", NO_TOOL_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deleted curator memories are inaccessible");
    }

    @Test
    void writeCuratorMemory_WhenOutsideRoot_ShouldThrow() {
        assertThatThrownBy(() -> curatorMemoryTools.writeCuratorMemory("/private/other/preferences", "Nope", NO_TOOL_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/private/curator");
    }

    @Test
    void writeCuratorMemory_WhenRelativeSegment_ShouldThrow() {
        assertThatThrownBy(() -> curatorMemoryTools.writeCuratorMemory("../preferences", "Nope", NO_TOOL_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relative path");
    }
}
