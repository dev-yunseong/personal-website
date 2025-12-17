package dev.yunseong.website.domain;

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
@Table(name = "game_projects")
public class GameProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "game_url")
    private String gameUrl;
    
    @ManyToOne
    @JoinColumn(name = "memo_id")
    private Memo memo;
    
    public GameProject(String gameUrl, Memo memo) {
        this.gameUrl = gameUrl;
        this.memo = memo;
    }
    
    public String getTitle() {
        if (memo == null) return "";
        return memo.getTitle();
    }
}
