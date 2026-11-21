package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.dto.ItemRequest;
import com.example.catalog.vmazz.dto.ItemResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemRepository itemRepository;
    @Mock CurrentUserService currentUser;

    @InjectMocks ItemService service;

    Company company = TestData.company(1L);

    @BeforeEach
    void setUp() {
        when(currentUser.requireCompanyUser()).thenReturn(TestData.admin(2L, company));
    }

    private ItemRequest request(String code, Uom uom, String price, String mrp, String stock) {
        return new ItemRequest(code, "Basmati Rice", ItemCategory.GROCERIES, "Rice", "Brand",
                "Description", "Long grain", "1006", uom,
                new BigDecimal(price), mrp == null ? null : new BigDecimal(mrp), null,
                new BigDecimal("5"), new BigDecimal(stock), null, null, null);
    }

    @Test
    void create_blankCode_generatesCodeFromId_andAppliesDefaults() {
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(12L);
            return i;
        });

        ItemResponse res = service.create(request(null, Uom.KG, "145", "165", "250.5"));

        assertEquals("ITM000012", res.getItemCode());
        assertEquals("INR", res.getCurrency());
        assertEquals(RecordStatus.ACTIVE, res.getStatus());
        assertEquals(new BigDecimal("145.00"), res.getUnitPrice());
        assertEquals(new BigDecimal("1.000"), res.getMinOrderQuantity());
        assertEquals(1L, res.getSupplierCompanyId());
        assertEquals(12, res.getDiscountPercent());           // (165 - 145) / 165 = 12 %
        assertTrue(res.isInStock());
    }

    @Test
    void create_givenCode_isUpperCased_andMustBeUnique() {
        when(itemRepository.existsByItemCodeIgnoreCase("RICE-01")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(request(" rice-01 ", Uom.KG, "145", null, "10")));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void create_rejectsMrpBelowPrice_andFractionsForCountableUnits() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(null, Uom.KG, "145", "100", "10")));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(null, Uom.PCS, "145", null, "10.5")));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void update_ofAnotherCompanysItem_isNotFound() {
        when(itemRepository.findByIdAndCompany_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, request(null, Uom.KG, "145", null, "10")));
    }

    @Test
    void update_changesFields_butNeverTheItemCode() {
        Item existing = TestData.item(5L, company, "100", "5", "10");
        when(itemRepository.findByIdAndCompany_Id(5L, 1L)).thenReturn(Optional.of(existing));

        ItemResponse res = service.update(5L, request("DIFFERENT", Uom.KG, "150", null, "20"));

        assertEquals("ITM5", res.getItemCode());
        assertEquals(new BigDecimal("150.00"), res.getUnitPrice());
        assertEquals("Basmati Rice", res.getItemName());
    }
}
