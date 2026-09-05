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
import static org.junit.jupiter.api.Assertions.assertNull;
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


		AtomicLong idCounter = new AtomicLong(0);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = KeePassTreeConverter
			.toTreeNode(root, null, idCounter::incrementAndGet);

		for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : rootNode
			.getChildren())
		{
		}

		assertEquals(3, rootNode.getChildren().size(), "root should have exactly 3 children");
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> generalNode = rootNode
			.getChildren().stream().filter(n -> "General".equals(name(n))).findFirst()
			.orElseThrow();
		assertEquals(1, generalNode.getChildren().size(),
			"General should have exactly 1 child (Internet)");
		assertTrue(generalNode.getChildren().stream().anyMatch(n -> "Internet".equals(name(n))));

		DefaultMutableTreeNode swingRoot = BaseTreeNodeFactory.newDefaultMutableTreeNode(rootNode);
		assertEquals(3, swingRoot.getChildCount(), "swing root should have exactly 3 children");
		DefaultMutableTreeNode swingGeneral = (DefaultMutableTreeNode)swingRoot.getChildAt(0);
		assertEquals(1, swingGeneral.getChildCount(),
			"swing General node should have exactly 1 child");
	}

	@Test
	public void testGroupMetadataIsPreservedOnImportAndIconOnExport()
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup root = database.getRootGroup();
		SimpleGroup general = database.newGroup("General");
		general.setIcon(new org.linguafranca.pwdb.kdbx.simple.SimpleIcon(3));
		root.addGroup(general);

		AtomicLong idCounter = new AtomicLong(0);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = KeePassTreeConverter
			.toTreeNode(root, null, idCounter::incrementAndGet);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> generalNode = rootNode
			.getChildren().iterator().next();

		assertEquals(general.getUuid(),
			generalNode.getValue().getProperties().get(KeePassTreeConverter.KEEPASS_UUID_PROPERTY));
		assertEquals(3, generalNode.getValue().getProperties()
			.get(KeePassTreeConverter.KEEPASS_ICON_INDEX_PROPERTY));
		assertEquals(false, generalNode.getValue().getProperties()
			.get(KeePassTreeConverter.KEEPASS_RECYCLE_BIN_PROPERTY));

		SimpleDatabase exportDatabase = new SimpleDatabase();
		SimpleGroup exportedGroup = KeePassTreeConverter.toSimpleGroup(exportDatabase, generalNode,
			exportDatabase.getRootGroup());
		assertEquals(3, exportedGroup.getIcon().getIndex());
	}

	/**
	 * The icon index alone stays invisible: the tree cell renderer of this application draws
	 * {@code GenericTreeElement.iconPath}, so an imported group only shows its KeePass icon once
	 * the index is turned into that path (#206). The text flag matters just as much - the renderer
	 * blanks a node's name as soon as it has an icon and is not marked as carrying text.
	 */
	@Test
	public void testGroupIconIndexBecomesTheIconTheRendererDraws()
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup root = database.getRootGroup();
		SimpleGroup general = database.newGroup("General");
		general.setIcon(new org.linguafranca.pwdb.kdbx.simple.SimpleIcon(48));
		root.addGroup(general);

		AtomicLong idCounter = new AtomicLong(0);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = KeePassTreeConverter
			.toTreeNode(root, null, idCounter::incrementAndGet);
		GenericTreeElement<List<MysticCryptEntryModelBean>> general48 = rootNode.getChildren()
			.iterator().next().getValue();

		assertEquals("img/keepass/C48_Folder.png", general48.getIconPath());
		assertTrue(general48.isWithText(),
			"a node with an icon keeps its name only with this flag");
		assertEquals("General", general48.getName());
	}

	/**
	 * A KeePass database may point at an icon this application does not ship - a custom icon lives
	 * beyond the built in set. Such a group keeps its index for the way back out, but must not be
	 * given a path to an image that is not there
	 */
	@Test
	public void testAGroupWithAnIconOutsideTheShippedSetGetsNoIconPath()
	{
		SimpleDatabase database = new SimpleDatabase();
		SimpleGroup root = database.getRootGroup();
		SimpleGroup general = database.newGroup("General");
		general.setIcon(new org.linguafranca.pwdb.kdbx.simple.SimpleIcon(200));
		root.addGroup(general);

		AtomicLong idCounter = new AtomicLong(0);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = KeePassTreeConverter
			.toTreeNode(root, null, idCounter::incrementAndGet);
		GenericTreeElement<List<MysticCryptEntryModelBean>> unknownIcon = rootNode.getChildren()
			.iterator().next().getValue();

		assertNull(unknownIcon.getIconPath());
		assertEquals(200,
			unknownIcon.getProperties().get(KeePassTreeConverter.KEEPASS_ICON_INDEX_PROPERTY));
		assertEquals("General", unknownIcon.getName());
	}

	private String name(
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node)
	{
		return node.getValue().getName();
	}


	/**
	 * A group node whose content list is null and which has no children at all - the two guards in
	 * {@link KeePassTreeConverter#toSimpleGroup} that a tree built by hand can easily hit, for
	 * instance a freshly created empty folder.
	 */
	@Test
	public void testToSimpleGroupWithoutEntriesAndWithoutChildren()
	{
		SimpleDatabase database = new SimpleDatabase();
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = new GenericTreeElement<>();
		element.setName("Empty folder");
		element.setDefaultContent(null);
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(element).build();

		SimpleGroup group = KeePassTreeConverter.toSimpleGroup(database, node,
			database.getRootGroup());

		assertEquals("Empty folder", group.getName());
		assertTrue(group.getEntries().isEmpty(), "a group without content must have no entries");
		assertTrue(group.getGroups().isEmpty(), "a group without children must have no subgroups");
	}

	@Test
	public void testToSimpleGroupWithEntriesAndAChild()
	{
		SimpleDatabase database = new SimpleDatabase();
		GenericTreeElement<List<MysticCryptEntryModelBean>> childElement = new GenericTreeElement<>();
		childElement.setName("Child folder");
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(2L)
			.value(childElement).build();

		GenericTreeElement<List<MysticCryptEntryModelBean>> element = new GenericTreeElement<>();
		element.setName("Parent folder");
		element.setDefaultContent(
			List.of(MysticCryptEntryModelBean.builder().title("An entry").build()));
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(element).build();
		node.addChild(child);

		SimpleGroup group = KeePassTreeConverter.toSimpleGroup(database, node,
			database.getRootGroup());

		assertEquals(1, group.getEntries().size(), "the content must become an entry");
		assertEquals("An entry", group.getEntries().get(0).getTitle());
		assertEquals(1, group.getGroups().size(), "the child node must become a subgroup");
		assertEquals("Child folder", group.getGroups().get(0).getName());
	}

}
