package com.clipit.job_service.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clipit.job_service.entity.Job;
import com.clipit.job_service.repository.JobRepository;

@Service
public class FileCleanupService {

    @Autowired
    private JobRepository jobRepository;

    // Cron expression: At minute 0 of every hour
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupOldFiles() {
        System.out.println("[Cleanup] Starting hourly file cleanup task...");

        // 1. Calculate the cutoff time (24 hours ago)
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // 2. Find eligible jobs
        // We look for 'COMPLETED' jobs where 'createdAt' is older than 24h
        // AND 'filePath' is not null (so we don't process already cleaned jobs)
        List<Job> oldJobs = jobRepository.findByStatusAndCreatedAtBefore("COMPLETED", cutoff);

        int count = 0;
        for (Job job : oldJobs) {
            if (job.getFilePath() != null) {
                // 3. Delete the physical file
                File file = new File(job.getFilePath());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("[Cleanup] Deleted file: " + job.getFilePath());
                    } else {
                        System.err.println("[Cleanup] Failed to delete file: " + job.getFilePath());
                    }
                }

                // 4. Update Database
                // We set filePath to null so the user sees "Expired" instead of a broken download link
                job.setFilePath(null);
                jobRepository.save(job);
                count++;
            }
        }

        System.out.println("[Cleanup] Task finished. Processed " + count + " files.");
    }
}