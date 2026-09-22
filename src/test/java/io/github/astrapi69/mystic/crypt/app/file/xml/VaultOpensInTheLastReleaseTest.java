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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * A vault this build writes opens in the last published release, as long as it holds nothing that
 * release has no field for - and one that does is refused by it (#402, the maintainer's D1).
 * <p>
 * Measured against the release itself, not a model of it: the application jar is taken out of the
 * installer published on GitHub, checked against the sha256 published beside it
 * ({@code scripts/fetch-release-jar.sh}), and a child JVM with only that jar on its class path
 * opens the vault through the release's own password sign-in path
 * ({@code compat/OpenWithTheRelease.java}).
 * <p>
 * The refusal is tested too, so the test is seen to fail when compatibility is lost: an entry's
 * history and the names of its protected properties are exactly what that release cannot read, and
 * it says "Password is not valid" for them.
 * <p>
 * <b>A missing jar fails in CI and skips locally.</b> Decided by the maintainer in #402: CI fetches
 * and caches the jar before the build and must not report a compatibility it did not check, the
 * same rule as for the plugin zips; a local run without network skips and says why.
 */
class VaultOpensInTheLastReleaseTest
{

	private static final String RELEASE_JAR_PROPERTY = "mystic.crypt.ui.release.jar";

	private static final String RELEASE_VERSION_PROPERTY = "mystic.crypt.ui.release.version";

	private static final String PROBE_RESOURCE = "/compat/OpenWithTheRelease.java";

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("a vault without history or protected properties opens in the last release, with every entry as written")
	void aVaultWithoutTheNewFields_opensInTheLastRelease(@TempDir File directory) throws Exception
	{
		File releaseJar = requireTheReleaseJar();
		char[] password = TestPasswords.throwawayChars();
		ApplicationModelBean model = aVaultWith(entry -> {
		});

		ReleaseRead read = openWithTheRelease(releaseJar, save(model, directory, password),
			password, directory);

		assertEquals(0, read.exitCode(),
			"the release refused a vault it has every field for: " + read.output());
		assertEquals(expectedEntryLines(model), read.entryLines(),
			"the release reads every entry as this build wrote it - " + releaseName());
	}

	/**
	 * A group imported from KeePass keeps its times in the element's properties since #413, as
	 * ISO-8601 text and a boolean. That is no new element for the release: its vault already
	 * carried a group's KeePass identifier in the same map. Measured rather than assumed - the
	 * release opens the vault and reads the values back
	 */
	@Test
	@DisplayName("a vault with a group's KeePass times opens in the last release, and it reads them")
	void aVaultWithGroupTimes_opensInTheLastRelease(@TempDir File directory) throws Exception
	{
		File releaseJar = requireTheReleaseJar();
		char[] password = TestPasswords.throwawayChars();
		ApplicationModelBean model = aVaultWith(entry -> {
		});
		model.setRootTreeAsMap(aTreeWithAnImportedGroup());

		ReleaseRead read = openWithTheRelease(releaseJar, save(model, directory, password),
			password, directory);

		assertEquals(0, read.exitCode(), "the release refused a vault whose group carries its "
			+ "KeePass times: " + read.output());
		assertTrue(
			read.output().contains(
				"GROUP name=Team keepass.creationTime=2026-09-16T07:20:28Z keepass.expires=false"),
			"and the release reads what was written: " + read.output());
	}

	private static Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> aTreeWithAnImportedGroup()
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> rootElement = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("root").build();
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(rootElement).leaf(false).build();
		GenericTreeElement<List<MysticCryptEntryModelBean>> team = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("Team").build();
		team.getProperties().put(KeePassTreeConverter.KEEPASS_CREATION_TIME_PROPERTY,
			"2026-09-16T07:20:28Z");
		team.getProperties().put(KeePassTreeConverter.KEEPASS_LAST_MODIFICATION_TIME_PROPERTY,
			"2026-09-16T07:23:37Z");
		team.getProperties().put(KeePassTreeConverter.KEEPASS_LAST_ACCESS_TIME_PROPERTY,
			"2026-09-16T07:23:37Z");
		team.getProperties().put(KeePassTreeConverter.KEEPASS_EXPIRY_TIME_PROPERTY,
			"2026-09-16T07:20:28Z");
		team.getProperties().put(KeePassTreeConverter.KEEPASS_EXPIRES_PROPERTY, false);
		root.addChild(
			BaseTreeNode.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
				.id(2L).value(team).parent(root).leaf(true).build());
		return BaseTreeNodeTransformer.toKeyMap(root);
	}

	@ParameterizedTest(name = "a vault carrying {0} is refused by the last release")
	@MethodSource("whatTheReleaseHasNoFieldFor")
	void aVaultWithANewField_isRefusedByTheLastRelease(
		final Consumer<MysticCryptEntryModelBean> addition, @TempDir File directory)
		throws Exception
	{
		File releaseJar = requireTheReleaseJar();
		char[] password = TestPasswords.throwawayChars();

		ReleaseRead read = openWithTheRelease(releaseJar,
			save(aVaultWith(addition), directory, password), password, directory);

		assertEquals(2, read.exitCode(), "the release opened a vault it has no field for, so "
			+ "this test would not notice a compatibility it lost: " + read.output());
		assertTrue(read.output().contains("Password is not valid"),
			"and what the user of that release sees is the wrong-password message: "
				+ read.output());
	}

	static Stream<Named<Consumer<MysticCryptEntryModelBean>>> whatTheReleaseHasNoFieldFor()
	{
		return Stream.of(
			Named.of("an entry's history",
				entry -> entry.setHistory(new ArrayList<>(List.of(MysticCryptEntryModelBean
					.builder().title("the bank, before".toCharArray()).build())))),
			Named.of("the names of protected properties",
				entry -> entry.setProtectedPropertyKeys(Set.of("TOTP seed"))));
	}

	private static File requireTheReleaseJar()
	{
		File releaseJar = new File(System.getProperty(RELEASE_JAR_PROPERTY, ""));
		if (releaseJar.isFile())
		{
			return releaseJar;
		}
		String reason = "the application jar of " + releaseName() + " is not at " + releaseJar
			+ ". 'make release-jar' downloads the published installer, checks its sha256 and "
			+ "extracts it (needs network)";
		if ("true".equals(System.getenv("GITHUB_ACTIONS")))
		{
			throw new AssertionError(reason + ". CI fetches it before the build, so its absence "
				+ "here is a broken step, not a skip (#402)");
		}
		Assumptions.abort("SKIPPED locally: " + reason + ". In CI this fails (#402)");
		return releaseJar;
	}

	private static String releaseName()
	{
		return "release " + System.getProperty(RELEASE_VERSION_PROPERTY, "(unknown version)");
	}

	private static ApplicationModelBean aVaultWith(
		final Consumer<MysticCryptEntryModelBean> addition)
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

	private static File save(final ApplicationModelBean model, final File directory,
		final char[] password)
	{
		File vault = new File(directory, "written-by-this-build.mcrdb");
		model.setMasterPwFileModelBean(
			MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
				.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password.clone())
				.withMasterPw(true).withKeyFile(false).build());
		return ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);
	}

	private static List<String> expectedEntryLines(final ApplicationModelBean model)
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

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}

	private ReleaseRead openWithTheRelease(final File releaseJar, final File vault,
		final char[] password, final File directory) throws IOException, InterruptedException
	{
		File probe = new File(directory, "OpenWithTheRelease.java");
		try (InputStream source = getClass().getResourceAsStream(PROBE_RESOURCE))
		{
			Files.copy(source, probe.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		String java = ProcessHandle.current().info().command().orElse("java");
		Process process = new ProcessBuilder(java, "-Djava.awt.headless=true", "-cp",
			releaseJar.getAbsolutePath(), probe.getAbsolutePath(), vault.getAbsolutePath())
				.redirectErrorStream(true).start();
		try (OutputStream toProcess = process.getOutputStream())
		{
			toProcess.write((new String(password) + "\n").getBytes(StandardCharsets.UTF_8));
		}
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		if (!process.waitFor(2, TimeUnit.MINUTES))
		{
			process.destroyForcibly();
			throw new IllegalStateException(
				"the release did not finish opening the vault: " + output);
		}
		return new ReleaseRead(process.exitValue(), output);
	}

	private record ReleaseRead(int exitCode, String output) {

		List<String> entryLines()
		{
			return output.lines().filter(line -> line.startsWith("ENTRY ")).toList();
		}
	}
}
