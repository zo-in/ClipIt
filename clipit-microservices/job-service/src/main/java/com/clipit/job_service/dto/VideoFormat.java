package com.clipit.job_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoFormat {
    private String id;
    private String extension;
    private String resolution;
    private String fps;
    
    public String getLabel() {
        return resolution + " (" + fps + "fps)";
    }
}

