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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * The wallet in the vault, as the decision record describes it
 * (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md): sealed as the attachment
 * {@code wallet.lethwf}, marked by {@code lethenon.wallet}, created, taken over, restored,
 * exported and unsealed in memory only.
 */
class WalletVaultTest
{

	private static final char[] PASSWORD = "wallet password".toCharArray();

	@TempDir
	Path directory;

	@Test
	@DisplayName("a created wallet is sealed in the entry, marked, and its words are shown, not stored")
	void creatingSealsTheWalletMarksTheEntryAndKeepsTheWordsOut() throws IOException
	{
		Set<Path> before = filesIn(directory);

		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			WalletEntry entry = created.entry();
			Wallet opened = WalletFile.read(entry.sealed(), PASSWORD);

			assertTrue(WalletVault.isWallet(entry));
			assertEquals("1", entry.textOf(WalletVault.WALLET));
			assertEquals("testnet", entry.textOf(WalletEntry.NETWORK));
			assertEquals(opened.address().toText(), entry.addressForCheck());
			List<String> words = List.of(new String(created.words()).split(" "));
			assertEquals(24, words.size());
			assertEquals(opened.phrase(), new String(created.words()));
			assertFalse(entry.hasProperty(WalletInVault.MNEMONIC), "the default keeps the words out");
			for (String name : entry.propertyNames())
			{
				assertFalse(words.stream().anyMatch(entry.textOf(name)::contains),
					"no property may hold a word: " + name);
			}
		}
		assertEquals(before, filesIn(directory), "creating writes nothing to disk");
	}

	@Test
	@DisplayName("closing the created wallet overwrites the buffer that held its words")
	void closingOverwritesTheWords()
	{
		CreatedWallet created = WalletVault.create("mainnet", PASSWORD);
		char[] words = created.words();

		created.close();

		assertArrayEquals(new char[words.length], words, "every character must be zero");
	}

	@Test
	@DisplayName("taking over a wallet file stores its sealed bytes and leaves the file untouched")
	void takingOverKeepsTheFileAsItWas() throws IOException
	{
		Wallet wallet = Wallet.create();
		Path file = directory.resolve("my.lethwf");
		WalletFile.write(file, wallet, PASSWORD);
		byte[] original = Files.readAllBytes(file);
		FileTime modified = Files.getLastModifiedTime(file);

		WalletEntry entry = WalletVault.takeOver(file, PASSWORD, "testnet");

		assertArrayEquals(original, entry.sealed(), "the attachment is the file, unchanged");
		assertEquals(wallet.address().toText(), entry.addressForCheck());
		assertTrue(WalletVault.isWallet(entry));
		assertArrayEquals(original, Files.readAllBytes(file), "the original file is untouched");
		assertEquals(modified, Files.getLastModifiedTime(file));
		assertEquals(Set.of(file), filesIn(directory), "the user decides about the file, not this");
	}

	@Test
	@DisplayName("a wallet file that does not open with the password is refused and left untouched")
	void aWrongPasswordIsRefusedOnTakeOver() throws IOException
	{
		Path file = directory.resolve("my.lethwf");
		WalletFile.write(file, Wallet.create(), PASSWORD);
		byte[] original = Files.readAllBytes(file);

		SecurityException refused = assertThrows(SecurityException.class,
			() -> WalletVault.takeOver(file, "wrong".toCharArray(), "testnet"));

		assertTrue(refused.getMessage().contains("does not open"), refused.getMessage());
		assertArrayEquals(original, Files.readAllBytes(file));
	}

	@Test
	@DisplayName("restoring from the 24 words seals the same wallet straight into the entry")
	void restoringFromTheWordsGivesTheSameWallet()
	{
		Wallet wallet = Wallet.create();
		char[] newPassword = "new password".toCharArray();
		Set<Path> before = filesIn(directory);

		WalletEntry entry = WalletVault.restore(wallet.phrase().toCharArray(), newPassword,
			"mainnet");

		assertEquals(wallet.address(), WalletFile.read(entry.sealed(), newPassword).address());
		assertEquals(wallet.address().toText(), entry.addressForCheck());
		assertEquals("mainnet", entry.textOf(WalletEntry.NETWORK));
		assertFalse(entry.hasProperty(WalletInVault.MNEMONIC));
		assertEquals(before, filesIn(directory), "restoring writes nothing to disk");
	}

	@Test
	@DisplayName("an export without the confirmation after the warning writes nothing")
	void anExportWithoutConsentWritesNothing()
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			Path target = directory.resolve("backup.lethwf");

			IllegalStateException refused = assertThrows(IllegalStateException.class,
				() -> WalletVault.export(created.entry(), target, ExportConsent.NOT_GIVEN));

			assertTrue(refused.getMessage().contains("outside the vault"), refused.getMessage());
			assertFalse(Files.exists(target));
		}
	}

	@Test
	@DisplayName("a confirmed export writes the sealed file and returns the SHA-256 of what it wrote")
	void aConfirmedExportWritesTheSealedFileAndItsChecksum()
		throws IOException, NoSuchAlgorithmException
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			Path target = directory.resolve("backup.lethwf");

			String checksum = WalletVault.export(created.entry(), target,
				ExportConsent.GIVEN_AFTER_WARNING);

			byte[] written = Files.readAllBytes(target);
			assertArrayEquals(created.entry().sealed(), written, "the export is the sealed file");
			assertEquals(HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(written)), checksum);
			assertEquals(Set.of(target), filesIn(directory), "no decrypted copy in between");
		}
	}

	@Test
	@DisplayName("an export never overwrites a file that is already there")
	void anExportDoesNotOverwrite() throws IOException
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			Path target = directory.resolve("backup.lethwf");
			Files.writeString(target, "already here");

			assertThrows(IOException.class, () -> WalletVault.export(created.entry(), target,
				ExportConsent.GIVEN_AFTER_WARNING));

			assertEquals("already here", Files.readString(target));
		}
	}

	@Test
	@DisplayName("unsealing a wallet whose recorded address matches gives no warning")
	void unsealingWithTheMatchingAddressWarnsOfNothing()
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			Set<Path> before = filesIn(directory);

			UnsealedWallet unsealed = WalletVault.unseal(created.entry(), PASSWORD);

			assertEquals(created.entry().addressForCheck(), unsealed.wallet().address().toText());
			assertTrue(unsealed.addressWarning().isEmpty());
			assertEquals(before, filesIn(directory), "unsealing happens in memory only");
		}
	}

	@Test
	@DisplayName("unsealing a wallet whose recorded address differs warns, naming both, and changes nothing")
	void unsealingWithAnotherAddressWarnsAndChangesNothing()
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			WalletEntry entry = created.entry();
			String foreign = Wallet.create().address().toText();
			entry.setProperty(WalletEntry.ADDRESS, foreign.toCharArray(), false);
			byte[] sealed = entry.sealed().clone();

			UnsealedWallet unsealed = WalletVault.unseal(entry, PASSWORD);

			String warning = unsealed.addressWarning().orElseThrow();
			assertTrue(warning.contains(foreign), warning);
			assertTrue(warning.contains(unsealed.wallet().address().toText()), warning);
			assertEquals(foreign, entry.addressForCheck(), "the recorded address stays as it is");
			assertArrayEquals(sealed, entry.sealed(), "the attachment stays as it is");
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("entryShapes")
	@DisplayName("an entry is a wallet only with both the marker and the attachment")
	void anEntryIsAWalletOnlyWithMarkerAndAttachment(final String shape,
		final Consumer<WalletEntry> change, final boolean wallet)
	{
		try (CreatedWallet created = WalletVault.create("testnet", PASSWORD))
		{
			change.accept(created.entry());

			assertEquals(wallet, WalletVault.isWallet(created.entry()));
		}
	}

	static Stream<Arguments> entryShapes()
	{
		return Stream.of(Arguments.of("marker and attachment", noChange(), true),
			Arguments.of("attachment without the marker",
				(Consumer<WalletEntry>)entry -> entry.removeProperty(WalletVault.WALLET), false),
			Arguments.of("marker without the attachment",
				(Consumer<WalletEntry>)entry -> entry.setSealed(null), false),
			Arguments.of("a marker of another format version",
				(Consumer<WalletEntry>)entry -> entry.setProperty(WalletVault.WALLET,
					"2".toCharArray(), false),
				false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("refusedInputs")
	@DisplayName("a network or a word list that cannot be a wallet is refused with the reason")
	void inputsThatCannotBeAWalletAreRefused(final String input, final Runnable action,
		final String reason)
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			action::run);

		assertTrue(refused.getMessage().contains(reason), refused.getMessage());
	}

	static Stream<Arguments> refusedInputs()
	{
		String phrase = Wallet.create().phrase();
		String twentyThree = phrase.substring(0, phrase.lastIndexOf(' '));
		return Stream.of(
			Arguments.of("network 'regtest'",
				(Runnable)() -> WalletVault.create("regtest", PASSWORD), "regtest"),
			Arguments.of("an empty network", (Runnable)() -> WalletVault.create("", PASSWORD),
				"testnet or mainnet"),
			Arguments.of("23 words",
				(Runnable)() -> WalletVault.restore(twentyThree.toCharArray(), PASSWORD, "testnet"),
				"23"),
			Arguments.of("an unknown word", (Runnable)() -> WalletVault
				.restore(phrase.replaceFirst("^\\S+", "lethenonx").toCharArray(), PASSWORD,
					"testnet"),
				"lethenonx"));
	}

	private static Consumer<WalletEntry> noChange()
	{
		return entry -> {
		};
	}

	private static Set<Path> filesIn(final Path folder)
	{
		try (Stream<Path> files = Files.walk(folder))
		{
			return files.filter(Files::isRegularFile).collect(Collectors.toSet());
		}
		catch (IOException unreadable)
		{
			throw new IllegalStateException("cannot list " + folder, unreadable);
		}
	}
}
