package com.pareidolia.projecttracker.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyProjectSummary {

    private String reportingMonth;

    private Integer totalAssigned;

    private Integer freshAssigned;

    private Integer carryForward;

    private Integer submitted;

    private Integer issue;

}