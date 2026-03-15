package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.MiniApp;
import dev.yunseong.website.blog.repository.MiniAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MiniAppService {

    private final MiniAppRepository miniAppRepository;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public MiniApp getMiniApp(Long id) {
        return miniAppRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mini App Not Found"));
    }

    @Transactional(readOnly = true)
    public List<MiniApp> getAllMiniApps() {
        return miniAppRepository.findAll();
    }

    public MiniApp createMiniApp(String title, String description, String appUrl) {
        MiniApp miniApp = new MiniApp(title, description, appUrl);
        return miniAppRepository.save(miniApp);
    }

    public MiniApp createMiniAppFromFile(String title, String description, MultipartFile file) throws IOException {
        String appUrl = s3StorageService.uploadFile(file);
        MiniApp miniApp = new MiniApp(title, description, appUrl);
        return miniAppRepository.save(miniApp);
    }

    public MiniApp updateMiniApp(Long id, String title, String description, String appUrl) {
        MiniApp miniApp = getMiniApp(id);
        miniApp.setTitle(title);
        miniApp.setDescription(description);
        miniApp.setAppUrl(appUrl);
        return miniAppRepository.save(miniApp);
    }

    public MiniApp updateMiniAppFromFile(Long id, String title, String description, MultipartFile file) throws IOException {
        MiniApp miniApp = getMiniApp(id);
        miniApp.setTitle(title);
        miniApp.setDescription(description);
        miniApp.setAppUrl(s3StorageService.uploadFile(file));
        return miniAppRepository.save(miniApp);
    }

    public MiniApp updateMiniAppTitleAndDescription(Long id, String title, String description) {
        MiniApp miniApp = getMiniApp(id);
        miniApp.setTitle(title);
        miniApp.setDescription(description);
        return miniAppRepository.save(miniApp);
    }

    public void deleteMiniApp(Long id) {
        miniAppRepository.deleteById(id);
    }
}
