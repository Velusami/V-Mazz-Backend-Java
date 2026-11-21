package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Cart;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.dto.AddToCartRequest;
import com.example.catalog.vmazz.dto.CartResponse;
import com.example.catalog.vmazz.dto.UpdateCartItemRequest;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.CartRepository;
import com.example.catalog.vmazz.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock ItemRepository itemRepository;
    @Mock CurrentUserService currentUser;

    @InjectMocks CartService service;

    AppUser customer = TestData.customer(5L);
    Company supplier = TestData.company(1L);
    Cart cart = Cart.builder().id(1L).user(customer).build();
    Item item = TestData.item(10L, supplier, "100", "12", "100");

    @BeforeEach
    void setUp() {
        when(currentUser.requireCustomer()).thenReturn(customer);
        when(cartRepository.findByUser_Id(5L)).thenReturn(Optional.of(cart));
    }

    private void itemExists() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void addItem_addsALine_andTotalsIncludeTax() {
        itemExists();

        CartResponse res = service.addItem(new AddToCartRequest(10L, new BigDecimal("3")));

        assertEquals(1, res.getTotalLines());
        assertMoney("300.00", res.getSubTotal());
        assertMoney("36.00", res.getTaxTotal());          // 12 % of 300
        assertMoney("336.00", res.getGrandTotal());
        assertEquals("INR", res.getCurrency());
        assertFalse(res.isHasUnavailableItems());
        assertEquals("Supplier 1", res.getItems().get(0).getSupplierName());
    }

    @Test
    void addItem_sameItemAgain_increasesTheQuantity() {
        itemExists();
        cart.getItems().add(TestData.line(cart, item, "2"));

        CartResponse res = service.addItem(new AddToCartRequest(10L, new BigDecimal("3")));

        assertEquals(1, res.getTotalLines());
        assertMoney("5", res.getItems().get(0).getQuantity());
    }

    @Test
    void addItem_inactiveItem_isRejected() {
        item.setStatus(RecordStatus.INACTIVE);
        itemExists();

        assertThrows(ConflictException.class,
                () -> service.addItem(new AddToCartRequest(10L, BigDecimal.ONE)));
    }

    @Test
    void addItem_moreThanStock_isRejected_evenAcrossRepeatedAdds() {
        itemExists();
        cart.getItems().add(TestData.line(cart, item, "90"));

        assertThrows(ConflictException.class,
                () -> service.addItem(new AddToCartRequest(10L, new BigDecimal("11"))));
    }

    @Test
    void addItem_unknownItem_isNotFound() {
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addItem(new AddToCartRequest(10L, BigDecimal.ONE)));
    }

    @Test
    void addItem_differentCurrencyThanTheCart_isRejected() {
        itemExists();
        Item dollars = TestData.item(11L, supplier, "10", "0", "50");
        dollars.setCurrency("USD");
        cart.getItems().add(TestData.line(cart, dollars, "1"));

        assertThrows(ConflictException.class,
                () -> service.addItem(new AddToCartRequest(10L, BigDecimal.ONE)));
    }

    @Test
    void updateItem_withZero_removesTheLine() {
        cart.getItems().add(TestData.line(cart, item, "2"));

        CartResponse res = service.updateItem(10L, new UpdateCartItemRequest(BigDecimal.ZERO));

        assertEquals(0, res.getTotalLines());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateItem_setsTheNewQuantity() {
        cart.getItems().add(TestData.line(cart, item, "2"));

        CartResponse res = service.updateItem(10L, new UpdateCartItemRequest(new BigDecimal("7")));

        assertMoney("7", res.getItems().get(0).getQuantity());
        assertMoney("784.00", res.getGrandTotal());       // 700 + 12 % tax
    }

    @Test
    void cart_flagsLinesThatCanNoLongerBeBought() {
        cart.getItems().add(TestData.line(cart, item, "5"));
        item.setAvailableQuantity(new BigDecimal("3"));   // stock dropped after the item was added

        CartResponse res = service.getCart();

        assertTrue(res.isHasUnavailableItems());
        assertFalse(res.getItems().get(0).isAvailable());
        assertTrue(res.getItems().get(0).getIssue().contains("Only 3"));
    }

    @Test
    void removeItem_andClear() {
        cart.getItems().add(TestData.line(cart, item, "2"));
        assertEquals(0, service.removeItem(10L).getTotalLines());

        cart.getItems().add(TestData.line(cart, item, "2"));
        assertEquals(0, service.clear().getTotalLines());
    }
}
