/*
 * JBoss, Home of Professional Open Source. Copyright 2022 Red Hat, Inc., and individual
 * contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jboss.pnc.bacon.pig.impl.addons.camel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Cunningham, tcunning@redhat.com <br>
 *         Date: 2023-10-02
 *
 *         TreeParser is a class that parses mvn:dependency-tree stacks that it finds in logs and allows you to find
 *         unique first level dependencies with a specific parentage.
 *
 *         This is useful because we are interested in finding the number of direct dependencies of org.apache.camel
 *         that are productized and the number of direct dependencies of org.apache.camel that still need to be.
 *
 *         TreeParser is used by CamelRuntimeDependenciesToAlignTree to produce a log with percentages, but it can also
 *         be used on a PNC build log that has been downloaded.
 *
 *         Example : % java -cp cli/target/bacon.jar org.jboss.pnc.bacon.pig.impl.addons.camel.TreeParser
 *         A3PHDS4K2MYAG_jkube-1.13.1_SUCCESS.txt
 *
 */
public class TreeParser {
    private static final Logger log = LoggerFactory.getLogger(TreeParser.class);

    private int indexOfPattern(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    private String parseDependency(String line) {
        String dependency = line.replace("[INFO] ", "");
        dependency = dependency.replaceFirst("([+-|\\s]+\\s+)", "");
        return dependency;
    }

    public ArrayList parse(String fileName) throws Exception {
        File parseFile = new File(fileName);
        if (!parseFile.exists()) {
            throw new RuntimeException("Could not find file " + fileName);
        }

        ArrayList<String> results = new ArrayList<String>();
        BufferedReader fr = null;
        fr = new BufferedReader(new FileReader(parseFile));
        String line = null;
        while ((line = fr.readLine()) != null) {
            results.add(line);
        }
        fr.close();
        return parse(results);
    }

    public ArrayList parse(List<String> fileContents) throws Exception {
        ArrayList treeList = new ArrayList();

        TreeNode parent = new TreeNode();
        try {
            boolean rootNode = false;
            boolean parsing = false;
            TreeNode current = new TreeNode();
            TreeNode currentParent = current;
            int currentindex = 0;
            for (String line : fileContents) {

                if (line.contains("Downloading") || (line.contains("Downloaded"))) {
                    continue;
                }

                if ((line.contains("maven-dependency-plugin:") || line.contains("dependency:")) && line.contains("tree")) {
                    rootNode = true;
                    parsing = true;
                    continue;
                }

                if (line.trim().endsWith("[INFO]")) {
                    if (parsing) {
                        treeList.add(parent);
                        parent = new TreeNode();
                        current = new TreeNode();
                        currentParent = current;
                        currentindex = 0;
                        parsing = false;
                        continue;
                    }
                }

                String dep = parseDependency(line);
                int index = indexOfPattern(line, "- [A-Za-z]");

                if (rootNode) {
                    rootNode = false;
                    current.setDependencyName(dep);
                    current.setParent(parent);
                    currentParent = current;
                    parent.addChild(current);
                    continue;
                } else if (parsing) {
                    if (index > currentindex) {
                        currentParent = current;
                        TreeNode newnode = new TreeNode(dep);
                        newnode.setParent(currentParent);
                        currentParent.addChild(newnode);
                        currentindex = index;
                        current = newnode;
                    } else if (index == currentindex) {
                        TreeNode newnode = new TreeNode(dep);
                        newnode.setParent(currentParent);
                        currentParent.addChild(newnode);
                        currentindex = index;
                        current = newnode;
                    } else {
                        int diff = (currentindex - index) / 3;
                        for (int i = 0; i < diff; i++) {
                            if (currentParent.getParent() != null) {
                                currentParent = currentParent.getParent();
                            }
                        }
                        TreeNode newnode = new TreeNode(dep);
                        newnode.setParent(currentParent);
                        currentParent.addChild(newnode);
                        current = newnode;
                        currentindex = index;
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return treeList;
    }

    public ArrayList<String> collectFirstLevelDependencies(ArrayList<TreeNode> treenodes) {
        ArrayList<String> result = new ArrayList<String>();
        for (TreeNode tn : treenodes) {
            if (tn != null) {
                ArrayList<TreeNode> childNodes = (ArrayList<TreeNode>) tn.getChildren();
                for (int i = 0; i < childNodes.size(); i++) {
                    TreeNode node = childNodes.get(i);
                    // Check that the parent is a org.apache.camel/org.apache.cxf artifact that contains "redhat"
                    // We are parsing for first level dependencies of camel / cxf artifacts
                    if ((node.getParent() != null) && (node.getParent().getDependencyName() != null)
                            && (node.getParent().isCamelArtifact() || node.getParent().isCXF() || node.isFuseSource()
                                    || node.getParent().isJkube() || node.getParent().isSnowDrop())
                            && (node.getParent().getDependencyName().contains("redhat"))) {
                        // If the we're looking at a camel or a CXF artifact, we don't want to add unsupported
                        // artifacts to our count
                        if ((node.isCamelArtifact() || node.isCXF())) {
                            if (node.isProductized()) {
                                result.add(childNodes.get(i).getDependencyName());
                            }
                            // Anything other than a camel or CXF artifact, we want to add it to the count at this point
                            // Exclude spring-boot, we're not productizing it
                        } else {
                            if (!node.isSpringBoot())
                                result.add(childNodes.get(i).getDependencyName());
                        }
                    }
                    ArrayList<TreeNode> nodeArrayList = new ArrayList<TreeNode>();
                    nodeArrayList.add(childNodes.get(i));

                    for (String s : collectFirstLevelDependencies(nodeArrayList)) {
                        result.add(s);
                    }
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {
        TreeParser treeparser = new TreeParser();
        try {
            ArrayList<TreeNode> al = treeparser.parse(args[0]);
            System.out.println("Size of tree node list : " + al.size());
            TreeNode tn = (TreeNode) al.get(0);

            ArrayList<String> dependencies = treeparser.collectFirstLevelDependencies(al);

            Set<String> uniqueDependencies = new HashSet<String>(dependencies);

            int counter = 0;
            int camelCounter = 0;
            int test = 0;
            int uniques = 0;
            for (String prod : uniqueDependencies) {

                if (prod.endsWith(":test")) {
                    test++;
                    continue;
                }

                if (prod.contains(".redhat")) {
                    counter++;
                }

                if (prod.contains("org.apache.camel")) {
                    camelCounter++;
                }

                if ((args.length > 1) && ("--list".equals(args[1]))) {
                    System.out.println("dependency " + prod);
                }

                uniques++;

            }
            System.out.println("File parsing : " + args[0]);
            System.out.println("Size : " + dependencies.size());
            System.out.println("Number of Camel dependencies : " + camelCounter);
            System.out.println("Number of test dependencies (not counted) : " + test);
            System.out.println("Number of unique productized dependencies: " + counter);
            System.out.println("Number of unique dependencies: " + uniques);
            float pct = (float) counter / uniques;
            System.out.println("Build from source percentage : " + String.format("%.2f", pct * 100) + "%\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
