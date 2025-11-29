package com.example.controller;

import com.example.api.FilesApi;
import com.example.model.ErrorResponse;
import com.example.model.FileMetadata;
import com.example.model.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reactive controller implementation for file operations.
 * <p>
 * This controller demonstrates the proper way to handle file uploads
 * in Spring WebFlux using Mono<FilePart> instead of MultipartFile.
 * <p>
 * NOTE: The generated interface FilesApi still uses MultipartFile because
 * Swagger Codegen 3.x has limitations with custom templates. This controller
 * shows the CORRECT reactive implementation.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FilesController {

    /**
     * Upload a file with metadata - REACTIVE VERSION
     * <p>
     * This is the correct implementation using Mono<FilePart> for reactive file handling.
     *
     * @param file       The file to upload (reactive)
     * @param fileName   Name of the file
     * @param uploadedBy User ID who uploaded the file
     * @param metadata   File metadata object
     * @param tags       Optional tags
     * @return Mono of ResponseEntity with upload response
     */
    @PostMapping(
        value = "/files",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<FileUploadResponse>> uploadFile(
        @RequestPart(value = "file", required = true) Mono<FilePart> file,  // ✅ REACTIVE
        @RequestPart(value = "fileName", required = true) String fileName,
        @RequestPart(value = "uploadedBy", required = true) String uploadedBy,
        @RequestPart(value = "metadata", required = false) FileMetadata metadata,
        @RequestPart(value = "tags", required = false) List<String> tags
    ) {
        log.info("Uploading file: {} by user: {}", fileName, uploadedBy);

        return file.flatMap(filePart -> {
            // Access reactive FilePart properties
            String originalFilename = filePart.filename();
            long size = filePart.headers().getContentLength();

            log.info("File part received: {}, size: {} bytes", originalFilename, size);

            // In a real application, you would:
            // 1. Save file to storage (S3, filesystem, etc.)
            // 2. Store metadata in database
            // 3. Return actual file ID

            // For demo, create mock response
            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(UUID.randomUUID());
            response.setMessage("File uploaded successfully: " + fileName);
            response.setUploadedAt(OffsetDateTime.now());

            return Mono.just(ResponseEntity.ok(response));
        }).onErrorResume(error -> {
            log.error("Error uploading file: {}", error.getMessage(), error);

            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setError("UPLOAD_FAILED");
            errorResponse.setMessage(error.getMessage());
            errorResponse.setTimestamp(OffsetDateTime.now());

            return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FileUploadResponse()));
        });
    }

    /**
     * Get file metadata by ID - REACTIVE VERSION
     *
     * @param fileId UUID of the file
     * @return Mono of ResponseEntity with file metadata
     */
    @GetMapping(
        value = "/files/{fileId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<FileMetadata>> getFileMetadata(
        @PathVariable("fileId") UUID fileId
    ) {
        log.info("Fetching metadata for file: {}", fileId);

        // In a real application, fetch from database
        // For demo, create mock response
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setFileName("sample-file.pdf");
        metadata.setFileSize(1024L * 1024L); // 1 MB
        metadata.setContentType("application/pdf");
        metadata.setUploadedBy("user123");
        metadata.setUploadedAt(OffsetDateTime.now());
        metadata.setTags(List.of("sample", "demo"));

        return Mono.just(ResponseEntity.ok(metadata));
    }
}
