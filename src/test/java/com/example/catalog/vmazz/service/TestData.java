package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Address;
import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Cart;
import com.example.catalog.vmazz.domain.CartItem;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.domain.Uom;

import java.math.BigDecimal;

/** Small builders shared by the service tests. */
final class TestData {

    private TestData() { }

    static Company company(long id) {
        return Company.builder().id(id).companyCode("CMP" + id).companyName("Supplier " + id)
                .email("s" + id + "@x.com").status(RecordStatus.ACTIVE).build();
    }

    static AppUser customer(long id) {
        return AppUser.builder().id(id).role(Role.CUSTOMER).fullName("Buyer " + id)
                .email("buyer" + id + "@x.com").passwordHash("x").status(RecordStatus.ACTIVE).build();
    }

    static AppUser admin(long id, Company company) {
        return AppUser.builder().id(id).role(Role.ADMIN).company(company).fullName("Admin " + id)
                .email("admin" + id + "@x.com").passwordHash("x").status(RecordStatus.ACTIVE).build();
    }

    static Item item(long id, Company company, String price, String taxPercent, String stock) {
        return Item.builder()
                .id(id).company(company).itemCode("ITM" + id).itemName("Item " + id)
                .category(ItemCategory.GOODS).uom(Uom.PCS)
                .unitPrice(new BigDecimal(price)).currency("INR").taxPercent(new BigDecimal(taxPercent))
                .availableQuantity(new BigDecimal(stock)).minOrderQuantity(BigDecimal.ONE)
                .status(RecordStatus.ACTIVE).build();
    }

    static CartItem line(Cart cart, Item item, String quantity) {
        return CartItem.builder().cart(cart).item(item).quantity(new BigDecimal(quantity)).build();
    }

    static Address address() {
        return Address.builder().addressLine1("1 Main Street").city("Chennai").state("Tamil Nadu")
                .postalCode("600001").country("India").build();
    }
}
