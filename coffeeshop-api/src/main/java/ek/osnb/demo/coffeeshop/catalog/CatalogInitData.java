package ek.osnb.demo.coffeeshop.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Fills an empty catalog with categories and products when the application starts.
 * Does nothing if there already are products, so data survives a restart.
 *
 * Lives in the catalog package, so the catalog stays self-contained:
 * a new feature that needs test data gets its own InitData class.
 */
@Component
class CatalogInitData implements CommandLineRunner {

    private final CategoryRepository categories;
    private final ProductRepository products;

    CatalogInitData(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }

        Category beans = categories.save(Category.create("Coffee beans"));
        Category ground = categories.save(Category.create("Ground coffee"));
        Category equipment = categories.save(Category.create("Equipment"));

        // ---------- Coffee beans (250 g) ----------
        coffee(beans, "Ethiopia Yirgacheffe", "Ethiopia", RoastLevel.LIGHT, "119.00", 40,
                "Floral and bright, with notes of jasmine, bergamot and lemon. 250 g whole beans.");
        coffee(beans, "Kenya Nyeri AA", "Kenya", RoastLevel.LIGHT, "135.00", 25,
                "Juicy blackcurrant and grapefruit with a sparkling acidity. 250 g whole beans.");
        coffee(beans, "Colombia Huila", "Colombia", RoastLevel.MEDIUM, "99.00", 60,
                "Caramel, red apple and milk chocolate. A balanced everyday coffee. 250 g whole beans.");
        coffee(beans, "Brazil Cerrado", "Brazil", RoastLevel.MEDIUM, "89.00", 80,
                "Nutty and sweet with low acidity. Great as espresso or in a French press. 250 g whole beans.");
        coffee(beans, "Guatemala Antigua", "Guatemala", RoastLevel.MEDIUM, "109.00", 35,
                "Cocoa, orange peel and a hint of spice. 250 g whole beans.");
        coffee(beans, "Sumatra Mandheling", "Indonesia", RoastLevel.DARK, "115.00", 30,
                "Earthy and full-bodied, with dark chocolate and cedar. 250 g whole beans.");
        coffee(beans, "House Espresso Blend", "Brazil, Colombia, Ethiopia", RoastLevel.DARK, "95.00", 100,
                "Our espresso blend: syrupy body, dark chocolate and a long finish. 250 g whole beans.");

        // ---------- Ground coffee (250 g) ----------
        coffee(ground, "Filter Blend", "Colombia, Brazil", RoastLevel.MEDIUM, "85.00", 50,
                "Medium ground for drip and pour-over. Smooth and sweet. 250 g ground.");
        coffee(ground, "Decaf Colombia", "Colombia", RoastLevel.MEDIUM, "99.00", 20,
                "Sugarcane-processed decaf with notes of toffee and red fruit. 250 g ground.");
        coffee(ground, "French Roast", "Brazil, Indonesia", RoastLevel.DARK, "85.00", 3,
                "Coarse ground for the French press. Smoky and bold. 250 g ground.");

        // ---------- Equipment (no origin or roast level) ----------
        equipment(equipment, "Hand Grinder", "449.00", 12,
                "Steel burr hand grinder with 30 grind settings, from espresso to French press.");
        equipment(equipment, "Pour-over Dripper", "199.00", 15,
                "Ceramic cone dripper for one or two cups. Uses size 02 paper filters.");
        equipment(equipment, "French Press 1 L", "279.00", 8,
                "Borosilicate glass and stainless steel. Makes 8 cups.");
        equipment(equipment, "Paper Filters (100 pcs)", "49.00", 200,
                "Size 02 paper filters for the pour-over dripper.");
        equipment(equipment, "Gooseneck Kettle", "499.00", 0,
                "Electric kettle with temperature control and a precise spout. Back in stock soon.");
    }

    private void coffee(
            Category category,
            String name,
            String origin,
            RoastLevel roastLevel,
            String price,
            int stock,
            String description) {

        products.save(Product.create(
                name, description, category, origin, roastLevel, new BigDecimal(price), stock));
    }

    private void equipment(
            Category category,
            String name,
            String price,
            int stock,
            String description) {

        products.save(Product.create(
                name, description, category, null, null, new BigDecimal(price), stock));
    }
}
