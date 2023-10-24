package io.nuvalence.ds4g.documentmanagement.service;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingRequest;
import io.nuvalence.ds4g.documentmanagement.service.model.DocumentMetadata;
import io.nuvalence.ds4g.documentmanagement.service.model.FileContent;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatus;
import io.nuvalence.ds4g.documentmanagement.service.model.ScanStatusResponse;
import io.nuvalence.ds4g.documentmanagement.service.service.DocumentProcessingService;
import io.nuvalence.ds4g.documentmanagement.service.storage.StorageProvider;
import kotlin.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * These tests use Spring's MockMVC.
 * Wherever possible, unit tests should not require any Spring initialization and
 * just use plain old Java. The tests run faster and have more focused scope.
 * Tests in this package are ones that require some
 * Spring initialization to work (for example, a class that returns a URL to
 * another Spring endpoint using {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}).
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = "ALLOWED_MIME_TYPES=image/png, image/jpeg")
@TestPropertySource(properties = "ALLOWED_OCTET_STREAM_EXTENSIONS=.mpa")
class DocumentManagementApplicationTests {
    @Autowired MockMvc mockMvc;

    @MockBean AuthorizationHandler authorizationHandler;

    @MockBean SecurityContext securityContext;

    @MockBean StorageProvider storage;

    @MockBean private DocumentProcessingService documentProcessingService;

    @BeforeEach
    void mockAuthorization() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication())
                .thenReturn(
                        new UserToken(
                                "USER-ID",
                                "01234567-89ab-cdef-0123-456789abcdef",
                                "TEST-TOKEN-STRING",
                                "8.8.8.8",
                                "agency",
                                Collections.emptyList()));
    }

    @Test
    void contextLoads() {
        Assertions.assertTrue(true);
    }

    @Test
    void realFileSmokeTest() throws Exception {

        // TODO: Refactor this test to use JUnit patterns.
        ClassPathResource resource = new ClassPathResource("drivers_license.jpg");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        resource.getInputStream());

        when(storage.upload(any(), any())).thenReturn(String.valueOf(UUID.randomUUID()));

        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setUploadedBy("USER-ID");
        metadata.setOriginalFilename(file.getOriginalFilename());
        when(storage.getMetadata(any())).thenReturn(Optional.of(metadata));
        ScanStatusResponse response = new ScanStatusResponse(ScanStatus.READY);

        // Upload
        MvcResult uploadResult =
                mockMvc.perform(multipart("/api/v1/documents").file(file))
                        .andExpect(status().isAccepted())
                        .andExpect(jsonPath("$.document_id", not(blankOrNullString())))
                        .andReturn();

        verify(storage, times(1)).upload(any(), any());

        String documentId =
                JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.document_id");

        when(storage.getStatus(documentId)).thenReturn(response.getScanStatus());
        // Get document metadata - this will be refactored later to be more comprehensive
        mockMvc.perform(get("/api/v1/documents/" + documentId)).andExpect(status().isOk());

        verify(storage, times(1)).getMetadata(any());

        MediaType mediaType =
                MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));

        FileContent fileContent = new FileContent(resource, mediaType);

        when(storage.getFileData(documentId)).thenReturn(fileContent);

        // Download
        mockMvc.perform(get("/api/v1/documents/" + documentId + "/file-data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(Objects.requireNonNull(file.getContentType())))
                .andExpect(content().bytes(file.getBytes()));

        verify(storage, times(1)).getFileData(documentId);
    }

    @Test
    void unsupportedMimeTypeThrowsError() throws Exception {
        ClassPathResource resource = new ClassPathResource("what_is_image_Processing.avif");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.ALL_VALUE,
                        resource.getInputStream());

        // Upload
        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error-code", equalTo("UNSUPPORTED_TYPE")))
                .andExpect(jsonPath("$.message", equalTo("File type not allowed.")))
                .andReturn();
    }

    @Test
    void unsupportedMimeTypeWithAllowedExtensionThrowsError() throws Exception {
        ClassPathResource resource = new ClassPathResource("sample_file.avif.txt");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.ALL_VALUE,
                        resource.getInputStream());

        // Upload
        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error-code", equalTo("UNSUPPORTED_TYPE")))
                .andExpect(jsonPath("$.message", equalTo("File type not allowed.")))
                .andReturn();
    }

    @Test
    void uploadAllowedDocumentWithoutExtension() throws Exception {
        ClassPathResource resource = new ClassPathResource("drivers_license");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        resource.getInputStream());
        when(storage.upload(any(), any())).thenReturn(String.valueOf(UUID.randomUUID()));
        // Upload
        mockMvc.perform(multipart("/api/v1/documents").file(file)).andExpect(status().isAccepted());
    }

    @Test
    void unsupportedOctetStreamThrowsError() throws Exception {
        ClassPathResource resource = new ClassPathResource("drivers_license.jpg");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        resource.getInputStream());

        // Upload
        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error-code", equalTo("UNSUPPORTED_TYPE")))
                .andExpect(jsonPath("$.message", equalTo("File type not allowed.")))
                .andReturn();
    }

    @Test
    void uploadDocumentsWithoutPermissions() throws Exception {
        ClassPathResource resource = new ClassPathResource("drivers_license.jpg");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        resource.getInputStream());

        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(false);

        // Upload
        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDocumentNotFound() throws Exception {
        String documentId = "01234567-89ab-cdef-0123-456789abcdef";

        when(storage.getMetadata(documentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/documents/" + documentId)).andExpect(status().isNotFound());
    }

    @Test
    void uploadDocumentsWithoutUserToken() throws Exception {
        ClassPathResource resource = new ClassPathResource("drivers_license.jpg");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "drivers_license.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        resource.getInputStream());

        when(SecurityContextHolder.getContext().getAuthentication())
                .thenReturn(mock(Authentication.class));

        mockMvc.perform(multipart("/api/v1/documents").file(file)).andExpect(status().isNotFound());
    }

    @Test
    void getFileDataWithNotFoundException() throws Exception {
        when(storage.getFileData(any())).thenThrow(new IOException());
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/file-data"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFileDataWithException() throws Exception {
        when(storage.getFileData(any())).thenThrow(new IOException());
        UUID documentId = UUID.randomUUID();
        when(storage.getMetadata(documentId.toString()))
                .thenReturn(Optional.of(new DocumentMetadata()));

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/file-data"))
                .andExpect(status().isInternalServerError());
    }

    Pair<String, List<DocumentProcessingRequest>>
            testEnqueueDocumentProcessingRequestStatusReadySetup() throws Exception {
        ScanStatusResponse scanStatusResponse = new ScanStatusResponse(ScanStatus.READY);
        when(storage.getStatus(anyString())).thenReturn(scanStatusResponse.getScanStatus());

        doNothing()
                .when(documentProcessingService)
                .enqueueDocumentProcessingRequest(anyString(), any());

        String documentId = UUID.randomUUID().toString();
        List<DocumentProcessingRequest> documentProcessingRequests = new ArrayList<>();
        DocumentProcessingRequest documentProcessingRequest = new DocumentProcessingRequest();
        documentProcessingRequest.setProcessorId("test-processor");
        documentProcessingRequests.add(documentProcessingRequest);

        doNothing()
                .when(documentProcessingService)
                .enqueueDocumentProcessingRequest(documentId, documentProcessingRequests);

        return new Pair<>(documentId, documentProcessingRequests);
    }

    @Test
    void testEnqueueDocumentProcessingRequest_StatusReady_reprocess() throws Exception {
        Pair<String, List<DocumentProcessingRequest>> setupResult =
                testEnqueueDocumentProcessingRequestStatusReadySetup();
        String documentId = setupResult.getFirst();
        List<DocumentProcessingRequest> documentProcessingRequests = setupResult.getSecond();

        this.mockMvc
                .perform(
                        post("/api/v1/documents/" + documentId + "/process?reprocess=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(documentProcessingRequests)))
                .andExpect(status().isOk());
        verify(documentProcessingService, never())
                .findByDocumentIdAndListOfProcessorIds(
                        eq(UUID.fromString(documentId)), any(ArrayList.class));

        verify(documentProcessingService, times(1))
                .enqueueDocumentProcessingRequest(documentId, documentProcessingRequests);
    }

    @Test
    void testEnqueueDocumentProcessingRequest_StatusReady_no_reprocess() throws Exception {
        Pair<String, List<DocumentProcessingRequest>> setupResult =
                testEnqueueDocumentProcessingRequestStatusReadySetup();
        String documentId = setupResult.getFirst();
        List<DocumentProcessingRequest> documentProcessingRequests = setupResult.getSecond();

        this.mockMvc
                .perform(
                        post("/api/v1/documents/" + documentId + "/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(documentProcessingRequests)))
                .andExpect(status().isOk());
        verify(documentProcessingService, times(1))
                .findByDocumentIdAndListOfProcessorIds(
                        eq(UUID.fromString(documentId)), any(ArrayList.class));

        verify(documentProcessingService, times(1))
                .enqueueDocumentProcessingRequest(documentId, documentProcessingRequests);
    }

    @Test
    void testEnqueueDocumentProcessingRequest_StatusFailedAvScan() throws Exception {
        ScanStatusResponse scanStatusResponse = new ScanStatusResponse(ScanStatus.FAILED_SCAN);
        when(storage.getStatus(anyString())).thenReturn(scanStatusResponse.getScanStatus());

        String documentId = UUID.randomUUID().toString();
        List<DocumentProcessingRequest> documentProcessingRequests = new ArrayList<>();
        documentProcessingRequests.add(new DocumentProcessingRequest());

        this.mockMvc
                .perform(
                        post("/api/v1/documents/" + documentId + "/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(documentProcessingRequests)))
                .andExpect(status().isGone());
    }

    @Test
    void testEnqueueDocumentProcessingRequest_noProcessingDataProvided() throws Exception {
        ScanStatusResponse scanStatusResponse = new ScanStatusResponse(ScanStatus.FAILED_SCAN);
        when(storage.getStatus(anyString())).thenReturn(scanStatusResponse.getScanStatus());

        String documentId = UUID.randomUUID().toString();
        List<DocumentProcessingRequest> documentProcessingRequests = new ArrayList<>();

        this.mockMvc
                .perform(
                        post("/api/v1/documents/" + documentId + "/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        new ObjectMapper()
                                                .writeValueAsString(documentProcessingRequests)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDocumentProcessingResults() throws Exception {

        String documentId = UUID.randomUUID().toString();
        DocumentMetadata dm = new DocumentMetadata();
        when(storage.getMetadata(documentId)).thenReturn(Optional.of(dm));

        when(authorizationHandler.isAllowedForInstance("view", dm)).thenReturn(true);

        List<DocumentProcessorResult> mockResultList =
                Arrays.asList(new DocumentProcessorResult(), new DocumentProcessorResult());
        when(documentProcessingService.getProcessingResultsForDocument(any(UUID.class)))
                .thenReturn(mockResultList);
        when(storage.getStatus(documentId)).thenReturn(ScanStatus.READY);

        this.mockMvc
                .perform(
                        get("/api/v1/documents/" + documentId + "/processing-result")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].processorId", is("antivirus-scanner")))
                .andExpect(jsonPath("$[2].status", is("COMPLETE")))
                .andExpect(jsonPath("$[2].result.code", is("READY")))
                .andExpect(jsonPath("$[2].result.status", is(200)));
    }

    @Test
    void testGetDocumentProcessingResultsPendingAvScan() throws Exception {

        String documentId = UUID.randomUUID().toString();
        DocumentMetadata dm = new DocumentMetadata();
        when(storage.getStatus(documentId)).thenReturn(ScanStatus.AWAITING_SCAN);
        when(storage.getUnscannedMetadata(documentId)).thenReturn(Optional.of(dm));

        when(authorizationHandler.isAllowedForInstance("view", dm)).thenReturn(true);

        List<DocumentProcessorResult> mockResultList =
                Arrays.asList(new DocumentProcessorResult(), new DocumentProcessorResult());
        when(documentProcessingService.getProcessingResultsForDocument(any(UUID.class)))
                .thenReturn(mockResultList);

        this.mockMvc
                .perform(
                        get("/api/v1/documents/" + documentId + "/processing-result")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].processorId", is("antivirus-scanner")))
                .andExpect(jsonPath("$[2].status", is("PENDING")))
                .andExpect(jsonPath("$[2].result.code", is("AWAITING_SCAN")))
                .andExpect(jsonPath("$[2].result.status", is(202)));
    }

    @Test
    void testGetDocumentProcessingResults_NoAccess() throws Exception {

        String documentId = UUID.randomUUID().toString();
        DocumentMetadata dm = new DocumentMetadata();
        when(storage.getStatus(documentId)).thenReturn(ScanStatus.READY);
        when(storage.getMetadata(documentId)).thenReturn(Optional.of(dm));

        when(authorizationHandler.isAllowedForInstance("view", dm)).thenReturn(false);

        this.mockMvc
                .perform(
                        get("/api/v1/documents/" + documentId + "/processing-result")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
