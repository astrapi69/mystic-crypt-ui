/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.app.file.xml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The vault the release probes write and read, and the one line per entry both sides print, so a
 * comparison is a comparison of the same text (#402, #490). The probes in
 * {@code src/test/resources/compat} print the same line shape from inside a release
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
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, new ArrayList<>(List.of(entry)));
		dataOfNodes.put(2L, new ArrayList<>(List.of(second)));
		return ApplicationModelBean.builder().dataOfNodes(dataOfNodes).lastId(2L).build();
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
	 * One line per entry, in the shape the probes print
	 *
	 * @param model
	 *            the vault
	 * @return the lines, in node order
	 */
	static List<String> entryLines(final ApplicationModelBean model)
	{
		List<String> lines = new ArrayList<>();
		model.getDataOfNodes()
			.forEach((node, entries) -> entries.forEach(entry -> lines.add("ENTRY node=" + node
				+ " title=" + text(entry.getTitle()) + " userName=" + text(entry.getUserName())
				+ " password=" + text(entry.getPassword()) + " url=" + text(entry.getUrl())
				+ " notes=" + text(entry.getNotes()) + " properties=" + entry.getProperties().size()
				+ " attachments=" + entry.getResources().size())));
		return lines;
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
