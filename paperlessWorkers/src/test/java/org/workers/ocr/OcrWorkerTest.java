package org.workers.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.workers.dto.FileMessageDto;
import org.workers.dto.OcrResultDto;
import org.workers.service.FileStorage;
import org.workers.service.TesseractOcrService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrWorkerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private TesseractOcrService tesseractOcrService;

    @InjectMocks
    private OcrWorker ocrWorker;

    private static final String GENAI_QUEUE = "genai-worker-queue";
    private static final String BUCKET_NAME = "test-bucket";
    private static final String TESSERACT_VERSION = "Tesseract OCR v5.13.0 + Ghostscript";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ocrWorker, "genAiQueueName", GENAI_QUEUE);
        ReflectionTestUtils.setField(ocrWorker, "bucketName", BUCKET_NAME);
    }

    @Test
    void testProcessOcrTask_WithPdfFile_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(1L, "document.pdf", "PDF", "documents/doc1.pdf");
        byte[] fileContent = "PDF content bytes".getBytes();
        String extractedText = "This is extracted text from PDF";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromPdf(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("documents/doc1.pdf");
        verify(tesseractOcrService).extractTextFromPdf(fileContent, "document.pdf");

        //catch the argument sent to RabbitMQ
        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(1L, result.getDocumentId());
        assertEquals("documents/doc1.pdf", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithPngImage_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(2L, "scan.png", "PNG", "images/scan1.png");
        byte[] fileContent = "PNG image bytes".getBytes();
        String extractedText = "Text from PNG image";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("images/scan1.png");
        verify(tesseractOcrService).extractTextFromImage(fileContent, "scan.png");

        //catch the argument sent to RabbitMQ
        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(2L, result.getDocumentId());
        assertEquals("images/scan1.png", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithJpgImage_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(3L, "photo.jpg", "JPG", "images/photo.jpg");
        byte[] fileContent = "JPG image bytes".getBytes();
        String extractedText = "Text from JPG image";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("images/photo.jpg");
        verify(tesseractOcrService).extractTextFromImage(fileContent, "photo.jpg");

        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(3L, result.getDocumentId());
        assertEquals("images/photo.jpg", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithJpegImage_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(4L, "scan.jpeg", "JPEG", "images/scan.jpeg");
        byte[] fileContent = "JPEG image bytes".getBytes();
        String extractedText = "Text from JPEG image";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("images/scan.jpeg");
        verify(tesseractOcrService).extractTextFromImage(fileContent, "scan.jpeg");

        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(4L, result.getDocumentId());
        assertEquals("images/scan.jpeg", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithTiffImage_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(5L, "document.tiff", "TIFF", "images/doc.tiff");
        byte[] fileContent = "TIFF image bytes".getBytes();
        String extractedText = "Text from TIFF image";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("images/doc.tiff");
        verify(tesseractOcrService).extractTextFromImage(fileContent, "document.tiff");

        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(5L, result.getDocumentId());
        assertEquals("images/doc.tiff", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithBmpImage_Success() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(6L, "scan.bmp", "BMP", "images/scan.bmp");
        byte[] fileContent = "BMP image bytes".getBytes();
        String extractedText = "Text from BMP image";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("images/scan.bmp");
        verify(tesseractOcrService).extractTextFromImage(fileContent, "scan.bmp");

        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertEquals(6L, result.getDocumentId());
        assertEquals("images/scan.bmp", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    void testProcessOcrTask_WithUnsupportedFileType_ReturnsErrorMessage() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(7L, "document.txt", "TXT", "docs/doc.txt");
        byte[] fileContent = "Text file content".getBytes();

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("docs/doc.txt");
        verify(tesseractOcrService, never()).extractTextFromPdf(any(), any());
        verify(tesseractOcrService, never()).extractTextFromImage(any(), any());

        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertTrue(result.getExtractedText().contains("OCR not supported for file type"));
    }

    @Test
    void testProcessOcrTask_WithFileStorageException_LogsError() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(8L, "document.pdf", "PDF", "docs/missing.pdf");

        when(fileStorage.download(message.getObjectKey()))
                .thenThrow(new RuntimeException("File not found in MinIO"));

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(fileStorage).download("docs/missing.pdf");
        verify(tesseractOcrService, never()).extractTextFromPdf(any(), any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testProcessOcrTask_WithCaseInsensitiveFileTypes() throws Exception {
        // Test lowercase 'pdf'
        FileMessageDto messageLowercase = createFileMessage(10L, "test.pdf", "pdf", "docs/test.pdf");
        byte[] fileContent = "PDF content".getBytes();

        when(fileStorage.download(messageLowercase.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromPdf(fileContent, messageLowercase.getFilename()))
                .thenReturn("Extracted text");
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        ocrWorker.processOcrTask(messageLowercase);

        verify(tesseractOcrService).extractTextFromPdf(fileContent, "test.pdf");
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), any(OcrResultDto.class));
    }

    @Test
    void testProcessOcrTask_VerifiesMessageSentToCorrectQueue() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(11L, "test.png", "PNG", "images/test.png");
        byte[] fileContent = "Image bytes".getBytes();
        String extractedText = "Test text";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromImage(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), any(OcrResultDto.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testProcessOcrTask_VerifiesAllFieldsInOcrResultDto() throws Exception {
        // Arrange
        FileMessageDto message = createFileMessage(13L, "complete.pdf", "PDF", "docs/complete.pdf");
        byte[] fileContent = "PDF content".getBytes();
        String extractedText = "Complete extracted text";

        when(fileStorage.download(message.getObjectKey())).thenReturn(fileContent);
        when(tesseractOcrService.extractTextFromPdf(fileContent, message.getFilename()))
                .thenReturn(extractedText);
        when(tesseractOcrService.getVersion()).thenReturn(TESSERACT_VERSION);

        // Act
        ocrWorker.processOcrTask(message);

        // Assert
        ArgumentCaptor<OcrResultDto> resultCaptor = ArgumentCaptor.forClass(OcrResultDto.class);
        verify(rabbitTemplate).convertAndSend(eq(GENAI_QUEUE), resultCaptor.capture());

        OcrResultDto result = resultCaptor.getValue();
        assertNotNull(result);
        assertNotNull(result.getDocumentId());
        assertNotNull(result.getObjectKey());
        assertNotNull(result.getBucketName());
        assertNotNull(result.getExtractedText());
        assertNotNull(result.getProcessedAt());
        assertNotNull(result.getOcrEngine());

        assertEquals(13L, result.getDocumentId());
        assertEquals("docs/complete.pdf", result.getObjectKey());
        assertEquals(BUCKET_NAME, result.getBucketName());
        assertEquals(extractedText, result.getExtractedText());
        assertEquals(TESSERACT_VERSION, result.getOcrEngine());
    }

    private FileMessageDto createFileMessage(Long id, String filename, String fileType, String objectKey) {
        return new FileMessageDto(
                id,
                filename,
                "test-author",
                fileType,
                1024L,
                Instant.now(),
                objectKey
        );
    }
}
