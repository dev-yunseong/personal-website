package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.Model;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemoControllerTest {

    @Mock
    private MemoService memoService;

    @Mock
    private GameProjectService gameProjectService;

    @Mock
    private ProfileService profileService;

    @Mock
    private Model model;

    @InjectMocks
    private AdminMemoController controller;

    @Test
    void postMemo_RejectsInvalidProfileBeforeDatabaseInsert() {
        doThrow(new IllegalArgumentException("about is required"))
                .when(profileService).validate("invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.postMemo(
                "/profile",
                "invalid",
                null,
                null,
                model,
                response
        );

        assertEquals("memo/new", view);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        verify(memoService, never()).saveMemo("/profile", "invalid");
        verify(model).addAttribute("validationError", "about is required");
        verify(model).addAttribute("content", "invalid");
    }

    @Test
    void editMemo_RejectsInvalidProfileBeforeDatabaseUpdate() {
        Memo memo = new Memo(1L, "/profile", "old content", null, null);
        when(memoService.getMemo(1L)).thenReturn(memo);
        doThrow(new IllegalArgumentException("name is required"))
                .when(profileService).validate("invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.editMemo(
                1L,
                "/profile",
                "invalid",
                null,
                null,
                model,
                response
        );

        assertEquals("memo/edit", view);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        verify(memoService, never()).updateMemo(1L, "/profile", "invalid");
        verify(model).addAttribute("validationError", "name is required");
        verify(model).addAttribute("memo", memo);
    }

    @Test
    void editMemo_ValidatesProfileThenUpdatesDatabase() {
        Memo memo = new Memo(1L, "/profile", "valid", null, null);
        when(memoService.getMemo(1L)).thenReturn(memo);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.editMemo(
                1L,
                "/profile",
                "valid",
                null,
                null,
                model,
                response
        );

        assertEquals("redirect:/public/memos/1", view);
        verify(profileService).validate("valid");
        verify(memoService).updateMemo(1L, "/profile", "valid");
    }

    @Test
    void editMemo_DoesNotValidateOrdinaryMarkdownMemo() {
        Memo memo = new Memo(2L, "/notes/example", "# Markdown", null, null);
        when(memoService.getMemo(2L)).thenReturn(memo);

        controller.editMemo(
                2L,
                "/notes/example",
                "# Markdown",
                null,
                null,
                model,
                new MockHttpServletResponse()
        );

        verify(profileService, never()).validate("# Markdown");
        verify(memoService).updateMemo(2L, "/notes/example", "# Markdown");
    }

    @Test
    void validateProfile_ReturnsActionableResult() {
        ResponseEntity<Map<String, Object>> valid = controller.validateProfile("valid");

        assertEquals(HttpStatus.OK, valid.getStatusCode());
        assertTrue((Boolean) valid.getBody().get("valid"));

        doThrow(new IllegalArgumentException("about is required"))
                .when(profileService).validate("invalid");
        ResponseEntity<Map<String, Object>> invalid = controller.validateProfile("invalid");

        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        assertFalse((Boolean) invalid.getBody().get("valid"));
        assertEquals("about is required", invalid.getBody().get("message"));
    }
}
