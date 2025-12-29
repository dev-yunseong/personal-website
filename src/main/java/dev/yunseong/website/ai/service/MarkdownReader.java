package dev.yunseong.website.ai.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarkdownReader {

    public List<Document> read(String name, String content) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeBlockquote(true)
                .withIncludeCodeBlock(true)
                .withAdditionalMetadata("name", name)
                .build();

        MarkdownDocumentReader markdownReader = new MarkdownDocumentReader(new ByteArrayResource(content.getBytes()), config);

        return markdownReader.read();
    }
}
