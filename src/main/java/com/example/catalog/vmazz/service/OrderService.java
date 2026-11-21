package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Cart;
import com.example.catalog.vmazz.domain.CartItem;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.domain.PurchaseOrder;
import com.example.catalog.vmazz.domain.PurchaseOrderItem;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.dto.CheckoutRequest;
import com.example.catalog.vmazz.dto.CheckoutResponse;
import com.example.catalog.vmazz.dto.OrderResponse;
import com.example.catalog.vmazz.dto.OrderSummaryResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.CartRepository;
import com.example.catalog.vmazz.repository.ItemRepository;
import com.example.catalog.vmazz.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Checkout turns the cart into purchase orders (one per supplier company),
 * deducts stock, and clears the cart - all in one transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PLACED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PACKED, OrderStatus.CANCELLED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final CurrentUserService currentUser;
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final PurchaseOrderRepository orderRepository;

    // ------------------------------------------------------------------
    // Checkout
    // ------------------------------------------------------------------

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {

        AppUser customer = currentUser.requireCustomer();

        Cart cart = cartRepository.findByUser_Id(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }

        // Lock the items in id order (no deadlocks) and re-check stock against the locked rows.
        List<CartItem> lines = new ArrayList<>(cart.getItems());
        lines.sort(Comparator.comparing(ci -> ci.getItem().getId()));

        Map<Long, Item> locked = new LinkedHashMap<>();
        String currency = null;

        for (CartItem line : lines) {
            Item item = itemRepository.findWithLockById(line.getItem().getId())
                    .orElseThrow(() -> new ConflictException("An item in your cart is no longer available"));

            if (item.getStatus() != RecordStatus.ACTIVE || item.getCompany().getStatus() != RecordStatus.ACTIVE) {
                throw new ConflictException(item.getItemName() + " is no longer available. Remove it from your cart.");
            }
            QuantityRules.validate(item, line.getQuantity());

            if (currency == null) {
                currency = item.getCurrency();
            } else if (!currency.equals(item.getCurrency())) {
                throw new ConflictException("Your cart has items in different currencies");
            }
            locked.put(item.getId(), item);
        }

        // One purchase order per supplier company.
        Map<Long, List<CartItem>> bySupplier = new LinkedHashMap<>();
        for (CartItem line : lines) {
            Long supplierId = locked.get(line.getItem().getId()).getCompany().getId();
            bySupplier.computeIfAbsent(supplierId, k -> new ArrayList<>()).add(line);
        }

        List<OrderResponse> orders = new ArrayList<>();
        BigDecimal grandTotal = Pricing.ZERO;

        for (List<CartItem> group : bySupplier.values()) {

            Company supplier = locked.get(group.get(0).getItem().getId()).getCompany();
            PurchaseOrder po = newOrder(customer, supplier, request, currency);

            BigDecimal subTotal = Pricing.ZERO;
            BigDecimal taxTotal = Pricing.ZERO;
            int lineNumber = 1;

            for (CartItem line : group) {
                Item item = locked.get(line.getItem().getId());

                BigDecimal lineSub = Pricing.lineSubTotal(item.getUnitPrice(), line.getQuantity());
                BigDecimal lineTax = Pricing.tax(lineSub, item.getTaxPercent());

                po.getItems().add(PurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .item(item)
                        .lineNumber(lineNumber++)
                        .itemCode(item.getItemCode())
                        .itemName(item.getItemName())
                        .category(item.getCategory())
                        .specification(item.getSpecification())
                        .hsnCode(item.getHsnCode())
                        .uom(item.getUom())
                        .quantity(line.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .taxPercent(item.getTaxPercent())
                        .lineSubTotal(lineSub)
                        .taxAmount(lineTax)
                        .lineTotal(lineSub.add(lineTax))
                        .build());

                item.setAvailableQuantity(item.getAvailableQuantity().subtract(line.getQuantity()));

                subTotal = subTotal.add(lineSub);
                taxTotal = taxTotal.add(lineTax);
            }

            po.setSubTotal(subTotal);
            po.setTaxTotal(taxTotal);
            po.setGrandTotal(subTotal.add(taxTotal));

            PurchaseOrder saved = orderRepository.save(po);
            saved.setPoNumber(String.format("PO-%d-%06d", saved.getOrderDate().getYear(), saved.getId()));

            grandTotal = grandTotal.add(saved.getGrandTotal());
            orders.add(OrderResponse.from(saved));
            log.info("Purchase order {} placed by {} for supplier {}",
                    saved.getPoNumber(), customer.getEmail(), supplier.getCompanyCode());
        }

        cart.getItems().clear();

        return new CheckoutResponse(orders, grandTotal, "Order placed successfully");
    }

    // ------------------------------------------------------------------
    // Customer: my orders
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> myOrders(Pageable pageable) {
        AppUser customer = currentUser.requireCustomer();
        return PageResponse.of(
                orderRepository.findByCustomer_Id(customer.getId(), pageable),
                OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(Long id) {
        AppUser customer = currentUser.requireCustomer();
        return OrderResponse.from(orderRepository.findByIdAndCustomer_Id(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id)));
    }

    /** A customer can cancel while the supplier has not confirmed the order yet. */
    @Transactional
    public OrderResponse cancelMyOrder(Long id) {

        AppUser customer = currentUser.requireCustomer();
        PurchaseOrder po = orderRepository.findByIdAndCustomer_Id(id, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (po.getStatus() != OrderStatus.PLACED) {
            throw new ConflictException("Only orders in PLACED status can be cancelled. "
                    + "Please contact the supplier for order " + po.getPoNumber());
        }

        po.setStatus(OrderStatus.CANCELLED);
        restoreStock(po);
        return OrderResponse.from(po);
    }

    // ------------------------------------------------------------------
    // Company users: orders received
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> companyOrders(OrderStatus status, Pageable pageable) {

        AppUser user = currentUser.requireCompanyUser();
        Long companyId = user.getCompany().getId();

        Page<PurchaseOrder> page = status == null
                ? orderRepository.findBySupplierCompany_Id(companyId, pageable)
                : orderRepository.findBySupplierCompany_IdAndStatus(companyId, status, pageable);

        return PageResponse.of(page, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse companyOrder(Long id) {
        AppUser user = currentUser.requireCompanyUser();
        return OrderResponse.from(findForCompany(id, user));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {

        AppUser user = currentUser.requireCompanyUser();
        PurchaseOrder po = findForCompany(id, user);

        if (!TRANSITIONS.get(po.getStatus()).contains(newStatus)) {
            throw new ConflictException("Order " + po.getPoNumber() + " cannot move from "
                    + po.getStatus() + " to " + newStatus);
        }

        po.setStatus(newStatus);
        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(po);
        }
        return OrderResponse.from(po);
    }

    // ------------------------------------------------------------------

    private PurchaseOrder findForCompany(Long id, AppUser user) {
        return orderRepository.findByIdAndSupplierCompany_Id(id, user.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private PurchaseOrder newOrder(AppUser customer, Company supplier, CheckoutRequest r, String currency) {

        return PurchaseOrder.builder()
                .poNumber("TMP-" + UUID.randomUUID())              // replaced once the id is known
                .customer(customer)
                .supplierCompany(supplier)
                .status(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now())
                .requiredDeliveryDate(r.requiredDeliveryDate())
                .paymentMethod(r.paymentMethod())
                .buyerReference(blankToNull(r.buyerReference()))
                .notes(blankToNull(r.notes()))
                .contactName(r.contactName().trim())
                .contactPhone(r.contactPhone().trim())
                .shippingAddress(r.shippingAddress().toEntity())
                .billingAddress(r.billingAddress() != null
                        ? r.billingAddress().toEntity()
                        : r.shippingAddress().toEntity())
                .currency(currency)
                .subTotal(Pricing.ZERO)
                .taxTotal(Pricing.ZERO)
                .grandTotal(Pricing.ZERO)
                .build();
    }

    // Give the stock of a cancelled order back to the item master.
    private void restoreStock(PurchaseOrder po) {

        List<PurchaseOrderItem> lines = new ArrayList<>(po.getItems());
        lines.sort(Comparator.comparing(l -> l.getItem().getId()));

        for (PurchaseOrderItem line : lines) {
            itemRepository.findWithLockById(line.getItem().getId())
                    .ifPresent(item -> item.setAvailableQuantity(
                            item.getAvailableQuantity().add(line.getQuantity())));
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
