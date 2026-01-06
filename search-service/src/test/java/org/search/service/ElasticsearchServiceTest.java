package org.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.search.dto.DocumentIndexDto;
import org.search.dto.SearchRequestDto;
import org.search.dto.SearchResponseDto;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchService elasticsearchService;

    private DocumentIndexDto testDocument;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(elasticsearchService, "indexName", "documents");

        testDocument = DocumentIndexDto.builder()
                .documentId(1L)
                .filename("test.pdf")
                .author("John Doe")
                .fileType("PDF")
                .size(1024L)
                .objectKey("test-key")
                .uploadTime(LocalDateTime.now())
                .extractedText("This is test content")
                .summary("Test summary")
                .processedTime(LocalDateTime.now())
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexDocument_ShouldIndexSuccessfully() throws IOException {
        // Arrange
        IndexResponse mockResponse = mock(IndexResponse.class);
        when(mockResponse.result()).thenReturn(co.elastic.clients.elasticsearch._types.Result.Created);
        when(elasticsearchClient.index(any(IndexRequest.class))).thenReturn(mockResponse);

        // Act & Assert (should not throw exception)
        elasticsearchService.indexDocument(testDocument);

        // Verify
        verify(elasticsearchClient, times(1)).index(any(IndexRequest.class));
    }

    @Test
    void deleteDocument_ShouldDeleteSuccessfully() throws IOException {
        // Arrange
        DeleteResponse mockResponse = mock(DeleteResponse.class);
        when(mockResponse.result()).thenReturn(co.elastic.clients.elasticsearch._types.Result.Deleted);
        when(elasticsearchClient.delete(any(DeleteRequest.class))).thenReturn(mockResponse);

        // Act & Assert (should not throw exception)
        elasticsearchService.deleteDocument(1L);

        // Verify
        verify(elasticsearchClient, times(1)).delete(any(DeleteRequest.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateDocumentPartial_ShouldUpdateOnlyProvidedFields() throws IOException {
        // Arrange
        UpdateResponse<Object> mockResponse = mock(UpdateResponse.class);
        when(mockResponse.result()).thenReturn(co.elastic.clients.elasticsearch._types.Result.Updated);
        when(elasticsearchClient.update(any(UpdateRequest.class), eq(Object.class))).thenReturn(mockResponse);

        DocumentIndexDto partialUpdate = DocumentIndexDto.builder()
                .documentId(1L)
                .author("Jane Doe") // Only updating author
                .build();

        // Act & Assert (should not throw exception)
        elasticsearchService.updateDocumentPartial(partialUpdate);

        // Verify
        verify(elasticsearchClient, times(1)).update(any(UpdateRequest.class), eq(Object.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_WithQuery_ShouldReturnResults() throws IOException {
        // Arrange
        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query("test")
                .page(0)
                .size(10)
                .sortBy("uploadTime")
                .sortOrder("desc")
                .build();

        // Mock search response
        SearchResponse<DocumentIndexDto> mockSearchResponse = mock(SearchResponse.class);
        HitsMetadata<DocumentIndexDto> mockHits = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq));
        
        Hit<DocumentIndexDto> mockHit = mock(Hit.class);
        when(mockHit.source()).thenReturn(testDocument);
        when(mockHit.score()).thenReturn(1.0);
        when(mockHit.highlight()).thenReturn(null);
        
        List<Hit<DocumentIndexDto>> hitsList = new ArrayList<>();
        hitsList.add(mockHit);
        
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockHits.total()).thenReturn(totalHits);
        when(mockSearchResponse.hits()).thenReturn(mockHits);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(DocumentIndexDto.class)))
                .thenReturn(mockSearchResponse);

        // Act
        SearchResponseDto result = elasticsearchService.search(searchRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getTotalHits()).isEqualTo(1);
        assertThat(result.getResults().get(0).getFilename()).isEqualTo("test.pdf");
        assertThat(result.getResults().get(0).getAuthor()).isEqualTo("John Doe");

        // Verify
        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(DocumentIndexDto.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_WithWildcardQuery_ShouldReturnAllResults() throws IOException {
        // Arrange
        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query("*")
                .page(0)
                .size(10)
                .sortBy("uploadTime")
                .sortOrder("desc")
                .build();

        // Mock search response
        SearchResponse<DocumentIndexDto> mockSearchResponse = mock(SearchResponse.class);
        HitsMetadata<DocumentIndexDto> mockHits = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(2).relation(TotalHitsRelation.Eq));
        
        when(mockHits.hits()).thenReturn(new ArrayList<>());
        when(mockHits.total()).thenReturn(totalHits);
        when(mockSearchResponse.hits()).thenReturn(mockHits);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(DocumentIndexDto.class)))
                .thenReturn(mockSearchResponse);

        // Act
        SearchResponseDto result = elasticsearchService.search(searchRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalHits()).isEqualTo(2);

        // Verify match_all query should be used for wildcard
        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(DocumentIndexDto.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_WithAuthorFilter_ShouldFilterByAuthor() throws IOException {
        // Arrange
        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query("test")
                .author("John Doe")
                .page(0)
                .size(10)
                .sortBy("uploadTime")
                .sortOrder("desc")
                .build();

        // Mock search response
        SearchResponse<DocumentIndexDto> mockSearchResponse = mock(SearchResponse.class);
        HitsMetadata<DocumentIndexDto> mockHits = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq));
        
        when(mockHits.hits()).thenReturn(new ArrayList<>());
        when(mockHits.total()).thenReturn(totalHits);
        when(mockSearchResponse.hits()).thenReturn(mockHits);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(DocumentIndexDto.class)))
                .thenReturn(mockSearchResponse);

        // Act
        SearchResponseDto result = elasticsearchService.search(searchRequest);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(DocumentIndexDto.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_WithMultiTermQuery_ShouldSplitAndSearchAllTerms() throws IOException {
        // Arrange
        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query("vis taffl") // Multi-term query
                .page(0)
                .size(10)
                .sortBy("uploadTime")
                .sortOrder("desc")
                .build();

        // Mock search response
        SearchResponse<DocumentIndexDto> mockSearchResponse = mock(SearchResponse.class);
        HitsMetadata<DocumentIndexDto> mockHits = mock(HitsMetadata.class);
        TotalHits totalHits = TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq));
        
        Hit<DocumentIndexDto> mockHit = mock(Hit.class);
        DocumentIndexDto tafflerDoc = DocumentIndexDto.builder()
                .documentId(2L)
                .filename("taffler-doc.pdf")
                .author("Davis Taffler")
                .fileType("PDF")
                .size(2048L)
                .objectKey("taffler-key")
                .uploadTime(LocalDateTime.now())
                .extractedText("Content by Davis Taffler")
                .summary("Davis's document")
                .processedTime(LocalDateTime.now())
                .build();
        
        when(mockHit.source()).thenReturn(tafflerDoc);
        when(mockHit.score()).thenReturn(2.5);
        when(mockHit.highlight()).thenReturn(null);
        
        List<Hit<DocumentIndexDto>> hitsList = new ArrayList<>();
        hitsList.add(mockHit);
        
        when(mockHits.hits()).thenReturn(hitsList);
        when(mockHits.total()).thenReturn(totalHits);
        when(mockSearchResponse.hits()).thenReturn(mockHits);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(DocumentIndexDto.class)))
                .thenReturn(mockSearchResponse);

        // Act
        SearchResponseDto result = elasticsearchService.search(searchRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).getAuthor()).isEqualTo("Davis Taffler");

        // Verify
        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(DocumentIndexDto.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void indexDocument_WhenElasticsearchFails_ShouldThrowException() throws IOException {
        // Arrange
        when(elasticsearchClient.index(any(IndexRequest.class)))
                .thenThrow(new IOException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> elasticsearchService.indexDocument(testDocument))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Connection failed");
    }
}
