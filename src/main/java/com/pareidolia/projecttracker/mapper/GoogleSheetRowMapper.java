package com.pareidolia.projecttracker.mapper;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static com.pareidolia.projecttracker.constants.SheetColumns.*;

@Component
@Slf4j
public class GoogleSheetRowMapper {

    public GoogleSheetRowDto mapRow(
            List<Object> row,
            Map<String, Integer> columnIndexMap) {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();

        dto.setSerialNumber(
                safeGet(row, columnIndexMap, SERIAL_NUMBER)
        );

        dto.setProjectName(
                safeGet(row, columnIndexMap, PROJECT_NAME)
        );

        dto.setProjectType(
                safeGet(row, columnIndexMap, PROJECT_TYPE)
        );

        dto.setPriority(
                safeGet(row, columnIndexMap, PRIORITY)
        );

        dto.setTotalDataRequestedByAml(
                safeGet(row, columnIndexMap, TOTAL_DATA_REQUESTED_BY_AML)
        );

        dto.setVendor(
                safeGet(row, columnIndexMap, VENDOR)
        );

        dto.setDataRequested(
                safeGet(row, columnIndexMap, DATA_REQUESTED)
        );

        dto.setDataReceived(
                safeGet(row, columnIndexMap, DATA_RECEIVED)
        );

        dto.setNumberOfCasesReceived(
                safeGet(row, columnIndexMap, NUMBER_OF_CASES_RECEIVED)
        );

        dto.setCasesInGroundTruth(
                safeGet(row, columnIndexMap, CASES_IN_GROUND_TRUTH)
        );

        dto.setOngoingRedbrickUploads(
                safeGet(row, columnIndexMap, ONGOING_REDBRICK_UPLOADS)
        );

        dto.setAssignedToPareidolia(
                safeGet(row, columnIndexMap, ASSIGNED_TO_PAREIDOLIA)
        );

        dto.setWeekOf(
                safeGet(row, columnIndexMap, WEEK_OF)
        );

        dto.setAssignedDate(
                safeGet(row, columnIndexMap, ASSIGNED_DATE)
        );

        dto.setAnnotationPendingFromPareidolia(
                safeGet(
                        row,
                        columnIndexMap,
                        ANNOTATION_PENDING_FROM_PAREIDOLIA
                )
        );

        dto.setReviewsPendingFromPareidolia(
                safeGet(
                        row,
                        columnIndexMap,
                        REVIEWS_PENDING_FROM_PAREIDOLIA
                )
        );

        dto.setPendingRapidAiReview(
                safeGet(
                        row,
                        columnIndexMap,
                        PENDING_RAPID_AI_REVIEW
                )
        );

        dto.setRapidReviewWeeklyAssigned(
                safeGet(
                        row,
                        columnIndexMap,
                        RAPID_REVIEW_WEEKLY_ASSIGNED
                )
        );

        dto.setAnnotationsSubmittedPareidolia(
                safeGet(
                        row,
                        columnIndexMap,
                        ANNOTATIONS_SUBMITTED_PAREIDOLIA
                )
        );

        dto.setTotalAnnotationsSubmitted(
                safeGet(
                        row,
                        columnIndexMap,
                        TOTAL_ANNOTATIONS_SUBMITTED
                )
        );

        dto.setCasesNotAnnotatedDueToQuality(
                safeGet(
                        row,
                        columnIndexMap,
                        CASES_NOT_ANNOTATED_DUE_TO_QUALITY
                )
        );

        dto.setIssueCases(
                safeGet(row, columnIndexMap, ISSUE_CASES)
        );

        dto.setCasesMovedToLabels(
                safeGet(
                        row,
                        columnIndexMap,
                        CASES_MOVED_TO_LABELS
                )
        );

        dto.setEtd(
                safeGet(row, columnIndexMap, ETD)
        );

        dto.setAnnotationsCompleted(
                safeGet(
                        row,
                        columnIndexMap,
                        columnIndexMap.containsKey(normalize(GROUND_TRUTH_CASES))
                                ? GROUND_TRUTH_CASES
                                : columnIndexMap.containsKey(normalize(GROUND_TRUTH))
                                ? GROUND_TRUTH
                                : columnIndexMap.containsKey(normalize(ANNOTATIONS_COMPLETED_SPACED))
                                ? ANNOTATIONS_COMPLETED_SPACED : ANNOTATIONS_COMPLETED
                )
        );

        dto.setProjectInstruction(
                validateProjectInstruction(safeGet(row, columnIndexMap, PROJECT_INSTRUCTION))
        );

        dto.setPendingAnnotations(
                safeGet(
                        row,
                        columnIndexMap,
                        PENDING_ANNOTATIONS
                )
        );

        dto.setNotes(
                safeGet(row, columnIndexMap, NOTES)
        );

        return dto;
    }

    private String validateProjectInstruction(String value) {
        if (value.isBlank()) {
            return null;
        }

        if (value.length() > 2048) {
            return invalidProjectInstruction();
        }

        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !(host.equalsIgnoreCase("drive.google.com")
                         || host.equalsIgnoreCase("docs.google.com"))
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return invalidProjectInstruction();
            }
            return value;
        } catch (URISyntaxException ex) {
            return invalidProjectInstruction();
        }
    }

    private String invalidProjectInstruction() {
        log.warn("Project Instruction ignored: invalid, unsupported, or overlength URL.");
        return null;
    }

    private String safeGet(
            List<Object> row,
            Map<String, Integer> columnIndexMap,
            String columnName) {

        Integer index =
                columnIndexMap.get(normalize(columnName));

        if (index == null || index >= row.size()) {
            return "";
        }

        Object value = row.get(index);

        return value == null
                ? ""
                : value.toString().trim();
    }

    private String normalize(String header) {

        return header == null
                ? ""
                : header.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}
