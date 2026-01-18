package dev.yunseong.website.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.comparator.Comparators;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.MemoDirectory;
import dev.yunseong.website.blog.domain.MemoFile;
import dev.yunseong.website.blog.domain.MemoMeta;
import dev.yunseong.website.blog.repository.MemoFileRepository;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemoFileService {

    private final MemoRepository memoRepository;
    private final MemoFileRepository memoFileRepository;

    @Getter
    public MemoDirectory root;

    public void initMemoFileSystem() {
        List<MemoMeta> memoMetas = memoFileRepository.findAllMemoMeta();

        memoMetas.sort((m1, m2) -> Comparators.comparable().compare(m1.fullPath(), m2.fullPath()));

        root = new MemoDirectory(); // Root directory

        for (MemoMeta memoMeta : memoMetas) {
            String fullPath = memoMeta.fullPath(); // e.g., "/category/subcategory/filename.md"
            // Remove leading slash if any
            if (fullPath.startsWith("/")) {
                fullPath = fullPath.substring(1);
            }

            String[] pathSegments = fullPath.split("/"); // e.g., ["category", "subcategory", "filename.md"]

            MemoDirectory currentDirectory = root;
            // Iterate through path segments to create directories
            for (int i = 0; i < pathSegments.length - 1; i++) {
                String segment = pathSegments[i];
                MemoDirectory finalCurrentDirectory = currentDirectory;
                currentDirectory = currentDirectory.getChildren().stream()
                        .filter(dir -> dir.getName().equals(segment))
                        .findFirst()
                        .orElseGet(() -> {
                            MemoDirectory newDir = new MemoDirectory(segment, finalCurrentDirectory);
                            finalCurrentDirectory.getChildren().add(newDir);
                            return newDir;
                        });
            }

            String fileName = pathSegments[pathSegments.length - 1];

            MemoFile memoFile = new MemoFile(memoMeta.id(), fileName, currentDirectory);
            currentDirectory.getFiles().add(memoFile);
        }
    }

    public Memo getMemo(MemoDirectory workingDirectory, String path) {
        MemoFile memoFile = workingDirectory.getFile(path);
        return memoRepository.findById(memoFile.getMemoId())
                .orElseThrow(() -> new IllegalArgumentException("Memo not found"));
    }

    public MemoDirectory changeDirectory(MemoDirectory workingDirectory, String path) {
        return workingDirectory.cd(path);
    }

    public List<String> listNames(MemoDirectory workingDirectory, String path) {
        return listNames(workingDirectory.cd(path));
    }

    public List<String> listNames(MemoDirectory memoDirectory) {
        return memoDirectory.ls();
    }
}
