package com.example.catalog.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "zestpart_products_v3")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String mpn;

    @Field(type = FieldType.Text)
    private String itemName;

    @Field(type = FieldType.Keyword)
    private String manufacturer;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Integer)
    private Integer quantity;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String packageType;

    @Field(type = FieldType.Text)
    private String specifications;

    @Field(name = "version", type = FieldType.Long)
    private Long documentVersion;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] embedding;
}