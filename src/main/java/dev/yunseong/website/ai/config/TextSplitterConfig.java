package dev.yunseong.website.ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextSplitterConfig {

    @Bean
    public TokenTextSplitter textSplitter() {
        int chunkSize = 400;
        int minChunkSizeChars = 50;
        int minChunkLengthToEmbed = 10;
        int maxNumChunks = 100;
        boolean keepSeparator = true;

        return new TokenTextSplitter(
                chunkSize,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                keepSeparator
        );
    }
}
