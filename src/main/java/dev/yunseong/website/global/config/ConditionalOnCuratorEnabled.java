package dev.yunseong.website.global.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Registers the annotated bean only while the curator is available.
 *
 * <p>The curator is on by default and switched off automatically when no OpenAI API key is
 * configured — see {@link CuratorAvailabilityEnvironmentPostProcessor}. Beans that reach a
 * Spring AI type, directly or through another bean, need this so the application still starts
 * without a key.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "app.curator", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnCuratorEnabled {
}
