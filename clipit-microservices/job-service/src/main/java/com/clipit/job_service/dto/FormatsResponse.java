package com.clipit.job_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormatsResponse {
    private List<VideoFormat> videoFormats;
    private List<AudioFormat> audioFormats;
}