package com.yunseong.website.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@Data
@AllArgsConstructor
public class Memo {
    private Long id;
    private String title;
    private String content;

    public String getHtml() {
        Parser parser = Parser.builder().build();
        Node node = parser.parse(content);
        return HtmlRenderer.builder().build().render(node);
    }
}
