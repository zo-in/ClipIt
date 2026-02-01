package com.clipit.job_service.service;

import com.clipit.job_service.dto.FormatsResponse;
import com.clipit.job_service.dto.VideoFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormatService {

    @Value("${app.tools.yt-dlp}")
    private String ytDlpPath;

    public FormatsResponse getFormats(String videoUrl) {
        Map<String, FormatCandidate> bestFormats = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(ytDlpPath, "-F", videoUrl);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    
                    if (!line.contains("video only")) continue;

                    VideoFormat format = parseLine(line);
                    if (format != null) {
                        String key = format.getResolution() + "@" + format.getFps();
                        
                        int currentRank = getCodecRank(line);

                        if (!bestFormats.containsKey(key) || currentRank < bestFormats.get(key).rank) {
                            bestFormats.put(key, new FormatCandidate(format, currentRank));
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sort Highest Resolution -> Highest FPS
        List<VideoFormat> sortedList = new ArrayList<>();
        for (FormatCandidate candidate : bestFormats.values()) {
            sortedList.add(candidate.format);
        }

        sortedList.sort((f1, f2) -> {
            int h1 = parseHeight(f1.getResolution());
            int h2 = parseHeight(f2.getResolution());
            int resComp = Integer.compare(h2, h1);
            if (resComp != 0) return resComp;
            
            int fps1 = parseFps(f1.getFps());
            int fps2 = parseFps(f2.getFps());
            return Integer.compare(fps2, fps1);
        });

        FormatsResponse response = new FormatsResponse();
        response.setVideoFormats(sortedList);
        response.setAudioFormats(new ArrayList<>()); 

        return response;
    }

    private int getCodecRank(String line) {
        
        if (line.contains("avc1")) return 1; 
        
        if (line.contains("av01")) return 2; 

        if (line.contains("vp9")) return 3;  
        
        return 4;
    }

    private VideoFormat parseLine(String line) {
        Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\w+)\\s+(\\d+x\\d+)\\s+([\\d\\s]+).*");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String id = matcher.group(1);
            String extension = matcher.group(2);
            String resolution = matcher.group(3);
            String fps = matcher.group(4).trim().split("\\s+")[0];

            return new VideoFormat(id, extension, resolution, fps);
        }
        return null;
    }

    private int parseHeight(String resolution) {
        try {
            String[] parts = resolution.split("x");
            return parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } catch (Exception e) { return 0; }
    }

    private int parseFps(String fps) {
        try {
            return Integer.parseInt(fps);
        } catch (Exception e) { return 0; }
    }

    private static class FormatCandidate {
        VideoFormat format;
        int rank;
        public FormatCandidate(VideoFormat format, int rank) {
            this.format = format;
            this.rank = rank;
        }
    }
}