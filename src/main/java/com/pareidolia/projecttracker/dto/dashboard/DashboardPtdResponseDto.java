package com.pareidolia.projecttracker.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardPtdResponseDto {

    private String projectName;

    private Integer assignedWeekly;
    private Integer assignedPtd;

    private Integer submittedWeekly;
    private Integer submittedPtd;

    private Integer issueWeekly;
    private Integer issuePtd;

    private Integer acceptedPtd;

    private Integer rejectedWeekly;
    private Integer rejectedPtd;

    private Double acceptedPtdPercentage;
    private Double rejectedPtdPercentage;
}