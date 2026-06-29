package com.pdfman.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.pdfman.exception.InvalidPayloadException;
import jakarta.enterprise.context.ApplicationScoped;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Renders a PDF from a template string and a variable payload.
 *
 * <p>If the template content looks like HTML (starts with &lt; after trimming),
 * it is processed through Thymeleaf + OpenPDF HTMLWorker pipeline.
 * Otherwise, the payload is rendered as a simple text-based PDF document.
 */
@ApplicationScoped
public class PdfRenderingService {

    private final TemplateEngine templateEngine;

    public PdfRenderingService() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    /**
     * Renders a PDF from the given template string and payload.
     *
     * @param templateContent template string (HTML or JSON)
     * @param payload         variables to bind into the template
     * @return PDF bytes
     */
    public byte[] render(String templateContent, Map<String, Object> payload) {
        if (templateContent != null && templateContent.trim().startsWith("<")) {
            // HTML template — convert input elements to text, then render
            try {
                String processedHtml = convertInputsToText(templateContent);
                // Try Thymeleaf processing if it has th: attributes
                String html;
                if (processedHtml.contains("th:")) {
                    html = renderHtml(processedHtml, payload);
                } else {
                    html = processedHtml;
                }
                return convertHtmlToPdf(html);
            } catch (Exception e) {
                // If HTML rendering fails, fall back to text-based PDF
                return generateTextPdf(templateContent, payload);
            }
        } else {
            // Non-HTML (JSON template) — generate a simple text PDF from the payload
            return generateTextPdf(templateContent, payload);
        }
    }

    // --- private helpers ---

    /**
     * Converts HTML <input> elements to their value text for PDF rendering.
     * Also strips <style> blocks from captured innerHTML (CSS is in the wrapper).
     */
    private String convertInputsToText(String html) {
        // Remove <style>...</style> blocks (they'll be in the wrapper head)
        String result = html.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        // Replace <input type="text" value="xxx" /> with styled span
        result = result.replaceAll(
                "<input[^>]*value=\"([^\"]*)\"[^>]*/?>",
                "<span style=\"font-size:14px;color:#333;border-bottom:1px solid #999;padding:2px 4px;\">$1</span>"
        );
        // Remove img tags that reference local files (can't resolve in PDF)
        result = result.replaceAll("<img[^>]*src=\"images/[^\"]*\"[^>]*/?>", "");
        return result;
    }

    private byte[] generateTextPdf(String templateContent, Map<String, Object> payload) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Generated Document"));
            document.add(new Paragraph(" "));

            if (payload != null && !payload.isEmpty()) {
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    document.add(new Paragraph(entry.getKey() + ": " + value));
                }
            } else if (templateContent != null && !templateContent.isBlank()) {
                document.add(new Paragraph("Template:"));
                document.add(new Paragraph(templateContent));
            } else {
                document.add(new Paragraph("(No content)"));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new InvalidPayloadException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String renderHtml(String templateContent, Map<String, Object> payload) {
        Context context = new Context();
        if (payload != null) {
            context.setVariables(payload);
            // Also expose the full map as "payload" for generic templates that iterate over entries
            context.setVariable("payload", payload);
        }
        try {
            return templateEngine.process(templateContent, context);
        } catch (Exception e) {
            throw new InvalidPayloadException(
                    "Template rendering failed: " + e.getMessage());
        }
    }

    private byte[] convertHtmlToPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String xhtml = html;
            // If it's just a fragment (from captured innerHTML), wrap with full document + styles
            if (!xhtml.trim().toLowerCase().startsWith("<html")) {
                xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                      + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>"
                      + "body{font-family:Arial,sans-serif;margin:0;padding:0;color:#333}"
                      + ".banner{background-color:#1a1a2e;color:#fff;padding:8px 20px;font-size:11px}"
                      + ".header{background-color:#14558f;color:#fff;padding:15px 20px}"
                      + ".header h1{margin:0;font-size:18px;font-weight:normal}"
                      + ".header h2{margin:4px 0 0 0;font-size:11px;color:#388557;font-weight:bold;text-transform:uppercase}"
                      + ".title-bar{padding:15px 20px;border-bottom:2px solid #388557}"
                      + ".title-bar h3{margin:0;font-size:16px;color:#388557}"
                      + ".content{padding:20px}"
                      + ".form-field{margin-bottom:16px}"
                      + ".form-field label{display:block;font-weight:bold;color:#14558f;margin-bottom:4px;font-size:13px}"
                      + ".section{margin-top:20px;margin-bottom:10px;padding:15px;border:1px solid #e0e0e0;background-color:#f9f9f9;clear:both}"
                      + ".section-title{font-size:15px;font-weight:bold;color:#388557;margin-bottom:12px;padding-bottom:8px;border-bottom:1px solid #ddd}"
                      + ".section-fields{overflow:hidden}"
                      + ".section-fields .form-field{float:left;width:48%;margin-right:2%}"
                      + ".footer{background-color:#f0f0f0;border-top:1px solid #ccc;padding:12px 20px;font-size:10px;color:#555;margin-top:30px}"
                      + "span{font-size:14px;color:#333;border-bottom:1px solid #999;padding:2px 4px}"
                      + "</style></head><body>" + xhtml + "</body></html>";
            } else {
                // Full HTML document — ensure XML declaration for Flying Saucer
                if (!xhtml.trim().startsWith("<?xml")) {
                    xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xhtml;
                }
                // Add xmlns if missing
                if (!xhtml.contains("xmlns")) {
                    xhtml = xhtml.replace("<html", "<html xmlns=\"http://www.w3.org/1999/xhtml\"");
                }
            }

            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new InvalidPayloadException("Template rendered to an empty document");
            }
            return bytes;
        } catch (InvalidPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPayloadException("Failed to convert HTML to PDF: " + e.getMessage());
        }
    }
}
