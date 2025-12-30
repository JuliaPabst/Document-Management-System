package org.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.dto.DocumentIndexDto;
import org.search.dto.SearchRequestDto;
import org.search.dto.SearchResultDto;
import org.search.dto.SearchResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.index.documents}")
    private String indexName;

    @PostConstruct
    public void init() {
        try {
            createIndexIfNotExists();
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
        }
    }

    private void createIndexIfNotExists() throws IOException {
        ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
        boolean exists = elasticsearchClient.indices().exists(existsRequest).value();

        if (!exists) {
            log.info("Creating Elasticsearch index: {}", indexName);
            CreateIndexRequest createIndexRequest = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("documentId", p -> p.long_(l -> l))
                            .properties("filename", p -> p.text(t -> t.analyzer("standard")))
                            .properties("author", p -> p.text(t -> t.analyzer("standard").fields("keyword", f -> f.keyword(k -> k))))
                            .properties("fileType", p -> p.keyword(k -> k))
                            .properties("size", p -> p.long_(l -> l))
                            .properties("objectKey", p -> p.keyword(k -> k))
                            .properties("uploadTime", p -> p.date(d -> d.format("strict_date_optional_time")))
                            .properties("extractedText", p -> p.text(t -> t.analyzer("standard")))
                            .properties("summary", p -> p.text(t -> t.analyzer("standard")))
                            .properties("processedTime", p -> p.date(d -> d.format("strict_date_optional_time")))
                    )
            );
            elasticsearchClient.indices().create(createIndexRequest);
            log.info("Elasticsearch index created successfully: {}", indexName);
        } else {
            log.info("Elasticsearch index already exists: {}", indexName);
        }
    }

    public void indexDocument(DocumentIndexDto document) throws IOException {
        log.info("Indexing document ID: {} - {}", document.getDocumentId(), document.getFilename());

        IndexRequest<DocumentIndexDto> request = IndexRequest.of(i -> i
                .index(indexName)
                .id(String.valueOf(document.getDocumentId()))
                .document(document)
        );

        IndexResponse response = elasticsearchClient.index(request);
        log.info("Document indexed successfully: {} with result: {}", document.getDocumentId(), response.result());
    }

    public void deleteDocument(Long documentId) throws IOException {
        log.info("Deleting document from index: {}", documentId);

        DeleteRequest request = DeleteRequest.of(d -> d
                .index(indexName)
                .id(String.valueOf(documentId))
        );

        DeleteResponse response = elasticsearchClient.delete(request);
        log.info("Document deleted: {} with result: {}", documentId, response.result());
    }
    
    // Partially update a document (preserves extractedText and other fields that are not in FileMetadata)
    public void updateDocumentPartial(DocumentIndexDto document) throws IOException {
        log.info("Partially updating document ID: {} - {}", document.getDocumentId(), document.getFilename());

        // Map with only non-null fields to preserve existing fields in Elasticsearch
        Map<String, Object> updates = new HashMap<>();
        if (document.getFilename() != null) updates.put("filename", document.getFilename());
        if (document.getAuthor() != null) updates.put("author", document.getAuthor());
        if (document.getFileType() != null) updates.put("fileType", document.getFileType());
        if (document.getSize() != null) updates.put("size", document.getSize());
        if (document.getObjectKey() != null) updates.put("objectKey", document.getObjectKey());
        if (document.getSummary() != null) updates.put("summary", document.getSummary());
        if (document.getUploadTime() != null) updates.put("uploadTime", document.getUploadTime());
        if (document.getProcessedTime() != null) updates.put("processedTime", document.getProcessedTime());
        // extractedText not included because it should be preserved

        log.info("Updating {} fields for document {}", updates.size(), document.getDocumentId());

        UpdateRequest<Object, Object> request = UpdateRequest.of(u -> u
                .index(indexName)
                .id(String.valueOf(document.getDocumentId()))
                .doc(updates)
                .docAsUpsert(false) // don't create if doesn't exist
        );

        UpdateResponse<Object> response = elasticsearchClient.update(request, Object.class);
        log.info("Document partially updated: {} with result: {}", document.getDocumentId(), response.result());
    }

    public SearchResponseDto search(SearchRequestDto searchRequest) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("Searching for: {} with searchField: {}", searchRequest.getQuery(), searchRequest.getSearchField());

        List<Query> mustQueries = new ArrayList<>();

        // If query is "*" or empty, use match_all, otherwise use wildcard queries for partial matching
        if (searchRequest.getQuery() != null && 
            !searchRequest.getQuery().isBlank() && 
            !"*".equals(searchRequest.getQuery().trim())) {
            
            // Convert query to lowercase and add wildcards for case-insensitive substring matching
            String wildcardQuery = "*" + searchRequest.getQuery().toLowerCase() + "*";
            
            // Determine which fields to search based on searchField parameter
            List<Query> shouldQueries = new ArrayList<>();
            String searchField = searchRequest.getSearchField();
            
            if (searchField != null && !searchField.isBlank()) {
                switch (searchField.toLowerCase()) {
                    case "filename":
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("filename")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                        )));
                        break;
                    case "extractedtext":
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("extractedText")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                        )));
                        break;
                    case "summary":
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("summary")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                        )));
                        break;
                    default: // "all" or any other value
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("filename")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                                .boost(3.0f)
                        )));
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("author")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                                .boost(2.0f)
                        )));
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("extractedText")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                                .boost(2.0f)
                        )));
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                                .field("summary")
                                .value(wildcardQuery)
                                .caseInsensitive(true)
                                .boost(2.0f)
                        )));
                }
            } else {
                // Default (search all fields)
                shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                        .field("filename")
                        .value(wildcardQuery)
                        .caseInsensitive(true)
                        .boost(3.0f)
                )));
                shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                        .field("author")
                        .value(wildcardQuery)
                        .caseInsensitive(true)
                        .boost(2.0f)
                )));
                shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                        .field("extractedText")
                        .value(wildcardQuery)
                        .caseInsensitive(true)
                        .boost(2.0f)
                )));
                shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                        .field("summary")
                        .value(wildcardQuery)
                        .caseInsensitive(true)
                        .boost(2.0f)
                )));
            }
            
            // Bool query with should (OR) for multiple fields
            Query searchQuery = Query.of(q -> q.bool(b -> b.should(shouldQueries).minimumShouldMatch("1")));
            mustQueries.add(searchQuery);
        } else {
            // match_all for wildcard or empty query
            Query matchAllQuery = Query.of(q -> q.matchAll(m -> m));
            mustQueries.add(matchAllQuery);
        }

        // Author filter (exact match using keyword)
        if (searchRequest.getAuthor() != null && !searchRequest.getAuthor().isBlank()) {
            Query authorQuery = Query.of(q -> q
                    .term(t -> t
                            .field("author.keyword")
                            .value(FieldValue.of(searchRequest.getAuthor()))
                    )
            );
            mustQueries.add(authorQuery);
        }

        // File type filter
        if (searchRequest.getFileType() != null && !searchRequest.getFileType().isBlank()) {
            Query fileTypeQuery = Query.of(q -> q
                    .term(t -> t
                            .field("fileType")
                            .value(FieldValue.of(searchRequest.getFileType()))
                    )
            );
            mustQueries.add(fileTypeQuery);
        }

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(mustQueries));

        SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q.bool(boolQuery))
                .from(searchRequest.getPage() * searchRequest.getSize())
                .size(searchRequest.getSize())
                .sort(so -> so
                        .field(f -> f
                                .field(searchRequest.getSortBy())
                                .order("asc".equalsIgnoreCase(searchRequest.getSortOrder())
                                        ? co.elastic.clients.elasticsearch._types.SortOrder.Asc
                                        : co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                        )
                )
                .highlight(h -> h
                        .fields("extractedText", hf -> hf.numberOfFragments(1).fragmentSize(150))
                        .fields("summary", hf -> hf.numberOfFragments(1).fragmentSize(150))
                )
        );

        SearchResponse<DocumentIndexDto> response = elasticsearchClient.search(request, DocumentIndexDto.class);
        HitsMetadata<DocumentIndexDto> hits = response.hits();

        List<SearchResultDto> results = hits.hits().stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());

        long totalHits = hits.total() != null ? hits.total().value() : 0;
        int totalPages = (int) Math.ceil((double) totalHits / searchRequest.getSize());

        long searchTime = System.currentTimeMillis() - startTime;
        log.info("Search completed in {}ms, found {} results", searchTime, totalHits);

        return SearchResponseDto.builder()
                .results(results)
                .totalHits(totalHits)
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .totalPages(totalPages)
                .searchTimeMs(searchTime)
                .build();
    }

    private SearchResultDto mapToSearchResult(Hit<DocumentIndexDto> hit) {
        DocumentIndexDto source = hit.source();
        String highlightedText = null;

        if (hit.highlight() != null) {
            if (hit.highlight().containsKey("extractedText")) {
                highlightedText = hit.highlight().get("extractedText").get(0);
            } else if (hit.highlight().containsKey("summary")) {
                highlightedText = hit.highlight().get("summary").get(0);
            }
        }

        return SearchResultDto.builder()
                .documentId(source.getDocumentId())
                .filename(source.getFilename())
                .author(source.getAuthor())
                .fileType(source.getFileType())
                .size(source.getSize())
                .objectKey(source.getObjectKey())
                .uploadTime(source.getUploadTime())
                .summary(source.getSummary())
                .score(hit.score())
                .highlightedText(highlightedText)
                .build();
    }
}
