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

import java.util.ArrayList;
import java.util.List;

/**
 * TreeNode is a tree node datatype containing a dependency name, a parent, and children. Tree nodes can be printed and
 * a variety of getters and setters are available along with a few methods that classify the dependency.
 */
public class TreeNode {
    private String dependencyName = null;
    private List<TreeNode> children = new ArrayList<>();
    private TreeNode parent = null;

    /**
     * Constructor
     */
    public TreeNode() {
        super();
    }

    /**
     * Constructor
     *
     * @param dependencyName dependency name
     */
    public TreeNode(String dependencyName) {
        super();
        this.dependencyName = dependencyName;
    }

    /**
     * @return whether or not the dependency is productized - does it contain "\.redhat"?
     */
    public boolean isProductized() {
        return dependencyName.contains(".redhat");
    }

    /**
     * @return whether or not the dependency is a jkube group artifact
     */
    public boolean isJkube() {
        return dependencyName.contains("org.eclipse.jkube");
    }

    /**
     * @return whether or not the dependency is a fusesource group artifact
     */
    public boolean isFuseSource() {
        return dependencyName.contains("org.fusesource");
    }

    /**
     * @return whether or not the dependency is a snowdrop group artifact
     */
    public boolean isSnowDrop() {
        return dependencyName.contains("me.snowdrop");
    }

    /**
     * @return whether or not the dependency is camel group artifact
     */
    public boolean isCamelArtifact() {
        return dependencyName.contains("org.apache.camel");
    }

    /**
     * @return whether or not the dependency is camel group artifact
     */
    public boolean isCXF() {
        return dependencyName.contains("org.apache.cxf");
    }

    /**
     * @return whether or not the dependency is org.springframework
     */
    public boolean isSpringBoot() {
        return dependencyName.contains("org.springframework");
    }

    /**
     * Dependency name getter.
     *
     * @return the dependency name
     */
    public String getDependencyName() {
        return dependencyName;
    }

    /**
     * Dependency name setter.
     *
     * @param dependencyName dependency name
     */
    public void setDependencyName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    /**
     * Parent tree node getter.
     *
     * @return parent tree node
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Parent tree node setter.
     *
     * @param parent parent tree node
     */
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * Add a child.
     *
     * @param child child to be added
     */
    public void addChild(TreeNode child) {
        child.setParent(this);
        children.add(child);
    }

    /**
     * Getter for a list of tree node children.
     *
     * @return list of tree node children
     */
    public List<TreeNode> getChildren() {
        return children;
    }

    /**
     * Print the tree node and its children.
     *
     * @param tn parent tree node to print
     * @param depth the depth to distinguish dependency levels
     */
    public void printTree(TreeNode tn, int depth) {
        if (tn != null) {
            List<TreeNode> childNodes = tn.getChildren();
            for (int i = 0; i < childNodes.size(); i++) {
                for (int j = 0; j < depth + 1; j++) {
                    System.out.print("====");
                }
                System.out.println(childNodes.get(i).getDependencyName());
                printTree(childNodes.get(i), (depth + 1));
            }
        }
        return;
    }

    /**
     * Print the tree node and its children.
     *
     * @param tn the tree node to print
     */
    public void printTree(TreeNode tn) {
        System.out.println(tn.getDependencyName());
        printTree(tn, 0);
    }
}
