package com.pareidolia.projecttracker.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardProjectDto {

    private String projectName;

    private String reportingMonth;

    private String weekOf;

    /* Weekly */

    private Integer assignedWeekly;

    private Integer submittedWeekly;

    private Integer issueWeekly;

    /* PTD */

    private Integer assignedPtd;

    private Integer submittedPtd;

    private Integer issuePtd;

}