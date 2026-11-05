package ek.osnb.demo.coffeeshop.catalog;

import ek.osnb.demo.coffeeshop.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the read-only part of CatalogApi with the real application and H2,
 * against the catalog that CatalogInitData fills in. This is what an AI tool
 * calls, so it is worth testing on its own: a tool is then only a thin wrapper.
 *
 * Other tests change the stock, so these tests never assume a stock number.
 */
@SpringBootTest
class CatalogApiTest {

    @Autowired
    private CatalogApi catalog;

    @Test
    void listsTheCategoriesByName() {
        assertThat(catalog.listCategories())
                .extracting(CategoryInfo::name)
                .containsExactly("Coffee beans", "Equipment", "Ground coffee");
    }

    @Test
    void searchesByNameOrOriginIgnoringCase() {
        assertThat(catalog.searchProducts(null, "KENYA"))
                .extracting(ProductDetailsInfo::name)
                .containsExactly("Kenya Nyeri AA");

        assertThat(catalog.searchProducts(null, "indonesia"))
                .extracting(ProductDetailsInfo::name)
                .containsExactly("French Roast", "Sumatra Mandheling");
    }

    @Test
    void searchesInOneCategory() {
        Long equipment = categoryId("Equipment");

        List<ProductDetailsInfo> products = catalog.searchProducts(equipment, null);

        assertThat(products).hasSize(5);
        assertThat(products).extracting(ProductDetailsInfo::category).containsOnly("Equipment");
    }

    @Test
    void blankTextMatchesEverything() {
        assertThat(catalog.searchProducts(null, "  ")).hasSameSizeAs(catalog.searchProducts(null, null));
    }

    @Test
    void coffeeHasOriginAndRoastLevel() {
        ProductDetailsInfo kenya = catalog.searchProducts(null, "kenya").getFirst();

        ProductDetailsInfo details = catalog.getProductDetails(kenya.id());

        assertThat(details.category()).isEqualTo("Coffee beans");
        assertThat(details.origin()).isEqualTo("Kenya");
        assertThat(details.roastLevel()).isEqualTo("LIGHT");
        assertThat(details.description()).contains("blackcurrant");
    }

    @Test
    void equipmentHasNoOriginOrRoastLevel() {
        ProductDetailsInfo grinder = catalog.searchProducts(null, "hand grinder").getFirst();

        assertThat(grinder.origin()).isNull();
        assertThat(grinder.roastLevel()).isNull();
    }

    @Test
    void unknownProductIsNotFound() {
        assertThatThrownBy(() -> catalog.getProductDetails(999_999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found: 999999");
    }

    private Long categoryId(String name) {
        return catalog.listCategories().stream()
                .filter(category -> category.name().equals(name))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
