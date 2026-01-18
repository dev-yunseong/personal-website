package dev.yunseong.website.blog.domain;

import lombok.Getter;

@Getter
public class MemoFile {

    private Long memoId;
    private String name;
    private MemoDirectory parent;

    public MemoFile(Long memoId, String name, MemoDirectory parent) {
        this.memoId = memoId;
        this.name = name;
        this.parent = parent;
    }

    public String getNameWithExtension() {
        return name + ".md";
    }

    public String getFullPath() {
        return parent.getFullPath() + "/" + name;
    }
}
