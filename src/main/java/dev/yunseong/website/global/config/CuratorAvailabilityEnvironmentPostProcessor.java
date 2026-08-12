package dev.yunseong.website.global.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Turns the curator off when no OpenAI API key is configured, so the site still boots.
 *
 * <p>Spring AI validates the key while <em>constructing</em> beans rather than when calling
 * the API, so an absent key otherwise fails startup outright — and lazy initialisation does
 * not help, because {@code ChatCliConfig} contributes a {@code CommandLineRunner} that runs
 * eagerly regardless.
 *
 * <p>Rather than excluding auto-configuration classes by name, this flips Spring AI's own
 * switches. Every relevant auto-configuration is already guarded by
 * {@code @ConditionalOnProperty(..., matchIfMissing = true)}, so setting them away from their
 * default value takes the whole graph out. That survives Spring AI upgrades in a way that a
 * hard-coded {@code spring.autoconfigure.exclude} list would not.
 *
 * <p>An explicitly configured {@code app.curator.enabled} always wins, so this can be
 * overridden in either direction.
 */
public class CuratorAvailabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ENABLED_PROPERTY = "app.curator.enabled";
    private static final String API_KEY_PROPERTY = "spring.ai.openai.api-key";
    private static final String PROPERTY_SOURCE_NAME = "curatorAvailability";

    /**
     * Any value other than a known provider name takes the auto-configuration out; Spring AI has
     * no constant for it, so this is the conventional spelling.
     */
    private static final String NO_PROVIDER = "none";

    /**
     * Every model type Spring AI can auto-configure. All of them are needed, not just chat and
     * embedding: the OpenAI starter also contributes image, moderation and audio models, and each
     * one validates the key while being constructed.
     *
     * <p>Referenced through {@code SpringAIModelProperties} on purpose — if an upgrade renames or
     * adds a switch, this fails to compile instead of silently letting a bean through.
     */
    private static final List<String> MODEL_TYPE_PROPERTIES = List.of(
            SpringAIModelProperties.CHAT_MODEL,
            SpringAIModelProperties.EMBEDDING_MODEL,
            SpringAIModelProperties.TEXT_EMBEDDING_MODEL,
            SpringAIModelProperties.MULTI_MODAL_EMBEDDING_MODEL,
            SpringAIModelProperties.IMAGE_MODEL,
            SpringAIModelProperties.AUDIO_TRANSCRIPTION_MODEL,
            SpringAIModelProperties.AUDIO_SPEECH_MODEL,
            SpringAIModelProperties.MODERATION_MODEL);

    private final Log log;

    public CuratorAvailabilityEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        // The logging system is not up yet at this point; a deferred log replays once it is.
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(ENABLED_PROPERTY)) {
            return;
        }
        if (StringUtils.hasText(environment.getProperty(API_KEY_PROPERTY))) {
            return;
        }

        Map<String, Object> disabled = new LinkedHashMap<>();
        disabled.put(ENABLED_PROPERTY, "false");
        MODEL_TYPE_PROPERTIES.forEach(property -> disabled.put(property, NO_PROVIDER));
        disabled.put(SpringAIVectorStoreTypes.TYPE, NO_PROVIDER);
        disabled.put("spring.ai.chat.client.enabled", "false");
        disabled.put("spring.ai.mcp.client.enabled", "false");

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, disabled));

        // Warn, not info: a curator that is silently off in production looks like a bug in the
        // chat page rather than a missing key, and this line is the only clue.
        log.warn("No " + API_KEY_PROPERTY + " configured — curator, RAG sync and mini app metadata "
                + "generation are disabled. Set OPENAI_API_KEY to enable them.");
    }

    @Override
    public int getOrder() {
        // After config data so .env, imported via spring.config.import, has already been read.
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
