package com.example.catalog;

import com.example.catalog.dto.ProductRequest;
import com.example.catalog.dto.ResponseDTO;
import com.example.catalog.entity.Product;
import com.example.catalog.event.ProductIndexEvent;
import com.example.catalog.exception.DuplicateProductException;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.ProductSearchRepository;
import com.example.catalog.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock ProductSearchRepository productSearchRepository;
    @Mock ApplicationEventPublisher events;

    @InjectMocks ProductServiceImpl service;

    private ProductRequest request(String name, String price) {
        ProductRequest r = new ProductRequest();
        r.setMpn("TEST-001");
        r.setItemName(name);
        r.setManufacturer("Test Manufacturer");
        r.setPrice(new BigDecimal(price));
        r.setCurrency("INR");
        r.setQuantity(10);
        return r;
    }

    private Product existing() {
        return Product.builder()
                .id(1L).mpn("TEST-001").itemName("Test Item")
                .manufacturer("Test Manufacturer")
                .price(new BigDecimal("100.00")).currency("INR")
                .quantity(10).status("ACTIVE").build();
    }

    private ProductIndexEvent capturedEvent() {
        ArgumentCaptor<ProductIndexEvent> captor = ArgumentCaptor.forClass(ProductIndexEvent.class);
        verify(events).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void create_savesProduct_andPublishesUpsertWithReembed() {
        when(productRepository.existsByMpn("TEST-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ResponseDTO res = service.create(request("Test Item", "100.00"));

        assertTrue(res.isStatus());
        ProductIndexEvent event = capturedEvent();
        assertEquals(1L, event.productId());
        assertEquals(ProductIndexEvent.Action.UPSERT, event.action());
        assertTrue(event.reembed());
    }

    @Test
    void create_duplicateMpn_throwsAndDoesNotSaveOrIndex() {
        when(productRepository.existsByMpn("TEST-001")).thenReturn(true);

        assertThrows(DuplicateProductException.class,
                () -> service.create(request("Test Item", "100.00")));

        verify(productRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void update_nameChanged_requestsNewEmbedding() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, request("Renamed Item", "100.00"));

        assertTrue(capturedEvent().reembed());
    }

    @Test
    void update_priceOnly_reusesExistingEmbedding() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, request("Test Item", "250.00"));

        assertFalse(capturedEvent().reembed());
    }

    @Test
    void update_specificationsChanged_requestsNewEmbedding() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest r = request("Test Item", "100.00");
        r.setSpecifications("Dual channel, 1 MHz");

        service.update(1L, r);

        assertTrue(capturedEvent().reembed());
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> service.update(99L, request("Test Item", "100.00")));

        verifyNoInteractions(events);
    }

    @Test
    void delete_removesProduct_andPublishesDeleteEvent() {
        Product product = existing();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.delete(1L);

        verify(productRepository).delete(product);
        ProductIndexEvent event = capturedEvent();
        assertEquals(ProductIndexEvent.Action.DELETE, event.action());
        assertEquals(1L, event.productId());
    }
}
