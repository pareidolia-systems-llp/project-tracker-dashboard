package com.pareidolia.projecttracker.service.dashboard;

import com.pareidolia.projecttracker.entity.Project;

import java.util.List;

public interface HistoryService {

    List<Project> getWeeklyHistory(String projectName);

}