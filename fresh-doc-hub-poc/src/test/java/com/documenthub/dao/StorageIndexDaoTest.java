package com.documenthub.dao;

import com.documenthub.dto.StorageIndexDto;
import com.documenthub.entity.StorageIndexEntity;
import com.documenthub.repository.StorageIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageIndexDao.
 * Tests the single_document_flag enforcement logic for uploads.
 */
@ExtendWith(MockitoExtension.class)
public class StorageIndexDaoTest {

    @Mock
    private StorageIndexRepository repository;

    private StorageIndexDao storageIndexDao;

    private static final String REF_KEY = "REF-123";
    private static final String REF_KEY_TYPE = "ORDER";
    private static final String TEMPLATE_TYPE = "INVOICE";

    @BeforeEach
    void setUp() {
        storageIndexDao = new StorageIndexDao(repository);
    }

    @Nested
    @DisplayName("updateEndDate Tests")
    class UpdateEndDateTests {

        @Test
        @DisplayName("Should update end_date and timestamps of document")
        void shouldUpdateEndDate() {
            // Given
            StorageIndexEntity entity = createStorageEntity(null);
            UUID storageIndexId = entity.getStorageIndexId();
            Long newEndDate = System.currentTimeMillis();

            when(repository.findById(storageIndexId))
                .thenReturn(Mono.just(entity));
            when(repository.save(any(StorageIndexEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            Mono<StorageIndexDto> result = storageIndexDao.updateEndDate(storageIndexId, newEndDate);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(saved ->
                    newEndDate.equals(saved.getEndDate()) &&
                    saved.getUpdatedTimestamp() != null &&
                    "SYSTEM".equals(saved.getUpdatedBy()))
                .verifyComplete();

            ArgumentCaptor<StorageIndexEntity> captor = ArgumentCaptor.forClass(StorageIndexEntity.class);
            verify(repository).save(captor.capture());
            StorageIndexEntity captured = captor.getValue();
            assertEquals(newEndDate, captured.getEndDate());
            assertNotNull(captured.getUpdatedTimestamp());
            assertEquals("SYSTEM", captured.getUpdatedBy());
        }
    }

    @Nested
    @DisplayName("findActiveByReferenceKey Tests")
    class FindActiveByReferenceKeyTests {

        @Test
        @DisplayName("Should return only accessible documents")
        void shouldReturnOnlyAccessibleDocuments() {
            // Given
            StorageIndexEntity accessibleDoc = createStorageEntity(null);
            accessibleDoc.setAccessibleFlag(true);

            StorageIndexEntity inaccessibleDoc = createStorageEntity(null);
            inaccessibleDoc.setAccessibleFlag(false);

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(accessibleDoc, inaccessibleDoc));

            // When
            Flux<StorageIndexDto> result = storageIndexDao.findActiveByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE);

            // Then
            StepVerifier.create(result)
                .expectNextMatches(doc -> doc.getAccessibleFlag())
                .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty when no accessible documents")
        void shouldReturnEmptyWhenNoAccessibleDocuments() {
            // Given
            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.empty());

            // When
            Flux<StorageIndexDto> result = storageIndexDao.findActiveByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE);

            // Then
            StepVerifier.create(result)
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateEndDateByReferenceKey Tests - Overlap Logic")
    class UpdateEndDateByReferenceKeyTests {

        @Test
        @DisplayName("Should update docs with null end_date (overlapping)")
        void shouldUpdateDocsWithNullEndDate() {
            // Given
            StorageIndexEntity docWithNullEndDate = createStorageEntity(null);
            Long newDocStartDate = System.currentTimeMillis();

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(docWithNullEndDate));
            when(repository.save(any(StorageIndexEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, newDocStartDate);

            // Then
            StepVerifier.create(result)
                .expectNext(1L)
                .verifyComplete();

            verify(repository).save(argThat(entity ->
                newDocStartDate.equals(entity.getEndDate())));
        }

        @Test
        @DisplayName("Should update docs with end_date after new start_date (overlapping)")
        void shouldUpdateDocsWithEndDateAfterNewStartDate() {
            // Given
            Long newDocStartDate = 1000L;
            Long existingEndDate = 2000L; // After new start date = overlapping

            StorageIndexEntity overlappingDoc = createStorageEntity(existingEndDate);

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(overlappingDoc));
            when(repository.save(any(StorageIndexEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, newDocStartDate);

            // Then
            StepVerifier.create(result)
                .expectNext(1L)
                .verifyComplete();

            verify(repository).save(argThat(entity ->
                newDocStartDate.equals(entity.getEndDate())));
        }

        @Test
        @DisplayName("Should NOT update docs with end_date before new start_date (not overlapping)")
        void shouldNotUpdateDocsWithEndDateBeforeNewStartDate() {
            // Given
            Long newDocStartDate = 2000L;
            Long existingEndDate = 1000L; // Before new start date = NOT overlapping

            StorageIndexEntity nonOverlappingDoc = createStorageEntity(existingEndDate);

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(nonOverlappingDoc));

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, newDocStartDate);

            // Then
            StepVerifier.create(result)
                .expectNext(0L)
                .verifyComplete();

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should only update overlapping docs when mixed")
        void shouldOnlyUpdateOverlappingDocsWhenMixed() {
            // Given
            Long newDocStartDate = 1500L;

            StorageIndexEntity overlappingDoc1 = createStorageEntity(null); // null = overlapping
            StorageIndexEntity overlappingDoc2 = createStorageEntity(2000L); // > 1500 = overlapping
            StorageIndexEntity nonOverlappingDoc = createStorageEntity(1000L); // < 1500 = NOT overlapping

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(overlappingDoc1, overlappingDoc2, nonOverlappingDoc));
            when(repository.save(any(StorageIndexEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, newDocStartDate);

            // Then
            StepVerifier.create(result)
                .expectNext(2L) // Only 2 overlapping docs updated
                .verifyComplete();

            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should return 0 when no documents found")
        void shouldReturnZeroWhenNoDocumentsFound() {
            // Given
            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.empty());

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, System.currentTimeMillis());

            // Then
            StepVerifier.create(result)
                .expectNext(0L)
                .verifyComplete();

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should treat all as overlapping when newDocStartDate is null")
        void shouldTreatAllAsOverlappingWhenNewStartDateIsNull() {
            // Given
            StorageIndexEntity doc1 = createStorageEntity(1000L);
            StorageIndexEntity doc2 = createStorageEntity(2000L);

            when(repository.findByReferenceKeyAndTemplate(
                eq(REF_KEY), eq(REF_KEY_TYPE), eq(TEMPLATE_TYPE), isNull(), anyLong()))
                .thenReturn(Flux.just(doc1, doc2));
            when(repository.save(any(StorageIndexEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            Mono<Long> result = storageIndexDao.updateEndDateByReferenceKey(
                REF_KEY, REF_KEY_TYPE, TEMPLATE_TYPE, null);

            // Then
            StepVerifier.create(result)
                .expectNext(2L)
                .verifyComplete();

            verify(repository, times(2)).save(any());
        }
    }

    private StorageIndexEntity createStorageEntity(Long endDate) {
        return StorageIndexEntity.builder()
            .storageIndexId(UUID.randomUUID())
            .templateType(TEMPLATE_TYPE)
            .referenceKey(REF_KEY)
            .referenceKeyType(REF_KEY_TYPE)
            .accessibleFlag(true)
            .endDate(endDate)
            .build();
    }
}
