# Enterprise Project Tracker Dashboard
## Dashboard Functional Specification (DFS)

## Objective
Build an enterprise reporting dashboard while continuing to use Google Sheets.

## Dashboards
- Executive Dashboard
- Client Dashboard
- Project Dashboard

## Workflow
Total Data Requested → Data Received → Cases in Ground Truth → Ongoing Redbrick Uploads → Assigned to Pareidolia → Annotation Pending → Reviews Pending → Client Review Pending → Cases Moved to Labels → Accepted

## KPIs
- Total Data Requested
- Cases Received
- Ground Truth
- Assigned to Pareidolia
- Total Annotations Submitted
- Issue Cases
- Cases Moved to Labels
- Accepted
- Rejected
- Acceptance %
- Rejection %
- Completion %
- ETD

## Historical Reporting
Weekly, Monthly, Project-to-Date, Custom Date Range.

Business Week = Monday to Sunday.

## Filters
Client, Project, Vendor, Priority, Project Type, Business Week, Date Range.

## Roles
Admin
Client

# Email Notification

Trigger when:
Health = Red
Pending cases > 0
ETD ≤ 2 days
Send reminder to configured recipients.

# Business Reporting Period

Week 1 = 01–07
Week 2 = 08–14
Week 3 = 15–21
Week 4 = 22–Last day of month

The final reporting period always ends on the month's last day and does not extend into the following month.


Final Reporting Model (I recommend we freeze this)
1. Weekly (Business Reporting Period)

This comes directly from the Google Sheet.

Example:

Business Period	Assigned	Submitted	Issue
01–07 Jun	20	18	2
08–14 Jun	15	12	1

This is the operational data.

2. Project-to-Date (PTD)

This is calculated from the day the project started, using all available historical sheets plus ongoing scheduler snapshots.

Example:

| Business Period | Weekly Assigned | PTD Assigned |
| --------------- | --------------: | -----------: |
| 01–07 Jun       |              20 |           20 |
| 08–14 Jun       |              15 |           35 |
| 15–21 Jun       |              18 |           53 |
| 22–30 Jun       |              22 |           75 |


This is exactly what management wants to see.

3. Historical Trends

Using the stored snapshots, the dashboard can show:

Weekly trends
Monthly trends
Custom date ranges
Entire project history

These are all generated from the same historical data.

Which fields have PTD?

After today's discussion, I'd freeze it as:

| Field                                | Weekly | Project-to-Date |
| ------------------------------------ | :----: | :-------------: |
| Assigned to Pareidolia (L)           |    ✅   |  ✅ (Calculated) |
| Annotations Submitted Pareidolia (S) |    ✅   |  ✅ (Calculated) |
| Issue Cases (V)                      |    ✅   |  ✅ (Calculated) |
| Acceptance %                         |    ❌   |  ✅ (Calculated) |
| Rejection %                          |    ❌   |  ✅ (Calculated) |
| Overall Completion %                 |    ❌   |  ✅ (Calculated) |


Workflow : 

Historical Sheets (Feb 2025 → Current)
│
▼
One-time Historical Import
│
▼
MySQL Database
│
▼
Spring Scheduler
(keeps data updated)
│
▼
Weekly + Project-to-Date + Trends

