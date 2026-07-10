package dev.yunseong.website.blog.aspect;

import dev.yunseong.website.blog.domain.CategoryNode;
import dev.yunseong.website.blog.domain.Memo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentVisibilityAspectTest {

    private final ContentVisibilityAspect aspect = new ContentVisibilityAspect();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersPrivateMemoFromList() throws Throwable {
        // Given
        Memo publicMemo = new Memo(1L, "/public/note", "public", null, null);
        Memo privateMemo = new Memo(2L, "/private/note", "private", null, null);
        ProceedingJoinPoint joinPoint = joinPoint(List.of(publicMemo, privateMemo));

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        List<?> resultList = (List<?>) result;
        assertThat(resultList).hasSize(1);
        assertThat(resultList.getFirst()).isSameAs(publicMemo);
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersPrivateMemoFromPage() throws Throwable {
        // Given
        Memo publicMemo = new Memo(1L, "/public/note", "public", null, null);
        Memo privateMemo = new Memo(2L, "/private/note", "private", null, null);
        Page<Memo> page = new PageImpl<>(List.of(publicMemo, privateMemo), PageRequest.of(0, 10), 2);
        ProceedingJoinPoint joinPoint = joinPoint(page);

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        Page<?> resultPage = (Page<?>) result;
        assertThat(resultPage.getContent()).hasSize(1);
        assertThat(resultPage.getContent().getFirst()).isSameAs(publicMemo);
        assertThat(resultPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersPrivateMemoFromSet() throws Throwable {
        // Given
        Memo publicMemo = new Memo(1L, "/public/note", "public", null, null);
        Memo privateMemo = new Memo(2L, "/private/note", "private", null, null);
        ProceedingJoinPoint joinPoint = joinPoint(new LinkedHashSet<>(List.of(publicMemo, privateMemo)));

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        Set<?> resultSet = (Set<?>) result;
        assertThat(resultSet).hasSize(1);
        assertThat(resultSet.iterator().next()).isSameAs(publicMemo);
    }

    @Test
    void filterVisibleContent_WhenAnonymous_ThrowsForPrivateMemo() throws Throwable {
        // Given
        Memo privateMemo = new Memo(2L, "/private/note", "private", null, null);
        ProceedingJoinPoint joinPoint = joinPoint(privateMemo);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> aspect.filterVisibleContent(joinPoint));
    }

    @Test
    void filterVisibleContent_WhenAuthenticated_ReturnsOriginalResult() throws Throwable {
        // Given
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        List<Memo> memos = List.of(new Memo(2L, "/private/note", "private", null, null));
        ProceedingJoinPoint joinPoint = joinPoint(memos);

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        assertSame(memos, result);
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersPrivateCategoryPathStrings() throws Throwable {
        // Given
        ProceedingJoinPoint joinPoint = joinPoint(List.of("/public", "/private", "/private/notes"));

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        List<?> resultList = (List<?>) result;
        assertThat(resultList).hasSize(1);
        assertThat(resultList.getFirst()).isEqualTo("/public");
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersPrivateCategoryNodes() throws Throwable {
        // Given
        CategoryNode publicNode = new CategoryNode("public", "/public", 1);
        CategoryNode privateNode = new CategoryNode("private", "/private", 1);
        ProceedingJoinPoint joinPoint = joinPoint(List.of(publicNode, privateNode));

        // When
        Object result = aspect.filterVisibleContent(joinPoint);

        // Then
        List<?> resultList = (List<?>) result;
        assertThat(resultList).hasSize(1);
        assertThat(resultList.getFirst()).isSameAs(publicNode);
    }

    @Test
    void filterVisibleContent_WhenAnonymous_FiltersNestedPrivateCategoryNodes() throws Throwable {
        // Given
        CategoryNode publicNode = new CategoryNode("public", "/public", 1);
        publicNode.addChild(new CategoryNode("notes", "/public/notes", 2));
        publicNode.addChild(new CategoryNode("private", "/private", 1));
        ProceedingJoinPoint joinPoint = joinPoint(List.of(publicNode));

        // When
        aspect.filterVisibleContent(joinPoint);

        // Then
        assertThat(publicNode.getChildren()).hasSize(1);
        assertThat(publicNode.getChildren().getFirst().getFullPath()).isEqualTo("/public/notes");
    }

    private ProceedingJoinPoint joinPoint(Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }
}
