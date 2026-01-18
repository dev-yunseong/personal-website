package dev.yunseong.website.blog.domain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class MemoDirectory {

    private String name;
    private MemoDirectory parent;
    private List<MemoDirectory> children;
    private List<MemoFile> files;

    public MemoDirectory(String name, MemoDirectory parent) {
        this.name = name;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.files = new ArrayList<>();
    }

    // Default constructor for the root directory
    public MemoDirectory() {
        this(null, null);
    }

    public MemoFile getFile(String relativePath) {
        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path must be relative (should not start with '/')");
        }

        if (!relativePath.endsWith(".md")) {
            throw new IllegalArgumentException("Path is not invalid file path: " + relativePath);
        }

        String temp = relativePath.substring(0, relativePath.length() - 3);

        int lastSlashIndex = temp.lastIndexOf("/");

        MemoDirectory memoDirectory = this;
        String fileName;
        if (lastSlashIndex != -1) {
            String parentPath = temp.substring(0, lastSlashIndex);
            fileName = temp.substring(lastSlashIndex + 1);
            memoDirectory = cd(parentPath);
        } else {
            fileName = temp;
        }

        return memoDirectory.files.stream()
                .filter(memoFile -> memoFile.getName().equals(fileName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("File not found: '%s' in current path '%s'",
                                relativePath, getFullPath())
                ));
    }

    public MemoDirectory cd(String relativePath) {
        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path must be relative (should not start with '/')");
        }

        String[] segments = relativePath.split("/", 2);

        String targetName = segments[0];

        MemoDirectory nextDirectory = findByName(targetName);

        if (segments.length == 1 || segments[1].isEmpty()) {
            return nextDirectory;
        }

        String remainingPath = segments[1];
        return nextDirectory.cd(remainingPath);
    }

    private MemoDirectory findByName(String name) {
        if (name.contains("/")) {
            throw new IllegalArgumentException("Name cannot contain '/': " + name);
        }

        if (name.equals(".")) return this;

        if (name.equals("..")) {
            if (parent == null) {
                throw new IllegalArgumentException("Root directory has no parent (..) path.");
            }
            return parent;
        }

        return children.stream()
                .filter(directory -> directory.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Directory not found: '%s' in current path '%s'", name, getFullPath())
                ));
    }

    public List<String> ls() {
        List<String> temp = new ArrayList<>(List.of("."));
        if (parent != null) {
            temp.add("..");
        }

        temp.addAll(children.stream().map(MemoDirectory::getName).toList());
        temp.addAll(files.stream().map(MemoFile::getNameWithExtension).toList());

        return temp;
    }

    public String getFullPath() {
        if (parent == null) {
            return "/";
        }

        String parentPath = parent.getFullPath();
        if (parentPath.equals("/")) {
            return "/" + name;
        }

        return parentPath + "/" + name;
    }
}
