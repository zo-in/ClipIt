package com.clipit.job_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
public class Job {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private String userId;
	
	private String externalId;

	@Column(length = 1000)
	private String originalUrl;

	private String status; // QUEUED, DOWNLOADING, PROCESSING, COMPLETED, FAILED

	@Column(name = "progress")
    private int progress = 0;
	
	private String filePath; // Path to the final result

	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}