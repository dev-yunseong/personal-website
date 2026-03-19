package dev.yunseong.website.ai.tool;

import dev.yunseong.website.ai.domain.ToolEventPublisher;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.MemoDirectory;
import dev.yunseong.website.blog.service.MemoFileService;
import dev.yunseong.website.blog.service.MemoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogTools {

    private final MemoFileService memoFileService;
    private final MemoService memoService;
    private final ToolEventPublisher toolEventPublisher;

    public static final String BLOG_TOOL_PROMPT = """
    You are a highly competent Archive Curator for Yunsung's Blog.
    
    [CORE NAVIGATION CONCEPT]
    The blog content is structured like a file system. You must navigate it using the provided tools:
    - Use 'pwd' to see where you are.
    - Use 'ls' to explore what's inside the current directory or a given path.
    - Use 'cd' to move between directories.
    - Use 'cat' to read the actual content of a memo.
    - Use 'search' to find memos by keyword.
    
    [WORKING GUIDELINE]
    1. When a user asks about a topic, don't just rely on your memory or RAG context.
    2. If the RAG context is insufficient, proactively use 'ls' or 'search' to find relevant content.
    3. You must use 'cat' to provide accurate details if you identify a specific memo file.
    
    [BLOG-SPECIFIC CONTEXT]
    If a user inquires about anything related to the blog, its projects, or past posts, explicitly utilize the navigation tools (ls, cd, cat, search) to provide a grounded and evidence-based response. Do not hallucinate paths; verify them first.
    
    Ensure your answers are strictly grounded in the blog's information.
    """;

    private MemoDirectory workingDirectory;

    @PostConstruct
    public void init() {
        memoFileService.initMemoFileSystem();
        this.workingDirectory = memoFileService.getRoot();
    }

    @Tool(description = "Search for memos by a keyword. Returns a list of memo full path.")
    public List<String> search(String keyword) {
        log.info("search: {}", keyword);
        toolEventPublisher.emitTool("search", keyword);
        return memoService.searchMemo(keyword, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(Memo::getName)
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the current directory, similar to 'pwd' in a file system.")
    public String pwd() {
        String fullPath = workingDirectory.getFullPath();
        log.info("pwd: {}", fullPath);
        toolEventPublisher.emitTool("pwd", null);
        return fullPath;
    }

    @Tool(description = "Change the current directory, similar to 'cd' in a file system.")
    public String cd(String path) {
        this.workingDirectory = memoFileService.changeDirectory(workingDirectory, path);
        log.info("cd: {}", path);
        toolEventPublisher.emitTool("cd", path);
        String fullPath = this.workingDirectory.getFullPath();
        return "Current directory changed to: " + fullPath;
    }

    @Tool(description = "List files and directories in the current or a specified directory, similar to 'ls' in a file system.")
    public List<String> lsAt(String path) {
        List<String> names = memoFileService.listNames(workingDirectory, path);
        log.info("ls: {}, result: {}", path, names);
        toolEventPublisher.emitTool("ls", path);
        return names;
    }

    @Tool(description = "List files and directories in the current directory, similar to 'ls' in a file system.")
    public List<String> ls() {
        List<String> names = memoFileService.listNames(workingDirectory);
        log.info("ls: result: {}", names);
        toolEventPublisher.emitTool("ls", null);
        return names;
    }

    @Tool(description = "Get the content of a specific memo by its path, similar to 'cat' in a file system.")
    public String cat(String path) {
        log.info("cat: {}", path);
        toolEventPublisher.emitTool("cat", path);
        return memoFileService.getMemo(workingDirectory, path).toString();
    }
}

