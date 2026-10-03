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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.ChainState;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.lethenon.WalletScan;

/**
 * Sweeping moves what arrived at the wallet's one-time destinations onto its own account, one
 * transfer per destination, and the round trip is the proof: the pending file is read back with the
 * chain library's own reader, mined into the next block, and the replay of that block moves the
 * money. The plugin writes no sweeping loop of its own; these tests hold it to what lethenon's
 * {@code Sweeps.prepare} decides.
 */
class SweepSupportTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File directory;

	private Wallet wallet;

	private KeyPair payer;

	private Path chainFile;

	private Path walletFile;

	private Bytes account;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = new File(directory, "chain.lethenon").toPath();
		Files.write(chainFile,
			CanonicalEncoding.encodeChain(LethenonFixtures.aChainPayingTheWallet(payer, wallet)));
		account = wallet.spendKey(SignatureSuite.ED25519);
	}

	@Test
	@DisplayName("a swept payment is read back from the pending file and the next block moves it onto the account")
	void sweep_movesTheOneTimePaymentOntoTheAccount() throws Exception
	{
		SweptPayments swept = SweepSupport.sweep(order("0.1", "swept"), PASSWORD.toCharArray());

		assertEquals(1, swept.transfers());
		assertEquals(Amount.parseLeth("4.9"), swept.total(), "five LETH less the fee of 0.1");
		assertEquals(account.toString(), swept.account());
		assertEquals(1, swept.waiting());
		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		assertEquals(1, pending.size());
		SignedTransaction transfer = pending.getFirst();
		Destination oneTime = oneTimeDestinationOfTheFixture();
		assertEquals(oneTime.key(), transfer.body().sender(),
			"the destination is the sender: it is an account of its own");
		assertEquals(Destination.direct(account), transfer.body().recipient());
		assertEquals("swept", transfer.body().memo());

		ChainState state = Replay.verify(mineTheNextBlock(pending)).finalState();
		assertEquals(Amount.parseLeth("7.9"), state.balanceOf(account),
			"three LETH it held directly and the 4.9 swept onto it");
		assertEquals(Amount.ZERO, state.balanceOf(oneTime.key()));
	}

	@Test
	@DisplayName("every one-time destination gets its own transfer, signed with its own nonce")
	void sweep_twoMoreDestinations_signsOneTransferEach() throws Exception
	{
		payTheWalletsOwnAddressTwiceAndMine();

		SweptPayments swept = SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray());

		assertEquals(3, swept.transfers());
		assertEquals(Amount.parseLeth("7"), swept.total(), "5 + 1 + 1 LETH, no fee");
		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		Set<Bytes> senders = new HashSet<>();
		pending.forEach(transfer -> senders.add(transfer.body().sender()));
		assertEquals(3, senders.size(), "three destinations, three senders");
		pending.forEach(transfer -> assertEquals(0L, transfer.body().nonce(),
			"each destination is an account that has never sent before"));
		ChainState state = Replay.verify(mineTheNextBlock(pending)).finalState();
		assertEquals(Amount.parseLeth("8"), state.balanceOf(account),
			"one LETH left after paying two to itself, and seven swept back");
	}

	@Test
	@DisplayName("sweeping again before the next block finds nothing more, and writes nothing")
	void sweep_twice_signsNothingTheSecondTime() throws Exception
	{
		SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray());

		SweptPayments again = SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray());

		assertEquals(0, again.transfers());
		assertEquals(Amount.ZERO, again.total());
		assertEquals(1, new ChainFile(chainFile).readPending().size(),
			"a second transfer from the same destination would reuse its nonce");
	}

	@Test
	@DisplayName("a sweep that finds nothing does not touch the transfers already waiting")
	void sweep_findingNothing_leavesThePendingFileAlone() throws Exception
	{
		SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray());
		FileTime before = FileTime.fromMillis(1_000_000_000_000L);
		Files.setLastModifiedTime(pendingFile(), before);

		SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray());

		assertEquals(before, Files.getLastModifiedTime(pendingFile()),
			"nothing to sweep is nothing to write: the file the command line may be appending to "
				+ "is left as it was");
	}

	@Test
	@DisplayName("a fee that eats the whole payment sweeps nothing and writes no pending file")
	void sweep_withAFeeAsLargeAsThePayment_writesNothing() throws Exception
	{
		SweptPayments swept = SweepSupport.sweep(order("5", ""), PASSWORD.toCharArray());

		assertEquals(0, swept.transfers());
		assertFalse(Files.exists(pendingFile()),
			"moving a destination for exactly its fee gains nothing and publishes the link");
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = { "a fee that is no number|five|five",
			"a fee with nine decimals|0.000000001|0.000000001", "a negative fee|-1|-1" })
	void sweep_withAFeeTheChainCannotTake_isRefusedBeforeAnythingIsWritten(final String name,
		final String fee, final String namedInTheMessage)
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SweepSupport.sweep(order(fee, ""), PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains(namedInTheMessage), refused.getMessage());
		assertFalse(Files.exists(pendingFile()));
	}

	@Test
	@DisplayName("a wrong password is refused with the wallet file's name and without the password")
	void sweep_withAWrongPassword_isRefusedWithoutRepeatingIt()
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SweepSupport.sweep(order("0", ""), "not the password".toCharArray()));

		assertTrue(refused.getMessage().contains("wallet.lethenon-wallet"), refused.getMessage());
		assertFalse(refused.getMessage().contains("not the password"), refused.getMessage());
		assertFalse(Files.exists(pendingFile()));
	}

	@Test
	@DisplayName("there is nothing to sweep from when the chain file is missing")
	void sweep_withoutAChainFile_isRefused() throws Exception
	{
		Files.delete(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> SweepSupport.sweep(order("0", ""), PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains("there is no chain file"), refused.getMessage());
	}

	@Test
	@DisplayName("the password array is overwritten once the sweep is signed")
	void sweep_wipesThePassword() throws Exception
	{
		char[] password = PASSWORD.toCharArray();

		SweepSupport.sweep(order("0", ""), password);

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	@Test
	@DisplayName("the password array is overwritten when the sweep is refused as well")
	void sweep_wipesThePassword_whenRefused()
	{
		char[] password = PASSWORD.toCharArray();

		assertThrows(IllegalArgumentException.class,
			() -> SweepSupport.sweep(order("-1", ""), password));

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	private SweepOrder order(final String fee, final String memo)
	{
		return new SweepOrder(chainFile, walletFile, fee, memo);
	}

	private Path pendingFile()
	{
		return Path.of(chainFile + ".pending");
	}

	private Destination oneTimeDestinationOfTheFixture() throws Exception
	{
		return WalletScan.over(new ChainFile(chainFile).require(), wallet.address(),
			wallet.viewKeyPair().getPrivate()).received().getFirst().destination();
	}

	/**
	 * The wallet's own account pays its own published address twice through the send support, and
	 * the next block carries both: two more one-time destinations, one LETH each
	 */
	private void payTheWalletsOwnAddressTwiceAndMine() throws Exception
	{
		String address = wallet.address().toText();
		for (int payment = 0; payment < 2; payment++)
		{
			TransferSupport.send(
				new TransferOrder(chainFile, walletFile, SignatureSuite.ED25519,
					RecipientKind.PUBLISHED_ADDRESS, address, "1", "0", "to myself"),
				PASSWORD.toCharArray());
		}
		ChainFile file = new ChainFile(chainFile);
		file.write(mineTheNextBlock(file.readPending()));
		file.writePending(List.of());
	}

	private List<BlockBody> mineTheNextBlock(final List<SignedTransaction> pending) throws Exception
	{
		List<BlockBody> chain = new ArrayList<>(new ChainFile(chainFile).require());
		BlockBody next = Blocks
			.mine(Mining.nextBlock(chain, TransactionSigner.asBytes(payer.getPublic()), pending,
				"a pun to carry it", chain.getLast().timestamp() + 120_000L), 1_000_000L)
			.orElseThrow();
		chain.add(next);
		return chain;
	}
}
