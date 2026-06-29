package com.pdfadministrator.dto;

import java.util.Map;

public record GeneratePdfRequest(
        Long templateId,
        Map<String, Object> payload
) {}
