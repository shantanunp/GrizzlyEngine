package com.grizzly;

import com.grizzly.format.json.JsonTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests product catalog transformation with category-based display logic.
 * Exercises f-strings, list comprehensions, and chained ternary.
 */
class ProductCatalogTransformTest {

    private static final String TEMPLATE = """
        def mapProducts(INPUT, OUTPUT):
            items = INPUT?.inventory?.items
            OUTPUT["products"] = [
                {
                    "sku": item?.sku,
                    "label": f"{item?.name or ''} - {item?.brand or ''} ({item?.category or ''})",
                    "tier": (
                        "Premium"
                        if (item?.category == "electronics")
                        else "Standard"
                        if (item?.category == "grocery")
                        else "Outlet"
                        if (item?.category == "clearance")
                        else "Regular"
                        if (item?.category == "clothing" or item?.category == "home")
                        else None
                    ),
                    "price": item?.price
                }
                for item in (items or [])
            ]

        def transform(INPUT):
            OUTPUT = {}
            mapProducts(INPUT, OUTPUT)
            return OUTPUT
        """;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getFirstProduct(Map<String, Object> output) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) output.get("products");
        return products.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getProductAt(Map<String, Object> output, int index) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) output.get("products");
        return products.get(index);
    }

    @Nested
    @DisplayName("Electronics tier")
    class ElectronicsTierTests {

        @Test
        @DisplayName("electronics maps to Premium tier")
        void electronicsMapsToPremium() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "LAP-001",
                                "name": "UltraBook Pro",
                                "brand": "TechCorp",
                                "category": "electronics",
                                "price": 999.99
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isEqualTo("Premium");
            assertThat(product.get("label")).isEqualTo("UltraBook Pro - TechCorp (electronics)");
        }
    }

    @Nested
    @DisplayName("Grocery tier")
    class GroceryTierTests {

        @Test
        @DisplayName("grocery maps to Standard tier")
        void groceryMapsToStandard() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "ORG-442",
                                "name": "Organic Oats",
                                "brand": "FarmFresh",
                                "category": "grocery",
                                "price": 5.49
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isEqualTo("Standard");
            assertThat(product.get("label")).isEqualTo("Organic Oats - FarmFresh (grocery)");
        }
    }

    @Nested
    @DisplayName("Clearance tier")
    class ClearanceTierTests {

        @Test
        @DisplayName("clearance maps to Outlet tier")
        void clearanceMapsToOutlet() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "CLR-99",
                                "name": "Last Season Jacket",
                                "brand": "OutdoorGear",
                                "category": "clearance",
                                "price": 29.99
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isEqualTo("Outlet");
        }
    }

    @Nested
    @DisplayName("Clothing and home tier")
    class ClothingHomeTierTests {

        @Test
        @DisplayName("clothing maps to Regular tier")
        void clothingMapsToRegular() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "TSH-101",
                                "name": "Cotton Tee",
                                "brand": "Basics",
                                "category": "clothing",
                                "price": 14.99
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isEqualTo("Regular");
        }

        @Test
        @DisplayName("home maps to Regular tier")
        void homeMapsToRegular() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "LAMP-7",
                                "name": "Desk Lamp",
                                "brand": "BrightIdeas",
                                "category": "home",
                                "price": 34.50
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isEqualTo("Regular");
        }
    }

    @Nested
    @DisplayName("Unmatched category")
    class UnmatchedCategoryTests {

        @Test
        @DisplayName("unknown category returns null tier")
        void unknownCategoryReturnsNull() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "MISC-1",
                                "name": "Mystery Item",
                                "category": "unknown"
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("tier")).isNull();
        }
    }

    @Nested
    @DisplayName("Label with partial data")
    class LabelPartialDataTests {

        @Test
        @DisplayName("handles missing brand and category in label")
        void labelHandlesMissingFields() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "X-1",
                                "name": "Widget"
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            Map<String, Object> product = getFirstProduct(output);

            assertThat(product.get("label")).isEqualTo("Widget -  ()");
        }
    }

    @Nested
    @DisplayName("Multiple items")
    class MultipleItemsTests {

        @Test
        @DisplayName("maps each item with correct tier")
        void multipleItemsEachMappedCorrectly() {
            String input = """
                {
                    "inventory": {
                        "items": [
                            {
                                "sku": "A1",
                                "name": "Phone",
                                "category": "electronics"
                            },
                            {
                                "sku": "B2",
                                "name": "Bread",
                                "category": "grocery"
                            }
                        ]
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);

            assertThat(getProductAt(output, 0).get("tier")).isEqualTo("Premium");
            assertThat(getProductAt(output, 1).get("tier")).isEqualTo("Standard");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("empty items list produces empty products")
        void emptyList() {
            String input = """
                {
                    "inventory": {
                        "items": []
                    }
                }
                """;
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            List<?> products = (List<?>) output.get("products");
            assertThat(products).isEmpty();
        }

        @Test
        @DisplayName("missing inventory produces empty products")
        void missingInventory() {
            String input = "{}";
            Map<String, Object> output = JsonTemplate.compile(TEMPLATE).transformToMap(input);
            List<?> products = (List<?>) output.get("products");
            assertThat(products).isEmpty();
        }
    }
}
