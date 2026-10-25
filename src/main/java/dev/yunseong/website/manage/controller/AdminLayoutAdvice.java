package dev.yunseong.website.manage.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminLayoutAdvice {
    @ModelAttribute("adminSection")
    public String section(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/admin")) {
            return null;
        }
        if (path.startsWith("/admin/console/drafts")) return "drafts";
        if (path.startsWith("/admin/console/traffic") || path.startsWith("/admin/console/requests")) return "traffic";
        if (path.startsWith("/admin/chat")) return "chat";
        if (path.startsWith("/admin/memos")) return "memo";
        if (path.startsWith("/admin/miniapps")) return "apps";
        return "overview";
    }
}
