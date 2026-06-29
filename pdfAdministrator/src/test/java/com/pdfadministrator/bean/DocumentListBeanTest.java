package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import com.pdfadministrator.dto.DocumentDto;
import jakarta.faces.application.FacesMessage;
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

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentListBean}.
 *
 * <p>Covers:
 * <ul>
 *   <li>listDocuments() called on load (init / loadDocuments)</li>
 *   <li>Connectivity error message set when ProcessingException thrown (Requirement 15.4)</li>
 *   <li>Error message set on WebApplicationException</li>
 *   <li>Page never left blank — documents list is always non-null</li>
 * </ul>
 *
 * Requirements: 15.1, 15.4
 */
@ExtendWith(MockitoExtension.class)
class DocumentListBeanTest {

    @Mock
    PdfManClient pdfManClient;

    @Mock
    FacesContext facesContext;

    MockedStatic<FacesContext> facesContextStatic;

    DocumentListBean bean;

    @BeforeEach
    void setUp() throws Exception {
        facesContextStatic = mockStatic(FacesContext.class);
        facesContextStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

        bean = new DocumentListBean();
        injectField(bean, "pdfManClient", pdfManClient);
    }

    @AfterEach
    void tearDown() {
        facesContextStatic.close();
    }

    // -------------------------------------------------------------------------
    // loadDocuments — happy path (Requirement 15.1)
    // -------------------------------------------------------------------------

    @Test
    void loadDocuments_callsListDocumentsOnClient() {
        // Arrange
        DocumentDto doc = sampleDocument(1L, 2L);
        when(pdfManClient.listDocuments()).thenReturn(List.of(doc));

        // Act
        bean.loadDocuments();

        // Assert
        verify(pdfManClient).listDocuments();
        assertThat(bean.getDocuments()).containsExactly(doc);
    }

    @Test
    void init_callsLoadDocuments() {
        // Arrange
        when(pdfManClient.listDocuments()).thenReturn(List.of());

        // Act — @PostConstruct; invoke directly in unit test
        bean.init();

        // Assert
        verify(pdfManClient).listDocuments();
    }

    @Test
    void loadDocuments_returnsAllDocumentsFromClient() {
        // Arrange
        List<DocumentDto> docs = List.of(
                sampleDocument(1L, 10L),
                sampleDocument(2L, 10L),
                sampleDocument(3L, 11L));
        when(pdfManClient.listDocuments()).thenReturn(docs);

        // Act
        bean.loadDocuments();

        // Assert
        assertThat(bean.getDocuments()).hasSize(3);
        assertThat(bean.getDocuments()).extracting(DocumentDto::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    // -------------------------------------------------------------------------
    // loadDocuments — connectivity error (Requirement 15.4)
    // -------------------------------------------------------------------------

    @Test
    void loadDocuments_onProcessingException_addsConnectivityErrorMessage() {
        // Arrange — simulate pdfMan service unreachable
        ProcessingException ex = new ProcessingException("Connection refused: pdfMan");
        when(pdfManClient.listDocuments()).thenThrow(ex);

        // Act
        bean.loadDocuments();

        // Assert — connectivity-specific error message set
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        FacesMessage msg = captor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary())
                .as("Connectivity error summary should mention service unavailability")
                .contains("unavailable");
    }

    @Test
    void loadDocuments_onProcessingException_documentsListIsEmptyNotNull() {
        // Arrange
        when(pdfManClient.listDocuments()).thenThrow(new ProcessingException("Timeout"));

        // Act
        bean.loadDocuments();

        // Assert — page must never be left blank (Requirement 15.4)
        assertThat(bean.getDocuments())
                .as("Documents list must be non-null even on connectivity failure")
                .isNotNull()
                .isEmpty();
    }

    // -------------------------------------------------------------------------
    // loadDocuments — HTTP error path
    // -------------------------------------------------------------------------

    @Test
    void loadDocuments_onWebApplicationException_addsErrorMessageAndReturnsEmptyList() {
        // Arrange
        WebApplicationException ex = new WebApplicationException(
                Response.status(503).entity("{\"error\":\"service unavailable\"}").build());
        when(pdfManClient.listDocuments()).thenThrow(ex);

        // Act
        bean.loadDocuments();

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        FacesMessage msg = captor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Error loading documents");
        assertThat(bean.getDocuments()).isNotNull().isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DocumentDto sampleDocument(Long id, Long templateId) {
        return new DocumentDto(id, templateId, "doc-" + id, null, "pdfs/" + id + ".pdf",
                OffsetDateTime.now(), null, "system", null);
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
