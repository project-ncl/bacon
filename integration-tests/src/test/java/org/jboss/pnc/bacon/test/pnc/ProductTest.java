package org.jboss.pnc.bacon.test.pnc;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import org.jboss.pnc.bacon.test.AbstractTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.bacon.test.Endpoints.PRODUCT;
import org.jboss.pnc.dto.Product;
import org.jboss.pnc.dto.response.Page;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author jbrazdil
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductTest extends AbstractTest {
    private static final String PRODUCT_NAME_PREFIX = "BT New Product Name ";

    private String productId;

    @Test
    @Order(1)
    public void shouldCreateProduct() throws JsonProcessingException {

        final String suffix = getRandomString();
        final String productName = PRODUCT_NAME_PREFIX + suffix;
        final String abbreviation = "BT-NPN-" + suffix;
        final String description = "*";

        Product response = Product.builder().id("666").name(productName).abbreviation(abbreviation).description(description)
                .build();
        wmock.creation(PRODUCT, response);

        Product product = executeAndDeserialize(Product.class, "pnc", "product", "create", productName, "--abbreviation",
                abbreviation, "--description", description);

        assertThat(product.getName()).isEqualTo(productName);
        assertThat(product.getAbbreviation()).isEqualTo(abbreviation);
        assertThat(product.getId()).isNotBlank();
        productId = product.getId();
    }

    @Test
    @Order(2)
    public void shouldGetProduct() throws JsonProcessingException {
        Assumptions.assumeTrue(productId != null);

        Product response = Product.builder().id(productId).name(PRODUCT_NAME_PREFIX + " suffix").build();

        wmock.get(PRODUCT, response);

        Product product = executeAndDeserialize(Product.class, "pnc", "product", "get", productId);

        assertThat(product.getId()).isEqualTo(productId);
        assertThat(product.getName()).startsWith(PRODUCT_NAME_PREFIX);
    }

    @Test
    @Order(2)
    public void shouldListProducts() throws JsonProcessingException {
        Assumptions.assumeTrue(productId != null);

        Product response = Product.builder().id(productId).name(PRODUCT_NAME_PREFIX + "suffix").build();
        Page<Product> responsePage = new Page<>(0, 50, 1, Collections.singleton(response));
        wmock.list(PRODUCT, responsePage);

        Product[] products = executeAndDeserialize(Product[].class, "pnc", "product", "list");

        assertThat(products).extracting(Product::getId).contains(productId);
    }

    @Test
    @Order(3)
    @Disabled(value = "NCL-5509")
    public void shouldUpdateProduct() throws JsonProcessingException, InterruptedException {
        Assumptions.assumeTrue(productId != null);

        Product response = Product.builder().id(productId).name(PRODUCT_NAME_PREFIX + "suffix").build();
        Product updatedResponse = Product.builder().id(productId).name(PRODUCT_NAME_PREFIX + "suffix updated").build();
        wmock.update(PRODUCT, response, updatedResponse);

        Product originalProduct = executeAndDeserialize(Product.class, "pnc", "product", "get", productId);

        String originalName = originalProduct.getName();
        String newName = originalName + " updated";

        execute("pnc", "product", "update", productId, "--name", newName);
        Product refreshedProduct = executeAndDeserialize(Product.class, "pnc", "product", "get", productId);

        assertThat(refreshedProduct.getName()).isEqualTo(newName);
    }

}
