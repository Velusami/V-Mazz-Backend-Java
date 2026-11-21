package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.dto.CheckoutRequest;
import com.example.catalog.vmazz.dto.CheckoutResponse;
import com.example.catalog.vmazz.dto.OrderResponse;
import com.example.catalog.vmazz.dto.OrderSummaryResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "6. Checkout and my orders (customer)")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Checkout: place the cart as purchase order(s)",
            description = "Needs contact, shipping address and payment method. "
                    + "One purchase order is created per supplier company; stock is deducted and the cart is emptied.")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping
    @Operation(summary = "My purchase orders")
    public PageResponse<OrderSummaryResponse> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.myOrders(Paging.of(page, size,
                Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of my purchase orders with item details")
    public OrderResponse myOrder(@PathVariable Long id) {
        return orderService.myOrder(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel my order (only while it is PLACED)")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelMyOrder(id);
    }
}
