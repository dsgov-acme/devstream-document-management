package io.nuvalence.platform;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.nuvalence.platform.utils.cucumber.contexts.AuthorizationContext;
import io.nuvalence.platform.utils.cucumber.contexts.ScenarioContext;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Steps for the cucumber features surrounding out of the box user experience.
 */
public class UserExperienceStepDefinitions {
    private final HttpClient httpClient;
    private final ScenarioContext scenarioContext;
    private final AuthorizationContext authorizationContext;
    private URI endpoint;
    private HttpResponse lastApiResponse;
    private String documentId;

    public UserExperienceStepDefinitions(
            ScenarioContext scenarioContext, AuthorizationContext authorizationContext) {
        this.authorizationContext = authorizationContext;
        this.scenarioContext = scenarioContext;
        httpClient =
                HttpClientBuilder.create()
                        .addInterceptorFirst(scenarioContext::applyAuthorization)
                        .build();
    }

    @Given("^the document upload endpoint (.+)$")
    public void theDocumentUploadEndpoint(String path) throws URISyntaxException {
        this.endpoint = new URI(scenarioContext.getBaseUri() + path);
    }

    @Given("^the file (.+) has been uploaded to (.+)$")
    public void theDocumentHasBeenUploaded(String filename, String path)
            throws IOException, URISyntaxException {
        this.endpoint = new URI(scenarioContext.getBaseUri() + path);
        HttpResponse response = uploadDocument(filename);
        this.documentId =
                JsonPath.read(EntityUtils.toString(response.getEntity()), "$.document_id");
    }

    @When("^the file is downloaded$")
    public void theFileIsDownloaded() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            lastApiResponse = getFileData();
                            Assertions.assertEquals(
                                    200, lastApiResponse.getStatusLine().getStatusCode());
                        });
    }

    @When("^the file (.+) is uploaded$")
    public void uploadFile(String filename) throws IOException {
        this.lastApiResponse = uploadDocument(filename);
    }

    @When("the document metadata is retrieved")
    public void theDocumentMetadataIsRetrieved() {
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            this.lastApiResponse = getDocumentMetadata();
                            Assertions.assertEquals(
                                    200, lastApiResponse.getStatusLine().getStatusCode());
                        });
    }

    @When("^the octet stream file (.+) is uploaded$")
    public void uploadOctetFile(String filename) throws IOException {
        this.lastApiResponse = uploadOctetStreamFile(filename);
    }

    @When("^the unsupported mime type file (.+) is uploaded$")
    public void theUnsupportedFileDrivers_licensePngIsUploaded(String filename) throws IOException {
        this.lastApiResponse = uploadUnsupportedMimeType(filename);
    }

    @Then("^it should return (\\d+) and contain the JSON property (.+)$")
    public void itShouldReturn(int statusCode, String propertyName) throws IOException {
        Assertions.assertEquals(statusCode, lastApiResponse.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(lastApiResponse.getEntity());
        String value = JsonPath.read(content, "$." + propertyName);
        Assertions.assertNotNull(value);
    }

    @Then("^it should return (\\d+)$")
    public void itShouldReturn(int statusCode) {
        Assertions.assertEquals(statusCode, lastApiResponse.getStatusLine().getStatusCode());
    }

    @Then("^it should return (\\d+) and contain the JSON properties (.+)$")
    public void itShouldReturnJsonProperties(int statusCode, String properties) throws IOException {
        String[] jsonProperties = properties.replaceAll("\\s", "").split(",");

        Assertions.assertEquals(statusCode, lastApiResponse.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(lastApiResponse.getEntity());

        for (String propertyName : jsonProperties) {
            Object value = JsonPath.read(content, "$." + propertyName);
            Assertions.assertNotNull(value);
        }
    }

    @Then("^it should be identical to uploaded file$")
    public void theFileIsIdentical() throws IOException, URISyntaxException {
        String filename = "drivers_license.png";
        File uploadedFile =
                new File(
                        Thread.currentThread()
                                .getContextClassLoader()
                                .getResource(filename)
                                .toURI());
        File downloadedFile = Files.createTempFile("download", ".png").toFile();
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            lastApiResponse = getFileData();
                            try (InputStream inputStream =
                                    lastApiResponse.getEntity().getContent()) {
                                FileUtils.copyInputStreamToFile(inputStream, downloadedFile);
                            }

                            Assertions.assertTrue(
                                    FileUtils.contentEquals(downloadedFile, uploadedFile),
                                    String.format(
                                            "Downloaded file did not match uploaded %s file: %s",
                                            filename, downloadedFile.getAbsolutePath()));
                        });
    }

    private HttpResponse uploadDocument(String filename) throws IOException {
        try (InputStream filestream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            HttpEntity entity =
                    MultipartEntityBuilder.create()
                            .addBinaryBody("file", filestream, ContentType.IMAGE_PNG, filename)
                            .build();

            HttpPost request = new HttpPost(this.endpoint);
            request.setEntity(entity);
            request.addHeader("authorization", "Bearer " + authorizationContext.getToken());
            return httpClient.execute(request);
        }
    }

    private HttpResponse getFileData() throws URISyntaxException, IOException {
        URI uri = new URI(String.join("/", this.endpoint.toString(), documentId, "file-data"));
        HttpGet request = new HttpGet(uri);
        request.addHeader("authorization", "Bearer " + authorizationContext.getToken());
        return httpClient.execute(request);
    }

    private HttpResponse getDocumentMetadata() throws URISyntaxException, IOException {
        URI uri = new URI(String.join("/", this.endpoint.toString(), documentId));
        HttpGet request = new HttpGet(uri);
        request.addHeader("authorization", "Bearer " + authorizationContext.getToken());
        return httpClient.execute(request);
    }

    private HttpResponse uploadOctetStreamFile(String filename) throws IOException {
        try (InputStream filestream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            HttpEntity entity =
                    MultipartEntityBuilder.create()
                            .addBinaryBody(
                                    "file",
                                    filestream,
                                    ContentType.APPLICATION_OCTET_STREAM,
                                    filename)
                            .build();

            HttpPost request = new HttpPost(this.endpoint);
            request.setEntity(entity);
            request.addHeader("authorization", "Bearer " + authorizationContext.getToken());
            return httpClient.execute(request);
        }
    }

    private HttpResponse uploadUnsupportedMimeType(String filename) throws IOException {
        try (InputStream filestream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            HttpEntity entity =
                    MultipartEntityBuilder.create()
                            .addBinaryBody("file", filestream, ContentType.WILDCARD, filename)
                            .build();

            HttpPost request = new HttpPost(this.endpoint);
            request.setEntity(entity);
            request.addHeader("authorization", "Bearer " + authorizationContext.getToken());
            return httpClient.execute(request);
        }
    }

    @When("the file is downloaded it should return (\\d+)$")
    public void theFileIsDownloadedItShouldReturn(int statusCode)
            throws URISyntaxException, IOException {
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            this.lastApiResponse = getFileData();
                            Assertions.assertEquals(
                                    statusCode, lastApiResponse.getStatusLine().getStatusCode());
                        });
    }

    @When("the document metadata is retrieved it should return (\\d+)$")
    public void theDocumentMetadataIsRetrievedItShouldReturn(int statusCode)
            throws URISyntaxException, IOException {
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            this.lastApiResponse = getDocumentMetadata();
                            Assertions.assertEquals(
                                    statusCode, lastApiResponse.getStatusLine().getStatusCode());
                        });
    }
}
