package dev.yunseong.website.blog.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.commonmark.node.Node;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.AttributeProvider;

import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memos")
public class Memo {

    public static final String PRIVATE_PREFIX = "/private";
    public static final String DELETED_PREFIX = PRIVATE_PREFIX + "/deleted";

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

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameProject> gameProjects = new ArrayList<>();

    public String getHtml() {
        Parser parser = Parser.builder().build();
        Node node = parser.parse(content);

        HtmlRenderer renderer = HtmlRenderer.builder()
                .attributeProviderFactory(context -> new CodeBlockLanguageAttributeProvider())
                .build();

        return renderer.render(node);
    }

    /**
     * AttributeProvider that adds language class to fenced code blocks for syntax highlighting.
     */
    private static class CodeBlockLanguageAttributeProvider implements AttributeProvider {

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if (node instanceof FencedCodeBlock && "code".equals(tagName)) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                String info = codeBlock.getInfo();
                if (info != null && !info.isEmpty()) {
                    // Sanitize the language info to prevent XSS attacks
                    // Only allow alphanumeric characters, hyphens, and underscores
                    String sanitizedInfo = info.replaceAll("[^a-zA-Z0-9\\-_]", "");
                    if (!sanitizedInfo.isEmpty()) {
                        attributes.put("class", "language-" + sanitizedInfo);
                    }
                }
            }
        }
    }

    public String getTitle() {
        if (name == null || name.isEmpty())
            return "";
        String[] parts = name.split("/");

        if (parts.length == 0) {
            return "";
        }
        return parts[parts.length - 1];
    }

    public String getPath() {
        if (name == null || name.isEmpty())
            return "";
        int lastSlash = name.lastIndexOf("/");
        if (lastSlash <= 0)
            return "/";
        return name.substring(0, lastSlash);
    }

    public boolean isPrivate() {
        return name != null && name.startsWith(PRIVATE_PREFIX);
    }

    public boolean isDeleted() {
        return name != null && name.startsWith(DELETED_PREFIX);
    }

    public Memo(String name, String content) {
        this(null, name, content, null, null);
    }

    public Memo(Long id, String name, String content, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
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

    @Override
    public String toString() {
        return String.format("""
                Memo{
                    id=%d,
                    name=%s,
                    url=https://yunseong.dev/public/memos/%d,
                    content=%s
                }
                """, id, name, id, content);
    }
}