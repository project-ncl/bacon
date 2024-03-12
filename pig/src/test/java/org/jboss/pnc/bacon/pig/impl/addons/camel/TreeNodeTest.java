package org.jboss.pnc.bacon.pig.impl.addons.camel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class TreeNodeTest {
    @Test
    public void testTreeNodes() {
        TreeNode tn = new TreeNode("foo");
        TreeNode bar = new TreeNode("bar");
        TreeNode foobar = new TreeNode("foobar");
        TreeNode secondLevel = new TreeNode("secondlevel");

        bar.addChild(secondLevel);

        tn.addChild(bar);
        tn.addChild(foobar);

        assertTrue(tn.getChildren().size() == 2);
    }
}
