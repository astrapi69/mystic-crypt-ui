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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;
import io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory;

/**
 * The plugin access over the host's real vault model: a new entry lands in the root group of the
 * tree, the vault is marked changed, and the entry survives the vault's own XML round trip
 * (`quality-checks.md`, round-trip acceptance).
 */
class ModelVaultStateTest
{

	@Test
	@DisplayName("an entry added through the access is in the root group and survives a save and a reopen")
	void anAddedEntrySurvivesTheVaultsRoundTrip()
	{
		ApplicationModelBean model = anOpenVault();
		VaultAccess access = new MarkedEntryAccess("lethenon.wallet", new ModelVaultState(model));
		MysticCryptEntryModelBean wallet = MysticCryptEntryModelBean.builder()
			.title("lethenon wallet (testnet)".toCharArray()).build();
		VaultEntryProperties.replace(wallet, Map.of("lethenon.wallet", "1"), Set.of());

		access.add(wallet);

		assertTrue(model.isDirty(), "the vault is marked changed");
		ApplicationModelBean reopened = VaultXmlCodec.toModel(VaultXmlCodec.toXml(model));
		reopened.setMasterPwFileModelBean(new MasterPwFileModelBean());
		reopened.setSignedIn(true);
		List<MysticCryptEntryModelBean> rootEntries = BaseTreeNodeTransformer
			.getRoot(reopened.getRootTreeAsMap()).getValue().getDefaultContent();
		assertEquals(1, rootEntries.size());
		assertEquals("lethenon wallet (testnet)", new String(rootEntries.get(0).getTitle()));
		assertEquals("1", VaultEntryProperties.valuesOf(rootEntries.get(0)).get("lethenon.wallet"));
		assertEquals(1, new MarkedEntryAccess("lethenon.wallet", new ModelVaultState(reopened))
			.entries().size(), "the reopened vault shows the plugin its entry");
	}

	@Test
	@DisplayName("entries in any group are found, and only the marked ones are shown")
	void findsMarkedEntriesInEveryGroup()
	{
		ApplicationModelBean model = anOpenVault();
		List<MysticCryptEntryModelBean> childEntries = childGroupEntries(model);
		MysticCryptEntryModelBean moved = MysticCryptEntryModelBean.builder().id(UUID.randomUUID())
			.build();
		VaultEntryProperties.replace(moved, Map.of("lethenon.wallet", "1"), Set.of());
		childEntries.add(moved);
		childEntries.add(MysticCryptEntryModelBean.builder().id(UUID.randomUUID()).build());

		List<MysticCryptEntryModelBean> seen = new MarkedEntryAccess("lethenon.wallet",
			new ModelVaultState(model)).entries();

		assertEquals(1, seen.size());
		assertEquals(moved.getId(), seen.get(0).getId());
	}

	private static List<MysticCryptEntryModelBean> childGroupEntries(
		final ApplicationModelBean model)
	{
		for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : model
			.getRootTreeAsMap().values())
		{
			if ("mykeys".equals(node.getValue().getName()))
			{
				return node.getValue().getDefaultContent();
			}
		}
		throw new IllegalStateException("the test vault has no group 'mykeys'");
	}

	private static ApplicationModelBean anOpenVault()
	{
		LongIdGenerator ids = LongIdGenerator.of(0L);
		GenericTreeElement<List<MysticCryptEntryModelBean>> root = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("root").build()
			.setDefaultContent(new ArrayList<>());
		GenericTreeElement<List<MysticCryptEntryModelBean>> child = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("mykeys").build()
			.setDefaultContent(new ArrayList<>());
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> rootNode = BaseTreeNodeFactory
			.initializeTreeNodeWithTreeElement(root, null, ids);
		BaseTreeNodeFactory.initializeTreeNodeWithTreeElement(child, rootNode, ids);
		ApplicationModelBean model = ApplicationModelBean.builder()
			.rootTreeAsMap(BaseTreeNodeTransformer.toKeyMap(rootNode)).build();
		model.setMasterPwFileModelBean(new MasterPwFileModelBean());
		model.setSignedIn(true);
		return model;
	}
}
