package com.documenthub.service;

import com.documenthub.dto.MasterTemplateDto;
import com.documenthub.dto.StorageIndexDto;
import com.documenthub.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentResponseBuilderTest {

    @Mock
    private DocumentAccessControlService accessControlService;

    private ObjectMapper objectMapper;
    private DocumentResponseBuilder responseBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        responseBuilder = new DocumentResponseBuilder(objectMapper, accessControlService);
        ReflectionTestUtils.setField(responseBuilder, "defaultPageSize", 20);
        ReflectionTestUtils.setField(responseBuilder, "maxPageSize", 100);
    }

    @Nested
    @DisplayName("convertToNodes Tests")
    class ConvertToNodesTests {

        @Test
        @DisplayName("Should convert storage entities to document nodes")
        void shouldConvertEntitiesToNodes() {
            // Given
            StorageIndexDto entity = createStorageEntity();
            MasterTemplateDto template = createTemplate();
            List<String> permittedActions = Arrays.asList("View", "Download");

            when(accessControlService.getPermittedActions(any(), anyString()))
                    .thenReturn(permittedActions);
            when(accessControlService.buildLinksForDocument(any(), any()))
                    .thenReturn(new Links());

            // When
            List<DocumentDetailsNode> result = responseBuilder.convertToNodes(
                    List.of(entity), template, "CUSTOMER");

            // Then
            assertEquals(1, result.size());
            assertNotNull(result.get(0).getDocumentId());
            assertEquals("test-file.pdf", result.get(0).getDisplayName());
            assertEquals("TestTemplate", result.get(0).getDocumentType());
        }

        @Test
        @DisplayName("Should return empty list for empty entities")
        void shouldReturnEmptyListForEmptyEntities() {
            // Given
            MasterTemplateDto template = createTemplate();
            when(accessControlService.getPermittedActions(any(), anyString()))
                    .thenReturn(Arrays.asList("View", "Download"));

            // When
            List<DocumentDetailsNode> result = responseBuilder.convertToNodes(
                    Collections.emptyList(), template, "CUSTOMER");

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should populate lineOfBusiness from template")
        void shouldPopulateLineOfBusiness() {
            // Given
            StorageIndexDto entity = createStorageEntity();
            MasterTemplateDto template = createTemplate();
            template.setLineOfBusiness("CREDIT_CARD");

            when(accessControlService.getPermittedActions(any(), anyString()))
                    .thenReturn(Arrays.asList("View", "Download"));
            when(accessControlService.buildLinksForDocument(any(), any()))
                    .thenReturn(new Links());

            // When
            List<DocumentDetailsNode> result = responseBuilder.convertToNodes(
                    List.of(entity), template, "CUSTOMER");

            // Then
            assertEquals("CREDIT_CARD", result.get(0).getLineOfBusiness());
        }
    }

    @Nested
    @DisplayName("buildResponse Tests")
    class BuildResponseTests {

        @Test
        @DisplayName("Should build response with pagination")
        void shouldBuildResponseWithPagination() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(5);

            // When
            DocumentRetrievalResponse response = responseBuilder.buildResponse(
                    documents, 50, 0, 10, 100);

            // Then
            assertEquals(5, response.getDocumentList().size());
            assertNotNull(response.getPagination());
            assertEquals(50, response.getPagination().getTotalItems());
            assertEquals(0, response.getPagination().getPageNumber());
            assertEquals(10, response.getPagination().getPageSize());
            assertEquals(5, response.getPagination().getTotalPages());
        }

        @Test
        @DisplayName("Should calculate total pages correctly")
        void shouldCalculateTotalPagesCorrectly() {
            // Given - 25 total documents, page size 10 = 3 pages
            List<DocumentDetailsNode> documents = createDocumentNodes(10);

            // When
            DocumentRetrievalResponse response = responseBuilder.buildResponse(
                    documents, 25, 0, 10, 100);

            // Then
            assertEquals(3, response.getPagination().getTotalPages());
        }

        @Test
        @DisplayName("Should handle single page result")
        void shouldHandleSinglePageResult() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(5);

            // When
            DocumentRetrievalResponse response = responseBuilder.buildResponse(
                    documents, 5, 0, 10, 100);

            // Then
            assertEquals(1, response.getPagination().getTotalPages());
        }
    }

    @Nested
    @DisplayName("buildEmptyResponse Tests")
    class BuildEmptyResponseTests {

        @Test
        @DisplayName("Should build empty response with default pagination")
        void shouldBuildEmptyResponse() {
            // When
            DocumentRetrievalResponse response = responseBuilder.buildEmptyResponse();

            // Then
            assertNotNull(response);
            assertTrue(response.getDocumentList().isEmpty());
            assertEquals(0, response.getPagination().getTotalItems());
            assertEquals(20, response.getPagination().getPageSize()); // default
        }
    }

    @Nested
    @DisplayName("buildErrorResponse Tests")
    class BuildErrorResponseTests {

        @Test
        @DisplayName("Should build error response as empty response")
        void shouldBuildErrorResponse() {
            // Given
            Exception error = new RuntimeException("Test error");

            // When
            DocumentRetrievalResponse response = responseBuilder.buildErrorResponse(error);

            // Then
            assertNotNull(response);
            assertTrue(response.getDocumentList().isEmpty());
        }
    }

    @Nested
    @DisplayName("paginate Tests")
    class PaginateTests {

        @Test
        @DisplayName("Should paginate first page correctly")
        void shouldPaginateFirstPage() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(25);

            // When
            List<DocumentDetailsNode> result = responseBuilder.paginate(documents, 0, 10);

            // Then
            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("Should paginate middle page correctly")
        void shouldPaginateMiddlePage() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(25);

            // When
            List<DocumentDetailsNode> result = responseBuilder.paginate(documents, 1, 10);

            // Then
            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("Should paginate last partial page correctly")
        void shouldPaginateLastPartialPage() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(25);

            // When
            List<DocumentDetailsNode> result = responseBuilder.paginate(documents, 2, 10);

            // Then
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("Should return empty list when page beyond data")
        void shouldReturnEmptyWhenPageBeyondData() {
            // Given
            List<DocumentDetailsNode> documents = createDocumentNodes(10);

            // When
            List<DocumentDetailsNode> result = responseBuilder.paginate(documents, 5, 10);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty document list")
        void shouldHandleEmptyDocumentList() {
            // When
            List<DocumentDetailsNode> result = responseBuilder.paginate(
                    Collections.emptyList(), 0, 10);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("determinePageSize Tests")
    class DeterminePageSizeTests {

        @Test
        @DisplayName("Should return default page size when null")
        void shouldReturnDefaultWhenNull() {
            // When
            int result = responseBuilder.determinePageSize(null);

            // Then
            assertEquals(20, result);
        }

        @Test
        @DisplayName("Should return default page size when zero or negative")
        void shouldReturnDefaultWhenZeroOrNegative() {
            assertEquals(20, responseBuilder.determinePageSize(BigDecimal.ZERO));
            assertEquals(20, responseBuilder.determinePageSize(BigDecimal.valueOf(-5)));
        }

        @Test
        @DisplayName("Should return requested page size when within limits")
        void shouldReturnRequestedWhenWithinLimits() {
            // When
            int result = responseBuilder.determinePageSize(BigDecimal.valueOf(50));

            // Then
            assertEquals(50, result);
        }

        @Test
        @DisplayName("Should cap page size at max")
        void shouldCapAtMax() {
            // When
            int result = responseBuilder.determinePageSize(BigDecimal.valueOf(500));

            // Then
            assertEquals(100, result); // maxPageSize
        }
    }

    @Nested
    @DisplayName("determinePageNumber Tests")
    class DeterminePageNumberTests {

        @Test
        @DisplayName("Should return 0 when null")
        void shouldReturnZeroWhenNull() {
            assertEquals(0, responseBuilder.determinePageNumber(null));
        }

        @Test
        @DisplayName("Should return 0 when negative")
        void shouldReturnZeroWhenNegative() {
            assertEquals(0, responseBuilder.determinePageNumber(BigDecimal.valueOf(-1)));
        }

        @Test
        @DisplayName("Should return requested page number when valid")
        void shouldReturnRequestedWhenValid() {
            assertEquals(5, responseBuilder.determinePageNumber(BigDecimal.valueOf(5)));
        }
    }

    @Nested
    @DisplayName("getDefaultPageSize Tests")
    class GetDefaultPageSizeTests {

        @Test
        @DisplayName("Should return configured default page size")
        void shouldReturnDefaultPageSize() {
            assertEquals(20, responseBuilder.getDefaultPageSize());
        }
    }

    // Helper methods
    private StorageIndexDto createStorageEntity() {
        StorageIndexDto entity = new StorageIndexDto();
        entity.setStorageIndexId(UUID.randomUUID());
        entity.setStorageDocumentKey(UUID.randomUUID());
        entity.setFileName("test-file.pdf");
        entity.setDocCreationDate(System.currentTimeMillis());
        return entity;
    }

    private MasterTemplateDto createTemplate() {
        MasterTemplateDto template = new MasterTemplateDto();
        template.setTemplateType("TestTemplate");
        template.setTemplateCategory("TestCategory");
        template.setTemplateDescription("Test Description");
        template.setLineOfBusiness("CREDIT_CARD");
        return template;
    }

    private List<DocumentDetailsNode> createDocumentNodes(int count) {
        List<DocumentDetailsNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DocumentDetailsNode node = new DocumentDetailsNode();
            node.setDocumentId("doc-" + i);
            node.setDisplayName("Document " + i);
            nodes.add(node);
        }
        return nodes;
    }
}
