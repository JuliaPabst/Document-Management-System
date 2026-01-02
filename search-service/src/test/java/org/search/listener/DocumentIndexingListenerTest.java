package org.search.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.search.dto.DocumentIndexDto;
import org.search.dto.DocumentUpdateEventDto;
import org.search.service.ElasticsearchService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingListenerTest {

    @Mock
    private ElasticsearchService elasticsearchService;

    @InjectMocks
    private DocumentIndexingListener listener;

    private ObjectMapper objectMapper;
    private DocumentIndexDto testDocument;
    private DocumentUpdateEventDto testUpdateEvent;
    private DocumentUpdateEventDto testDeleteEvent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new DocumentIndexingListener(elasticsearchService, objectMapper);

        testDocument = DocumentIndexDto.builder()
                .documentId(1L)
                .filename("test.pdf")
                .author("John Doe")
                .fileType("application/pdf")
                .size(1024L)
                .objectKey("test-key")
                .summary("Test summary")
                .extractedText("Test content")
                .build();

        testUpdateEvent = DocumentUpdateEventDto.builder()
                .documentId(1L)
                .filename("updated.pdf")
                .author("Jane Doe")
                .fileType("application/pdf")
                .size(2048L)
                .objectKey("updated-key")
                .summary("Updated summary")
                .eventType(DocumentUpdateEventDto.EventType.UPDATE)
                .build();

        testDeleteEvent = DocumentUpdateEventDto.builder()
                .documentId(1L)
                .eventType(DocumentUpdateEventDto.EventType.DELETE)
                .build();
    }

    @Test
    void handleMessage_WithDocumentIndexDto_ShouldIndexDocument() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testDocument);
        Message message = new Message(json.getBytes(), new MessageProperties());

        // Act
        listener.handleDocumentIndexing(message);

        // Assert
        verify(elasticsearchService, times(1)).indexDocument(any(DocumentIndexDto.class));
    }

    @Test
    void handleMessage_WithUpdateEvent_ShouldPartiallyUpdateDocument() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testUpdateEvent);
        Message message = new Message(json.getBytes(), new MessageProperties());

        // Act
        listener.handleDocumentIndexing(message);

        // Assert
        verify(elasticsearchService, times(1)).updateDocumentPartial(any(DocumentIndexDto.class));
    }

    @Test
    void handleMessage_WithDeleteEvent_ShouldDeleteDocument() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testDeleteEvent);
        Message message = new Message(json.getBytes(), new MessageProperties());

        // Act
        listener.handleDocumentIndexing(message);

        // Assert
        verify(elasticsearchService, times(1)).deleteDocument(1L);
    }

    @Test
    void handleMessage_WhenIndexingFails_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testDocument);
        Message message = new Message(json.getBytes(), new MessageProperties());
        doThrow(new RuntimeException("Elasticsearch error"))
                .when(elasticsearchService).indexDocument(any(DocumentIndexDto.class));

        // Act & Assert: should propagate RuntimeException (not caught by IOException handler)
        assertThrows(RuntimeException.class, () -> listener.handleDocumentIndexing(message));
        verify(elasticsearchService, times(1)).indexDocument(any(DocumentIndexDto.class));
    }

    @Test
    void handleMessage_WhenUpdateFails_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testUpdateEvent);
        Message message = new Message(json.getBytes(), new MessageProperties());
        doThrow(new RuntimeException("Update failed"))
                .when(elasticsearchService).updateDocumentPartial(any(DocumentIndexDto.class));

        // Act & Assert: should propagate RuntimeException (not caught by IOException handler)
        assertThrows(RuntimeException.class, () -> listener.handleDocumentIndexing(message));
        verify(elasticsearchService, times(1)).updateDocumentPartial(any(DocumentIndexDto.class));
    }

    @Test
    void handleMessage_WhenDeleteFails_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        String json = objectMapper.writeValueAsString(testDeleteEvent);
        Message message = new Message(json.getBytes(), new MessageProperties());
        doThrow(new RuntimeException("Delete failed"))
                .when(elasticsearchService).deleteDocument(anyLong());

        // Act & Assert: should propagate RuntimeException (not caught by IOException handler)
        assertThrows(RuntimeException.class, () -> listener.handleDocumentIndexing(message));
        verify(elasticsearchService, times(1)).deleteDocument(1L);
    }

    @Test
    void handleMessage_WithDocumentContainingExtractedText_ShouldIndexWithOCRContent() throws Exception {
        // Arrange
        DocumentIndexDto documentWithOCR = DocumentIndexDto.builder()
                .documentId(2L)
                .filename("scanned.pdf")
                .author("Scanner User")
                .fileType("application/pdf")
                .size(5120L)
                .objectKey("scanned-key")
                .summary("Scanned document")
                .extractedText("This is extracted text from OCR processing")
                .build();
        String json = objectMapper.writeValueAsString(documentWithOCR);
        Message message = new Message(json.getBytes(), new MessageProperties());

        // Act
        listener.handleDocumentIndexing(message);

        // Assert
        verify(elasticsearchService, times(1)).indexDocument(any(DocumentIndexDto.class));
    }

    @Test
    void handleMessage_WithMultipleDocuments_ShouldIndexAll() throws Exception {
        // Arrange
        DocumentIndexDto doc1 = DocumentIndexDto.builder()
                .documentId(1L)
                .filename("doc1.pdf")
                .build();
        DocumentIndexDto doc2 = DocumentIndexDto.builder()
                .documentId(2L)
                .filename("doc2.pdf")
                .build();
        String json1 = objectMapper.writeValueAsString(doc1);
        String json2 = objectMapper.writeValueAsString(doc2);
        Message message1 = new Message(json1.getBytes(), new MessageProperties());
        Message message2 = new Message(json2.getBytes(), new MessageProperties());

        // Act
        listener.handleDocumentIndexing(message1);
        listener.handleDocumentIndexing(message2);

        // Assert
        verify(elasticsearchService, times(2)).indexDocument(any(DocumentIndexDto.class));
    }
}
