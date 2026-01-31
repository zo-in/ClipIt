package com.clipit.job_service.controller;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clipit.job_service.dto.FormatsResponse;
import com.clipit.job_service.dto.JobRequest;
import com.clipit.job_service.entity.Job;
import com.clipit.job_service.repository.JobRepository;
import com.clipit.job_service.service.FormatService;
import com.clipit.job_service.service.JobProcessorService;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobProcessorService jobProcessorService;
    
    @Autowired
    private FormatService formatService;

 // 1. Start Job
    @PostMapping("/start-job")
    public ResponseEntity<String> startJob(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody JobRequest request) {
        
        String externalId = UUID.randomUUID().toString();

        Job job = new Job();
        job.setUserId(userId); // <--- Save the user
        job.setExternalId(externalId);
        job.setOriginalUrl(request.getYoutubeUrl());
        job.setStatus("QUEUED");
        
        jobRepository.save(job);

        // Async processing
        jobProcessorService.processJob(externalId, request);

        return ResponseEntity.ok(externalId);
    }

    // 2. Get My Jobs - Filter by User ID
    @GetMapping
    public ResponseEntity<List<Job>> getMyJobs(@RequestHeader("X-User-Id") String userId) {
        List<Job> userJobs = jobRepository.findByUserId(userId);
        return ResponseEntity.ok(userJobs);
    }

    // 3. Check Status
    @GetMapping("/status/{externalId}")
    public ResponseEntity<Job> getJobStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String externalId) {
            
        // Use the secure find method
        return jobRepository.findByExternalIdAndUserId(externalId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download/{externalId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String externalId) {
        Job job = jobRepository.findByExternalId(externalId).orElse(null);

        if (job == null || !"COMPLETED".equals(job.getStatus()) || job.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(job.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.status(500).build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
    
    @GetMapping("/formats")
    public ResponseEntity<?> getFormats(@RequestParam String url) {
        try {
            FormatsResponse formats = formatService.getFormats(url);
            return ResponseEntity.ok(formats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching formats: " + e.getMessage());
        }
    }
}