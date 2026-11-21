package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    public ProductDocument toDocument(Product product, float[] embedding) {
        return ProductDocument.builder()
                .id(product.getId())
                .mpn(product.getMpn())
                .itemName(product.getItemName())
                .manufacturer(product.getManufacturer())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .quantity(product.getQuantity())
                .status(product.getStatus())
                .category(product.getCategory())
                .packageType(product.getPackageType())
                .specifications(product.getSpecifications())
                .documentVersion(product.getVersion())
                .embedding(embedding)
                .build();
    }
}
