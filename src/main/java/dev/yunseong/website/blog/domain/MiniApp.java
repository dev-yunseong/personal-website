package dev.yunseong.website.blog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mini_apps")
public class MiniApp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "app_url", nullable = false)
    private String appUrl;

    public MiniApp(String title, String description, String appUrl) {
        this.title = title;
        this.description = description;
        this.appUrl = appUrl;
    }
}
