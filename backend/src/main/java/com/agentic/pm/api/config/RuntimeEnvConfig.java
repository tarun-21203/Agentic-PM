package com.agentic.pm.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RuntimeEnvConfig {

    @Value("${BUCKET_NAME:agentic-pm-documents}")
    private String bucketName;

    @Value("${JIRA_EPIC_NAME_FIELD:customfield_10011}")
    private String jiraEpicNameField;

    @Value("${JIRA_EPIC_LINK_FIELD:customfield_10014}")
    private String jiraEpicLinkField;

    public String bucketName() {
        return bucketName;
    }

    public String jiraEpicNameField() {
        return jiraEpicNameField;
    }

    public String jiraEpicLinkField() {
        return jiraEpicLinkField;
    }
}

