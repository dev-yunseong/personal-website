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
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.AttributeProviderFactory;

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
            .attributeProviderFactory(new AttributeProviderFactory() {
                @Override
                public AttributeProvider create(AttributeProviderContext context) {
                    return new AttributeProvider() {
                        @Override
                        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
                            if (node instanceof FencedCodeBlock && "code".equals(tagName)) {
                                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                                String info = codeBlock.getInfo();
                                if (info != null && !info.isEmpty()) {
                                    attributes.put("class", "language-" + info);
                                }
                            }
                        }
                    };
                }
            })
            .build();
        
        return renderer.render(node);
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
