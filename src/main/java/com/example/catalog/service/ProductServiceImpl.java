package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ProductResponse;
import com.example.catalog.dto.ResponseDTO;
import com.example.catalog.entity.Product;
import com.example.catalog.event.ProductIndexEvent;
import com.example.catalog.exception.DuplicateProductException;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * SQL is the source of truth. Elasticsearch is updated AFTER the SQL
 * transaction commits (see ProductIndexer), so a search/embedding failure can
 * never make a successful save look like a failure, or vice versa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public ResponseDTO create(ProductRequest request) {

        String mpn = request.getMpn().trim();

        if (productRepository.existsByMpn(mpn)) {
            throw new DuplicateProductException(
                    "Product already exists with MPN: " + mpn);
        }

        Product saved = productRepository.save(Product.builder()
                .mpn(mpn)
                .itemName(request.getItemName())
                .manufacturer(request.getManufacturer())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .quantity(request.getQuantity())
                .category(blankToNull(request.getCategory()))
                .packageType(blankToNull(request.getPackageType()))
                .specifications(blankToNull(request.getSpecifications()))
                .status("ACTIVE")
                .build());

        log.info("Product saved. id={}, mpn={}", saved.getId(), saved.getMpn());

        events.publishEvent(ProductIndexEvent.upsert(saved.getId(), true));

        ResponseDTO res = new ResponseDTO();
        res.setStatus(true);
        res.setMessage("Product " + mpn + " created");
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = findOrThrow(id);

        // The embedding is built from name, category, specs, package, manufacturer
        // and MPN, so a price/stock change does not need a new (slow) embedding call.
        boolean reembed =
                !Objects.equals(product.getItemName(), request.getItemName())
                        || !Objects.equals(product.getManufacturer(), request.getManufacturer())
                        || !Objects.equals(product.getCategory(), blankToNull(request.getCategory()))
                        || !Objects.equals(product.getPackageType(), blankToNull(request.getPackageType()))
                        || !Objects.equals(product.getSpecifications(), blankToNull(request.getSpecifications()));

        product.setItemName(request.getItemName());
        product.setManufacturer(request.getManufacturer());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setQuantity(request.getQuantity());
        product.setCategory(blankToNull(request.getCategory()));
        product.setPackageType(blankToNull(request.getPackageType()));
        product.setSpecifications(blankToNull(request.getSpecifications()));

        Product updated = productRepository.save(product);

        events.publishEvent(ProductIndexEvent.upsert(updated.getId(), reembed));

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
        events.publishEvent(ProductIndexEvent.delete(id));
    }

    @Override
    public List<ProductDocument> search(String keyword) {
        return productSearchRepository.searchByItemName(keyword);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .mpn(product.getMpn())
                .itemName(product.getItemName())
                .manufacturer(product.getManufacturer())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .quantity(product.getQuantity())
                .category(product.getCategory())
                .packageType(product.getPackageType())
                .specifications(product.getSpecifications())
                .status(product.getStatus())
                .version(product.getVersion())
                .build();
    }
}
