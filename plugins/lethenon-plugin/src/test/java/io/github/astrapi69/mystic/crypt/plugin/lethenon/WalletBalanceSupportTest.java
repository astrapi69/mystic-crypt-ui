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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * The balance behind the plugin's balance window: computed from a replayed chain and a wallet file,
 * never asked of anybody, and never counting a payment it cannot move as money it can.
 * <p>
 * The fixture is real: a wallet file sealed with a password the way lethenon's command line writes
 * it, and a chain in which a payer pays the wallet 3 LETH to its Ed25519 account and 5 LETH to a
 * one-time destination of its published address.
 */
class WalletBalanceSupportTest
{

	private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

	@TempDir
	File directory;

	private final KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);

	private Wallet wallet;

	private Path walletFile;

	@BeforeEach
	void writeTheWalletFile() throws Exception
	{
		wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, PASSWORD);
	}

	@Test
	@DisplayName("the direct accounts are spendable, the one-time payment is counted apart")
	void balance_separatesSpendableFromOneTimePayments() throws Exception
	{
		Path chainFile = write("chain.lethenon",
			CanonicalEncoding.encodeChain(aChainPayingTheWallet()));

		WalletBalance balance = WalletBalanceSupport.balance(chainFile, walletFile,
			PASSWORD.clone());

		assertEquals(2, balance.accounts().size(), "one account per signature suite");
		AccountBalance ed25519 = balance.accounts().getFirst();
		assertEquals("ed25519", ed25519.suite());
		assertEquals(wallet.spendKey(SignatureSuite.ED25519).toString(), ed25519.account());
		assertEquals(Amount.ofLeth(3L), ed25519.spendable());
		assertEquals(Amount.ZERO, balance.accounts().get(1).spendable());
		assertEquals(1, balance.oneTimePayments());
		assertEquals(Amount.ofLeth(5L), balance.oneTimeAmount(),
			"the one-time payment is reported, and reported apart from what can be spent");
		assertTrue(balance.replaySummary().contains("replayed 3 blocks"), balance.replaySummary());
	}

	@Test
	@DisplayName("a wrong password names the file and never the password")
	void balance_refusesAWrongPassword_withoutRepeatingIt() throws Exception
	{
		Path chainFile = write("chain.lethenon",
			CanonicalEncoding.encodeChain(aChainPayingTheWallet()));
		char[] wrong = "not the password at all".toCharArray();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> WalletBalanceSupport.balance(chainFile, walletFile, wrong.clone()));

		assertTrue(refused.getMessage().contains("wallet.lethenon-wallet"), refused.getMessage());
		assertFalse(refused.getMessage().contains("not the password at all"),
			"a password must never travel in a message: " + refused.getMessage());
	}

	@Test
	@DisplayName("a chain that does not verify has no balance at all")
	void balance_refusesAChainThatDoesNotReplay() throws Exception
	{
		List<BlockBody> chain = new ArrayList<>(aChainPayingTheWallet());
		BlockBody last = chain.getLast();
		chain.set(chain.size() - 1,
			new BlockBody(last.chainIdentifier(), last.height(), Bytes.of(new byte[32]),
				last.beneficiary(), last.transactions(), last.timestamp(), last.difficulty(),
				last.pun()));
		Path chainFile = write("tampered.lethenon", CanonicalEncoding.encodeChain(chain));

		assertThrows(ChainRejected.class,
			() -> WalletBalanceSupport.balance(chainFile, walletFile, PASSWORD.clone()));
	}

	@Test
	@DisplayName("a wallet file that is not there is named, not replayed against")
	void balance_refusesAMissingWalletFile() throws Exception
	{
		Path chainFile = write("chain.lethenon",
			CanonicalEncoding.encodeChain(aChainPayingTheWallet()));
		Path missing = new File(directory, "no-wallet-here").toPath();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> WalletBalanceSupport.balance(chainFile, missing, PASSWORD.clone()));

		assertTrue(refused.getMessage().contains("no-wallet-here"), refused.getMessage());
	}

	@Test
	@DisplayName("the password array is wiped once the wallet is open")
	void balance_wipesThePasswordItWasGiven() throws Exception
	{
		Path chainFile = write("chain.lethenon",
			CanonicalEncoding.encodeChain(aChainPayingTheWallet()));
		char[] handedOver = PASSWORD.clone();

		WalletBalanceSupport.balance(chainFile, walletFile, handedOver);

		assertEquals(new String(new char[PASSWORD.length]), new String(handedOver),
			"the array is overwritten, not merely dropped");
	}

	private Path write(final String name, final byte[] bytes) throws Exception
	{
		Path file = new File(directory, name).toPath();
		Files.write(file, bytes);
		return file;
	}

	private List<BlockBody> aChainPayingTheWallet()
	{
		return LethenonFixtures.aChainPayingTheWallet(payer, wallet);
	}
}
