package org.jboss.pnc.bacon.pnc;

import org.apache.commons.lang.reflect.FieldUtils;
import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

class ProductReleaseCliTest {
    @Test
    void testCreateException() throws InterruptedException, IOException, IllegalAccessException {
        ProductReleaseCli.Create create = spy(new ProductReleaseCli.Create());
        File file = new File(ProductReleaseCliTest.class.getClassLoader().getResource("config.yaml").getFile());
        Config.configure(file.getParent(), "config.yaml", "default");
        Config.initialize();

        String version = "1";
        String milestone = "M";
        FieldUtils.writeDeclaredField(create, "productMilestoneId", milestone, true);
        FieldUtils.writeDeclaredField(create, "productReleaseVersion", version, true);

        try (MockedStatic<ProductReleaseCli> mockedStatic = Mockito.mockStatic(ProductReleaseCli.class)) {
            mockedStatic.when(() -> ProductReleaseCli.validateReleaseVersion(milestone, version)).thenReturn(false);
            try {
                create.call();
                fail("No exception thrown");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Product Release version ('1') and milestone ('M') is not valid!"));
            }
        }
    }
}
