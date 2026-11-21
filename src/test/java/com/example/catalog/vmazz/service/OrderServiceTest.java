package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Cart;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.domain.PaymentMethod;
import com.example.catalog.vmazz.domain.PurchaseOrder;
import com.example.catalog.vmazz.domain.PurchaseOrderItem;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.dto.AddressDto;
import com.example.catalog.vmazz.dto.CheckoutRequest;
import com.example.catalog.vmazz.dto.CheckoutResponse;
import com.example.catalog.vmazz.dto.OrderResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.repository.CartRepository;
import com.example.catalog.vmazz.repository.ItemRepository;
import com.example.catalog.vmazz.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock CurrentUserService currentUser;
    @Mock CartRepository cartRepository;
    @Mock ItemRepository itemRepository;
    @Mock PurchaseOrderRepository orderRepository;

    @InjectMocks OrderService service;

    AppUser customer = TestData.customer(5L);
    Company supplier1 = TestData.company(1L);
    Company supplier2 = TestData.company(2L);

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private CheckoutRequest checkoutRequest() {
        return new CheckoutRequest("Ravi Kumar", "9876543210",
                new AddressDto("12 Anna Salai", null, "Chennai", "Tamil Nadu", "600002", "India"),
                null, PaymentMethod.COD, null, "BR-77", "Call before delivery");
    }

    private void customerWithCart(Cart cart) {
        when(currentUser.requireCustomer()).thenReturn(customer);
        when(cartRepository.findByUser_Id(5L)).thenReturn(Optional.of(cart));
    }

    // ---------------- checkout ----------------

    @Test
    void checkout_createsOnePurchaseOrderPerSupplier_deductsStock_andEmptiesTheCart() {
        Item a = TestData.item(10L, supplier1, "100", "12", "100");
        Item b = TestData.item(11L, supplier2, "50", "5", "50");
        Cart cart = Cart.builder().id(1L).user(customer).build();
        cart.getItems().add(TestData.line(cart, a, "3"));
        cart.getItems().add(TestData.line(cart, b, "4"));

        customerWithCart(cart);
        when(itemRepository.findWithLockById(10L)).thenReturn(Optional.of(a));
        when(itemRepository.findWithLockById(11L)).thenReturn(Optional.of(b));
        AtomicLong ids = new AtomicLong(100);
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(ids.incrementAndGet());
            return po;
        });

        CheckoutResponse res = service.checkout(checkoutRequest());

        assertEquals(2, res.orders().size());
        assertMoney("546.00", res.grandTotal());                    // 336 + 210

        OrderResponse first = res.orders().get(0);
        assertEquals(String.format("PO-%d-000101", LocalDate.now().getYear()), first.getPoNumber());
        assertEquals(OrderStatus.PLACED, first.getStatus());
        assertEquals("Supplier 1", first.getSupplierName());
        assertEquals("INR", first.getCurrency());
        assertMoney("300.00", first.getSubTotal());
        assertMoney("36.00", first.getTaxTotal());
        assertMoney("336.00", first.getGrandTotal());
        assertEquals(1, first.getItems().size());
        assertEquals("ITM10", first.getItems().get(0).getItemCode());
        assertMoney("3", first.getItems().get(0).getQuantity());
        assertEquals("Ravi Kumar", first.getContactName());
        assertEquals("BR-77", first.getBuyerReference());
        assertEquals("Chennai", first.getShippingAddress().city());
        assertEquals("Chennai", first.getBillingAddress().city());   // billing defaults to shipping

        assertEquals(String.format("PO-%d-000102", LocalDate.now().getYear()), res.orders().get(1).getPoNumber());
        assertMoney("210.00", res.orders().get(1).getGrandTotal());  // 200 + 5 % tax

        assertMoney("97", a.getAvailableQuantity());
        assertMoney("46", b.getAvailableQuantity());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void checkout_itemsOfOneSupplier_shareOnePurchaseOrder() {
        Item a = TestData.item(10L, supplier1, "100", "0", "100");
        Item b = TestData.item(11L, supplier1, "20", "0", "100");
        Cart cart = Cart.builder().id(1L).user(customer).build();
        cart.getItems().add(TestData.line(cart, a, "1"));
        cart.getItems().add(TestData.line(cart, b, "2"));

        customerWithCart(cart);
        when(itemRepository.findWithLockById(10L)).thenReturn(Optional.of(a));
        when(itemRepository.findWithLockById(11L)).thenReturn(Optional.of(b));
        when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(1L);
            return po;
        });

        CheckoutResponse res = service.checkout(checkoutRequest());

        assertEquals(1, res.orders().size());
        assertEquals(2, res.orders().get(0).getItems().size());
        assertEquals(1, res.orders().get(0).getItems().get(0).getLineNumber());
        assertEquals(2, res.orders().get(0).getItems().get(1).getLineNumber());
        assertMoney("140.00", res.grandTotal());
    }

    @Test
    void checkout_emptyCart_isRejected() {
        customerWithCart(Cart.builder().id(1L).user(customer).build());

        assertThrows(IllegalArgumentException.class, () -> service.checkout(checkoutRequest()));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_whenStockIsNoLongerEnough_placesNothing() {
        Item a = TestData.item(10L, supplier1, "100", "12", "2");     // only 2 left
        Cart cart = Cart.builder().id(1L).user(customer).build();
        cart.getItems().add(TestData.line(cart, a, "3"));

        customerWithCart(cart);
        when(itemRepository.findWithLockById(10L)).thenReturn(Optional.of(a));

        assertThrows(ConflictException.class, () -> service.checkout(checkoutRequest()));

        verify(orderRepository, never()).save(any());
        assertMoney("2", a.getAvailableQuantity());
        assertEquals(1, cart.getItems().size());
    }

    // ---------------- cancel / status ----------------

    private PurchaseOrder placedOrder(Item item, String quantity) {
        PurchaseOrder po = PurchaseOrder.builder()
                .id(1L).poNumber("PO-2026-000001").customer(customer).supplierCompany(supplier1)
                .status(OrderStatus.PLACED).orderDate(LocalDateTime.now()).paymentMethod(PaymentMethod.COD)
                .contactName("Ravi").contactPhone("9876543210")
                .shippingAddress(TestData.address()).billingAddress(TestData.address())
                .currency("INR").subTotal(new BigDecimal("400.00")).taxTotal(new BigDecimal("0.00"))
                .grandTotal(new BigDecimal("400.00")).build();
        po.getItems().add(PurchaseOrderItem.builder()
                .purchaseOrder(po).item(item).lineNumber(1).itemCode(item.getItemCode())
                .itemName(item.getItemName()).category(ItemCategory.GOODS).uom(Uom.PCS)
                .quantity(new BigDecimal(quantity)).unitPrice(new BigDecimal("100.00"))
                .taxPercent(new BigDecimal("0.00")).lineSubTotal(new BigDecimal("400.00"))
                .taxAmount(new BigDecimal("0.00")).lineTotal(new BigDecimal("400.00")).build());
        return po;
    }

    @Test
    void cancelMyOrder_whilePlaced_restoresStock() {
        Item item = TestData.item(10L, supplier1, "100", "0", "10");
        PurchaseOrder po = placedOrder(item, "4");
        when(currentUser.requireCustomer()).thenReturn(customer);
        when(orderRepository.findByIdAndCustomer_Id(1L, 5L)).thenReturn(Optional.of(po));
        when(itemRepository.findWithLockById(10L)).thenReturn(Optional.of(item));

        OrderResponse res = service.cancelMyOrder(1L);

        assertEquals(OrderStatus.CANCELLED, res.getStatus());
        assertMoney("14", item.getAvailableQuantity());
    }

    @Test
    void cancelMyOrder_onceConfirmed_isRejected() {
        Item item = TestData.item(10L, supplier1, "100", "0", "10");
        PurchaseOrder po = placedOrder(item, "4");
        po.setStatus(OrderStatus.CONFIRMED);
        when(currentUser.requireCustomer()).thenReturn(customer);
        when(orderRepository.findByIdAndCustomer_Id(1L, 5L)).thenReturn(Optional.of(po));

        assertThrows(ConflictException.class, () -> service.cancelMyOrder(1L));
        assertMoney("10", item.getAvailableQuantity());
    }

    @Test
    void updateStatus_followsTheAllowedFlow() {
        Item item = TestData.item(10L, supplier1, "100", "0", "10");
        PurchaseOrder po = placedOrder(item, "4");
        when(currentUser.requireCompanyUser()).thenReturn(TestData.admin(2L, supplier1));
        when(orderRepository.findByIdAndSupplierCompany_Id(1L, 1L)).thenReturn(Optional.of(po));

        assertThrows(ConflictException.class, () -> service.updateStatus(1L, OrderStatus.SHIPPED));

        assertEquals(OrderStatus.CONFIRMED, service.updateStatus(1L, OrderStatus.CONFIRMED).getStatus());
        assertEquals(OrderStatus.PACKED, service.updateStatus(1L, OrderStatus.PACKED).getStatus());
        assertEquals(OrderStatus.SHIPPED, service.updateStatus(1L, OrderStatus.SHIPPED).getStatus());
        assertEquals(OrderStatus.DELIVERED, service.updateStatus(1L, OrderStatus.DELIVERED).getStatus());
        assertThrows(ConflictException.class, () -> service.updateStatus(1L, OrderStatus.CANCELLED));
    }

    @Test
    void updateStatus_toCancelled_putsTheStockBack() {
        Item item = TestData.item(10L, supplier1, "100", "0", "10");
        PurchaseOrder po = placedOrder(item, "4");
        when(currentUser.requireCompanyUser()).thenReturn(TestData.admin(2L, supplier1));
        when(orderRepository.findByIdAndSupplierCompany_Id(1L, 1L)).thenReturn(Optional.of(po));
        when(itemRepository.findWithLockById(10L)).thenReturn(Optional.of(item));

        service.updateStatus(1L, OrderStatus.CANCELLED);

        assertMoney("14", item.getAvailableQuantity());
    }
}
