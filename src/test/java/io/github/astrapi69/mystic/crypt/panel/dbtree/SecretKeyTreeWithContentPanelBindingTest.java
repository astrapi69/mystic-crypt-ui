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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Tests that the one entry component of {@link SecretKeyTreeWithContentPanel} - the combo box the
 * move dialog offers the possible new parents in - is bound to
 * {@link SecretKeyTreeWithContentPanelModel}: the node that is picked there is the node the move
 * reads out of the model afterwards.
 * <p>
 * The move itself sits behind a modal dialog and is driven end to end by the UI test that moves a
 * node under another one; what is proven here is that the value the move works with travels through
 * the model instead of being read back out of the widget.
 */
class SecretKeyTreeWithContentPanelBindingTest
{

	/** The node whose possible new parents this test picks from */
	private static final String NODE_TO_MOVE = "firstGrandChild";

	private BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root;

	private SecretKeyTreeWithContentPanel newPanel()
	{
		List<MysticCryptEntryModelBean> entries = new ArrayList<>();
		entries.add(MysticCryptEntryModelBean.builder().userName("bound").build());
		root = TestBaseTreeNodeFactory.initializeTestGenericTreeNodeElement(entries, entries,
			LongIdGenerator.of(0L));
		return new SecretKeyTreeWithContentPanel(BaseModel.of(root));
	}

	private List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targetsFor(
		String nodeName)
	{
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> nodeToMove = root
			.traverse().stream()
			.filter(treeNode -> nodeName.equals(treeNode.getValue().getName())).findFirst()
			.orElseThrow(() -> new IllegalStateException("no node named '" + nodeName + "'"));
		return SecretKeyTreeWithContentPanel.possibleMoveTargets(nodeToMove);
	}

	/**
	 * A dialog that is confirmed without touching the combo box must move the node under the target
	 * the combo box shows, so the model carries the first offered target from the start
	 */
	@Test
	void theMoveDialogStartsWithTheFirstOfferedTargetInTheModel()
	{
		SecretKeyTreeWithContentPanel panel = newPanel();
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = targetsFor(
			NODE_TO_MOVE);

		JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> targetChooser = panel
			.newMoveTargetChooser(targets);

		assertSame(targets.get(0), targetChooser.getSelectedItem());
		assertSame(targets.get(0), panel.getPanelModelObject().getMoveTarget(),
			"the target the dialog opens on is not the one the move would read");
	}

	/**
	 * Picking another target is what the move reads afterwards - out of the model, not out of the
	 * combo box
	 */
	@Test
	void pickingATargetPutsItIntoTheModelTheMoveReadsFrom()
	{
		SecretKeyTreeWithContentPanel panel = newPanel();
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = targetsFor(
			NODE_TO_MOVE);
		assertTrue(1 < targets.size(), "this node needs more than one possible new parent");
		JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> targetChooser = panel
			.newMoveTargetChooser(targets);

		targetChooser.setSelectedIndex(targets.size() - 1);

		assertSame(targets.get(targets.size() - 1), panel.getPanelModelObject().getMoveTarget(),
			"the picked target did not reach the model, so the move would use another node");
	}

	/**
	 * The combo box keeps the name the UI test looks it up by and offers exactly the targets the
	 * move rules allow, in the order they were computed in
	 */
	@Test
	void theTargetChooserKeepsItsNameAndOffersEveryPossibleTarget()
	{
		SecretKeyTreeWithContentPanel panel = newPanel();
		List<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> targets = targetsFor(
			NODE_TO_MOVE);

		JMComboBox<BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>, ?> targetChooser = panel
			.newMoveTargetChooser(targets);

		assertEquals("cmbMoveTarget", targetChooser.getName());
		assertEquals(targets.size(), targetChooser.getItemCount());
		for (int index = 0; index < targets.size(); index++)
		{
			assertSame(targets.get(index), targetChooser.getItemAt(index));
		}
	}
}
