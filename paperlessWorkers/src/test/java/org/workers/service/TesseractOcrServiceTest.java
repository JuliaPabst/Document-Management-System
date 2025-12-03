package org.workers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TesseractOcrService
 *
 * These tests focus on error handling and input validation.
 * Testing actual OCR functionality requires a properly configured
 * Tesseract environment with tessdata, which may not be available
 * in all test environments.
 */

class TesseractOcrServiceTest {

    private TesseractOcrService tesseractOcrService;

    @BeforeEach
    void setUp() {
        tesseractOcrService = new TesseractOcrService();
    }

    @Test
    void testGetVersion() {
        // Act
        String version = tesseractOcrService.getVersion();

        // Assert
        assertNotNull(version);
        assertEquals("Tesseract OCR v5.13.0 + Ghostscript", version);
    }

    @Test
    void testExtractTextFromImage_WithInvalidImageData() {
        // Arrange - Invalid image bytes
        byte[] invalidBytes = "This is not an image".getBytes();

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromImage(invalidBytes, "invalid.png");
        });

        assertTrue(exception.getMessage().contains("Failed to read image"),
                "Should throw IOException with 'Failed to read image' message");
    }

    @Test
    void testExtractTextFromImage_WithEmptyBytes() {
        // Arrange
        byte[] emptyBytes = new byte[0];

        // Act & Assert
        assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromImage(emptyBytes, "empty.png");
        }, "Should throw IOException for empty byte array");
    }

    @Test
    void testExtractTextFromImage_WithNullBytes() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            tesseractOcrService.extractTextFromImage(null, "null.png");
        }, "Should throw NullPointerException for null input");
    }

    @Test
    void testExtractTextFromImage_WithNullFilename() {
        // Arrange
        byte[] validBytes = new byte[]{0, 1, 2};

        // Act - filename is only used for logging, so it shouldn't cause an exception
        // The invalid image bytes will cause the IOException
        assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromImage(validBytes, null);
        });
    }

    @Test
    void testExtractTextFromPdf_WithInvalidPdfData() {
        // Arrange - Invalid PDF bytes
        byte[] invalidBytes = "This is not a PDF".getBytes();

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromPdf(invalidBytes, "invalid.pdf");
        });

        assertTrue(exception.getMessage().contains("PDF OCR processing failed") ||
                   exception.getMessage().contains("Ghostscript"),
                "Should throw IOException mentioning PDF processing or Ghostscript");
    }

    @Test
    void testExtractTextFromPdf_WithEmptyBytes() {
        // Arrange
        byte[] emptyBytes = new byte[0];

        // Act & Assert
        assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromPdf(emptyBytes, "empty.pdf");
        }, "Should throw IOException for empty PDF bytes");
    }

    @Test
    void testExtractTextFromPdf_WithNullBytes() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            tesseractOcrService.extractTextFromPdf(null, "null.pdf");
        }, "Should throw NullPointerException for null PDF input");
    }

    @Test
    void testExtractTextFromPdf_WithCorruptedPdfHeader() {
        // Arrange - Bytes that start like a PDF but are corrupted
        byte[] corruptedPdf = "%PDF-1.4\nGARBAGE".getBytes();

        // Act & Assert
        assertThrows(IOException.class, () -> {
            tesseractOcrService.extractTextFromPdf(corruptedPdf, "corrupted.pdf");
        }, "Should throw IOException for corrupted PDF");
    }

    @Test
    void testServiceInitialization() {
        // Assert that service was created successfully
        assertNotNull(tesseractOcrService, "Service should be initialized");

        // Verify version is available (indicates service initialized correctly)
        String version = tesseractOcrService.getVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }
}
