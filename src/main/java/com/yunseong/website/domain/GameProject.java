package com.yunseong.website.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameProject {
    private String id;
    private String gameUrl;
    private String blogName;
    
    public String getTitle() {
        if (blogName == null || blogName.isEmpty()) return "";
        String[] parts = blogName.split("/");
        
        if (parts.length == 0) {
            return "";
        }
        return parts[parts.length - 1];
    }
}
