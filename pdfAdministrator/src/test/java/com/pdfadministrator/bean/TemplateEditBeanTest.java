package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import com.pdfadministrator.dto.TemplateDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TemplateEditBean}.
 *
 * <p>Covers:
 * <ul>
 *   <li>POST (createTemplate) called when templateId is null — new template mode</li>
 *   <li>PUT (updateTemplate) called when templateId is set — edit mode</li>
 *   <li>FacesMessage set on WebApplicationException and ProcessingException for both paths</li>
 *   <li>loadTemplate populates form fields from the client response</li>
 * </ul>
 *
 * Requirements: 14.2, 14.3, 14.5
 */
@ExtendWith(MockitoExtension.class)
class TemplateEditBeanTest {

    @Mock
    PdfManClient pdfManClient;

    @Mock
    FacesContext facesContext;

    MockedStatic<FacesContext> facesContextStatic;

    TemplateEditBean bean;

    @BeforeEach
    void setUp() throws Exception {
        facesContextStatic = mockStatic(FacesContext.class);
        facesContextStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

        bean = new TemplateEditBean();
        injectField(bean, "pdfManClient", pdfManClient);
    }

    @AfterEach
    void tearDown() {
        facesContextStatic.close();
    }

    // -------------------------------------------------------------------------
    // loadTemplate
    // -------------------------------------------------------------------------

    @Test
    void loadTemplate_populatesFormFieldsFromClientResponse() {
        // Arrange
        Long id = 5L;
        TemplateDto dto = sampleTemplate(id, "invoice", "<html>Invoice</html>");
        when(pdfManClient.getTemplate(id)).thenReturn(dto);

        // Act
        bean.loadTemplate(id);

        // Assert
        assertThat(bean.getTemplateId()).isEqualTo(id);
        assertThat(bean.getTemplateType()).isEqualTo("invoice");
        assertThat(bean.getContent()).isEqualTo("<html>Invoice</html>");
        assertThat(bean.getTemplate()).isEqualTo(dto);
    }

    @Test
    void loadTemplate_onWebApplicationException_addsFacesMessage() {
        // Arrange
        Long id = 99L;
        when(pdfManClient.getTemplate(id))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        // Act
        bean.loadTemplate(id);

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(captor.getValue().getSummary()).isEqualTo("Error loading template");
    }

    @Test
    void loadTemplate_onProcessingException_addsFacesMessage() {
        // Arrange
        Long id = 3L;
        when(pdfManClient.getTemplate(id))
                .thenThrow(new ProcessingException("Connection refused"));

        // Act
        bean.loadTemplate(id);

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
    }

    // -------------------------------------------------------------------------
    // save — new template (POST) — Requirement 14.2
    // -------------------------------------------------------------------------

    @Test
    void save_whenTemplateIdIsNull_callsCreateTemplate() {
        // Arrange — templateId is null → new template mode
        bean.setTemplateType("report");
        bean.setContent("<html>Report</html>");
        TemplateDto created = sampleTemplate(10L, "report", "<html>Report</html>");
        when(pdfManClient.createTemplate("report", null, "<html>Report</html>")).thenReturn(created);

        // Act
        bean.save();

        // Assert — POST path invoked
        verify(pdfManClient).createTemplate("report", null, "<html>Report</html>");
        verify(pdfManClient, never()).updateTemplate(anyLong(), any(), anyString());

        // Success FacesMessage added
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        assertThat(captor.getValue().getSummary()).isEqualTo("Template created");

        // Form fields cleared after successful create
        assertThat(bean.getTemplateType()).isNull();
        assertThat(bean.getContent()).isNull();
    }

    @Test
    void save_newTemplate_onWebApplicationException_addsFacesMessageWithSaveFailedSummary() {
        // Arrange
        bean.setTemplateType("bad");
        bean.setContent("content");
        when(pdfManClient.createTemplate(anyString(), any(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(409).build()));

        // Act
        bean.save();

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(captor.getValue().getSummary()).isEqualTo("Save failed");
    }

    @Test
    void save_newTemplate_onProcessingException_addsFacesMessageWithServiceUnavailableSummary() {
        // Arrange
        bean.setTemplateType("x");
        bean.setContent("y");
        when(pdfManClient.createTemplate(anyString(), any(), anyString()))
                .thenThrow(new ProcessingException("Timeout"));

        // Act
        bean.save();

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(captor.getValue().getSummary()).isEqualTo("Service unavailable");
    }

    // -------------------------------------------------------------------------
    // save — existing template (PUT) — Requirement 14.3
    // -------------------------------------------------------------------------

    @Test
    void save_whenTemplateIdIsSet_callsUpdateTemplate() {
        // Arrange — templateId set → edit mode
        Long id = 7L;
        bean.setTemplateId(id);
        bean.setContent("<html>Updated</html>");
        TemplateDto updated = sampleTemplate(id, "invoice", "<html>Updated</html>");
        when(pdfManClient.updateTemplate(id, null, "<html>Updated</html>")).thenReturn(updated);

        // Act
        bean.save();

        // Assert — PUT path invoked
        verify(pdfManClient).updateTemplate(id, null, "<html>Updated</html>");
        verify(pdfManClient, never()).createTemplate(anyString(), any(), anyString());

        // Success FacesMessage added
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        assertThat(captor.getValue().getSummary()).isEqualTo("Template updated");
    }

    @Test
    void save_existingTemplate_onWebApplicationException_addsFacesMessageWithSaveFailedSummary() {
        // Arrange
        bean.setTemplateId(3L);
        bean.setContent("new content");
        when(pdfManClient.updateTemplate(anyLong(), any(), anyString()))
                .thenThrow(new WebApplicationException(Response.status(404).build()));

        // Act
        bean.save();

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(captor.getValue().getSummary()).isEqualTo("Save failed");
    }

    @Test
    void save_existingTemplate_onProcessingException_addsFacesMessageWithServiceUnavailableSummary() {
        // Arrange
        bean.setTemplateId(4L);
        bean.setContent("content");
        when(pdfManClient.updateTemplate(anyLong(), any(), anyString()))
                .thenThrow(new ProcessingException("Network error"));

        // Act
        bean.save();

        // Assert
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(captor.getValue().getSummary()).isEqualTo("Service unavailable");
    }

    // -------------------------------------------------------------------------
    // isEditMode
    // -------------------------------------------------------------------------

    @Test
    void isEditMode_returnsFalseWhenTemplateIdIsNull() {
        assertThat(bean.isEditMode()).isFalse();
    }

    @Test
    void isEditMode_returnsTrueWhenTemplateIdIsSet() {
        bean.setTemplateId(1L);
        assertThat(bean.isEditMode()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TemplateDto sampleTemplate(Long id, String templateType, String content) {
        return new TemplateDto(id, templateType, content,
                OffsetDateTime.now(), null, "system", null);
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
