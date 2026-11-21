package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.domain.PaymentMethod;
import com.example.catalog.vmazz.domain.PurchaseOrder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private final Long id;
    private final String poNumber;
    private final OrderStatus status;
    private final LocalDateTime orderDate;
    private final LocalDate requiredDeliveryDate;
    private final PaymentMethod paymentMethod;
    private final String buyerReference;
    private final String notes;

    private final Long customerId;
    private final String customerName;
    private final String customerEmail;
    private final Long supplierCompanyId;
    private final String supplierName;

    private final String contactName;
    private final String contactPhone;
    private final AddressDto shippingAddress;
    private final AddressDto billingAddress;

    private final String currency;
    private final BigDecimal subTotal;
    private final BigDecimal taxTotal;
    private final BigDecimal grandTotal;

    private final List<OrderItemResponse> items;

    public static OrderResponse from(PurchaseOrder po) {
        return OrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .requiredDeliveryDate(po.getRequiredDeliveryDate())
                .paymentMethod(po.getPaymentMethod())
                .buyerReference(po.getBuyerReference())
                .notes(po.getNotes())
                .customerId(po.getCustomer().getId())
                .customerName(po.getCustomer().getFullName())
                .customerEmail(po.getCustomer().getEmail())
                .supplierCompanyId(po.getSupplierCompany().getId())
                .supplierName(po.getSupplierCompany().getCompanyName())
                .contactName(po.getContactName())
                .contactPhone(po.getContactPhone())
                .shippingAddress(AddressDto.from(po.getShippingAddress()))
                .billingAddress(AddressDto.from(po.getBillingAddress()))
                .currency(po.getCurrency())
                .subTotal(po.getSubTotal())
                .taxTotal(po.getTaxTotal())
                .grandTotal(po.getGrandTotal())
                .items(po.getItems().stream().map(OrderItemResponse::from).toList())
                .build();
    }
}
