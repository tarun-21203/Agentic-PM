package com.agentic.pm.dto.documentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationResponseDto {
    private String downloadUrl;
    private String s3Key;
    private int expiresInSeconds;
}

