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
        if (result instanceof Memo memo) {
            if (memo.isPrivate()) {
                throw new IllegalArgumentException("Memo Not Found");
            }
            return memo;
        }

        if (result instanceof CategoryNode categoryNode) {
            if (isPrivateCategory(categoryNode)) {
                throw new IllegalArgumentException("Category Not Found");
            }
            filterChildCategories(categoryNode);
            return categoryNode;
        }

        if (result instanceof Page<?> page) {
            List<?> visible = filterIterable(page.getContent());
            if (visible.size() == page.getNumberOfElements()) {
                return page;
            }
            return new PageImpl<>(visible, page.getPageable(), visible.size());
        }

        if (result instanceof Slice<?> slice) {
            List<?> visible = filterIterable(slice.getContent());
            if (visible.size() == slice.getNumberOfElements()) {
                return slice;
            }
            return new SliceImpl<>(visible, slice.getPageable(), slice.hasNext());
        }

        if (result instanceof Set<?> set) {
            return new LinkedHashSet<>(filterIterable(set));
        }

        if (result instanceof Collection<?> collection) {
            return filterIterable(collection);
        }

        if (result instanceof Iterable<?> iterable) {
            return filterIterable(iterable);
        }

        return result;
    }

    private List<?> filterIterable(Iterable<?> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                .filter(this::isVisible)
                .peek(this::filterNestedContent)
                .toList();
    }

    private boolean isVisible(Object item) {
        if (item instanceof Memo memo) {
            return !memo.isPrivate();
        }
        if (item instanceof CategoryNode categoryNode) {
            return !isPrivateCategory(categoryNode);
        }
        if (item instanceof String path) {
            return !path.startsWith(Memo.PRIVATE_PREFIX);
        }
        return true;
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
