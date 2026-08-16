package dev.yunseong.website.ai.tool;

import dev.yunseong.website.ai.domain.ToolEventPublisher;
import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.MiniApp;
import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MiniAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentTools {

    private final GameProjectService gameProjectService;
    private final MiniAppService miniAppService;
    private final ToolEventPublisher toolEventPublisher;

    @Tool(description = "Get a list of all projects. Returns each project's id and title.")
    public List<String> listProjects(ToolContext toolContext) {
        log.info("listProjects");
        toolEventPublisher.emitTool(toolContext, "listProjects", null);
        return gameProjectService.getAllGameProjects().stream()
                .map(p -> "id=" + p.getId() + ", title=" + p.getTitle())
                .toList();
    }

    @Tool(description = "Get detailed information about a specific project by its id, including its URL.")
    public String getProject(Long id, ToolContext toolContext) {
        log.info("getProject: {}", id);
        toolEventPublisher.emitTool(toolContext, "getProject", String.valueOf(id));
        try {
            GameProject project = gameProjectService.getGameProject(id);
            return "id=" + project.getId()
                    + ", title=" + project.getTitle()
                    + ", url=" + project.getGameUrl();
        } catch (IllegalArgumentException e) {
            return "Project with id " + id + " not found.";
        }
    }

    @Tool(description = "Get a list of all mini-apps. Returns each mini-app's id and title.")
    public List<String> listMiniApps(ToolContext toolContext) {
        log.info("listMiniApps");
        toolEventPublisher.emitTool(toolContext, "listMiniApps", null);
        return miniAppService.getAllMiniApps().stream()
                .map(a -> "id=" + a.getId() + ", title=" + a.getTitle())
                .toList();
    }

    @Tool(description = "Get detailed information about a specific mini-app by its id, including its URL and description.")
    public String getMiniApp(Long id, ToolContext toolContext) {
        log.info("getMiniApp: {}", id);
        toolEventPublisher.emitTool(toolContext, "getMiniApp", String.valueOf(id));
        try {
            MiniApp app = miniAppService.getMiniApp(id);
            return "id=" + app.getId()
                    + ", title=" + app.getTitle()
                    + ", description=" + app.getDescription()
                    + ", url=" + app.getAppUrl();
        } catch (IllegalArgumentException e) {
            return "Mini-app with id " + id + " not found.";
        }
    }
}
