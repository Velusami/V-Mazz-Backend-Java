package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.domain.PurchaseOrder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderSummaryResponse {

    private final Long id;
    private final String poNumber;
    private final OrderStatus status;
    private final LocalDateTime orderDate;
    private final String customerName;
    private final String supplierName;
    private final int itemCount;
    private final String currency;
    private final BigDecimal grandTotal;

    public static OrderSummaryResponse from(PurchaseOrder po) {
        return OrderSummaryResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .customerName(po.getCustomer().getFullName())
                .supplierName(po.getSupplierCompany().getCompanyName())
                .itemCount(po.getItems().size())
                .currency(po.getCurrency())
                .grandTotal(po.getGrandTotal())
                .build();
    }
}
