package dev.yunseong.website.blog.aspect;

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
public class MemoVisibilityAspect {

    @Around("@annotation(dev.yunseong.website.blog.annotation.FilterVisibleMemos)")
    public Object filterVisibleMemos(ProceedingJoinPoint joinPoint) throws Throwable {
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
                .filter(item -> !(item instanceof Memo memo) || !memo.isPrivate())
                .toList();
    }
}
