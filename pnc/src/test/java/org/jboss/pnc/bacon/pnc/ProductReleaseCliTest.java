package org.jboss.pnc.bacon.pnc;

import org.aesh.command.CommandException;
import org.aesh.command.invocation.CommandInvocation;
import org.apache.commons.lang.reflect.FieldUtils;
import org.jboss.pnc.bacon.common.exception.FatalException;
import org.jboss.pnc.bacon.config.Config;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ProductReleaseCliTest {
    @Test
    public void testCreateException()
            throws CommandException, InterruptedException, IOException, IllegalAccessException {
        ProductReleaseCli.Create create = spy((new ProductReleaseCli()).new Create());
        CommandInvocation commandInvocation = mock(CommandInvocation.class);
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
                create.execute(commandInvocation);
                fail("No exception thrown");
            } catch (FatalException e) {
                assertTrue(e.getMessage().contains("Product Release version ('1') and milestone ('M') is not valid!"));
            }
        }
    }
}
