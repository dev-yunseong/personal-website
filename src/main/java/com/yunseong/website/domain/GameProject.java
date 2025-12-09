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
        
        // Find the last non-empty part
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                return parts[i];
            }
        }
        return "";
    }
}
