package com.agentic.pm.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDescriptionUploadUrlRequestDto {
    private String fileName;
    private String contentType;
}

