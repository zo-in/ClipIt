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

		updateStatus(job, "DOWNLOADING");

		// Ensure directories exist
		new File(tempDir).mkdirs();
		new File(outputDir).mkdirs();

		String fileBaseName = externalId;
		String videoActualPath = null;
		String audioActualPath = null;

		try {
			// Determine processing mode
			// Validate request based on mode
			if (request.isAudioOnly() && request.isVideoOnly()) {
				throw new RuntimeException("Cannot set both isAudioOnly and isVideoOnly to true");
			}

			if (request.isAudioOnly()) {
				// AUDIO ONLY MODE
				audioActualPath = downloadAudioOnly(fileBaseName, request);
				if (audioActualPath == null) {
					throw new RuntimeException("Audio download failed");
				}

				updateStatus(job, "PROCESSING");

				// Determine output format (default to mp3 for audio-only)
				String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat()
						: "mp3";
				String finalOutputPath = outputDir + fileBaseName + "." + format;

				// Process audio (trim if needed, convert format)
				boolean success = processAudioOnly(audioActualPath, finalOutputPath, request);

				if (success) {
					job.setFilePath(finalOutputPath);
					updateStatus(job, "COMPLETED");
				} else {
					updateStatus(job, "FAILED");
				}

			} else if (request.isVideoOnly()) {
				// VIDEO ONLY MODE
				if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
					throw new RuntimeException("videoId is required for video-only mode");
				}

				videoActualPath = downloadVideoOnly(fileBaseName, request);
				if (videoActualPath == null) {
					throw new RuntimeException("Video download failed");
				}

				updateStatus(job, "PROCESSING");

				// Determine output format (default to mp4 for video-only)
				String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat()
						: "mp4";
				String finalOutputPath = outputDir + fileBaseName + "." + format;

				// Process video (trim if needed, re-encode with GPU)
				boolean success = processVideoOnly(videoActualPath, finalOutputPath, request);

				if (success) {
					job.setFilePath(finalOutputPath);
					updateStatus(job, "COMPLETED");
				} else {
					updateStatus(job, "FAILED");
				}

			} else {
				// MERGE MODE (Default: Video + Audio)
				if (request.getVideoId() == null || request.getVideoId().isEmpty()) {
					throw new RuntimeException("videoId is required for merge mode");
				}

				String videoOutputTemplate = tempDir + fileBaseName + "_video.%(ext)s";
				String audioOutputTemplate = tempDir + fileBaseName + "_audio.%(ext)s";

				// Download both streams (best audio automatically selected)
				videoActualPath = runYtDlp(request.getVideoId(), videoOutputTemplate, request.getYoutubeUrl());
				if (videoActualPath == null) {
					throw new RuntimeException("Video download failed");
				}

				audioActualPath = runYtDlp("bestaudio", audioOutputTemplate, request.getYoutubeUrl());
				if (audioActualPath == null) {
					throw new RuntimeException("Audio download failed");
				}

				updateStatus(job, "PROCESSING");

				// Merge video and audio
				String format = (request.getFormat() != null && !request.getFormat().isEmpty()) ? request.getFormat()
						: "mp4";
				String finalOutputPath = outputDir + fileBaseName + "." + format;

				boolean success = mergeVideoAndAudio(videoActualPath, audioActualPath, finalOutputPath, request);

				if (success) {
					job.setFilePath(finalOutputPath);
					updateStatus(job, "COMPLETED");
				} else {
					updateStatus(job, "FAILED");
				}
			}

		} catch (Exception e) {
			System.err.println("Job processing failed for externalId: " + externalId);
			e.printStackTrace();
			updateStatus(job, "FAILED");
		} finally {
			// Cleanup temporary files
			cleanupTempFiles(videoActualPath, audioActualPath);
		}
	}

	private void updateStatus(Job job, String status) {
		job.setStatus(status);
		jobRepository.save(job);
	}

	// Download audio-only stream (automatically selects best audio)

	private String downloadAudioOnly(String fileBaseName, JobRequest request) throws Exception {
		String audioOutputTemplate = tempDir + fileBaseName + "_audio.%(ext)s";
		return runYtDlp("bestaudio", audioOutputTemplate, request.getYoutubeUrl());
	}

	// Download video-only stream

	private String downloadVideoOnly(String fileBaseName, JobRequest request) throws Exception {
		String videoOutputTemplate = tempDir + fileBaseName + "_video.%(ext)s";
		return runYtDlp(request.getVideoId(), videoOutputTemplate, request.getYoutubeUrl());
	}

	// Process audio-only file (trimming, format conversion)

	private boolean processAudioOnly(String audioPath, String outputPath, JobRequest request) throws Exception {
		List<String> command = new ArrayList<>();
		command.add(ffmpegPath);
		command.add("-y"); // Overwrite output

		// Add trimming if specified
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

		// --- DYNAMIC CODEC SELECTION ---
		command.add("-c:a");

		if (outputPath.endsWith(".mp3")) {
			// MP3 Container -> Must use libmp3lame
			command.add("libmp3lame");
			command.add("-q:a"); // Use variable bit rate quality
			command.add("2"); // ~170-210 kbps (Standard High Quality)
		} else if (outputPath.endsWith(".m4a")) {
			// M4A Container -> Use AAC
			command.add("aac");
			command.add("-b:a");
			command.add("192k");
		} else {
			// Fallback (e.g., for .wav or others, let FFmpeg decide or default to aac)
			// For safety, if uncertain, we default to AAC unless it's WAV
			if (outputPath.endsWith(".wav")) {
				command.add("pcm_s16le");
			} else {
				command.add("aac");
				command.add("-b:a");
				command.add("192k");
			}
		}

		command.add(outputPath);

		return executeFfmpegCommand(command);
	}

	// Process video-only file (trimming, GPU encoding)

	private boolean processVideoOnly(String videoPath, String outputPath, JobRequest request) throws Exception {
		List<String> command = new ArrayList<>();
		command.add(ffmpegPath);
		command.add("-y"); // Overwrite output

		// Add trimming if specified
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

		// Apply resolution filter if specified
		if (request.getResolution() != null && !request.getResolution().isEmpty()) {
			command.add("-vf");
			command.add("scale=" + request.getResolution());
		}

		// GPU encoding with NVIDIA h264_nvenc
		command.add("-c:v");
		command.add("h264_nvenc");
		command.add("-preset");
		command.add("p4"); // Quality preset for NVENC (p1-p7, p4 is balanced)
		command.add("-b:v");
		command.add("5M"); // Bitrate

		command.add(outputPath);

		return executeFfmpegCommand(command);
	}

	// Merge video and audio streams with GPU encoding

	private boolean mergeVideoAndAudio(String videoPath, String audioPath, String outputPath, JobRequest request)
			throws Exception {
		List<String> command = new ArrayList<>();
		command.add(ffmpegPath);
		command.add("-y"); // Overwrite output

		// Add trimming if specified (apply to inputs)
		if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
			command.add("-ss");
			command.add(request.getStartTime());
		}

		command.add("-i");
		command.add(videoPath);

		// For audio input, also apply start time
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

		// Apply resolution filter if specified
		if (request.getResolution() != null && !request.getResolution().isEmpty()) {
			command.add("-vf");
			command.add("scale=" + request.getResolution());
		}

		// GPU encoding with NVIDIA h264_nvenc
		command.add("-c:v");
		command.add("h264_nvenc");
		command.add("-preset");
		command.add("p4"); // Quality preset for NVENC
		command.add("-b:v");
		command.add("5M"); // Video bitrate

		// Audio codec settings
		command.add("-c:a");
		command.add("aac");
		command.add("-b:a");
		command.add("192k"); // Audio bitrate

		command.add(outputPath);

		return executeFfmpegCommand(command);
	}

	// Execute yt-dlp command to download a stream

	private String runYtDlp(String formatId, String outputTemplate, String url) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(ytDlpPath, "-f", formatId, "-o", outputTemplate, url);
		pb.redirectErrorStream(true);
		Process process = pb.start();

		String downloadedPath = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("[yt-dlp] " + line);

				// Parse the downloaded file path
				if (line.contains("Destination:")) {
					downloadedPath = line.substring(line.indexOf("Destination:") + 12).trim();
				} else if (line.contains("has already been downloaded")) {
					downloadedPath = line.substring(line.indexOf("] ") + 2, line.indexOf(" has already"));
				}
			}
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			System.err.println("yt-dlp failed with exit code: " + exitCode);
			return null;
		}

		return downloadedPath;
	}

	// Execute FFmpeg command

	private boolean executeFfmpegCommand(List<String> command) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process process = pb.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("[ffmpeg] " + line);
			}
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			System.err.println("FFmpeg failed with exit code: " + exitCode);
			return false;
		}

		return true;
	}

	// Cleanup temporary files after processing

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