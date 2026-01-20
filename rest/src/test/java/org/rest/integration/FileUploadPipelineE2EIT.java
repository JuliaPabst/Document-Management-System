package org.rest.integration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.dto.OcrResultDto;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import org.rest.service.FileStorage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Complete End-to-End Integration Test for File Upload Pipeline
 * 
 * This test validates the entire document processing workflow:
 * 1. File upload via REST API
 * 2. File storage in MinIO
 * 3. Message sent to OCR queue
 * 4. OCR worker processes the file (REAL Tesseract OCR when enabled, simulated otherwise)
 * 5. OCR result sent to GenAI queue
 * 6. GenAI processing (simulated)
 * 7. Final database update with summary
 * 
 * The OCR worker container is automatically built and started by Testcontainers when enabled.
 * 
 * USAGE:
 * - Default (with simulated OCR): ./mvnw test -Dtest=FileUploadPipelineE2EIT
 * - With real OCR worker:        ./mvnw test -Dtest=FileUploadPipelineE2EIT -Dtest.ocr.enabled=true
 * Note: First run with real OCR takes 2-3 minutes as it builds the Docker image from paperlessWorkers.
 *       Subsequent runs will reuse the built image (paperless-workers-test:latest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "openai.api.key=test-key",
        "openai.api.url=https://api.openai.com/v1/chat/completions",
        "openai.model=gpt-4",
        "rabbitmq.queue.ocr=ocr-worker-queue",
        "rabbitmq.queue.genai=genai-worker-queue",
        "rabbitmq.queue.genai.result=genai-result-queue"
})
@DisplayName("File Upload Pipeline E2E Integration Test")
class FileUploadPipelineE2EIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.ocr}")
    private String ocrQueue;

    @Value("${rabbitmq.queue.genai}")
    private String genaiQueue;

    @Value("${rabbitmq.queue.genai.result}")
    private String genaiResultQueue;

    @BeforeAll
    static void checkOcrEnabled() {
        boolean ocrEnabled = Boolean.parseBoolean(System.getProperty("test.ocr.enabled", "false"));
        System.out.println("=".repeat(80));
        if (!ocrEnabled) {
            System.out.println("ℹRunning with SIMULATED OCR (faster, no Docker build)");
            System.out.println("   To test with real Tesseract OCR, run:");
            System.out.println("   ./mvnw test -Dtest=FileUploadPipelineE2EIT -Dtest.ocr.enabled=true");
        } else {
            System.out.println("Running with REAL OCR WORKER");
            System.out.println("   Container will be built and started automatically by Testcontainers");
        }
        System.out.println("=".repeat(80));
    }

    @BeforeEach
    void setUp() throws InterruptedException {
        fileMetadataRepository.deleteAll();
        // Clean up queues before each test
        rabbitTemplate.receive(ocrQueue, 100);
        rabbitTemplate.receive(genaiQueue, 100);
        rabbitTemplate.receive(genaiResultQueue, 100);
        
        // Wait for OCR worker to be fully connected if OCR is enabled
        boolean ocrEnabled = Boolean.parseBoolean(System.getProperty("test.ocr.enabled", "false"));
        if (ocrEnabled) {
            System.out.println("⏳ Waiting for OCR worker to fully connect to RabbitMQ...");
            Thread.sleep(5000); // Give worker time to establish RabbitMQ listener
            System.out.println("✓ OCR worker should now be ready to consume messages");
        }
    }

    @Test
    @DisplayName("Should process complete file upload pipeline from upload to summary generation")
    void completeFileUploadPipeline() throws Exception {
        // Check if OCR is enabled
        boolean ocrEnabled = Boolean.parseBoolean(System.getProperty("test.ocr.enabled", "false"));
        
        // =====================================================================
        // STEP 1: Upload file through REST API
        // =====================================================================
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "ocr-test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                createSimplePdfContent() // Create a simple PDF with text
        );

        String authorName = "OCR Test Author";

        String responseBody = mockMvc.perform(multipart("/api/v1/files")
                        .file(pdfFile)
                        .param("author", authorName))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.filename").value("ocr-test-document.pdf"))
                .andExpect(jsonPath("$.author").value(authorName))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long documentId = extractDocumentIdFromResponse(responseBody);
        assertThat(documentId).isNotNull();

        FileMetadata uploadedMetadata = fileMetadataRepository.findById(documentId).orElseThrow();
        String objectKey = uploadedMetadata.getObjectKey();

        // Verify file was uploaded to MinIO
        assertThat(fileStorage.exists(objectKey)).isTrue();

        // =====================================================================
        // STEP 2: File upload complete - OCR worker will process
        // =====================================================================
        // Note: We do NOT consume from the OCR queue here, as that would steal
        // the message from the OCR worker. The worker needs to process it.
        
        System.out.println("✓ File uploaded successfully, OCR worker will process it");

        // =====================================================================
        // STEP 3: Wait for complete pipeline processing
        // =====================================================================
        if (ocrEnabled) {
            System.out.println("Waiting for real OCR worker pipeline to complete...");
            System.out.println("  Pipeline: OCR Worker → GenAI Worker → Database Update");
            
            // Wait for the entire pipeline to complete and database to be updated
            // The OCR worker will:
            // 1. Receive message from ocr-worker-queue
            // 2. Download file from MinIO
            // 3. Extract text using Tesseract OCR
            // 4. Send OcrResultDto to genai-worker-queue
            // 
            // The GenAI worker will:
            // 1. Receive OcrResultDto from genai-worker-queue
            // 2. Generate summary (or use fallback)
            // 3. Send GenAiResultDto to genai-result-queue
            //
            // The REST service listener will:
            // 1. Receive GenAiResultDto from genai-result-queue
            // 2. Update database with summary
            
            Awaitility.await()
                    .atMost(90, TimeUnit.SECONDS)  // Allow time for real OCR + GenAI processing
                    .pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        FileMetadata updatedMetadata = fileMetadataRepository.findById(documentId).orElseThrow();
                        assertThat(updatedMetadata.getSummary())
                                .as("Summary should be set after pipeline completes")
                                .isNotNull();
                    });
            
            FileMetadata finalMetadata = fileMetadataRepository.findById(documentId).orElseThrow();
            System.out.println("✓ Real OCR worker pipeline completed successfully!");
            System.out.println("  Summary: " + finalMetadata.getSummary().substring(0, 
                    Math.min(100, finalMetadata.getSummary().length())) + "...");

        } else {
            // =====================================================================
            // STEP 3: Simulate OCR worker processing when real OCR is disabled
            // =====================================================================
            System.out.println("ℹ Using simulated OCR processing (real OCR disabled)");
            
            String simulatedExtractedText = "This is extracted text from the PDF document. " +
                    "OCR processing completed successfully in test mode.";
            
            // Simulate OCR worker sending result to GenAI queue
            OcrResultDto ocrResult = new OcrResultDto(
                    documentId,
                    objectKey,
                    "test-documents",
                    simulatedExtractedText,
                    java.time.LocalDateTime.now(),
                    "Tesseract OCR v5.13.0 (Simulated)"
            );
            rabbitTemplate.convertAndSend(genaiQueue, ocrResult);
            System.out.println("✓ Simulated OCR result sent to GenAI queue");
            
            // Simulate GenAI worker sending result to result queue
            String simulatedSummary = "Document processed with simulated OCR. Test completed successfully.";
            org.rest.dto.GenAiResultDto genaiResult = new org.rest.dto.GenAiResultDto(
                    documentId,
                    objectKey,
                    simulatedExtractedText,
                    simulatedSummary,
                    java.time.LocalDateTime.now()
            );
            rabbitTemplate.convertAndSend(genaiResultQueue, genaiResult);
            System.out.println("✓ Simulated GenAI result sent to result queue");
            
            // Wait for database update
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        FileMetadata updatedMetadata = fileMetadataRepository.findById(documentId).orElseThrow();
                        assertThat(updatedMetadata.getSummary()).isNotNull();
                    });
            
            System.out.println("✓ Simulated pipeline completed successfully");
        }

        // =====================================================================
        // Final verification
        // =====================================================================
        mockMvc.perform(get("/api/v1/files/{id}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.filename").value("ocr-test-document.pdf"));

        System.out.println("=".repeat(70));
        System.out.println("FILE UPLOAD PIPELINE TEST WITH " + 
                (ocrEnabled ? "REAL OCR" : "SIMULATED OCR") + " COMPLETED!");
        System.out.println("=".repeat(70));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Creates a simple PDF content for OCR testing
     * Note: This is a minimal PDF - for real OCR testing, you'd want actual text
     */
    private byte[] createSimplePdfContent() {
        // This creates a very basic PDF with text that Tesseract can read
        // For a real test, you'd want to include an actual PDF file with text
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources 4 0 R /MediaBox [0 0 612 792] /Contents 5 0 R >>\nendobj\n" +
                "4 0 obj\n<< /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >>\nendobj\n" +
                "5 0 obj\n<< /Length 44 >>\nstream\n" +
                "BT /F1 12 Tf 100 700 Td (Hello OCR World!) Tj ET\n" +
                "endstream\nendobj\n" +
                "xref\n0 6\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000214 00000 n\n" +
                "0000000304 00000 n\n" +
                "trailer\n<< /Size 6 /Root 1 0 R >>\n" +
                "startxref\n397\n" +
                "%%EOF";
        return pdfContent.getBytes();
    }

    private Long extractDocumentIdFromResponse(String jsonResponse) {
        try {
            String idPattern = "\"id\":";
            int idStart = jsonResponse.indexOf(idPattern) + idPattern.length();
            int idEnd = jsonResponse.indexOf(",", idStart);
            if (idEnd == -1) {
                idEnd = jsonResponse.indexOf("}", idStart);
            }
            String idStr = jsonResponse.substring(idStart, idEnd).trim();
            return Long.parseLong(idStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract document ID from response: " + jsonResponse, e);
        }
    }

    private <T> T awaitMessageFromQueue(String queueName, Class<T> messageType, int timeoutSeconds) {
        return Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Object message = rabbitTemplate.receiveAndConvert(queueName, 100);
                    return message != null ? messageType.cast(message) : null;
                }, msg -> msg != null);
    }
}
