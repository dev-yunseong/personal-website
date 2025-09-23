package com.yunseong.website.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memos")
public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String content;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getHtml() {
        Parser parser = Parser.builder().build();
        Node node = parser.parse(content);
        return HtmlRenderer.builder().build().render(node);
    }

    public String getTitle() {
        if (name == null || name.isEmpty()) return "";
        String[] parts = name.split("/");

        if (parts.length == 0) {
            return "";
        }
        return parts[parts.length - 1];
    }

    public String getPath() {
        if (name == null || name.isEmpty()) return "";
        int lastSlash = name.lastIndexOf("/");
        if (lastSlash <= 0) return "/";
        return name.substring(0, lastSlash);
    }

    public Memo(String name, String content) {
        this(null, name, content, null, null);
    }

    public Memo(Long id, String name, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        if (!name.startsWith("/")) {
            this.name = String.format("/%s", name);
        } else {
            this.name = name;
        }
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
