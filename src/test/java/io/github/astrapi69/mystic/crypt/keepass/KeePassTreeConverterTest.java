/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.keepass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.jupiter.api.Test;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory;

public class KeePassTreeConverterTest
{

	@Test
	public void testToTreeNodeStructure()
	{
		// Root
		// ├── General
		// │ └── Internet
		// ├── Communication
		// └── Backup
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup root = database.getRootGroup();

		SimpleGroup general = database.newGroup("General");
		root.addGroup(general);
		SimpleGroup internet = database.newGroup("Internet");
		general.addGroup(internet);

		SimpleGroup communication = database.newGroup("Communication");
		root.addGroup(communication);

		SimpleGroup backup = database.newGroup("Backup");
		root.addGroup(backup);

		System.out.println("root.getGroups() names: "
			+ root.getGroups().stream().map(SimpleGroup::getName).toList());
		System.out.println("general.getGroups() names: "
			+ general.getGroups().stream().map(SimpleGroup::getName).toList());

		AtomicLong idCounter = new AtomicLong(0);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = KeePassTreeConverter
			.toTreeNode(root, null, idCounter::incrementAndGet);

		System.out.println(
			"rootNode children: " + rootNode.getChildren().stream().map(this::name).toList());
		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : rootNode
			.getChildren())
		{
			System.out.println("  " + name(child) + " children: "
				+ child.getChildren().stream().map(this::name).toList());
		}

		assertEquals(3, rootNode.getChildren().size(), "root should have exactly 3 children");
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> generalNode = rootNode
			.getChildren().stream().filter(n -> "General".equals(name(n))).findFirst()
			.orElseThrow();
		assertEquals(1, generalNode.getChildren().size(),
			"General should have exactly 1 child (Internet)");
		assertTrue(generalNode.getChildren().stream().anyMatch(n -> "Internet".equals(name(n))));

		DefaultMutableTreeNode swingRoot = BaseTreeNodeFactory.newDefaultMutableTreeNode(rootNode);
		printSwingTree(swingRoot, 0);
		assertEquals(3, swingRoot.getChildCount(), "swing root should have exactly 3 children");
		DefaultMutableTreeNode swingGeneral = (DefaultMutableTreeNode)swingRoot.getChildAt(0);
		assertEquals(1, swingGeneral.getChildCount(),
			"swing General node should have exactly 1 child");
	}

	private void printSwingTree(DefaultMutableTreeNode node, int depth)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> treeNode = (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>)node
			.getUserObject();
		System.out.println(
			"  ".repeat(depth) + name(treeNode) + " (childCount=" + node.getChildCount() + ")");
		for (int i = 0; i < node.getChildCount(); i++)
		{
			printSwingTree((DefaultMutableTreeNode)node.getChildAt(i), depth + 1);
		}
	}

	private String name(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		return node.getValue().getName();
	}

}
