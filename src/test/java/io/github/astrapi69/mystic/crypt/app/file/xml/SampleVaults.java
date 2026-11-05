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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The vault the release probes write and read, and the one line per entry both sides print, so a
 * comparison is a comparison of the same text (#402, #490). The probes in
 * {@code src/test/resources/compat} print the same line shape from inside a release.
 * <p>
 * The entries live where the application keeps them: in their group's element, the default content
 * of a {@link GenericTreeElement} in {@code rootTreeAsMap}, a root group and a group "mykeys" below
 * it. Up to #757 they were put into {@code dataOfNodes}, a field no code of the application fills,
 * so a comparison proved that field's round trip and not the entries'.
 */
final class SampleVaults
{

	private SampleVaults()
	{
	}

	/**
	 * Two entries: one with every field a release has - umlauts, an attachment, a custom property,
	 * an icon - and one with almost nothing
	 *
	 * @param addition
	 *            what to add to the first entry, for the fields only newer releases have
	 * @return the model
	 */
	static ApplicationModelBean aVaultWith(final Consumer<MysticCryptEntryModelBean> addition)
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).userName("account holder".toCharArray())
			.password(TestPasswords.throwawayChars()).url("https://bank.example.org".toCharArray())
			.notes("Grüße, with umlauts".toCharArray()).keePassIconIndex(57)
			.resources(new ArrayList<>(List.of(FileContentInfo.builder().name("codes.txt")
				.content("the recovery codes".getBytes(StandardCharsets.UTF_8)).build())))
			.properties(new ArrayList<>(List.of(KeyValuePair.<String, String> builder()
				.key("TOTP seed").value("JBSWY3DPEHPK3PXP").build())))
			.build();
		addition.accept(entry);
		MysticCryptEntryModelBean second = MysticCryptEntryModelBean.builder()
			.title("the mail".toCharArray()).password(TestPasswords.throwawayChars()).build();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(group("root", entry)).leaf(false).build();
		root.addChild(
			BaseTreeNode.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
				.id(2L).value(group("mykeys", second)).parent(root).leaf(false).build());
		return ApplicationModelBean.builder().rootTreeAsMap(BaseTreeNodeTransformer.toKeyMap(root))
			.lastId(2L).build();
	}

	private static GenericTreeElement<List<MysticCryptEntryModelBean>> group(final String name,
		final MysticCryptEntryModelBean entry)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).build();
		element.setDefaultContent(new ArrayList<>(List.of(entry)));
		return element;
	}

	/**
	 * The entries of the given group, as the tree panel reads them
	 *
	 * @param model
	 *            the vault
	 * @param group
	 *            the group's name
	 * @return its entries
	 */
	static List<MysticCryptEntryModelBean> entriesOf(final ApplicationModelBean model,
		final String group)
	{
		return model.getRootTreeAsMap().values().stream()
			.filter(node -> group.equals(node.getValue().getName()))
			.map(node -> contentOf(node.getValue())).findFirst().orElseThrow();
	}

	/**
	 * Saves the model through this build's own password save path
	 *
	 * @param model
	 *            the vault
	 * @param directory
	 *            where to write it
	 * @param password
	 *            the master password, copied and not modified
	 * @return the file written
	 */
	static File save(final ApplicationModelBean model, final File directory, final char[] password)
	{
		File vault = new File(directory, "written-by-this-build.mcrdb");
		model.setMasterPwFileModelBean(credentialsFor(vault, password));
		return ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);
	}

	/**
	 * Opens a vault through this build's own password sign-in path
	 *
	 * @param vault
	 *            the file
	 * @param password
	 *            the master password, copied and not modified
	 * @return the model read
	 */
	static ApplicationModelBean open(final File vault, final char[] password)
	{
		return ApplicationXmlFileReader
			.readApplicationFileWithPassword(credentialsFor(vault, password));
	}

	/**
	 * One line per entry, in the shape the probes print, taken from the groups as the tree panel
	 * reads them
	 *
	 * @param model
	 *            the vault
	 * @return the lines, sorted
	 */
	static List<String> entryLines(final ApplicationModelBean model)
	{
		return model.getRootTreeAsMap().values().stream()
			.flatMap(node -> contentOf(node.getValue()).stream()
				.map(entry -> "ENTRY group=" + node.getValue().getName() + " title="
					+ text(entry.getTitle()) + " userName=" + text(entry.getUserName())
					+ " password=" + text(entry.getPassword()) + " url=" + text(entry.getUrl())
					+ " notes=" + text(entry.getNotes()) + " properties="
					+ entry.getProperties().size() + " attachments=" + entry.getResources().size()))
			.sorted().toList();
	}

	private static List<MysticCryptEntryModelBean> contentOf(
		final GenericTreeElement<List<MysticCryptEntryModelBean>> element)
	{
		return element.getDefaultContent() == null ? List.of() : element.getDefaultContent();
	}

	private static MasterPwFileModelBean credentialsFor(final File vault, final char[] password)
	{
		return MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password.clone())
			.withMasterPw(true).withKeyFile(false).build();
	}

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}
}
