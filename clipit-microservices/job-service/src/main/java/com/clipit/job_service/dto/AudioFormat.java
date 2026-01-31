package com.clipit.job_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioFormat {
    private String id;
    private String bitRate;
    private String codec;
}