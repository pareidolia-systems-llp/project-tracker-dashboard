package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.dashboard.HistoryService;
import com.pareidolia.projecttracker.util.ProjectHistorySorter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl
        implements HistoryService {

    private final ProjectRepository projectRepository;

    @Override
    public List<Project> getWeeklyHistory(String projectName) {

        List<Project> history = new ArrayList<>(
                projectRepository.findByProjectName(projectName)
        );

        ProjectHistorySorter.sort(history);

        return history;

    }

}