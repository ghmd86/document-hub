package com.documenthub.service;

import com.documenthub.entity.MasterTemplateDefinitionEntity;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.model.Links;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentAccessControlServiceTest {

    private DocumentAccessControlService accessControlService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        accessControlService = new DocumentAccessControlService(objectMapper);
        ReflectionTestUtils.setField(accessControlService, "linkExpirationSeconds", 600);
    }

    @Nested
    @DisplayName("getPermittedActions Tests")
    class GetPermittedActionsTests {

        @Test
        @DisplayName("Should return default actions when access_control is null")
        void shouldReturnDefaultActions_whenAccessControlNull() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then
            assertEquals(Arrays.asList("View", "Download"), actions);
        }

        @Test
        @DisplayName("Should return all actions for SYSTEM requestor")
        void shouldReturnAllActions_forSystemRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "SYSTEM");

            // Then
            assertTrue(actions.contains("View"));
            assertTrue(actions.contains("Update"));
            assertTrue(actions.contains("Delete"));
            assertTrue(actions.contains("Download"));
            assertTrue(actions.contains("Upload"));
        }

        @Test
        @DisplayName("Should return View/Download/Upload for AGENT requestor")
        void shouldReturnViewDownloadUpload_forAgentRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "AGENT");

            // Then
            assertEquals(Arrays.asList("View", "Download", "Upload"), actions);
        }

        @Test
        @DisplayName("Should return View/Download for null requestor")
        void shouldReturnViewDownload_forNullRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            List<String> actions = accessControlService.getPermittedActions(template, null);

            // Then
            assertEquals(Arrays.asList("View", "Download"), actions);
        }

        @Test
        @DisplayName("Should parse access_control JSON and return actions for role")
        void shouldParseAccessControlJson_andReturnActionsForRole() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\",\"actions\":[\"View\",\"Download\"]}," +
                    "{\"role\":\"agent\",\"actions\":[\"View\",\"Update\",\"Download\"]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then
            assertEquals(Arrays.asList("View", "Download"), actions);
        }

        @Test
        @DisplayName("Should return agent actions for agent role")
        void shouldReturnAgentActions_forAgentRole() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\",\"actions\":[\"View\",\"Download\"]}," +
                    "{\"role\":\"agent\",\"actions\":[\"View\",\"Update\",\"Download\"]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "AGENT");

            // Then
            assertEquals(Arrays.asList("View", "Update", "Download"), actions);
        }

        @Test
        @DisplayName("Should return default actions when role not found in config")
        void shouldReturnDefaultActions_whenRoleNotInConfig() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\",\"actions\":[\"View\"]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When - requesting as SYSTEM but only customer role is defined
            List<String> actions = accessControlService.getPermittedActions(template, "SYSTEM");

            // Then - should return default for SYSTEM
            assertTrue(actions.contains("View"));
            assertTrue(actions.contains("Update"));
            assertTrue(actions.contains("Delete"));
            assertTrue(actions.contains("Download"));
            assertTrue(actions.contains("Upload"));
        }

        @Test
        @DisplayName("Should return default actions when JSON is not array")
        void shouldReturnDefaultActions_whenJsonNotArray() {
            // Given
            String accessControlJson = "{\"role\":\"customer\",\"actions\":[\"View\"]}";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then
            assertEquals(Arrays.asList("View", "Download"), actions);
        }

        @Test
        @DisplayName("Should return default actions when JSON parsing fails")
        void shouldReturnDefaultActions_whenJsonParsingFails() {
            // Given - invalid JSON
            String invalidJson = "not valid json";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(invalidJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then - should return defaults
            assertEquals(Arrays.asList("View", "Download"), actions);
        }

        @Test
        @DisplayName("Should handle empty actions array")
        void shouldHandleEmptyActionsArray() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\",\"actions\":[]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("Should handle missing actions field")
        void shouldHandleMissingActionsField() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\"}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            List<String> actions = accessControlService.getPermittedActions(template, "CUSTOMER");

            // Then
            assertTrue(actions.isEmpty());
        }
    }

    @Nested
    @DisplayName("buildLinksForDocument Tests")
    class BuildLinksForDocumentTests {

        @Test
        @DisplayName("Should build download link when View is permitted")
        void shouldBuildDownloadLink_whenViewPermitted() {
            // Given
            StorageIndexEntity document = createStorageEntity();
            List<String> permittedActions = Arrays.asList("View");

            // When
            Links links = accessControlService.buildLinksForDocument(document, permittedActions);

            // Then
            assertNotNull(links.getDownload());
            assertTrue(links.getDownload().getHref().startsWith("/documents/"));
            assertEquals("GET", links.getDownload().getType());
            assertEquals("download", links.getDownload().getRel());
        }

        @Test
        @DisplayName("Should build download link when Download is permitted")
        void shouldBuildDownloadLink_whenDownloadPermitted() {
            // Given
            StorageIndexEntity document = createStorageEntity();
            List<String> permittedActions = Arrays.asList("Download");

            // When
            Links links = accessControlService.buildLinksForDocument(document, permittedActions);

            // Then
            assertNotNull(links.getDownload());
        }

        @Test
        @DisplayName("Should not build download link when neither View nor Download permitted")
        void shouldNotBuildDownloadLink_whenNotPermitted() {
            // Given
            StorageIndexEntity document = createStorageEntity();
            List<String> permittedActions = Arrays.asList("Update");

            // When
            Links links = accessControlService.buildLinksForDocument(document, permittedActions);

            // Then
            assertNull(links.getDownload());
        }

        @Test
        @DisplayName("Should set expiration time on download link")
        void shouldSetExpirationTime() {
            // Given
            StorageIndexEntity document = createStorageEntity();
            List<String> permittedActions = Arrays.asList("View", "Download");
            long beforeTime = System.currentTimeMillis() / 1000;

            // When
            Links links = accessControlService.buildLinksForDocument(document, permittedActions);

            // Then
            assertNotNull(links.getDownload().getExpiresAt());
            long expiresAt = links.getDownload().getExpiresAt();
            assertTrue(expiresAt > beforeTime);
            assertTrue(expiresAt <= beforeTime + 700); // 600 seconds + some buffer
        }

        @Test
        @DisplayName("Should set correct response types")
        void shouldSetCorrectResponseTypes() {
            // Given
            StorageIndexEntity document = createStorageEntity();
            List<String> permittedActions = Arrays.asList("View", "Download");

            // When
            Links links = accessControlService.buildLinksForDocument(document, permittedActions);

            // Then
            assertTrue(links.getDownload().getResponseTypes().contains("application/pdf"));
            assertTrue(links.getDownload().getResponseTypes().contains("application/octet-stream"));
        }
    }

    @Nested
    @DisplayName("canUpload Tests")
    class CanUploadTests {

        @Test
        @DisplayName("Should allow upload for SYSTEM requestor by default")
        void shouldAllowUpload_forSystemRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            boolean canUpload = accessControlService.canUpload(template, "SYSTEM");

            // Then
            assertTrue(canUpload);
        }

        @Test
        @DisplayName("Should allow upload for AGENT requestor by default")
        void shouldAllowUpload_forAgentRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            boolean canUpload = accessControlService.canUpload(template, "AGENT");

            // Then
            assertTrue(canUpload);
        }

        @Test
        @DisplayName("Should deny upload for CUSTOMER requestor by default")
        void shouldDenyUpload_forCustomerRequestor() {
            // Given
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(null);

            // When
            boolean canUpload = accessControlService.canUpload(template, "CUSTOMER");

            // Then
            assertFalse(canUpload);
        }

        @Test
        @DisplayName("Should allow upload when access_control grants Upload action")
        void shouldAllowUpload_whenAccessControlGrantsUpload() {
            // Given
            String accessControlJson = "[{\"role\":\"customer\",\"actions\":[\"View\",\"Upload\"]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            boolean canUpload = accessControlService.canUpload(template, "CUSTOMER");

            // Then
            assertTrue(canUpload);
        }

        @Test
        @DisplayName("Should deny upload when access_control does not include Upload")
        void shouldDenyUpload_whenAccessControlDoesNotIncludeUpload() {
            // Given
            String accessControlJson = "[{\"role\":\"agent\",\"actions\":[\"View\",\"Download\"]}]";
            MasterTemplateDefinitionEntity template = new MasterTemplateDefinitionEntity();
            template.setAccessControl(Json.of(accessControlJson));

            // When
            boolean canUpload = accessControlService.canUpload(template, "AGENT");

            // Then
            assertFalse(canUpload);
        }
    }

    // Helper methods
    private StorageIndexEntity createStorageEntity() {
        StorageIndexEntity entity = new StorageIndexEntity();
        entity.setStorageIndexId(UUID.randomUUID());
        entity.setStorageDocumentKey(UUID.randomUUID());
        entity.setFileName("test-file.pdf");
        return entity;
    }
}
