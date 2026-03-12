package dev.yunseong.website.manage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsoleControllerTest {

    @Mock
    private Model model;

    @InjectMocks
    private ConsoleController consoleController;

    @Test
    void console_ReturnsConsoleView() {
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "uri", "", "day");
        assertEquals("console/dashboard", viewName);
    }

    @Test
    void console_PassesParamsToModel() {
        consoleController.console(model, 30, 1, 2, "2xx", "3xx", "key_asc", "history", "ip", "4xx", "week");

        verify(model).addAttribute("days", 30);
        verify(model).addAttribute("page", 1);
        verify(model).addAttribute("topPage", 2);
        verify(model).addAttribute("statusFilter", "2xx");
        verify(model).addAttribute("topStatusFilter", "3xx");
        verify(model).addAttribute("topSort", "key_asc");
        verify(model).addAttribute("tab", "history");
        verify(model).addAttribute("topGroupBy", "ip");
        verify(model).addAttribute("timelineStatusFilter", "4xx");
        verify(model).addAttribute("timelineResolution", "week");
    }

    @Test
    void console_DefaultParams_ArePassedCorrectly() {
        consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "uri", "", "day");

        verify(model).addAttribute("days", 7);
        verify(model).addAttribute("tab", "top");
        verify(model).addAttribute("topGroupBy", "uri");
        verify(model).addAttribute("timelineResolution", "day");
    }
}
