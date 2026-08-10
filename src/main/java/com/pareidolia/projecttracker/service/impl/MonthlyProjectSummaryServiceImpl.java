package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.MonthlyProjectSummary;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.service.dashboard.MonthlyProjectSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonthlyProjectSummaryServiceImpl
        implements MonthlyProjectSummaryService {

    @Override
    public List<MonthlyProjectSummary> buildMonthlySummary(
            List<Project> weeklyHistory) {

        Map<String, List<Project>> groupedMonths =
                new LinkedHashMap<>();

        /*
         * Because weeklyHistory is already sorted,
         * LinkedHashMap preserves the month order.
         */

        for (Project project : weeklyHistory) {

            groupedMonths
                    .computeIfAbsent(
                            project.getReportingMonth(),
                            month -> new ArrayList<>())
                    .add(project);

        }

        List<MonthlyProjectSummary> summaryList =
                new ArrayList<>();

        for (Map.Entry<String, List<Project>> entry
                : groupedMonths.entrySet()) {

            List<Project> monthProjects = entry.getValue();

            /*
             * Column L exists only once for the month.
             * Usually the first weekly row contains it.
             */

            Integer assigned = 0;

            for (Project project : monthProjects) {

                if (project.getAssignedToPareidolia() != null) {

                    assigned =
                            project.getAssignedToPareidolia();

                    break;
                }

            }

            int submitted = 0;

            int issue = 0;

            for (Project project : monthProjects) {

                submitted += value(
                        project.getAnnotationsSubmittedPareidolia());

                issue += value(
                        project.getIssueCases());

            }

            summaryList.add(

                    MonthlyProjectSummary.builder()

                            .reportingMonth(entry.getKey())

                            .totalAssigned(assigned)

                            .submitted(submitted)

                            .issue(issue)

                            .build()

            );

        }

        return summaryList;

    }

    private int value(Integer number) {

        return number == null ? 0 : number;

    }

}