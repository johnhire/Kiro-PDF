package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentDetailBean}.
 *
 * <p>Covers:
 * <ul>
 *   <li>PDF bytes written to the response output stream</li>
 *   <li>Content-Type header set to {@code application/pdf}</li>
 *   <li>Content-Disposition attachment header set</li>
 *   <li>FacesContext.responseComplete() called after successful write</li>
 *   <li>FacesMessage set on WebApplicationException, ProcessingException, and IOException</li>
 * </ul>
 *
 * Requirement: 15.3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentDetailBeanTest {

    @Mock
    PdfManClient pdfManClient;

    @Mock
    FacesContext facesContext;

    @Mock
    ExternalContext externalContext;

    MockedStatic<FacesContext> facesContextStatic;

    DocumentDetailBean bean;

    @BeforeEach
    void setUp() throws Exception {
        facesContextStatic = mockStatic(FacesContext.class);
        facesContextStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        when(facesContext.getExternalContext()).thenReturn(externalContext);

        bean = new DocumentDetailBean();
        injectField(bean, "pdfManClient", pdfManClient);
    }

    @AfterEach
    void tearDown() {
        facesContextStatic.close();
    }

    // -------------------------------------------------------------------------
    // downloadPdf — happy path (Requirement 15.3)
    // -------------------------------------------------------------------------

    @Test
    void downloadPdf_writesPdfBytesToResponseOutputStream() throws IOException {
        // Arrange
        Long id = 1L;
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF magic bytes
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(pdfBytes);
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);

        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        when(externalContext.getResponseOutputStream()).thenReturn(capturedOutput);

        // Act
        bean.downloadPdf(id);

        // Assert — bytes written to stream
        assertThat(capturedOutput.toByteArray()).isEqualTo(pdfBytes);
    }

    @Test
    void downloadPdf_setsContentTypeApplicationPdf() throws IOException {
        // Arrange
        Long id = 2L;
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[]{1, 2, 3});
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);
        when(externalContext.getResponseOutputStream()).thenReturn(new ByteArrayOutputStream());

        // Act
        bean.downloadPdf(id);

        // Assert — Content-Type: application/pdf set on response
        verify(externalContext).setResponseContentType("application/pdf");
    }

    @Test
    void downloadPdf_setsContentDispositionAttachmentHeader() throws IOException {
        // Arrange
        Long id = 3L;
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[]{1});
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);
        when(externalContext.getResponseOutputStream()).thenReturn(new ByteArrayOutputStream());

        // Act
        bean.downloadPdf(id);

        // Assert — Content-Disposition attachment header set
        ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(externalContext).setResponseHeader(headerNameCaptor.capture(), headerValueCaptor.capture());
        assertThat(headerNameCaptor.getValue()).isEqualTo("Content-Disposition");
        assertThat(headerValueCaptor.getValue())
                .contains("attachment")
                .contains("document-" + id);
    }

    @Test
    void downloadPdf_callsResponseCompleteAfterSuccessfulWrite() throws IOException {
        // Arrange
        Long id = 4L;
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[]{1, 2});
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);
        when(externalContext.getResponseOutputStream()).thenReturn(new ByteArrayOutputStream());

        // Act
        bean.downloadPdf(id);

        // Assert — JSF view rendering skipped
        verify(facesContext).responseComplete();
    }

    @Test
    void downloadPdf_callsGetPdfOnClientWithCorrectId() throws IOException {
        // Arrange
        Long id = 42L;
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[]{});
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);
        when(externalContext.getResponseOutputStream()).thenReturn(new ByteArrayOutputStream());

        // Act
        bean.downloadPdf(id);

        // Assert
        verify(pdfManClient).getPdf(id);
    }

    // -------------------------------------------------------------------------
    // downloadPdf — error paths
    // -------------------------------------------------------------------------

    @Test
    void downloadPdf_onWebApplicationException_addsFacesMessageWithDownloadFailedSummary() {
        // Arrange
        Long id = 5L;
        when(pdfManClient.getPdf(id))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        // Act
        bean.downloadPdf(id);

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        FacesMessage msg = captor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Download failed");
    }

    @Test
    void downloadPdf_onProcessingException_addsFacesMessageWithDownloadFailedSummary() {
        // Arrange
        Long id = 6L;
        when(pdfManClient.getPdf(id))
                .thenThrow(new ProcessingException("Connection refused"));

        // Act
        bean.downloadPdf(id);

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        FacesMessage msg = captor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Download failed");
    }

    @Test
    void downloadPdf_onIOException_addsFacesMessageWithDownloadFailedSummary() throws IOException {
        // Arrange
        Long id = 7L;
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[]{1, 2, 3});
        when(pdfManClient.getPdf(id)).thenReturn(mockResponse);

        // Simulate OutputStream throwing IOException on write
        OutputStream failingStream = mock(OutputStream.class);
        doThrow(new IOException("Broken pipe")).when(failingStream).write(any(byte[].class));
        when(externalContext.getResponseOutputStream()).thenReturn(failingStream);

        // Act
        bean.downloadPdf(id);

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        FacesMessage msg = captor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Download failed");
    }

    @Test
    void downloadPdf_onError_doesNotCallResponseComplete() {
        // Arrange
        Long id = 8L;
        when(pdfManClient.getPdf(id))
                .thenThrow(new ProcessingException("Network error"));

        // Act
        bean.downloadPdf(id);

        // Assert — responseComplete must NOT be called when download fails
        verify(facesContext, never()).responseComplete();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
