package com.clipit.job_service.service;

import com.clipit.job_service.dto.FormatsResponse;
import com.clipit.job_service.dto.VideoFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormatService {

    @Value("${app.tools.yt-dlp}")
    private String ytDlpPath;

    public FormatsResponse getFormats(String videoUrl) {
        FormatsResponse response = new FormatsResponse();
        List<VideoFormat> videoFormats = new ArrayList<>();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(ytDlpPath, "-F", videoUrl);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    VideoFormat format = parseLine(line);
                    if (format != null) {
                        videoFormats.add(format);
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        response.setVideoFormats(videoFormats);
        // We leave audioFormats null or empty since we handle audio automatically
        response.setAudioFormats(new ArrayList<>()); 
        
        return response;
    }

    private VideoFormat parseLine(String line) {
        // Skip headers and irrelevant lines
        if (line.startsWith("[") || line.contains("ID") || line.contains("EXT")) {
            return null;
        }
        
        // Regex to capture: ID (digits), Extension, Resolution (WxH), FPS (digits)
        // Matches: "137  mp4   1920x1080   30"
        Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\w+)\\s+(\\d+x\\d+)\\s+(\\d+).*");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String id = matcher.group(1);
            // Group 2 is extension (e.g. mp4), we don't necessarily need to store it if we just want ID
            String extension = matcher.group(2);
            String resolution = matcher.group(3);
            String fps = matcher.group(4);

            // Filter out "audio only" or "images" if they accidentally match
            if (line.contains("audio only") || line.contains("images")) {
                return null;
            }

            // Create format object
            // We append FPS to resolution for clarity in UI, e.g., "1080p (60fps)"
            return new VideoFormat(id, extension, resolution, fps);
        }

        return null;
    }
}