package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.dto.ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ResponseDTO create(ProductRequest request);
    ProductResponse getById(Long id);
    Page<ProductResponse> getAll(Pageable pageable);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
    List<ProductDocument> search(String keyword);
}
