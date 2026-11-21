package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.dto.OrderResponse;
import com.example.catalog.vmazz.dto.OrderSummaryResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.dto.UpdateOrderStatusRequest;
import com.example.catalog.vmazz.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "7. Orders received (admin / sub user)")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Purchase orders placed with my company")
    public PageResponse<OrderSummaryResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.companyOrders(status, Paging.of(page, size,
                Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One purchase order with item details, address and contact")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.companyOrder(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Move an order forward: PLACED > CONFIRMED > PACKED > SHIPPED > DELIVERED (or CANCELLED)")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
