package com.pareidolia.projecttracker.service.dashboard;

import java.time.LocalDate;

public interface DashboardKpiPopulationService {

    int populateExistingKpis();

    int rebuildAllKpis();

    /*
     * Rebuild KPI rows beginning with the earliest
     * source month that may have changed.
     *
     * Example:
     * Aug change -> Aug onward
     * Jun change -> Jun onward
     */
    int rebuildKpisFromMonth(LocalDate startMonth);
}