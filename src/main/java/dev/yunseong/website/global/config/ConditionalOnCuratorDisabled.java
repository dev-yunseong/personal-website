package dev.yunseong.website.global.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Registers the annotated bean only while the curator is unavailable.
 *
 * <p>The counterpart to {@link ConditionalOnCuratorEnabled}, for the stand-ins that answer on
 * routes whose real handler is gone — so a caller gets a deliberate 503 rather than a 404 that
 * reads like a broken deployment.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "app.curator", name = "enabled", havingValue = "false")
public @interface ConditionalOnCuratorDisabled {
}
