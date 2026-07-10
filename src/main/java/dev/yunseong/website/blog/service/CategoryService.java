package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.CategoryNode;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MemoRepository memoRepository;

    public List<String> getCategories() {
        Set<String> categories = new HashSet<>();
        memoRepository.findAll()
                .forEach(memo ->
                        categories.add(memo.getPath()));
        return categories.stream().sorted().toList();
    }

    public List<CategoryNode> getCategoryTree() {
        return getCategoryTree(memoRepository.findAll().stream());
    }

    public List<CategoryNode> getPublicCategoryTree() {
        return getCategoryTree(memoRepository.findAll().stream()
                .filter(memo -> !memo.isPrivate()));
    }

    private List<CategoryNode> getCategoryTree(Stream<Memo> memos) {
        Set<String> allPaths = new HashSet<>();
        memos.forEach(memo -> allPaths.add(memo.getPath()));

        // Build tree structure
        Map<String, CategoryNode> nodeMap = new HashMap<>();
        List<CategoryNode> roots = new ArrayList<>();

        // Sort paths to ensure parents are processed before children
        List<String> sortedPaths = allPaths.stream().sorted().toList();

        for (String path : sortedPaths) {
            if (path.equals("/")) {
                continue; // Skip root path
            }

            // Parse the path into segments
            String[] segments = path.startsWith("/") ? path.substring(1).split("/") : path.split("/"); // Remove leading "/" if present and split
            String currentPath = "";

            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                String parentPath = currentPath;
                currentPath = currentPath + "/" + segment;

                // Check if node already exists
                if (!nodeMap.containsKey(currentPath)) {
                    CategoryNode node = new CategoryNode(segment, currentPath, i + 1);
                    nodeMap.put(currentPath, node);

                    // Add to parent or roots
                    if (parentPath.isEmpty()) {
                        roots.add(node);
                    } else {
                        CategoryNode parent = nodeMap.get(parentPath);
                        if (parent != null) {
                            parent.addChild(node);
                        }
                    }
                }
            }
        }

        // Sort roots and children
        roots.sort(Comparator.comparing(CategoryNode::getName));
        sortChildren(roots);

        return roots;
    }

    private void sortChildren(List<CategoryNode> nodes) {
        for (CategoryNode node : nodes) {
            if (node.hasChildren()) {
                node.getChildren().sort(Comparator.comparing(CategoryNode::getName));
                sortChildren(node.getChildren());
            }
        }
    }
}
