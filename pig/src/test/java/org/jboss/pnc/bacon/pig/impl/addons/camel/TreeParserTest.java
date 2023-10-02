package org.jboss.pnc.bacon.pig.impl.addons.camel;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
public class TreeParserTest {

    @Test
    public void testTreeParser() {
        TreeParser treeparser = new TreeParser();

        try {
            File file = new File("src/test/resources/camel-build-log.txt");
            assertTrue(file.exists(), "Could not find file " + file.getName());
            ArrayList<TreeNode> al = treeparser.parse("src/test/resources/camel-build-log.txt");
            assertTrue(al.size() == 1, "Expected to find 1 TreeNode, found " + al.size());
            TreeNode tn = (TreeNode) al.get(0);

            ArrayList<String> dependencies = treeparser.collectFirstLevelDependencies(al);
            assertTrue(
                    dependencies.size() == 26,
                    "Expected to find 26 dependencies, # of dependencies was " + dependencies.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLargerScaleTreeParser() {
        TreeParser treeparser = new TreeParser();

        try {
            File file = new File("src/test/resources/camel-build-log.txt");
            assertTrue(file.exists(), "Could not find file " + file.getName());
            ArrayList<TreeNode> al = treeparser.parse("src/test/resources/camel-build-log-2.txt");
            assertTrue(al.size() == 169, "Expected to find 169 tree nodes, found " + al.size());
            TreeNode tn = (TreeNode) al.get(0);

            ArrayList<String> dependencies = treeparser.collectFirstLevelDependencies(al);
            assertTrue(
                    dependencies.size() == 3806,
                    "Expected to find 3854 dependencies, # of dependencies was " + dependencies.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
