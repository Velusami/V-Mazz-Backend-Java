package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Cart;
import com.example.catalog.vmazz.domain.CartItem;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.dto.AddToCartRequest;
import com.example.catalog.vmazz.dto.CartItemResponse;
import com.example.catalog.vmazz.dto.CartResponse;
import com.example.catalog.vmazz.dto.UpdateCartItemRequest;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.CartRepository;
import com.example.catalog.vmazz.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final CurrentUserService currentUser;

    @Transactional
    public CartResponse getCart() {
        AppUser user = currentUser.requireCustomer();
        return toResponse(getOrCreateCart(user));
    }

    /** Adds the item, or increases the quantity when it is already in the cart. */
    @Transactional
    public CartResponse addItem(AddToCartRequest request) {

        AppUser user = currentUser.requireCustomer();
        Cart cart = getOrCreateCart(user);

        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + request.itemId()));
        requirePurchasable(item);
        requireSameCurrency(cart, item);

        CartItem line = findLine(cart, item.getId());
        BigDecimal newQuantity = line == null ? request.quantity() : line.getQuantity().add(request.quantity());
        QuantityRules.validate(item, newQuantity);

        if (line == null) {
            cart.getItems().add(CartItem.builder().cart(cart).item(item).quantity(newQuantity).build());
        } else {
            line.setQuantity(newQuantity);
        }
        return toResponse(cart);
    }

    /** Sets the quantity of a cart line. 0 removes it. */
    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {

        AppUser user = currentUser.requireCustomer();
        Cart cart = getOrCreateCart(user);

        CartItem line = findLine(cart, itemId);
        if (line == null) {
            throw new ResourceNotFoundException("The item is not in your cart");
        }

        if (request.quantity().signum() == 0) {
            cart.getItems().remove(line);
        } else {
            requirePurchasable(line.getItem());
            QuantityRules.validate(line.getItem(), request.quantity());
            line.setQuantity(request.quantity());
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {

        AppUser user = currentUser.requireCustomer();
        Cart cart = getOrCreateCart(user);

        CartItem line = findLine(cart, itemId);
        if (line != null) {
            cart.getItems().remove(line);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clear() {
        AppUser user = currentUser.requireCustomer();
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        return toResponse(cart);
    }

    // ------------------------------------------------------------------

    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUser_Id(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartItem findLine(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    private void requirePurchasable(Item item) {
        if (!isPurchasable(item)) {
            throw new ConflictException(item.getItemName() + " is not available for purchase");
        }
    }

    private boolean isPurchasable(Item item) {
        return item.getStatus() == RecordStatus.ACTIVE
                && item.getCompany().getStatus() == RecordStatus.ACTIVE;
    }

    // A cart is paid in one currency.
    private void requireSameCurrency(Cart cart, Item item) {
        for (CartItem ci : cart.getItems()) {
            if (!ci.getItem().getCurrency().equals(item.getCurrency())) {
                throw new ConflictException("Your cart has items in " + ci.getItem().getCurrency()
                        + ". " + item.getItemName() + " is priced in " + item.getCurrency());
            }
        }
    }

    private CartResponse toResponse(Cart cart) {

        List<CartItemResponse> lines = new ArrayList<>();
        BigDecimal subTotal = Pricing.ZERO;
        BigDecimal taxTotal = Pricing.ZERO;
        String currency = null;
        boolean unavailable = false;

        for (CartItem ci : cart.getItems()) {
            Item item = ci.getItem();

            BigDecimal lineSub = Pricing.lineSubTotal(item.getUnitPrice(), ci.getQuantity());
            BigDecimal lineTax = Pricing.tax(lineSub, item.getTaxPercent());
            String issue = availabilityIssue(item, ci.getQuantity());

            subTotal = subTotal.add(lineSub);
            taxTotal = taxTotal.add(lineTax);
            if (currency == null) {
                currency = item.getCurrency();
            }
            unavailable = unavailable || issue != null;

            lines.add(CartItemResponse.builder()
                    .itemId(item.getId())
                    .itemCode(item.getItemCode())
                    .itemName(item.getItemName())
                    .category(item.getCategory())
                    .uom(item.getUom())
                    .imageUrl(item.getImageUrl())
                    .supplierCompanyId(item.getCompany().getId())
                    .supplierName(item.getCompany().getCompanyName())
                    .unitPrice(item.getUnitPrice())
                    .currency(item.getCurrency())
                    .taxPercent(item.getTaxPercent())
                    .quantity(ci.getQuantity())
                    .lineSubTotal(lineSub)
                    .taxAmount(lineTax)
                    .lineTotal(lineSub.add(lineTax))
                    .availableQuantity(item.getAvailableQuantity())
                    .available(issue == null)
                    .issue(issue)
                    .build());
        }

        return CartResponse.builder()
                .items(lines)
                .totalLines(lines.size())
                .currency(currency)
                .subTotal(subTotal)
                .taxTotal(taxTotal)
                .grandTotal(subTotal.add(taxTotal))
                .hasUnavailableItems(unavailable)
                .build();
    }

    private String availabilityIssue(Item item, BigDecimal quantity) {
        if (!isPurchasable(item)) {
            return "This item is no longer available";
        }
        if (quantity.compareTo(item.getAvailableQuantity()) > 0) {
            return "Only " + QuantityRules.plain(item.getAvailableQuantity()) + " " + item.getUom() + " in stock";
        }
        return null;
    }
}
