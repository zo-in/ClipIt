package com.clipit.job_service.dto;

import lombok.Data;

@Data
public class JobRequest {
    private String youtubeUrl;
    
    // Format ID selected by user (audio will be automatically selected as best)
    private String videoId;

    // Trimming (Optional)
    private String startTime; // e.g., "00:00:10"
    private String endTime;   // e.g., "00:01:30"

    // Modes
    private boolean isAudioOnly = false;  // Default false
    private boolean isVideoOnly = false;  // Default false


    // Metadata for processing
    private String resolution; // e.g., "1920x1080"
    private String format;     // e.g., "mp4", "mp3"
}