package com.pareidolia.projecttracker.constants;

public final class SheetColumns {

    private SheetColumns() {
        // Prevent instantiation
    }

    public static final String SERIAL_NUMBER = "Sl No";
    public static final String PROJECT_NAME = "Project Name";
    public static final String PROJECT_TYPE = "Project Type";
    public static final String PRIORITY = "Priority";
    public static final String TOTAL_DATA_REQUESTED_BY_AML = "Total Data Requested by AML";
    public static final String VENDOR = "Vendor";
    public static final String DATA_REQUESTED = "Data Requested";
    public static final String DATA_RECEIVED = "Data Received";
    public static final String NUMBER_OF_CASES_RECEIVED = "Number of Cases Received";
    public static final String CASES_IN_GROUND_TRUTH = "Cases in Ground Truth";
    public static final String ONGOING_REDBRICK_UPLOADS = "Ongoing Redbrick Uploads";
    public static final String ASSIGNED_TO_PAREIDOLIA = "Assigned to Pareidolia";
    public static final String WEEK_OF = "Week Of";
    public static final String ASSIGNED_DATE = "Assigned Date";
    public static final String ANNOTATION_PENDING_FROM_PAREIDOLIA = "Annotation Pending from Pareidolia";
    public static final String REVIEWS_PENDING_FROM_PAREIDOLIA = "Reviews Pending from Pareidolia";
    public static final String PENDING_RAPID_AI_REVIEW = "Pending RapidAI Review";
    public static final String RAPID_REVIEW_WEEKLY_ASSIGNED = "Rapid Review Weekly Assigned";
    public static final String ANNOTATIONS_SUBMITTED_PAREIDOLIA = "Annotations Submitted Pareidolia";
    public static final String TOTAL_ANNOTATIONS_SUBMITTED = "Total Annotations Submitted";
    public static final String CASES_NOT_ANNOTATED_DUE_TO_QUALITY = "Cases Not Annotated Due to Quality";
    public static final String ISSUE_CASES = "Issue Cases";
    public static final String CASES_MOVED_TO_LABELS = "Cases Moved to Labels";
    public static final String ETD = "ETD";
    public static final String ANNOTATIONS_COMPLETED =
            "Annotations Completed (GT+Rapid Review)";
    public static final String PENDING_ANNOTATIONS = "Pending Annotations + Cases Moved to Labels + Not Assigned Cases";
    public static final String NOTES = "Notes";
}