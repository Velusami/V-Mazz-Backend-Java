package com.example.catalog.vmazz.config;

import com.example.catalog.vmazz.domain.Address;
import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.repository.AppUserRepository;
import com.example.catalog.vmazz.repository.CompanyRepository;
import com.example.catalog.vmazz.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Creates demo data on an empty database: one supplier company with an admin and a sub user,
 * one customer, and a small item master across Food, Groceries, Goods and Raw Material.
 * Turn off with vmazz.seed.enabled=false. All values are test data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmazz.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ItemRepository itemRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${vmazz.seed.password:Password@123}")
    private String seedPassword;

    @Override
    public void run(ApplicationArguments args) {

        if (userRepository.count() > 0) {
            return;
        }

        Company company = companyRepository.save(Company.builder()
                .companyCode("CMP00001")
                .companyName("v-Mazz Demo Supplies Pvt Ltd")
                .email("supplier@vmazz.com")
                .phone("+91 44 4000 1000")
                .address(Address.builder()
                        .addressLine1("12, Industrial Estate Road")
                        .city("Chennai").state("Tamil Nadu").postalCode("600032").country("India")
                        .build())
                .status(RecordStatus.ACTIVE)
                .build());

        user(company, Role.ADMIN, "Demo Admin", "admin@vmazz.com", null);
        user(company, Role.SUB_ADMIN, "Demo Sub Admin", "subadmin@vmazz.com", null);
        user(null, Role.CUSTOMER, "Demo Buyer", "buyer@vmazz.com", "Demo Foods Pvt Ltd");

        // ---- Groceries ----
        item(company, "Basmati Rice Premium 25 kg Bag", ItemCategory.GROCERIES, "Rice & Grains", "v-Mazz Select",
                Uom.BAG, "1850", "2100", "5", "400", "1", "1006",
                "Long grain, aged 2 years, broken grains max 3%, 25 kg HDPE bag");
        item(company, "Toor Dal (Split Pigeon Pea)", ItemCategory.GROCERIES, "Pulses", "v-Mazz Select",
                Uom.KG, "145", "165", "5", "2000", "5", "0713",
                "Unpolished, uniform size, moisture max 12%");
        item(company, "Refined Sunflower Oil", ItemCategory.GROCERIES, "Edible Oil", "v-Mazz Select",
                Uom.LITRE, "135", "150", "5", "1500", "10", "1512",
                "Refined, filtered, free fatty acid max 0.1%");
        item(company, "Sugar (M30 Grade)", ItemCategory.GROCERIES, "Staples", "v-Mazz Select",
                Uom.KG, "44", "50", "5", "5000", "25", "1701",
                "Crystal sugar M30, ICUMSA 100-150");
        item(company, "Iodized Salt", ItemCategory.GROCERIES, "Staples", "v-Mazz Select",
                Uom.KG, "22", "28", "0", "3000", "10", "2501",
                "Free flow, iodine 15 ppm minimum");

        // ---- Food ----
        item(company, "Veg Biryani Ready Meal 350 g", ItemCategory.FOOD, "Ready to Eat", "v-Mazz Kitchen",
                Uom.PACK, "95", "120", "12", "600", "12", "2106",
                "Vegetarian, shelf life 9 months, 350 g retort pack");
        item(company, "Frozen Paneer Cubes", ItemCategory.FOOD, "Dairy", "v-Mazz Kitchen",
                Uom.KG, "320", "360", "12", "150", "2", "0406",
                "Store at -18 C, shelf life 6 months, 1 kg pack");
        item(company, "Mixed Fruit Juice 1 L", ItemCategory.FOOD, "Beverages", "v-Mazz Kitchen",
                Uom.BOTTLE, "85", "99", "12", "800", "12", "2009",
                "No added preservatives, shelf life 6 months");

        // ---- Additional grocery and fresh food range for the storefront ----
        item(company, "Fresh Tomato 1 kg", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.KG, "38", "45", "0", "900", "1", "0702", "Fresh red tomatoes, sorted and packed daily");
        item(company, "Fresh Onion 1 kg", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.KG, "42", "50", "0", "1200", "1", "0703", "Medium-size onions, dry outer skin");
        item(company, "Potato 1 kg", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.KG, "32", "40", "0", "1000", "1", "0701", "Fresh table potatoes, medium size");
        item(company, "Carrot 500 g", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.PACK, "35", "42", "0", "500", "1", "0706", "Fresh orange carrots, washed and packed");
        item(company, "Green Capsicum 500 g", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.PACK, "48", "55", "0", "400", "1", "0709", "Fresh green capsicum");
        item(company, "Banana Robusta 1 dozen", ItemCategory.GROCERIES, "Fresh Fruits", "Farm Fresh", Uom.DOZEN, "55", "65", "0", "600", "1", "0803", "Fresh ripe robusta bananas");
        item(company, "Apple Shimla 1 kg", ItemCategory.GROCERIES, "Fresh Fruits", "Farm Fresh", Uom.KG, "160", "190", "0", "350", "1", "0808", "Crisp Shimla apples, graded");
        item(company, "Cucumber 500 g", ItemCategory.GROCERIES, "Fresh Vegetables", "Farm Fresh", Uom.PACK, "24", "30", "0", "450", "1", "0707", "Fresh green cucumber");
        item(company, "Milk 1 L", ItemCategory.FOOD, "Dairy", "Aavin", Uom.BOTTLE, "32", "36", "0", "500", "1", "0401", "Pasteurised toned milk");
        item(company, "Curd 500 g", ItemCategory.FOOD, "Dairy", "Aavin", Uom.PACK, "38", "42", "0", "300", "1", "0403", "Fresh set curd");
        item(company, "Whole Wheat Bread 400 g", ItemCategory.FOOD, "Bakery", "Modern", Uom.PACK, "42", "48", "5", "250", "1", "1905", "Whole wheat sliced bread");
        item(company, "Orange Juice 1 L", ItemCategory.FOOD, "Beverages", "Real", Uom.BOTTLE, "110", "125", "12", "200", "1", "2009", "Orange fruit beverage");
        item(company, "Instant Noodles Pack of 4", ItemCategory.FOOD, "Snacks", "Mazz Foods", Uom.PACK, "56", "60", "12", "700", "1", "1902", "Instant noodles multipack");

        // ---- Goods ----
        item(company, "A4 Copier Paper 75 GSM (500 sheets)", ItemCategory.GOODS, "Stationery", "v-Mazz Office",
                Uom.PACK, "235", "260", "12", "900", "10", "4802",
                "A4, 75 GSM, brightness 95%, 500 sheets per ream");
        item(company, "Nitrile Safety Gloves (Box of 100)", ItemCategory.GOODS, "Safety", "v-Mazz Safety",
                Uom.BOX, "420", "499", "18", "300", "2", "4015",
                "Powder free, size M, 4 mil, 100 gloves per box");
        item(company, "LED Tube Light 20 W", ItemCategory.GOODS, "Electrical", "v-Mazz Lite",
                Uom.PCS, "180", "240", "12", "700", "10", "9405",
                "20 W, 4 ft, 6500 K cool daylight, 2000 lumens");

        // ---- Consumer electronics for the Zepto-style mixed catalogue ----
        item(company, "Apple iPhone 16 128 GB", ItemCategory.GOODS, "Mobiles", "Apple", Uom.PCS, "69900", "79900", "18", "25", "1", "8517", "128 GB smartphone, 5G, warranty as per manufacturer");
        item(company, "Samsung Galaxy S25 256 GB", ItemCategory.GOODS, "Mobiles", "Samsung", Uom.PCS, "74999", "82999", "18", "20", "1", "8517", "256 GB 5G smartphone");
        item(company, "HP 15s Laptop i5 16 GB", ItemCategory.GOODS, "Laptops", "HP", Uom.PCS, "58990", "64990", "18", "12", "1", "8471", "Intel Core i5, 16 GB RAM, 512 GB SSD");
        item(company, "Dell Inspiron 14 Laptop", ItemCategory.GOODS, "Laptops", "Dell", Uom.PCS, "62990", "69990", "18", "10", "1", "8471", "14 inch laptop, 16 GB RAM, 512 GB SSD");

        // ---- Raw material ----
        item(company, "Mild Steel Sheet 2 mm (IS 2062)", ItemCategory.RAW_MATERIAL, "Metals", null,
                Uom.KG, "68", null, "18", "12000", "100", "7208",
                "IS 2062 E250, thickness 2 mm, sheet 1250 x 2500 mm");
        item(company, "HDPE Granules (Injection Grade)", ItemCategory.RAW_MATERIAL, "Polymers", null,
                Uom.KG, "112", null, "18", "8000", "100", "3901",
                "Melt flow index 8 g/10 min, natural, 25 kg bags");
        item(company, "Electrolytic Copper Rod 8 mm", ItemCategory.RAW_MATERIAL, "Metals", null,
                Uom.KG, "810", null, "18", "2000", "50", "7408",
                "99.9% purity, 8 mm diameter, coil");
        item(company, "Cotton Yarn 30s Count", ItemCategory.RAW_MATERIAL, "Textile", null,
                Uom.KG, "245", null, "5", "3000", "25", "5205",
                "Combed cotton, 30s count, cone packing");

        log.info("v-Mazz demo data created. Logins (password '{}'): admin@vmazz.com, subadmin@vmazz.com, buyer@vmazz.com",
                seedPassword);
    }

    private void user(Company company, Role role, String name, String email, String organization) {
        userRepository.save(AppUser.builder()
                .company(company)
                .role(role)
                .fullName(name)
                .email(email)
                .organizationName(organization)
                .passwordHash(passwordEncoder.encode(seedPassword))
                .status(RecordStatus.ACTIVE)
                .build());
    }

    private void item(Company company, String name, ItemCategory category, String subCategory, String brand,
                      Uom uom, String price, String mrp, String tax, String qty, String minQty,
                      String hsn, String specification) {

        Item saved = itemRepository.save(Item.builder()
                .company(company)
                .itemCode("TMP-" + name.hashCode())
                .itemName(name)
                .category(category)
                .subCategory(subCategory)
                .brand(brand)
                .specification(specification)
                .hsnCode(hsn)
                .uom(uom)
                .unitPrice(new BigDecimal(price).setScale(2))
                .mrp(mrp == null ? null : new BigDecimal(mrp).setScale(2))
                .currency("INR")
                .taxPercent(new BigDecimal(tax).setScale(2))
                .availableQuantity(new BigDecimal(qty).setScale(3))
                .minOrderQuantity(new BigDecimal(minQty).setScale(3))
                .imageUrl(seedImage(category, subCategory, name))
                .status(RecordStatus.ACTIVE)
                .createdBy("seed")
                .build());

        saved.setItemCode(String.format("ITM%06d", saved.getId()));
        itemRepository.save(saved);
    }
    private String seedImage(ItemCategory category, String subCategory, String name) {
        String n = name.toLowerCase();
        if (n.contains("iphone") || n.contains("galaxy") || n.contains("mobile")) return "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=640";
        if (n.contains("laptop")) return "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=640";
        if (n.contains("tomato")) return "https://images.unsplash.com/photo-1546094096-0df4bcaaa337?w=640";
        if (n.contains("onion")) return "https://images.unsplash.com/photo-1508747703725-719777637510?w=640";
        if (n.contains("potato")) return "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=640";
        if (n.contains("carrot")) return "https://images.unsplash.com/photo-1445282768818-728615cc910a?w=640";
        if (n.contains("capsicum")) return "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=640";
        if (n.contains("banana")) return "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=640";
        if (n.contains("apple")) return "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=640";
        if (n.contains("cucumber")) return "https://images.unsplash.com/photo-1604977042946-1eecc30f269e?w=640";
        if (n.contains("rice")) return "https://images.unsplash.com/photo-1516684732162-798a0062be99?w=640";
        if (n.contains("dal") || n.contains("pigeon pea")) return "https://images.unsplash.com/photo-1515543904379-3d757afe72e4?w=640";
        if (n.contains("oil")) return "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=640";
        if (n.contains("sugar") || n.contains("salt")) return "https://images.unsplash.com/photo-1518843875459-f738682238a6?w=640";
        if (n.contains("milk") || n.contains("paneer") || n.contains("curd")) return "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=640";
        if (n.contains("bread")) return "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=640";
        if (n.contains("juice")) return "https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=640";
        if (n.contains("biryani") || n.contains("noodles")) return "https://images.unsplash.com/photo-1547592180-85f173990554?w=640";
        if (category == ItemCategory.GROCERIES) return "https://images.unsplash.com/photo-1542838132-92c53300491e?w=640";
        if (category == ItemCategory.FOOD) return "https://images.unsplash.com/photo-1547592180-85f173990554?w=640";
        return "https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=640";
    }

}
