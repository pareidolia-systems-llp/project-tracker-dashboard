package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.SheetSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SheetSyncStateRepository
        extends JpaRepository<SheetSyncState, Long> {

    Optional<SheetSyncState>
    findByReportingMonthDate(
            LocalDate reportingMonthDate
    );
}