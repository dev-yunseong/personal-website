package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.MemoNavigationEntry;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoNavigationService {
    private final MemoRepository repository;

    public String tree(String path, boolean includeDrafts) {
        String root = normalizePath(path);
        Node tree = new Node(root);
        for (MemoNavigationEntry entry : entries(root, includeDrafts)) {
            if (entry.name().equals(root)) {
                tree.entry = entry;
                continue;
            }
            String relative = entry.name().substring(prefix(root).length());
            Node node = tree;
            for (String part : relative.split("/")) {
                node = node.children.computeIfAbsent(part, Node::new);
            }
            node.entry = entry;
        }
        StringBuilder output = new StringBuilder(label(tree, true)).append('\n');
        renderChildren(tree, "", output);
        return output.toString();
    }

    public NavigationPage list(String category, String updatedAfter, int page, int limit, boolean includeDrafts) {
        if (page < 0 || limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or limit (1–100).");
        }
        String root = category == null ? "/" : normalizePath(category);
        LocalDateTime since = parseTime(updatedAfter);
        List<MemoNavigationEntry> filtered = entries(root, includeDrafts).stream()
                .filter(entry -> category == null || parent(entry.name()).equals(root))
                .filter(entry -> since == null || entry.updatedAt() != null && entry.updatedAt().isAfter(since))
                .sorted(Comparator.comparing(MemoNavigationEntry::updatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MemoNavigationEntry::name))
                .toList();
        int start = (int) Math.min((long) page * limit, filtered.size());
        int end = Math.min(start + limit, filtered.size());
        return new NavigationPage(filtered.subList(start, end), page, limit, end < filtered.size());
    }

    public String readDraft(Long memoId, String target) {
        if ((memoId == null) == (target == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specify either memoId or target.");
        }
        String name = memoId == null ? normalizePath(target)
                : repository.findPublicById(memoId).orElseThrow(MemoNotFoundException::new).getName();
        if (name.startsWith(Memo.PRIVATE_PREFIX) || name.equals("/")) throw new MemoNotFoundException();
        return repository.findByName(MemoDraftService.PREFIX + name)
                .orElseThrow(MemoNotFoundException::new).getContent();
    }

    private List<MemoNavigationEntry> entries(String root, boolean includeDrafts) {
        if (root.startsWith(Memo.PRIVATE_PREFIX)) return List.of();
        Map<String, MemoNavigationEntry> entries = new TreeMap<>();
        repository.findPublicNavigation(root, prefix(root)).forEach(entry -> entries.put(entry.name(), entry));
        if (includeDrafts) {
            String draftRoot = MemoDraftService.PREFIX + (root.equals("/") ? "" : root);
            for (MemoNavigationEntry draft : repository.findDraftNavigation(draftRoot, draftRoot + "/")) {
                String target = draft.name().substring(MemoDraftService.PREFIX.length());
                // Legacy/manual private drafts must not widen this token to other private namespaces.
                if (!target.startsWith("/") || target.startsWith(Memo.PRIVATE_PREFIX) || target.equals("/")) continue;
                MemoNavigationEntry published = entries.get(target);
                LocalDateTime updated = draft.updatedAt();
                if (published != null && published.updatedAt() != null
                        && (updated == null || published.updatedAt().isAfter(updated))) updated = published.updatedAt();
                entries.put(target, new MemoNavigationEntry(published == null ? null : published.id(), target, updated, true));
            }
        }
        return new ArrayList<>(entries.values());
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || path.length() > 255 || path.contains("\\")
                || path.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memo path.");
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.length() > 1 && normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (!normalized.equals("/")) {
            for (String part : normalized.substring(1).split("/", -1)) {
                if (part.isBlank() || part.equals(".") || part.equals("..")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memo path segment.");
                }
            }
        }
        return normalized;
    }

    private LocalDateTime parseTime(String value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "updatedAfter must be an ISO local date-time.");
        }
    }

    private String prefix(String root) { return root.equals("/") ? root : root + "/"; }

    private String parent(String name) {
        int slash = name.lastIndexOf('/');
        return slash <= 0 ? "/" : name.substring(0, slash);
    }

    private void renderChildren(Node node, String indent, StringBuilder output) {
        int index = 0;
        for (Node child : node.children.values()) {
            boolean last = ++index == node.children.size();
            output.append(indent).append(last ? "└── " : "├── ").append(label(child, false)).append('\n');
            renderChildren(child, indent + (last ? "    " : "│   "), output);
        }
    }

    private String label(Node node, boolean root) {
        String name = node.name.replaceAll("[\\p{Cntrl}]", "?");
        if ((!node.children.isEmpty() || root && node.entry == null) && !name.endsWith("/")) name += "/";
        if (node.entry == null) return name;
        if (node.entry.id() != null) name += " [id=" + node.entry.id() + "]";
        if (node.entry.hasDraft()) name += node.entry.id() == null ? " [draft only]" : " [draft]";
        return name;
    }

    private static class Node {
        final String name;
        final Map<String, Node> children = new TreeMap<>();
        MemoNavigationEntry entry;
        Node(String name) { this.name = name; }
    }

    public record NavigationPage(List<MemoNavigationEntry> items, int page, int limit, boolean hasNext) {}
}
