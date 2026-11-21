package com.example.catalog.service;

import com.example.catalog.entity.Product;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductEmbeddingService {

    private static final int DIMENSIONS = 768;
    private static final String DOCUMENT_PREFIX = "search_document: ";

    private final EmbeddingModel embeddingModel;

    public ProductEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(Product product) {
        return validate(embeddingModel.embed(toText(product)));
    }

    /** One call to the model for many products - used by reindexing. */
    public List<float[]> generateEmbeddings(List<Product> products) {
        List<String> texts = products.stream().map(this::toText).toList();
        List<float[]> vectors = embeddingModel.embed(texts);

        if (vectors == null || vectors.size() != products.size()) {
            throw new IllegalStateException(
                    "Embedding model returned an unexpected number of vectors");
        }
        vectors.forEach(this::validate);
        return vectors;
    }

    private String toText(Product product) {
        // Functional description first (name, category, specs, package), then maker / MPN.
        String text = Stream.of(
                        product.getItemName(),
                        product.getCategory(),
                        product.getSpecifications(),
                        product.getPackageType(),
                        product.getManufacturer(),
                        product.getMpn())
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "Product details are required to generate an embedding");
        }
        return DOCUMENT_PREFIX + text;
    }

    private float[] validate(float[] vector) {
        if (vector == null || vector.length != DIMENSIONS) {
            throw new IllegalStateException(
                    "Expected a " + DIMENSIONS + "-dimensional product embedding");
        }
        return vector;
    }
}
