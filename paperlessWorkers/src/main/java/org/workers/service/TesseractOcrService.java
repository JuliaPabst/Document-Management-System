package org.workers.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Service for performing OCR on documents using Tesseract
@Service
@Slf4j
public class TesseractOcrService {

	private final Tesseract tesseract;

	public TesseractOcrService() {
		this.tesseract = new Tesseract();

		// Try to set tessdata path from environment or use default
		String tessdataPath = System.getenv("TESSDATA_PREFIX");
		if (tessdataPath != null && !tessdataPath.isEmpty()) {
			tesseract.setDatapath(tessdataPath);
			log.info("Tesseract initialized with tessdata path: {}", tessdataPath);
		} else {
			// Default path for Alpine Linux
			tesseract.setDatapath("/usr/share/tessdata");
			log.info("Tesseract initialized with default tessdata path");
		}

		tesseract.setLanguage("eng+deu");   // English + German languages
		tesseract.setPageSegMode(1);        // Automatic page segmentation with OSD
		tesseract.setOcrEngineMode(3);      // Default

		log.info("TesseractOcrService initialized successfully with languages: eng+deu");
	}

	// Extract text from PDF document using Ghostscript for PDF to image conversion
	public String extractTextFromPdf(byte[] pdfBytes, String filename) throws IOException {
		log.info("Starting OCR processing for PDF using Ghostscript: {}", filename);

		Path tempDir = null;
		Path pdfPath = null;
		List<Path> imagePaths = new ArrayList<>();

		try {
			// Create temporary directory
			tempDir = Files.createTempDirectory("ocr-pdf-");
			pdfPath = tempDir.resolve("input.pdf");

			// Write PDF to temp file
			Files.write(pdfPath, pdfBytes);
			log.debug("PDF written to temporary file: {}", pdfPath);

			// Convert PDF to images using Ghostscript
			imagePaths = convertPdfToImagesWithGhostscript(pdfPath, tempDir);
			log.info("Ghostscript converted PDF to {} image(s)", imagePaths.size());

			// Perform OCR on each image
			StringBuilder extractedText = new StringBuilder();
			for (int i = 0; i < imagePaths.size(); i++) {
				Path imagePath = imagePaths.get(i);
				log.debug("Processing page {} of {}", i + 1, imagePaths.size());

				BufferedImage image = ImageIO.read(imagePath.toFile());
				String pageText = performOcr(image);
				extractedText.append(pageText).append("\n\n");

				log.debug("Extracted {} characters from page {}", pageText.length(), i + 1);
			}

			String result = extractedText.toString().trim();
			log.info("OCR completed for {}: extracted {} characters from {} page(s)",
					filename, result.length(), imagePaths.size());

			return result;

		} catch (IOException | InterruptedException e) {
			log.error("Failed to process PDF {}: {}", filename, e.getMessage(), e);
			throw new IOException("PDF OCR processing failed: " + e.getMessage(), e);
		} finally {
			// Cleanup temporary files
			cleanupTempFiles(pdfPath, imagePaths, tempDir);
		}
	}

	// Convert PDF to images using Ghostscript CLI
	private List<Path> convertPdfToImagesWithGhostscript(Path pdfPath, Path outputDir)
			throws IOException, InterruptedException {

		String outputPattern = outputDir.resolve("page-%d.png").toString();

		// Ghostscript command to convert PDF to PNG images at 300 DPI
		List<String> command = List.of(
				"gs",	                    // Ghostscript executable
				"-dSAFER",	                // Safe mode
				"-dBATCH",	                // Exit after processing
				"-dNOPAUSE",	            // No pause after each page
				"-sDEVICE=png16m",	        // Output device: 24-bit color PNG
				"-r300",	                // 300 DPI resolution
				"-sOutputFile=" + outputPattern,	// Output: page-1.png, page-2.png, ...
				pdfPath.toString());	    // Input PDF file

		log.debug("Executing Ghostscript command: {}", String.join(" ", command));

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();

		// Read output for logging
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				log.debug("Ghostscript: {}", line);
			}
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new IOException("Ghostscript conversion failed with exit code: " + exitCode);
		}

		// Find all generated PNG files
		List<Path> imagePaths = new ArrayList<>();
		try (var stream = Files.list(outputDir)) {
			stream.filter(path -> path.toString().endsWith(".png"))
					.sorted()
					.forEach(imagePaths::add);
		}

		if (imagePaths.isEmpty()) {
			throw new IOException("Ghostscript did not generate any images");
		}

		return imagePaths;
	}

	// Cleanup temporary files and directories
	private void cleanupTempFiles(Path pdfPath, List<Path> imagePaths, Path tempDir) {
		try {
			if (pdfPath != null && Files.exists(pdfPath)) {
				Files.delete(pdfPath);
			}
			for (Path imagePath : imagePaths) {
				if (Files.exists(imagePath)) {
					Files.delete(imagePath);
				}
			}
			if (tempDir != null && Files.exists(tempDir)) {
				Files.delete(tempDir);
			}
			log.debug("Temporary files cleaned up");
		} catch (IOException e) {
			log.warn("Failed to cleanup temporary files: {}", e.getMessage());
		}
	}

	// Extract text from image file
	public String extractTextFromImage(byte[] imageBytes, String filename) throws IOException {
		log.info("Starting OCR processing for image: {}", filename);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
			BufferedImage image = ImageIO.read(inputStream);

			if (image == null) {
				throw new IOException("Failed to read image: " + filename);
			}

			String result = performOcr(image);
			log.info("OCR completed for {}: extracted {} characters", filename, result.length());

			return result;

		} catch (IOException e) {
			log.error("Failed to process image {}: {}", filename, e.getMessage(), e);
			throw e;
		}
	}

	// Perform OCR on a BufferedImage
	private String performOcr(BufferedImage image) {
		try {
			String text = tesseract.doOCR(image);
			return text != null ? text.trim() : "";
		} catch (TesseractException e) {
			log.error("Tesseract OCR failed: {}", e.getMessage(), e);
			return "[OCR Error: " + e.getMessage() + "]";
		}
	}

	// Get Tesseract version info for logging
	public String getVersion() {
		return "Tesseract OCR v5.13.0 + Ghostscript";
	}
}