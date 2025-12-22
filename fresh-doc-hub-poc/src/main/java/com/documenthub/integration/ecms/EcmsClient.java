package com.documenthub.integration.ecms;

import com.documenthub.dto.upload.DocumentUploadRequest;
import com.documenthub.integration.ecms.dto.EcmsDocumentResponse;
import com.documenthub.integration.ecms.dto.EcmsErrorResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Client for ECMS (Enterprise Content Management System) API
 * Handles document upload to ECMS which stores documents in S3
 */
@Slf4j
@Component
public class EcmsClient {

    private final WebClient webClient;
    private final EcmsClientConfig config;

    public EcmsClient(WebClient.Builder webClientBuilder, EcmsClientConfig config) {
        this.config = config;

        // Configure connection pool for better performance under load
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ecms-pool")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMs())
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

        this.webClient = webClientBuilder
            .baseUrl(config.getBaseUrl())
            .defaultHeader("apiKey", config.getApiKey())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    /**
     * Upload a document to ECMS
     *
     * @param filePart The file to upload
     * @param request  Upload request with metadata
     * @return ECMS document response with document ID and link
     */
    public Mono<EcmsDocumentResponse> uploadDocument(FilePart filePart, DocumentUploadRequest request) {
        log.info("Uploading document to ECMS: fileName={}", request.getFileName());

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("content", filePart.content(), DataBuffer.class)
            .filename(request.getFileName());
        addCommonMultipartFields(builder, request);

        return executeUpload(builder, request.getFileName());
    }

    /**
     * Upload document from byte array
     *
     * @param fileContent File content as bytes
     * @param request     Upload request with metadata
     * @return ECMS document response
     */
    public Mono<EcmsDocumentResponse> uploadDocument(byte[] fileContent, DocumentUploadRequest request) {
        log.info("Uploading document to ECMS from bytes: fileName={}, size={}",
            request.getFileName(), fileContent.length);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("content", fileContent)
            .filename(request.getFileName());
        addCommonMultipartFields(builder, request);

        return executeUpload(builder, request.getFileName());
    }

    /**
     * Add common multipart fields (name, fileName, attributes, tags) to the builder
     */
    private void addCommonMultipartFields(MultipartBodyBuilder builder, DocumentUploadRequest request) {
        // Add required fields
        builder.part("name", request.getDisplayName() != null ? request.getDisplayName() : request.getFileName());
        builder.part("fileName", request.getFileName());

        // Add optional attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            for (int i = 0; i < request.getAttributes().size(); i++) {
                DocumentUploadRequest.DocumentAttribute attr = request.getAttributes().get(i);
                builder.part("attributes[" + i + "].name", attr.getName());
                builder.part("attributes[" + i + "].value", attr.getValue());
            }
        }

        // Add optional tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (int i = 0; i < request.getTags().size(); i++) {
                builder.part("tags[" + i + "]", request.getTags().get(i));
            }
        }
    }

    /**
     * Execute upload with retry logic for transient failures
     */
    private Mono<EcmsDocumentResponse> executeUpload(MultipartBodyBuilder builder, String fileName) {
        return webClient.post()
            .uri("/documents")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .onStatus(HttpStatus::isError, this::handleErrorResponse)
            .bodyToMono(EcmsDocumentResponse.class)
            .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(500))
                .maxBackoff(Duration.ofSeconds(5))
                .filter(this::isRetryableException)
                .doBeforeRetry(signal -> log.warn("Retrying ECMS upload, attempt {}: {}",
                    signal.totalRetries() + 1, signal.failure().getMessage())))
            .doOnSuccess(resp -> log.info("Document uploaded to ECMS successfully: id={}", resp.getId()))
            .doOnError(e -> log.error("Failed to upload document to ECMS: fileName={}", fileName, e));
    }

    /**
     * Determine if an exception is retryable (transient network/server errors)
     */
    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof EcmsClientException) {
            int status = ((EcmsClientException) ex).getStatusCode();
            // Retry on 5xx server errors and 429 (rate limiting)
            return status >= 500 || status == 429;
        }
        // Retry on network timeouts and connection errors
        return ex instanceof io.netty.channel.ConnectTimeoutException
            || ex instanceof io.netty.handler.timeout.ReadTimeoutException
            || ex instanceof java.net.ConnectException;
    }

    /**
     * Get document metadata from ECMS
     *
     * @param documentId ECMS document ID
     * @return Document metadata
     */
    public Mono<EcmsDocumentResponse> getDocument(UUID documentId) {
        log.debug("Getting document from ECMS: id={}", documentId);

        return webClient.get()
            .uri("/documents/{id}/metadata", documentId)
            .retrieve()
            .onStatus(HttpStatus::isError, this::handleErrorResponse)
            .bodyToMono(EcmsDocumentResponse.class);
    }

    /**
     * Get signed URL for document download
     *
     * @param documentId ECMS document ID
     * @return Signed URL response
     */
    public Mono<String> getDocumentUrl(UUID documentId) {
        log.debug("Getting document URL from ECMS: id={}", documentId);

        return webClient.get()
            .uri("/documents/{id}/URL", documentId)
            .retrieve()
            .onStatus(HttpStatus::isError, this::handleErrorResponse)
            .bodyToMono(UrlResponse.class)
            .map(UrlResponse::getLink);
    }

    /**
     * Delete a document from ECMS
     *
     * @param documentId ECMS document ID
     * @return Mono<Void> on success
     */
    public Mono<Void> deleteDocument(UUID documentId) {
        log.info("Deleting document from ECMS: id={}", documentId);

        return webClient.delete()
            .uri("/documents/{id}", documentId)
            .retrieve()
            .onStatus(HttpStatus::isError, this::handleErrorResponse)
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.info("Document deleted from ECMS: id={}", documentId));
    }

    /**
     * Download document binary content from ECMS
     *
     * @param documentId ECMS document ID (as string)
     * @return Flux of DataBuffer containing document content
     */
    public Mono<reactor.core.publisher.Flux<DataBuffer>> downloadDocument(String documentId) {
        log.info("Downloading document from ECMS: id={}", documentId);

        return webClient.get()
            .uri("/documents/{id}", documentId)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .exchangeToMono(response -> {
                if (response.statusCode().isError()) {
                    return handleErrorResponse(response).flatMap(Mono::error);
                }
                return Mono.just(response.bodyToFlux(DataBuffer.class));
            })
            .doOnSuccess(v -> log.info("Document download initiated: id={}", documentId))
            .doOnError(e -> log.error("Failed to download document from ECMS: id={}", documentId, e));
    }

    /**
     * Handle error response from ECMS
     */
    private Mono<Throwable> handleErrorResponse(ClientResponse response) {
        return response.bodyToMono(EcmsErrorResponse.class)
            .<Throwable>map(error -> new EcmsClientException(response.rawStatusCode(), error))
            .switchIfEmpty(Mono.just(new EcmsClientException(
                response.rawStatusCode(), "ECMS API error")));
    }

    @lombok.Data
    private static class UrlResponse {
        private String link;
    }
}
