package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.Links;
import com.documenthub.model.LinksDownload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for document access control and HATEOAS link generation.
 * Handles role-based permissions and link building.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentAccessControlService {

    private final ObjectMapper objectMapper;

    @Value("${app.links.download.expiration-seconds:600}")
    private int linkExpirationSeconds;

    /**
     * Check if a requestor type has access to perform a specific action on a template.
     *
     * @param template      The template to check access for
     * @param requestorType The type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @param action        The action to check (View, Download, Update, Delete)
     * @return true if access is permitted
     */
    public boolean hasAccess(
            MasterTemplateDefinitionEntity template,
            String requestorType,
            String action) {
        List<String> permittedActions = getPermittedActions(template, requestorType);
        return permittedActions.contains(action);
    }

    /**
     * Get permitted actions for a requestor type based on template access_control.
     */
    public List<String> getPermittedActions(
            MasterTemplateDefinitionEntity template,
            String requestorType) {

        if (template.getAccessControl() == null) {
            return getDefaultActions(requestorType);
        }

        try {
            JsonNode accessControlNode = objectMapper.readTree(
                    template.getAccessControl().asString());
            String role = mapRequestorTypeToRole(requestorType);

            return extractActionsForRole(accessControlNode, role, requestorType);
        } catch (Exception e) {
            log.warn("Failed to parse access_control: {}", e.getMessage());
            return getDefaultActions(requestorType);
        }
    }

    /**
     * Build HATEOAS links for a document based on permitted actions.
     * Delete link is never included in enquiry response.
     */
    public Links buildLinksForDocument(
            StorageIndexEntity document,
            List<String> permittedActions) {

        Links links = new Links();

        if (canDownload(permittedActions)) {
            links.setDownload(createDownloadLink(document));
        }

        return links;
    }

    private boolean canDownload(List<String> permittedActions) {
        return permittedActions.contains("Download")
                || permittedActions.contains("View");
    }

    private LinksDownload createDownloadLink(StorageIndexEntity document) {
        LinksDownload download = new LinksDownload();
        download.setHref("/documents/" + document.getStorageDocumentKey());
        download.setType("GET");
        download.setRel("download");
        download.setTitle("Download this document");
        download.setResponseTypes(Arrays.asList(
                "application/pdf", "application/octet-stream"));

        return download;
    }

    private List<String> extractActionsForRole(
            JsonNode accessControlNode,
            String role,
            String requestorType) {

        if (!accessControlNode.isArray()) {
            return getDefaultActions(requestorType);
        }

        for (JsonNode entry : accessControlNode) {
            String entryRole = entry.has("role")
                    ? entry.get("role").asText() : null;

            if (role.equalsIgnoreCase(entryRole)) {
                return extractActions(entry);
            }
        }

        log.debug("Role '{}' not found, using defaults", role);
        return getDefaultActions(requestorType);
    }

    private List<String> extractActions(JsonNode entry) {
        List<String> actions = new ArrayList<>();
        JsonNode actionsNode = entry.get("actions");

        if (actionsNode != null && actionsNode.isArray()) {
            for (JsonNode action : actionsNode) {
                actions.add(action.asText());
            }
        }

        return actions;
    }

    private String mapRequestorTypeToRole(String requestorType) {
        if (requestorType == null) {
            return "customer";
        }

        switch (requestorType.toUpperCase()) {
            case "AGENT":
                return "agent";
            case "SYSTEM":
                return "system";
            case "CUSTOMER":
            default:
                return "customer";
        }
    }

    private List<String> getDefaultActions(String requestorType) {
        if (requestorType == null) {
            return Arrays.asList("View", "Download");
        }

        switch (requestorType.toUpperCase()) {
            case "SYSTEM":
                return Arrays.asList("View", "Update", "Delete", "Download", "Upload");
            case "AGENT":
                return Arrays.asList("View", "Download", "Upload");
            case "CUSTOMER":
            default:
                return Arrays.asList("View", "Download");
        }
    }

    /**
     * Check if a requestor type has upload permission for a template.
     *
     * @param template      The template to check access for
     * @param requestorType The type of requestor (CUSTOMER, AGENT, SYSTEM)
     * @return true if upload is permitted
     */
    public boolean canUpload(MasterTemplateDefinitionEntity template, String requestorType) {
        return hasAccess(template, requestorType, "Upload");
    }
}
