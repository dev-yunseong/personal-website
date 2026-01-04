package dev.yunseong.website.blog.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryNode {
    private String name;
    private String fullPath;
    private List<CategoryNode> children;
    private int level;

    public CategoryNode(String name, String fullPath, int level) {
        this.name = name;
        this.fullPath = fullPath;
        this.level = level;
        this.children = new ArrayList<>();
    }

    public void addChild(CategoryNode child) {
        this.children.add(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
