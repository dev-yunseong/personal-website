package dev.yunseong.website.blog.aspect;

import dev.yunseong.website.blog.domain.CategoryNode;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.global.util.AuthenticationUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

@Aspect
@Component
public class ContentVisibilityAspect {

    @Around("@annotation(dev.yunseong.website.blog.annotation.FilterVisibleContent)")
    public Object filterVisibleContent(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (AuthenticationUtil.isAuthenticated()) {
            return result;
        }

        return filter(result);
    }

    private Object filter(Object result) {
        return switch (result) {
            case null -> null;
            case Memo memo -> filterMemo(memo);
            case CategoryNode categoryNode -> filterCategoryNode(categoryNode);
            case Page<?> page -> filterPage(page);
            case Slice<?> slice -> filterSlice(slice);
            case Set<?> set -> new LinkedHashSet<>(filterIterable(set));
            case Collection<?> collection -> filterIterable(collection);
            case Iterable<?> iterable -> filterIterable(iterable);
            default -> result;
        };
    }

    private Memo filterMemo(Memo memo) {
        if (memo.isPrivate()) {
            throw new IllegalArgumentException("Memo Not Found");
        }
        return memo;
    }

    private CategoryNode filterCategoryNode(CategoryNode categoryNode) {
        if (isPrivateCategory(categoryNode)) {
            throw new IllegalArgumentException("Category Not Found");
        }
        filterChildCategories(categoryNode);
        return categoryNode;
    }

    private Page<?> filterPage(Page<?> page) {
        List<?> visible = filterIterable(page.getContent());
        if (visible.size() == page.getNumberOfElements()) {
            return page;
        }
        return new PageImpl<>(visible, page.getPageable(), visible.size());
    }

    private Slice<?> filterSlice(Slice<?> slice) {
        List<?> visible = filterIterable(slice.getContent());
        if (visible.size() == slice.getNumberOfElements()) {
            return slice;
        }
        return new SliceImpl<>(visible, slice.getPageable(), slice.hasNext());
    }

    private List<?> filterIterable(Iterable<?> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(this::isVisible)
                .peek(this::filterNestedContent)
                .toList();
    }

    private boolean isVisible(Object item) {
        return switch (item) {
            case null -> true;
            case Memo memo -> !memo.isPrivate();
            case CategoryNode categoryNode -> !isPrivateCategory(categoryNode);
            case String path -> !path.startsWith(Memo.PRIVATE_PREFIX);
            default -> true;
        };
    }

    private void filterNestedContent(Object item) {
        if (item instanceof CategoryNode categoryNode) {
            filterChildCategories(categoryNode);
        }
    }

    private void filterChildCategories(CategoryNode categoryNode) {
        categoryNode.getChildren().removeIf(child -> {
            if (isPrivateCategory(child)) {
                return true;
            }
            filterChildCategories(child);
            return false;
        });
    }

    private boolean isPrivateCategory(CategoryNode categoryNode) {
        return categoryNode.getFullPath() != null
                && categoryNode.getFullPath().startsWith(Memo.PRIVATE_PREFIX);
    }
}
