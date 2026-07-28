package com.pareidolia.projecttracker.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface ProjectService {

    void synchronizeProjects() throws GeneralSecurityException, IOException;

}