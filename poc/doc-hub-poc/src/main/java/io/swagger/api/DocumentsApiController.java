package io.swagger.api;

import io.swagger.model.DocumentDetailsNode;
import io.swagger.model.ErrorResponse;
import io.swagger.model.InlineResponse200;
import io.swagger.model.Metadata;
import org.springframework.core.io.Resource;
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
public class DocumentsApiController implements DocumentsApi {

    private static final Logger log = LoggerFactory.getLogger(DocumentsApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public DocumentsApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<InlineResponse200> addDocument(@Parameter(in = ParameterIn.HEADER, description = "Api version" ,required=true,schema=@Schema()) @RequestHeader(value="X-version", required=true) Integer xVersion
,@Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing" ,required=true,schema=@Schema()) @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId
,@Parameter(in = ParameterIn.HEADER, description = "Id of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId
,@Parameter(in = ParameterIn.HEADER, description = "Type of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-type", required=true) XRequestorType xRequestorType
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="documentType", required=true)  String documentType
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="createdBy", required=true)  UUID createdBy
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="templateId", required=true)  UUID templateId
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="referenceKey", required=true)  String referenceKey
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="referenceKeyType", required=true)  String referenceKeyType
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="accountKey", required=true)  UUID accountKey
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="customerKey", required=true)  UUID customerKey
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="category", required=true)  String category
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="fileName", required=true)  String fileName
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="activeStartDate", required=true)  Long activeStartDate
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="activeEndDate", required=true)  Long activeEndDate
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="threadId", required=true)  UUID threadId
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="correlationId", required=true)  UUID correlationId
,@Parameter(description = "") @Valid @RequestPart(value="content", required=true) MultipartFile content
,@Parameter(in = ParameterIn.DEFAULT, description = "", required=true,schema=@Schema()) @RequestPart(value="metadata", required=true)  Metadata metadata
) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<InlineResponse200>(objectMapper.readValue("{\n  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"\n}", InlineResponse200.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<InlineResponse200>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<InlineResponse200>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Void> documentDelete(@Parameter(in = ParameterIn.HEADER, description = "Api version" ,required=true,schema=@Schema()) @RequestHeader(value="X-version", required=true) Integer xVersion
,@Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing" ,required=true,schema=@Schema()) @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId
,@Parameter(in = ParameterIn.HEADER, description = "Id of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId
,@Parameter(in = ParameterIn.HEADER, description = "Type of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestorType", required=true) XRequestorType xRequestorType
,@Size(max=500) @Parameter(in = ParameterIn.PATH, description = "", required=true, schema=@Schema()) @PathVariable("documentId") String documentId
) {
        String accept = request.getHeader("Accept");
        return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Resource> documentDownload(@Parameter(in = ParameterIn.HEADER, description = "Api version" ,required=true,schema=@Schema()) @RequestHeader(value="X-version", required=true) Integer xVersion
,@Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing" ,required=true,schema=@Schema()) @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId
,@Size(max=500) @Parameter(in = ParameterIn.PATH, description = "", required=true, schema=@Schema()) @PathVariable("documentId") String documentId
,@Parameter(in = ParameterIn.HEADER, description = "Type of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-type", required=true) XRequestorType xRequestorType
,@Parameter(in = ParameterIn.HEADER, description = "Id of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId
) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Resource>(objectMapper.readValue("\"\"", Resource.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Resource>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<Resource>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<DocumentDetailsNode> documentMetadata(@Parameter(in = ParameterIn.HEADER, description = "Api version" ,required=true,schema=@Schema()) @RequestHeader(value="X-version", required=true) Integer xVersion
,@Parameter(in = ParameterIn.HEADER, description = "Correlation ID for request tracing" ,required=true,schema=@Schema()) @RequestHeader(value="X-correlation-id", required=true) String xCorrelationId
,@Size(max=500) @Parameter(in = ParameterIn.PATH, description = "", required=true, schema=@Schema()) @PathVariable("documentId") String documentId
,@Parameter(in = ParameterIn.HEADER, description = "Type of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-type", required=true) XRequestorType xRequestorType
,@Parameter(in = ParameterIn.HEADER, description = "Id of the requestor" ,required=true,schema=@Schema()) @RequestHeader(value="X-requestor-id", required=true) UUID xRequestorId
,@Parameter(in = ParameterIn.QUERY, description = "Indicates if ECMS document download url to be included in response" ,schema=@Schema( defaultValue="false")) @Valid @RequestParam(value = "includeDownloadUrl", required = false, defaultValue="false") Boolean includeDownloadUrl
) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<DocumentDetailsNode>(objectMapper.readValue("{\n  \"lineOfBusiness\" : \"CREDIT_CARD\",\n  \"lastClientDownload\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",\n  \"metadata\" : [ {\n    \"key\" : \"accountId\",\n    \"value\" : \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n  }, {\n    \"key\" : \"issueDate\",\n    \"value\" : \"2025-01-01\"\n  }, {\n    \"key\" : \"customerId\",\n    \"value\" : \"3fa85f64-5717-4562-b3fc-2c963f66a78\"\n  }, {\n    \"key\" : \"htmlLettername\",\n    \"value\" : \"FF083\"\n  } ],\n  \"documentType\" : \"PaymentLetter\",\n  \"_links\" : {\n    \"download\" : {\n      \"responseTypes\" : [ \"application/pdf\", \"application/octet-stream\" ],\n      \"rel\" : \"download\",\n      \"href\" : \"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\n      \"type\" : \"GET\",\n      \"title\" : \"Download this document\"\n    },\n    \"delete\" : {\n      \"rel\" : \"delete\",\n      \"href\" : \"/documents/eyJ21pbWFnZVBh2dGgiOi4IiL\",\n      \"type\" : \"DELETE\",\n      \"title\" : \"Delete this document\"\n    }\n  },\n  \"displayName\" : \"2024_PRIVACY_POLICY\",\n  \"description\" : \"2024_PRIVACY_POLICY\",\n  \"mimeType\" : \"application/pdf\",\n  \"languageCode\" : \"EN_US\",\n  \"sizeInMb\" : 1,\n  \"documentId\" : \"documentId\",\n  \"category\" : \"PaymentConfirmationNotice\",\n  \"datePosted\" : 1740523843\n}", DocumentDetailsNode.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<DocumentDetailsNode>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<DocumentDetailsNode>(HttpStatus.NOT_IMPLEMENTED);
    }

}
