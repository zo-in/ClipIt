package com.clipit.job_service.service;

import com.clipit.job_service.dto.JobRequest;
import com.clipit.job_service.entity.Job;
import com.clipit.job_service.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobProcessorService {

    @Autowired
    private JobRepository jobRepository;

    @Value("${app.tools.yt-dlp}")
    private String ytDlpPath;

    @Value("${app.tools.ffmpeg}")
    private String ffmpegPath;

    @Value("${app.storage.temp-dir}")
    private String tempDir;

    @Value("${app.storage.output-dir}")
    private String outputDir;

    @Async
    public void processJob(String externalId, JobRequest request) {
        Job job = jobRepository.findByExternalId(externalId).orElse(null);
        if (job == null) {
            System.err.println("Job not found for externalId: " + externalId);
            return;
        }

        updateStatus(job, "DOWNLOADING", 0);

        // Ensure directories exist
        new File(tempDir).mkdirs();
        new File(outputDir).mkdirs();

        String fileBaseName = externalId;
        String videoActualPath = null;
        String audioActualPath = null;

        try {
            // Validate request based on mode
            if (request.isAudioOnly() && request.isVideoOnly()) {
                throw new RuntimeException("Cannot set both isAudioOnly and isVideoOnly to true");
            }

            if (request.isAudioOnly()) {
                // AUDIO ONLY MODE
                audioActualPath = downloadAudioOnly(fileBaseName, request, job);
                if (audioActualPath == null) {
                    throw new RuntimeException("Audio download failed");
                }

                updateStatus(job, "PROCESSING", 0);

                // Determine output format (default to mp3 for audio-only)
                String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat() : "mp3";
                String finalOutputPath = outputDir + fileBaseName + "." + format;

                // Process audio (trim if needed, convert format)
                boolean success = processAudioOnly(audioActualPath, finalOutputPath, request, job);

                if (success) {
                    job.setFilePath(finalOutputPath);
                    updateStatus(job, "COMPLETED", 100);
                } else {
                    updateStatus(job, "FAILED", 0);
                }

            } else if (request.isVideoOnly()) {
                // VIDEO ONLY MODE
                if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
                    throw new RuntimeException("videoId is required for video-only mode");
                }

                videoActualPath = downloadVideoOnly(fileBaseName, request, job);
                if (videoActualPath == null) {
                    throw new RuntimeException("Video download failed");
                }

                updateStatus(job, "PROCESSING", 0);

                // Determine output format (default to mp4 for video-only)
                String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat() : "mp4";
                String finalOutputPath = outputDir + fileBaseName + "." + format;

                // Process video (trim if needed, re-encode with GPU)
                boolean success = processVideoOnly(videoActualPath, finalOutputPath, request, job);

                if (success) {
                    job.setFilePath(finalOutputPath);
                    updateStatus(job, "COMPLETED", 100);
                } else {
                    updateStatus(job, "FAILED", 0);
                }

            } else {
                // MERGE MODE (Default: Video + Audio)
                if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
                    throw new RuntimeException("videoId is required for merge mode");
                }

                String videoOutputTemplate = tempDir + fileBaseName + "_video.%(ext)s";
                String audioOutputTemplate = tempDir + fileBaseName + "_audio.%(ext)s";

                updateStatus(job, "DOWNLOADING", 0);
                
                // Use smart format selector with ID preference and resolution fallback
                String videoFormat = getVideoFormatSelector(request.getVideoId(), request.getResolution());
                videoActualPath = runYtDlp(videoFormat, videoOutputTemplate, request.getYoutubeUrl(), job);
                if (videoActualPath == null) {
                    throw new RuntimeException("Video download failed");
                }

                // Download audio with best audio format
                audioActualPath = runYtDlp("bestaudio", audioOutputTemplate, request.getYoutubeUrl(), job);
                if (audioActualPath == null) {
                    throw new RuntimeException("Audio download failed");
                }

                updateStatus(job, "PROCESSING", 0);

                // Merge video and audio
                String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat() : "mp4";
                String finalOutputPath = outputDir + fileBaseName + "." + format;

                boolean success = mergeVideoAndAudio(videoActualPath, audioActualPath, finalOutputPath, request, job);

                if (success) {
                    job.setFilePath(finalOutputPath);
                    updateStatus(job, "COMPLETED", 100);
                } else {
                    updateStatus(job, "FAILED", 0);
                }
            }

        } catch (Exception e) {
            System.err.println("Job processing failed for externalId: " + externalId);
            e.printStackTrace();
            updateStatus(job, "FAILED", 0);
        } finally {
            // Cleanup temporary files
            cleanupTempFiles(videoActualPath, audioActualPath);
        }
    }

    private void updateStatus(Job job, String status, int progress) {
        // Only update if status changed OR progress increased by at least 1%
        // This prevents spamming the Database with 100 updates per second
        if (!status.equals(job.getStatus()) || progress > job.getProgress()) {
            job.setStatus(status);
            job.setProgress(progress);
            jobRepository.save(job);
        }
    }

    /**
     * Converts user format selection to yt-dlp format selector with fallback
     * Tries specific format ID first, then falls back to resolution-based selector
     */
    private String getVideoFormatSelector(String videoId, String resolution) {
        StringBuilder format = new StringBuilder();
        
        // If user provided a specific format ID, try it first
        if (videoId != null && !videoId.isEmpty() && videoId.matches("\\d+")) {
            format.append(videoId);
        }
        
        // Add resolution-based fallback if resolution is provided
        if (resolution != null && !resolution.isEmpty()) {
            String[] parts = resolution.split("x");
            if (parts.length == 2) {
                String height = parts[1];
                if (format.length() > 0) {
                    format.append("/"); // fallback separator
                }
                format.append("bv*[height<=").append(height).append("]");
            }
        }
        
        // Final fallback: best video
        if (format.length() > 0) {
            format.append("/bv*");
        } else {
            format.append("bv*");
        }
        
        return format.toString();
    }

    // --- DOWNLOAD METHODS ---

    private String downloadAudioOnly(String fileBaseName, JobRequest request, Job job) throws Exception {
        String audioOutputTemplate = tempDir + fileBaseName + "_audio.%(ext)s";
        return runYtDlp("bestaudio", audioOutputTemplate, request.getYoutubeUrl(), job);
    }

    private String downloadVideoOnly(String fileBaseName, JobRequest request, Job job) throws Exception {
        String videoOutputTemplate = tempDir + fileBaseName + "_video.%(ext)s";
        
        // Use smart format selector with ID preference and resolution fallback
        String format = getVideoFormatSelector(request.getVideoId(), request.getResolution());
        return runYtDlp(format, videoOutputTemplate, request.getYoutubeUrl(), job);
    }

    // --- PROCESSING METHODS ---

    private boolean processAudioOnly(String audioPath, String outputPath, JobRequest request, Job job) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            command.add("-ss");
            command.add(request.getStartTime());
        }

        command.add("-i");
        command.add(audioPath);

        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            command.add("-to");
            command.add(request.getEndTime());
        }

        command.add("-c:a");
        if (outputPath.endsWith(".mp3")) {
            command.add("libmp3lame");
            command.add("-q:a");
            command.add("2");
        } else if (outputPath.endsWith(".m4a")) {
            command.add("aac");
            command.add("-b:a");
            command.add("192k");
        } else {
            if (outputPath.endsWith(".wav")) {
                command.add("pcm_s16le");
            } else {
                command.add("aac");
                command.add("-b:a");
                command.add("192k");
            }
        }

        command.add(outputPath);

        return executeFfmpegCommand(command, job);
    }

    private boolean processVideoOnly(String videoPath, String outputPath, JobRequest request, Job job) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            command.add("-ss");
            command.add(request.getStartTime());
        }

        command.add("-i");
        command.add(videoPath);

        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            command.add("-to");
            command.add(request.getEndTime());
        }

        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            command.add("-vf");
            command.add("scale=" + request.getResolution());
        }

        command.add("-c:v");
        command.add("h264_nvenc");
        command.add("-preset");
        command.add("p4");
        command.add("-b:v");
        command.add("5M");

        command.add(outputPath);

        return executeFfmpegCommand(command, job);
    }

    private boolean mergeVideoAndAudio(String videoPath, String audioPath, String outputPath, JobRequest request, Job job)
            throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            command.add("-ss");
            command.add(request.getStartTime());
        }

        command.add("-i");
        command.add(videoPath);

        if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
            command.add("-ss");
            command.add(request.getStartTime());
        }

        command.add("-i");
        command.add(audioPath);

        if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
            command.add("-to");
            command.add(request.getEndTime());
        }

        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            command.add("-vf");
            command.add("scale=" + request.getResolution());
        }

        command.add("-c:v");
        command.add("h264_nvenc");
        command.add("-preset");
        command.add("p4");
        command.add("-b:v");
        command.add("5M");

        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");

        command.add(outputPath);

        return executeFfmpegCommand(command, job);
    }


    private String runYtDlp(String formatId, String outputTemplate, String url, Job job) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);
        
        // Add headers
        command.add("--add-header");
        command.add("Referer:https://www.youtube.com/");
        
        // Retry logic
        command.add("--retries");
        command.add("15");
        command.add("--fragment-retries");
        command.add("15");
        command.add("--retry-sleep");
        command.add("5");  // Wait 5 seconds between retries
        
        // User agent
        command.add("--user-agent");
        command.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        command.add("--force-ipv4");
        
        // Throttled rate
        command.add("--throttled-rate");
        command.add("50K");  // Lowered threshold
        
        
        // Standard flags
        command.add("-f");
        command.add(formatId);
        command.add("-o");
        command.add(outputTemplate);
        command.add(url);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String downloadedPath = null;
        Pattern percentPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line);

                if (line.contains("Destination:")) {
                    downloadedPath = line.substring(line.indexOf("Destination:") + 12).trim();
                } else if (line.contains("has already been downloaded")) {
                    downloadedPath = line.substring(line.indexOf("] ") + 2, line.indexOf(" has already"));
                }

                if (line.contains("[download]")) {
                    Matcher matcher = percentPattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            double percentDouble = Double.parseDouble(matcher.group(1));
                            int percent = (int) percentDouble;
                            updateStatus(job, "DOWNLOADING", percent);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("yt-dlp failed with exit code: " + exitCode);
            if (downloadedPath != null && new File(downloadedPath).exists()) {
                return downloadedPath;
            }
            return null;
        }

        return downloadedPath;
    }

    private boolean executeFfmpegCommand(List<String> command, Job job) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        double totalDurationSeconds = 0;
        
        
        Pattern durationPattern = Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2}\\.\\d{2})");
        
        Pattern timePattern = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2}\\.\\d{2})");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ffmpeg] " + line);
                
                if (totalDurationSeconds == 0) {
                    Matcher dMatcher = durationPattern.matcher(line);
                    if (dMatcher.find()) {
                        int h = Integer.parseInt(dMatcher.group(1));
                        int m = Integer.parseInt(dMatcher.group(2));
                        double s = Double.parseDouble(dMatcher.group(3));
                        totalDurationSeconds = (h * 3600) + (m * 60) + s;
                    }
                }

                if (totalDurationSeconds > 0) {
                    Matcher tMatcher = timePattern.matcher(line);
                    if (tMatcher.find()) {
                        int h = Integer.parseInt(tMatcher.group(1));
                        int m = Integer.parseInt(tMatcher.group(2));
                        double s = Double.parseDouble(tMatcher.group(3));
                        double currentSeconds = (h * 3600) + (m * 60) + s;

                        int percent = (int) ((currentSeconds / totalDurationSeconds) * 100);
                        // Clamp to 99% so we don't prematurely say 100% until it's actually done
                        if (percent > 99) percent = 99;
                        
                        updateStatus(job, "PROCESSING", percent);
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("FFmpeg failed with exit code: " + exitCode);
            return false;
        }

        return true;
    }

    private void cleanupTempFiles(String... filePaths) {
        for (String filePath : filePaths) {
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Cleaned up temp file: " + filePath);
                    } else {
                        System.err.println("Failed to delete temp file: " + filePath);
                    }
                }
            }
        }
    }
}