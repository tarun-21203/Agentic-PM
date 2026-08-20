package com.agentic.pm.integration.jira;

import com.agentic.pm.utils.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class JiraClient {

    private final HttpClient httpClient;
    private final String baseUrl; // e.g. https://tenant.atlassian.net
    private final String authHeader; // Basic base64(email:token)

    public JiraClient(String baseUrl, String email, String apiToken) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        String raw = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> getMyself() {
        return request("GET", "/rest/api/3/myself", null);
    }

    public Map<String, Object> createProject(Map<String, Object> body) {
        return request("POST", "/rest/api/3/project", body);
    }

    public Map<String, Object> createIssue(Map<String, Object> body) {
        return request("POST", "/rest/api/3/issue", body);
    }

    /**
     * Issue types vary by Jira project template (team-managed vs company-managed, locale, etc.).
     * Use this to resolve ids before {@link #createIssue}.
     */
    public Map<String, Object> getIssueCreateMetadata(String projectKey) {
        String key = projectKey != null ? projectKey.trim() : "";
        String q = URLEncoder.encode(key, StandardCharsets.UTF_8);
        return request("GET", "/rest/api/3/issue/createmeta?projectKeys=" + q + "&expand=projects.issuetypes", null);
    }

    public void updateIssue(String issueKey, Map<String, Object> body) {
        requestNoBody("PUT", "/rest/api/3/issue/" + issueKey, body);
    }

    /** Jira Software Agile API — boards for a project (empty {@code values} if none). */
    public Map<String, Object> listAgileBoardsForProject(String projectKeyOrId) {
        String id = projectKeyOrId != null ? projectKeyOrId.trim() : "";
        String q = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return request("GET", "/rest/agile/1.0/board?projectKeyOrId=" + q + "&maxResults=50", null);
    }

    public Map<String, Object> createFilter(Map<String, Object> body) {
        return request("POST", "/rest/api/3/filter", body);
    }

    /** Jira Software Agile API — requires a saved filter and {@code write:board-scope:jira-software} scope. */
    public Map<String, Object> createAgileBoard(Map<String, Object> body) {
        return request("POST", "/rest/agile/1.0/board", body);
    }

    private Map<String, Object> request(String method, String path, Map<String, Object> jsonBody) {
        HttpRequest httpRequest = buildJsonRequest(method, path, jsonBody);
        try {
            HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return parseJsonResponse(method, path, resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("JIRA request interrupted: " + method + " " + path, e);
        } catch (IOException e) {
            throw new RuntimeException("JIRA request I/O error: " + method + " " + path + " — " + e.getMessage(), e);
        }
    }

    private void requestNoBody(String method, String path, Map<String, Object> jsonBody) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(jsonBody)))
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(method, path, resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("JIRA request interrupted: " + method + " " + path, e);
        } catch (IOException e) {
            throw new RuntimeException("JIRA request I/O error: " + method + " " + path + " — " + e.getMessage(), e);
        }
    }

    private HttpRequest buildJsonRequest(String method, String path, Map<String, Object> jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authHeader)
                .header("Accept", "application/json");
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(jsonBody)));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String method, String path, HttpResponse<String> resp) {
        ensureSuccess(method, path, resp);
        String body = resp.body() != null ? resp.body() : "";
        if (body.isBlank()) {
            return Map.of();
        }
        try {
            return JsonUtil.fromJson(body, Map.class);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                    "JIRA API " + method + " " + path + " returned HTTP " + resp.statusCode()
                            + " but body was not valid JSON: " + truncate(body, 1500),
                    e);
        }
    }

    private static void ensureSuccess(String method, String path, HttpResponse<String> resp) {
        int code = resp.statusCode();
        if (code >= 200 && code < 300) {
            return;
        }
        String body = resp.body() != null ? resp.body() : "";
        throw new RuntimeException(
                "JIRA API " + method + " " + path + " failed: HTTP " + code + " — " + truncate(body, 4000));
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "…(truncated)";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}

