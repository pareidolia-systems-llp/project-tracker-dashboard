package com.pareidolia.projecttracker.validation;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProjectSyncValidator {

    private static final int MAX_REPORTING_PERIOD = 5;

    public void validate(List<GoogleSheetRowDto> rows) {

        if (rows == null || rows.isEmpty()) {

            throw new IllegalStateException(
                    "Synchronization aborted: no rows were parsed from Google Sheets."
            );
        }
        Map<BusinessKey, Integer> firstSourceRowByBusinessKey =
                new HashMap<>();

        Map<ProjectMonthKey, Set<Integer>>
                periodsByProjectMonth =
                new HashMap<>();


        for (GoogleSheetRowDto dto : rows) {

            validateMandatoryFields(dto);

            Integer period =
                    dto.getReportingPeriod();

            /*
             * Period 0 = cumulative/context.
             * Periods 1-5 = genuine reporting periods.
             */
            if (period < 0
                    || period > MAX_REPORTING_PERIOD) {

                throw new IllegalStateException(
                        "Synchronization aborted: invalid reporting period"
                                + " | Project=[" + dto.getProjectName() + "]"
                                + " | Month=[" + dto.getReportingMonth() + "]"
                                + " | Period=[" + period + "]"
                                + " | Week=[" + dto.getWeekOf() + "]"
                                + " | SourceRow=[" + dto.getSourceRowNumber() + "]"
                );
            }


            String normalizedProjectName =
                    dto.getProjectName().trim();

            LocalDate monthDate =
                    dto.getReportingMonthDate();


            ProjectMonthKey projectMonthKey =
                    new ProjectMonthKey(
                            normalizedProjectName,
                            monthDate
                    );


            BusinessKey businessKey =
                    new BusinessKey(
                            normalizedProjectName,
                            monthDate,
                            period
                    );


            /*
             * Same project/month/period must never
             * occur twice.
             */
            Integer firstSourceRow =
                    firstSourceRowByBusinessKey.putIfAbsent(
                            businessKey,
                            dto.getSourceRowNumber()
                    );

            if (firstSourceRow != null) {

                throw new IllegalStateException(
                        "Synchronization aborted: duplicate reporting period"
                                + " | Project=[" + dto.getProjectName() + "]"
                                + " | Month=[" + dto.getReportingMonth() + "]"
                                + " | Period=[" + period + "]"
                                + " | FirstSourceRow=[" + firstSourceRow + "]"
                                + " | DuplicateSourceRow=["
                                + dto.getSourceRowNumber()
                                + "]"
                );
            }


            periodsByProjectMonth
                    .computeIfAbsent(
                            projectMonthKey,
                            ignored -> new HashSet<>()
                    )
                    .add(period);
        }


        validateConsecutivePeriods(
                periodsByProjectMonth
        );
    }


    private void validateMandatoryFields(
            GoogleSheetRowDto dto) {

        if (dto.getProjectName() == null
                || dto.getProjectName().isBlank()) {

            throw new IllegalStateException(
                    "Synchronization aborted: project name is missing"
                            + " | SourceRow=["
                            + dto.getSourceRowNumber()
                            + "]"
            );
        }


        if (dto.getReportingMonth() == null
                || dto.getReportingMonth().isBlank()) {

            throw new IllegalStateException(
                    "Synchronization aborted: reporting month is missing"
                            + " | Project=["
                            + dto.getProjectName()
                            + "]"
                            + " | SourceRow=["
                            + dto.getSourceRowNumber()
                            + "]"
            );
        }


        if (dto.getReportingMonthDate() == null) {

            throw new IllegalStateException(
                    "Synchronization aborted: invalid reporting month"
                            + " | Project=["
                            + dto.getProjectName()
                            + "]"
                            + " | Month=["
                            + dto.getReportingMonth()
                            + "]"
                            + " | SourceRow=["
                            + dto.getSourceRowNumber()
                            + "]"
            );
        }


        if (dto.getReportingPeriod() == null) {

            throw new IllegalStateException(
                    "Synchronization aborted: reporting period is missing"
                            + " | Project=["
                            + dto.getProjectName()
                            + "]"
                            + " | Month=["
                            + dto.getReportingMonth()
                            + "]"
                            + " | SourceRow=["
                            + dto.getSourceRowNumber()
                            + "]"
            );
        }
    }


    private void validateConsecutivePeriods(
            Map<ProjectMonthKey, Set<Integer>>
                    periodsByProjectMonth) {

        for (Map.Entry<ProjectMonthKey, Set<Integer>> entry
                : periodsByProjectMonth.entrySet()) {

            Set<Integer> periods =
                    entry.getValue();


            int highestPositivePeriod =
                    periods.stream()
                            .filter(period -> period > 0)
                            .max(Integer::compareTo)
                            .orElse(0);


            /*
             * Period 0 is optional.
             *
             * Genuine periods must be consecutive:
             *
             * 1,2,3,4       OK
             * 0,1,2,3,4     OK
             * 0,1,2,3,4,5   OK
             *
             * 0,1,2,4       NOT OK
             */
            for (int expected = 1;
                 expected <= highestPositivePeriod;
                 expected++) {

                if (!periods.contains(expected)) {

                    throw new IllegalStateException(
                            "Synchronization aborted: "
                                    + "non-consecutive reporting periods"
                                    + " | Project=["
                                    + entry.getKey().projectName()
                                    + "]"
                                    + " | Month=["
                                    + entry.getKey().reportingMonthDate()
                                    + "]"
                                    + " | MissingPeriod=["
                                    + expected
                                    + "]"
                                    + " | ActualPeriods="
                                    + periods
                    );
                }
            }
        }
    }


    private record ProjectMonthKey(
            String projectName,
            LocalDate reportingMonthDate) {
    }


    private record BusinessKey(
            String projectName,
            LocalDate reportingMonthDate,
            Integer reportingPeriod) {
    }
}