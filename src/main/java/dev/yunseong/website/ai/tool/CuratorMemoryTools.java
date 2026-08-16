package dev.yunseong.website.ai.tool;

import dev.yunseong.website.ai.domain.ToolEventPublisher;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CuratorMemoryTools {

    public static final String MEMORY_ROOT = "/private/curator";
    public static final String DELETED_MEMORY_ROOT = MEMORY_ROOT + "/deleted";
    public static final String AGENTS_MEMORY_PATH = MEMORY_ROOT + "/AGENTS.md";

    public static final String CURATOR_MEMORY_PROMPT = """
            # Global Curator Memory
            You have durable global memory through curator memory tools.
            - Memory is stored as private memos under `/private/curator`.
            - The memo at `/private/curator/AGENTS.md` is loaded into your system message on each request when it exists.
            - This memory persists globally across all user sessions and conversations.
            - Use `listCuratorMemories` and `readCuratorMemory` when prior preferences, recurring instructions, project decisions, corrections, or stable facts may help.
            - Use `writeCuratorMemory` proactively when the user shares stable preferences, durable facts, project decisions, or corrections worth preserving.
            - Keep memory concise and factual. Update an existing related memo instead of creating duplicates.
            - Use `deleteCuratorMemory` when memory should be removed; it moves the memo into an inaccessible deleted area with a timestamped name.
            - Deleted curator memories are not readable or writable through tools.
            - Do not store passwords, API keys, tokens, credentials, or highly sensitive personal data. Ask before storing sensitive private details.
            """;

    private static final int MAX_MEMORY_COUNT = 25;
    private static final int MAX_CONTENT_LENGTH = 20_000;
    private static final DateTimeFormatter DELETED_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS");

    private final MemoService memoService;
    private final ToolEventPublisher toolEventPublisher;

    @Tool(description = "List global curator memory memo paths stored under /private/curator.")
    public List<String> listCuratorMemories(ToolContext toolContext) {
        log.info("listCuratorMemories");
        toolEventPublisher.emitTool(toolContext, "listCuratorMemories", MEMORY_ROOT);
        return memoService.getMemosByNamePrefixExcludingPrefix(
                        MEMORY_ROOT + "/", DELETED_MEMORY_ROOT + "/", MAX_MEMORY_COUNT)
                .stream()
                .map(Memo::getName)
                .toList();
    }

    @Tool(description = "Read one global curator memory memo by path. The path must be under /private/curator.")
    public String readCuratorMemory(String path, ToolContext toolContext) {
        String memoryPath = normalizeMemoryPath(path);
        log.info("readCuratorMemory: {}", memoryPath);
        toolEventPublisher.emitTool(toolContext, "readCuratorMemory", memoryPath);
        return memoService.findMemoWithoutVisibilityFilter(memoryPath)
                .map(Memo::getContent)
                .orElse("No curator memory found at " + memoryPath + ".");
    }

    @Tool(description = "Create or update a global curator memory memo. The path must be under /private/curator. Content should be concise and factual.")
    public String writeCuratorMemory(String path, String content, ToolContext toolContext) {
        String memoryPath = normalizeMemoryPath(path);
        String memoryContent = normalizeContent(content);
        log.info("writeCuratorMemory: {}", memoryPath);
        toolEventPublisher.emitTool(toolContext, "writeCuratorMemory", memoryPath);
        memoService.upsertMemo(memoryPath, memoryContent);
        return "Saved curator memory at " + memoryPath + ".";
    }

    @Tool(description = "Soft-delete one global curator memory memo. The path must be under /private/curator and not under /private/curator/deleted. The memo is moved into a hidden deleted area with a timestamped name.")
    public String deleteCuratorMemory(String path, ToolContext toolContext) {
        String memoryPath = normalizeMemoryPath(path);
        String deletedPath = buildDeletedPath(memoryPath);
        log.info("deleteCuratorMemory: {} -> {}", memoryPath, deletedPath);
        toolEventPublisher.emitTool(toolContext, "deleteCuratorMemory", memoryPath);
        memoService.moveMemo(memoryPath, deletedPath);
        return "Deleted curator memory at " + memoryPath + ".";
    }

    private String normalizeMemoryPath(String path) {
        String trimmedPath = path == null ? "" : path.trim();
        if (trimmedPath.isBlank()) {
            throw new IllegalArgumentException("Memory path is required.");
        }

        String normalizedPath = trimmedPath.startsWith("/")
                ? trimmedPath
                : MEMORY_ROOT + "/" + trimmedPath;

        if (!normalizedPath.startsWith(MEMORY_ROOT + "/")) {
            throw new IllegalArgumentException("Memory path must be under " + MEMORY_ROOT + ".");
        }

        if (normalizedPath.startsWith(DELETED_MEMORY_ROOT + "/")) {
            throw new IllegalArgumentException("Deleted curator memories are inaccessible.");
        }

        if (normalizedPath.endsWith("/") || normalizedPath.contains("//")) {
            throw new IllegalArgumentException("Memory path must point to a memo, not a directory.");
        }

        for (String segment : normalizedPath.substring(1).split("/")) {
            if (segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Memory path cannot contain relative path segments.");
            }
        }

        return normalizedPath;
    }

    private String buildDeletedPath(String memoryPath) {
        String relativePath = memoryPath.substring(MEMORY_ROOT.length() + 1);
        String deletedName = relativePath.replace('/', '_');
        String timestamp = LocalDateTime.now().format(DELETED_TIMESTAMP_FORMATTER);
        return DELETED_MEMORY_ROOT + "/" + timestamp + "-" + deletedName;
    }

    private String normalizeContent(String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("Memory content is required.");
        }
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Memory content is too long.");
        }
        return normalizedContent;
    }
}
