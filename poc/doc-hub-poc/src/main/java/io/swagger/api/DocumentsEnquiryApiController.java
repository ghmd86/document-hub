package io.swagger.api;

import io.swagger.model.DocumentListRequest;
import io.swagger.model.DocumentRetrievalResponse;
import io.swagger.model.ErrorResponse;
import java.util.UUID;
import io.swagger.model.XRequestorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-11-24T22:19:46.681626062Z[GMT]")
@RestController
public class DocumentsEnquiryApiController implements DocumentsEnquiryApi {

    private static final Logger log = LoggerFactory.getLogger(DocumentsEnquiryApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public DocumentsEnquiryApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<DocumentRetrievalResponse> documentsEnquiryPost(@Parameter(in = ParameterIn.HEADER, description = "Api version" ,required=true,schema=@Schema()) @RequestHeader(value="X-version", required=true) Integer xVersion
,@Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing" ,required=true,schema=@Schema()) @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId
,@Parameter(in = ParameterIn.HEADER, description = "Id of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId
,@Parameter(in = ParameterIn.HEADER, description = "Type of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-type", required=true) XRequestorType xRequestorType
,@Parameter(in = ParameterIn.DEFAULT, description = "", schema=@Schema()) @Valid @RequestBody DocumentListRequest body
) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<DocumentRetrievalResponse>(objectMapper.readValue("{\"pagination\":{\"totalItems\":6,\"pageNumber\":5,\"totalPages\":1,\"pageSize\":0},\"documentList\":[{\"lineOfBusiness\":\"CREDIT_CARD\",\"lastClientDownload\":\"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",\"metadata\":[{\"key\":\"accountId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"},{\"key\":\"issueDate\",\"value\":\"2025-01-01\"},{\"key\":\"customerId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66a78\"},{\"key\":\"htmlLettername\",\"value\":\"FF083\"}],\"documentType\":\"PaymentLetter\",\"_links\":{\"download\":{\"responseTypes\":[\"application/pdf\",\"application/octet-stream\"],\"rel\":\"download\",\"href\":\"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\"type\":\"GET\",\"title\":\"Download this document\"},\"delete\":{\"rel\":\"delete\",\"href\":\"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\"type\":\"DELETE\",\"title\":\"Delete this document\"}},\"displayName\":\"2024_PRIVACY_POLICY\",\"description\":\"2024_PRIVACY_POLICY\",\"mimeType\":\"application/pdf\",\"languageCode\":\"EN_US\",\"sizeInMb\":1,\"documentId\":\"documentId\",\"category\":\"PaymentConfirmationNotice\",\"datePosted\":1740523843},{\"lineOfBusiness\":\"CREDIT_CARD\",\"lastClientDownload\":\"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",\"metadata\":[{\"key\":\"accountId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"},{\"key\":\"issueDate\",\"value\":\"2025-01-01\"},{\"key\":\"customerId\",\"value\":\"3fa85f64-5717-4562-b3fc-2c963f66a78\"},{\"key\":\"htmlLettername\",\"value\":\"FF083\"}],\"documentType\":\"PaymentLetter\",\"_links\":{\"download\":{\"responseTypes\":[\"application/pdf\",\"application/octet-stream\"],\"rel\":\"download\",\"href\":\"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\"type\":\"GET\",\"title\":\"Download this document\"},\"delete\":{\"rel\":\"delete\",\"href\":\"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\"type\":\"DELETE\",\"title\":\"Delete this document\"}},\"displayName\":\"2024_PRIVACY_POLICY\",\"description\":\"2024_PRIVACY_POLICY\",\"mimeType\":\"application/pdf\",\"languageCode\":\"EN_US\",\"sizeInMb\":1,\"documentId\":\"documentId\",\"category\":\"PaymentConfirmationNotice\",\"datePosted\":1740523843}],\"_links\":{\"self\":{\"href\":\"/documents-enquiry\",\"type\":\"POST\",\"requestBody\":{\"pageNumber\":2,\"pageSize\":10},\"rel\":\"self\",\"responseTypes\":[\"application/json\"]},\"next\":{\"href\":\"/documents-enquiry\",\"type\":\"POST\",\"requestBody\":{\"pageNumber\":2,\"pageSize\":10},\"rel\":\"next\",\"responseTypes\":[\"application/json\"]},\"prev\":{\"href\":\"/documents-enquiry\",\"type\":\"POST\",\"requestBody\":{\"pageNumber\":2,\"pageSize\":10},\"rel\":\"prev\",\"responseTypes\":[\"application/json\"]}}}", DocumentRetrievalResponse.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<DocumentRetrievalResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<DocumentRetrievalResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

}
