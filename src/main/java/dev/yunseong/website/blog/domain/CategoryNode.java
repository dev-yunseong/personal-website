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

    /**
     * Generate a safe HTML ID from the full path by replacing special characters
     */
    public String getSafeId() {
        return fullPath.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    /**
     * Check if this node or any of its descendants contains the selected category path
     */
    public boolean containsPath(String selectedPath) {
        if (selectedPath == null || selectedPath.isEmpty()) {
            return false;
        }
        // Check if this is the selected node
        if (selectedPath.equals(fullPath)) {
            return true;
        }
        // Check if selected path is a descendant of this node
        if (selectedPath.startsWith(fullPath + "/")) {
            return true;
        }
        return false;
    }
}
