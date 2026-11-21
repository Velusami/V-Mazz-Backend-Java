package com.example.catalog.repository;

import com.example.catalog.document.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument,Long> {

    @Query("""
    {
      "match": {
        "itemName": "?0"
      }
    }
    """)
        List<ProductDocument> searchByItemName(String keyword);

    @Query("""
    {
      "bool": {
        "must": [
          {
            "match": {
              "itemName": "?0"
            }
          },
          {
            "term": {
              "manufacturer": "?1"
            }
          },
          {
            "term": {
              "currency": "?2"
            }
          },
          {
            "term": {
              "status": "?3"
            }
          }
        ],
        "filter": [
          {
            "range": {
              "price": {
                "lte": "?4"
              }
            }
          },
          {
            "range": {
              "quantity": {
                "gte": "?5"
              }
            }
          }
        ]
      }
    }
    """)
    List<ProductDocument> searchProducts(
            String itemName,
            String manufacturer,
            String currency,
            String status,
            Double maxPrice,
            Integer minimumQuantity
    );

    List<ProductDocument> findByItemNameContaining(String itemName);

    Optional<ProductDocument> findByMpn(String mpn);

}
