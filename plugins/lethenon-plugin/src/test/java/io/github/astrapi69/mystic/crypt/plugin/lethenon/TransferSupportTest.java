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
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Sending writes a transfer that waits next to the chain file, and the round trip is the proof: the
 * pending file is read back with the chain library's own reader, mined into the next block, and the
 * replay of that block moves the money. A transfer the chain would refuse is refused before
 * anything is written.
 */
class TransferSupportTest
{

	private static final String PASSWORD = "correct horse battery staple";

	@TempDir
	File directory;

	private Wallet wallet;

	private KeyPair payer;

	private Path chainFile;

	private Path walletFile;

	private Bytes recipient;

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
		recipient = TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
	}

	@Test
	@DisplayName("a sent transfer is read back from the pending file and the next block accepts it")
	void send_writesATransferTheNextBlockAccepts() throws Exception
	{
		SentTransfer sent = TransferSupport.send(order("1.5", "0.1", "against the cameras"),
			PASSWORD.toCharArray());

		assertEquals(0L, sent.nonce());
		assertEquals(1, sent.waiting());
		assertEquals(Amount.parseLeth("1.5"), sent.amount());
		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		assertEquals(1, pending.size());
		SignedTransaction transfer = pending.getFirst();
		assertEquals("against the cameras", transfer.body().memo());
		assertEquals(Destination.direct(recipient), transfer.body().recipient());
		assertEquals(wallet.spendKey(SignatureSuite.ED25519), transfer.body().sender());
		assertEquals(Amount.parseLeth("0.1"), transfer.body().fee());

		ChainState state = Replay.verify(mineTheNextBlock(pending)).finalState();
		assertEquals(Amount.parseLeth("1.5"), state.balanceOf(recipient));
		assertEquals(Amount.parseLeth("1.4"),
			state.balanceOf(wallet.spendKey(SignatureSuite.ED25519)));
	}

	@Test
	@DisplayName("a second transfer before the next block gets the next nonce and waits with the first")
	void send_twice_givesTheSecondTransferTheNextNonce() throws Exception
	{
		TransferSupport.send(order("1", "0", "first"), PASSWORD.toCharArray());

		SentTransfer second = TransferSupport.send(order("1", "0", "second"),
			PASSWORD.toCharArray());

		assertEquals(1L, second.nonce());
		assertEquals(2, second.waiting());
		List<SignedTransaction> pending = new ChainFile(chainFile).readPending();
		assertEquals(List.of("first", "second"),
			pending.stream().map(transfer -> transfer.body().memo()).toList());
		Replay.verify(mineTheNextBlock(pending));
	}

	@Test
	@DisplayName("a transfer the account cannot cover is refused and nothing is written")
	void send_moreThanTheAccountHolds_isRefused() throws Exception
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order("3", "0.1", "too much"), PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains("holds 3.00000000"), refused.getMessage());
		assertFalse(Files.exists(pendingFile()), "a refused transfer leaves no pending file");
	}

	@Test
	@DisplayName("the ML-DSA-65 account holds nothing in this chain, so sending from it is refused")
	void send_fromAnEmptyAccount_isRefused()
	{
		TransferOrder fromTheOtherAccount = new TransferOrder(chainFile, walletFile,
			SignatureSuite.ML_DSA_65, recipient.toString(), "1", "0", "");

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(fromTheOtherAccount, PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains("holds 0.00000000"), refused.getMessage());
		assertFalse(Files.exists(pendingFile()));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = { "no recipient||1|0|the recipient",
			"a recipient that is not hexadecimal|not-a-key|1|0|not-a-key",
			"an amount that is no number|RECIPIENT|twelve|0|twelve",
			"an amount with nine decimals|RECIPIENT|0.000000001|0|0.000000001",
			"a negative amount|RECIPIENT|-1|0|-1", "a negative fee|RECIPIENT|1|-1|-1" })
	void send_withAnInputTheChainCannotTake_isRefusedBeforeAnythingIsWritten(final String name,
		final String recipientText, final String amount, final String fee,
		final String namedInTheMessage)
	{
		String to = "RECIPIENT".equals(recipientText)
			? recipient.toString()
			: recipientText == null ? "" : recipientText;
		TransferOrder order = new TransferOrder(chainFile, walletFile, SignatureSuite.ED25519, to,
			amount, fee, "");

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order, PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains(namedInTheMessage), refused.getMessage());
		assertFalse(Files.exists(pendingFile()));
	}

	@Test
	@DisplayName("a wrong password is refused with the wallet file's name and without the password")
	void send_withAWrongPassword_isRefusedWithoutRepeatingIt()
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order("1", "0", ""), "not the password".toCharArray()));

		assertTrue(refused.getMessage().contains("wallet.lethenon-wallet"), refused.getMessage());
		assertFalse(refused.getMessage().contains("not the password"), refused.getMessage());
		assertFalse(Files.exists(pendingFile()));
	}

	@Test
	@DisplayName("there is nothing to send from when the chain file is missing")
	void send_withoutAChainFile_isRefused() throws Exception
	{
		Files.delete(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order("1", "0", ""), PASSWORD.toCharArray()));

		assertTrue(refused.getMessage().contains("there is no chain file"), refused.getMessage());
	}

	@Test
	@DisplayName("the password array is overwritten once the transfer is signed")
	void send_wipesThePassword() throws Exception
	{
		char[] password = PASSWORD.toCharArray();

		TransferSupport.send(order("1", "0", ""), password);

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	@Test
	@DisplayName("the password array is overwritten when the transfer is refused as well")
	void send_wipesThePassword_whenRefused()
	{
		char[] password = PASSWORD.toCharArray();

		assertThrows(IllegalArgumentException.class,
			() -> TransferSupport.send(order("1000", "0", ""), password));

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	private TransferOrder order(final String amount, final String fee, final String memo)
	{
		return new TransferOrder(chainFile, walletFile, SignatureSuite.ED25519,
			recipient.toString(), amount, fee, memo);
	}

	private Path pendingFile()
	{
		return Path.of(chainFile + ".pending");
	}

	private List<BlockBody> mineTheNextBlock(final List<SignedTransaction> pending) throws Exception
	{
		List<BlockBody> chain = new ArrayList<>(new ChainFile(chainFile).require());
		BlockBody next = Blocks
			.mine(Mining.nextBlock(chain, TransactionSigner.asBytes(payer.getPublic()), pending,
				"a pun to carry it", 1_759_000_240_000L), 1_000_000L)
			.orElseThrow();
		chain.add(next);
		return chain;
	}
}
