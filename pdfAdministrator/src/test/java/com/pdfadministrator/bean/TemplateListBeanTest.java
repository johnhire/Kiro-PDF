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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TemplateListBean}.
 *
 * <p>FacesContext is statically mocked so no JSF runtime is required.
 * PdfManClient is injected via reflection to avoid CDI container overhead.
 *
 * Requirements: 14.1, 14.4, 14.5
 */
@ExtendWith(MockitoExtension.class)
class TemplateListBeanTest {

    @Mock
    PdfManClient pdfManClient;

    @Mock
    FacesContext facesContext;

    MockedStatic<FacesContext> facesContextStatic;

    TemplateListBean bean;

    @BeforeEach
    void setUp() throws Exception {
        facesContextStatic = mockStatic(FacesContext.class);
        facesContextStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

        bean = new TemplateListBean();
        injectField(bean, "pdfManClient", pdfManClient);
    }

    @AfterEach
    void tearDown() {
        facesContextStatic.close();
    }

    // -------------------------------------------------------------------------
    // loadTemplates — happy path
    // -------------------------------------------------------------------------

    @Test
    void loadTemplates_callsListTemplatesOnClient() {
        // Arrange
        TemplateDto dto = sampleTemplate(1L, "invoice");
        when(pdfManClient.listTemplates()).thenReturn(List.of(dto));

        // Act
        bean.loadTemplates();

        // Assert
        verify(pdfManClient).listTemplates();
        assertThat(bean.getTemplates()).containsExactly(dto);
    }

    @Test
    void init_callsLoadTemplates() {
        // Arrange
        when(pdfManClient.listTemplates()).thenReturn(List.of());

        // Act — init() is called by @PostConstruct; invoke directly in unit test
        bean.init();

        // Assert
        verify(pdfManClient).listTemplates();
    }

    // -------------------------------------------------------------------------
    // loadTemplates — error paths (Requirements 14.4, 14.5)
    // -------------------------------------------------------------------------

    @Test
    void loadTemplates_onWebApplicationException_addsFacesMessageAndReturnsEmptyList() {
        // Arrange
        WebApplicationException ex = new WebApplicationException(
                Response.status(500).entity("{\"error\":\"server error\"}").build());
        when(pdfManClient.listTemplates()).thenThrow(ex);

        // Act
        bean.loadTemplates();

        // Assert — FacesMessage with SEVERITY_ERROR added
        ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), msgCaptor.capture());
        FacesMessage msg = msgCaptor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Error loading templates");

        // List must be empty, not null
        assertThat(bean.getTemplates()).isNotNull().isEmpty();
    }

    @Test
    void loadTemplates_onProcessingException_addsFacesMessageAndReturnsEmptyList() {
        // Arrange
        ProcessingException ex = new ProcessingException("Connection refused");
        when(pdfManClient.listTemplates()).thenThrow(ex);

        // Act
        bean.loadTemplates();

        // Assert
        ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), msgCaptor.capture());
        FacesMessage msg = msgCaptor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Error loading templates");
        assertThat(bean.getTemplates()).isNotNull().isEmpty();
    }

    // -------------------------------------------------------------------------
    // deleteTemplate — happy path
    // -------------------------------------------------------------------------

    @Test
    void deleteTemplate_callsDeleteOnClientAndRefreshesList() {
        // Arrange
        Long id = 42L;
        TemplateDto dto = sampleTemplate(id, "contract");
        when(pdfManClient.listTemplates()).thenReturn(List.of(dto));

        // Act
        bean.deleteTemplate(id);

        // Assert
        verify(pdfManClient).deleteTemplate(id);
        verify(pdfManClient).listTemplates(); // refresh
        assertThat(bean.getTemplates()).containsExactly(dto);
    }

    // -------------------------------------------------------------------------
    // deleteTemplate — error paths (Requirements 14.4, 14.5)
    // -------------------------------------------------------------------------

    @Test
    void deleteTemplate_onWebApplicationException_addsFacesMessageWithDeleteFailedSummary() {
        // Arrange
        Long id = 99L;
        WebApplicationException ex = new WebApplicationException(
                Response.status(409).entity("{\"error\":\"template in use\"}").build());
        doThrow(ex).when(pdfManClient).deleteTemplate(id);

        // Act
        bean.deleteTemplate(id);

        // Assert
        ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), msgCaptor.capture());
        FacesMessage msg = msgCaptor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Delete failed");
    }

    @Test
    void deleteTemplate_onProcessingException_addsFacesMessageWithServiceUnavailableSummary() {
        // Arrange
        Long id = 7L;
        ProcessingException ex = new ProcessingException("Timeout");
        doThrow(ex).when(pdfManClient).deleteTemplate(id);

        // Act
        bean.deleteTemplate(id);

        // Assert
        ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), msgCaptor.capture());
        FacesMessage msg = msgCaptor.getValue();
        assertThat(msg.getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        assertThat(msg.getSummary()).isEqualTo("Service unavailable");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TemplateDto sampleTemplate(Long id, String templateType) {
        return new TemplateDto(id, templateType, "<html/>",
                OffsetDateTime.now(), null, "system", null);
    }

    /** Injects a field by name using reflection (avoids CDI container). */
    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
