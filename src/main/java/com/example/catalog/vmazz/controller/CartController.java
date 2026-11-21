package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.dto.AddToCartRequest;
import com.example.catalog.vmazz.dto.CartResponse;
import com.example.catalog.vmazz.dto.UpdateCartItemRequest;
import com.example.catalog.vmazz.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "5. Cart (customer)", description = "Add to cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "My cart with prices, tax and totals")
    public CartResponse get() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item (adds to the quantity if it is already in the cart)")
    public CartResponse add(@Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Set the quantity of a cart line (0 removes it)")
    public CartResponse update(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the cart")
    public CartResponse remove(@PathVariable Long itemId) {
        return cartService.removeItem(itemId);
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public CartResponse clear() {
        return cartService.clear();
    }
}
