package com.pareidolia.projecttracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sheet_sync_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sheet_sync_month",
                        columnNames = "reporting_month_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SheetSyncState {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "reporting_month_date",
            nullable = false
    )
    private LocalDate reportingMonthDate;

    @Column(
            name = "sheet_name",
            nullable = false,
            length = 150
    )
    private String sheetName;

    @Column(
            name = "content_hash",
            nullable = false,
            length = 64
    )
    private String contentHash;

    @Column(
            name = "last_checked_at",
            nullable = false
    )
    private LocalDateTime lastCheckedAt;

    @Column(
            name = "last_changed_at"
    )
    private LocalDateTime lastChangedAt;

    @Column(
            name = "sync_status",
            nullable = false,
            length = 30
    )
    private String syncStatus;
}