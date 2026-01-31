package com.clipit.job_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clipit.job_service.entity.Job;

public interface JobRepository extends JpaRepository<Job, Long> {
	Optional<Job> findByExternalId(String externalId);

	List<Job> findByUserId(String userId);

	Optional<Job> findByExternalIdAndUserId(String externalId, String userId);
	
	List<Job> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoffTime);
}