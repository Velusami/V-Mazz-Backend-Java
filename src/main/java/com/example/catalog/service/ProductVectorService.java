package com.example.catalog.service;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.catalog.document.ProductDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ProductVectorService {
    private final EmbeddingService embeddingService;
    private final ElasticsearchClient elasticsearchClient;

    public ProductVectorService(
            EmbeddingService embeddingService,
            ElasticsearchClient elasticsearchClient) {

        this.embeddingService = embeddingService;
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<ProductDocument> vectorSearch(String query) {

        float[] queryEmbedding =
                embeddingService.generateQueryEmbedding(query);

        List<Float> vector = new java.util.ArrayList<>();

        for (float value : queryEmbedding) {
            vector.add(value);
        }

        try {

            SearchResponse<ProductDocument> response =
                    elasticsearchClient.search(s -> s
                                    .index("zestpart_products_v3")
                                    .source(src -> src
                                            .filter(f -> f
                                                    .includes(
                                                            "id",
                                                            "mpn",
                                                            "itemName",
                                                            "manufacturer",
                                                            "price",
                                                            "currency",
                                                            "quantity",
                                                            "status",
                                                            "version"
                                                    )
                                            )
                                    )
                                    .knn(k -> k
                                            .field("embedding")
                                            .queryVector(vector)
                                            .k(5)
                                            .numCandidates(100)
                                    ),
                            ProductDocument.class
                    );

            return response.hits()
                    .hits()
                    .stream()
                    .map(hit -> hit.source())
                    .filter(java.util.Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Elasticsearch vector search failed", e);
        }
    }

}
