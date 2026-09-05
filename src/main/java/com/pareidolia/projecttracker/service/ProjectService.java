package com.pareidolia.projecttracker.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface ProjectService {

    /*
     * Routine sync:
     * latest two reporting months only.
     */
    void synchronizeProjects()
            throws GeneralSecurityException, IOException;


    /*
     * Infrequent historical reconciliation.
     */
    void auditHistoricalProjects()
            throws GeneralSecurityException, IOException;
}