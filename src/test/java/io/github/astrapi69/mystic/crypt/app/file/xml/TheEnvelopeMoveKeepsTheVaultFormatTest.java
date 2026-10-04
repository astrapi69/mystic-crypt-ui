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
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * The vault's passphrase construction moved out of this application's own
 * {@code crypto/PassphraseBox} into mystic-crypt's {@code PassphraseEnvelope} (#490). The layout is
 * meant to be byte for byte the same; this is the measurement that it is, against the last release
 * that sealed with the application's own copy - the published jar, not a model of it - and in both
 * directions:
 * <ul>
 * <li>a vault that release wrote, through its own save path, opens in this build</li>
 * <li>a vault this build writes opens in that release</li>
 * </ul>
 * Only together do they say the move did not touch the format: one direction alone would pass for a
 * reader that learned to read both layouts while writing a new one. The older promise - a vault
 * without the 8.6 fields opens in {@code formatCompatibilityRelease} - stays with
 * {@link VaultOpensInTheLastReleaseTest}.
 */
class TheEnvelopeMoveKeepsTheVaultFormatTest
{

	private static final ReleaseProbe RELEASE = ReleaseProbe.ENVELOPE_MOVE;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("a vault the release wrote, through its own save path, opens in this build with every entry as written")
	void aVaultTheReleaseWrote_opensInThisBuild(@TempDir File directory) throws Exception
	{
		RELEASE.requireJar();
		char[] password = TestPasswords.throwawayChars();
		File vault = new File(directory, "written-by-the-release.mcrdb");

		ReleaseProbe.ProbeRun written = RELEASE.run("/compat/WriteWithTheRelease.java", directory,
			password, vault.getAbsolutePath());
		assertEquals(0, written.exitCode(),
			RELEASE.name() + " could not write its vault: " + written.output());
		assertTrue(written.output().contains("WRITTEN"), written.output());

		ApplicationModelBean opened = SampleVaults.open(vault, password);

		assertEquals(written.entryLines(), SampleVaults.entryLines(opened),
			"this build reads every entry as " + RELEASE.name() + " wrote it");
		MysticCryptEntryModelBean bank = opened.getDataOfNodes().get(1L).get(0);
		assertEquals(1, bank.getHistory().size(), "the history the release wrote comes back");
		assertEquals("the bank, before", new String(bank.getHistory().get(0).getTitle()));
		assertEquals(Set.of("TOTP seed"), bank.getProtectedPropertyKeys(),
			"and so do the names of the protected properties");
	}

	@ParameterizedTest(name = "a vault this build writes, {0}, opens in the release")
	@MethodSource("vaultsThisBuildWrites")
	@DisplayName("a vault this build writes opens in the release, with every entry as written")
	void aVaultThisBuildWrites_opensInTheRelease(final Consumer<MysticCryptEntryModelBean> addition,
		@TempDir File directory) throws Exception
	{
		RELEASE.requireJar();
		char[] password = TestPasswords.throwawayChars();
		ApplicationModelBean model = SampleVaults.aVaultWith(addition);

		ReleaseProbe.ProbeRun read = RELEASE.run("/compat/OpenWithTheRelease.java", directory,
			password, SampleVaults.save(model, directory, password).getAbsolutePath());

		assertEquals(0, read.exitCode(),
			RELEASE.name() + " refused a vault this build wrote: " + read.output());
		assertEquals(SampleVaults.entryLines(model), read.entryLines(),
			RELEASE.name() + " reads every entry as this build wrote it");
	}

	static Stream<Named<Consumer<MysticCryptEntryModelBean>>> vaultsThisBuildWrites()
	{
		return Stream.of(Named.of("without history or protected properties", entry -> {
		}), Named.of("with an entry's history and the names of protected properties", entry -> {
			entry.setHistory(new ArrayList<>(List.of(MysticCryptEntryModelBean.builder()
				.title("the bank, before".toCharArray()).build())));
			entry.setProtectedPropertyKeys(Set.of("TOTP seed"));
		}));
	}
}
