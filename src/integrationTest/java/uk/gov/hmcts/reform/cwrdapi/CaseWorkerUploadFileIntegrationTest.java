package uk.gov.hmcts.reform.cwrdapi;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.poi.util.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.cwrdapi.controllers.response.CaseWorkerFileCreationResponse;
import uk.gov.hmcts.reform.cwrdapi.domain.CaseWorkerAudit;
import uk.gov.hmcts.reform.cwrdapi.domain.ExceptionCaseWorker;
import uk.gov.hmcts.reform.cwrdapi.util.AuthorizationEnabledIntegrationTest;
import uk.gov.hmcts.reform.cwrdapi.util.CaseWorkerConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.util.ResourceUtils.getFile;
import static uk.gov.hmcts.reform.cwrdapi.util.AuditStatus.FAILURE;
import static uk.gov.hmcts.reform.cwrdapi.util.AuditStatus.PARTIAL_SUCCESS;
import static uk.gov.hmcts.reform.cwrdapi.util.AuditStatus.SUCCESS;

public class CaseWorkerUploadFileIntegrationTest extends AuthorizationEnabledIntegrationTest {
    @Test
    public void shouldUploadCaseWorkerUsersXlsxFileSuccessfully() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserXlsxWithNoPassword.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "200 OK", cwdAdmin);
    }

    @Test
    public void shouldUploadCaseWorkerUsersXlsFileSuccessfully() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserXlsWithNoPassword.xls",
            CaseWorkerConstants.TYPE_XLSX, "200 OK", cwdAdmin);
    }

    @Test
    public void shouldUploadServiceRoleMappingsXlsxFileSuccessfully() throws IOException {

        String exceptedResponse = "{\"message\":\"Request Completed Successfully\","
            + "\"message_details\":\"4 record(s) uploaded\"}";
        Map<String, Object> response = uploadCaseWorkerFile("ServiceRoleMapping_BBA9.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "200 OK", cwdAdmin);

        //Audit & Exception for service Role Mapping
        String json = getJsonResponse(response);
        assertThat(objectMapper.readValue(json, CaseWorkerFileCreationResponse.class))
            .isEqualTo(objectMapper.readValue(exceptedResponse, CaseWorkerFileCreationResponse.class));
        List<CaseWorkerAudit> caseWorkerAudits = caseWorkerAuditRepository.findAll();
        assertThat(caseWorkerAudits.size()).isEqualTo(1);
        assertThat(caseWorkerAudits.get(0).getStatus()).isEqualTo(SUCCESS.getStatus());
        List<ExceptionCaseWorker> exceptionCaseWorkers = caseWorkerExceptionRepository.findAll();
        assertThat(exceptionCaseWorkers.size()).isEqualTo(0);
    }

    @Test
    public void shouldReturn400WhenFileFormatIsInvalid() throws IOException {
        uploadCaseWorkerFile("test.txt",
            CaseWorkerConstants.TYPE_XLSX, "400", cwdAdmin);
    }

    @Test
    public void shouldReturn400WhenXlsFileIsPasswordProtected() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserWithPassword.xls",
            CaseWorkerConstants.TYPE_XLS, "400", cwdAdmin);
    }

    @Test
    public void shouldReturn400WhenXlsxFileIsPasswordProtected() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserWithPassword.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "400", cwdAdmin);
    }

    @Test
    public void shouldReturn400WhenFileHasNoData() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserXlsxWithOnlyHeader.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "400", cwdAdmin);
    }

    @Test
    public void shouldReturn400WhenContentTypeIsInvalid() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserXlsxWithOnlyHeader.xlsx",
            "application/octet-stream", "400", cwdAdmin);
    }

    @Test
    public void shouldReturn403WhenRoleIsInvalid() throws IOException {
        uploadCaseWorkerFile("CaseWorkerUserXlsxWithOnlyHeader.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "403", "invalid");
    }

    @Test
    public void shouldReturn403WhenLdFeatureIsDisabled() throws IOException {
        Map<String, String> launchDarklyMap = new HashMap<>();
        launchDarklyMap.put("CaseWorkerRefController.caseWorkerFileUpload",
            "test-flag-1");
        when(featureToggleServiceImpl.isFlagEnabled(anyString(), anyString())).thenReturn(false);
        when(featureToggleServiceImpl.getLaunchDarklyMap()).thenReturn(launchDarklyMap);
        uploadCaseWorkerFile("CaseWorkerUserWithPassword.xlsx",
            CaseWorkerConstants.TYPE_XLSX, "403", cwdAdmin);
    }

    private Map<String, Object> uploadCaseWorkerFile(String fileName,
                                                     String fileType,
                                                     String status,
                                                     String role) throws IOException {

        response.clear();
        File file = getFile("src/integrationTest/resources/" + fileName);
        FileInputStream input = new FileInputStream(file);

        MockMultipartFile multipartFile = new MockMultipartFile("file",
            file.getName(), null, IOUtils.toByteArray(input));
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
            .builder("form-data")
            .name("file")
            .filename(fileName)
            .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        fileMap.add(HttpHeaders.CONTENT_TYPE, fileType);
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(multipartFile.getBytes(), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);

        response = caseworkerReferenceDataClient
            .uploadCwFile(body, role);

        assertThat(response).containsEntry("http_status", status);

        return response;
    }
}
