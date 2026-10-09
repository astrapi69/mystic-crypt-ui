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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.ChainState;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.Replay;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;
import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;

/**
 * Mining writes the next block to the chain file and takes the waiting transfers into it. The round
 * trip is the proof: the chain file is read back with the chain library's own reader and replayed,
 * and the transfer that waited has moved the money. A block the chain would refuse is never
 * written, and neither file changes.
 */
class MiningSupportTest
{

	private static final String PASSWORD = "correct horse battery staple";

	private static final long NOW = 1_759_000_240_000L;

	@TempDir
	File directory;

	private Wallet wallet;

	private Path chainFile;

	private Path walletFile;

	private Bytes miner;

	@BeforeEach
	void writeAChainAndAWallet() throws Exception
	{
		wallet = Wallet.create();
		walletFile = new File(directory, "wallet.lethenon-wallet").toPath();
		WalletFile.write(walletFile, wallet, PASSWORD.toCharArray());
		KeyPair payer = TransactionSigner.newKeyPair(SignatureSuite.ED25519);
		chainFile = new File(directory, "chain.lethenon").toPath();
		Files.write(chainFile,
			CanonicalEncoding.encodeChain(LethenonFixtures.aChainPayingTheWallet(payer, wallet)));
		miner = wallet.spendKey(SignatureSuite.ED25519);
	}

	@Test
	@DisplayName("a mined block carries the waiting transfer, is read back, and its replay moves the money")
	void mine_writesTheNextBlockWithTheWaitingTransfer() throws Exception
	{
		Bytes recipient = aRecipient();
		TransferSupport.send(
			new TransferOrder(chainFile, walletFile, SignatureSuite.ED25519,
				RecipientKind.ACCOUNT_KEY, recipient.toString(), "1", "0.5", "carried"),
			PASSWORD.toCharArray());
		Amount before = stateOf(new ChainFile(chainFile).require()).balanceOf(miner);

		MinedBlock mined = MiningSupport.mine(order("watching is not protecting", 10_000_000L),
			PASSWORD.toCharArray(), NOW);

		assertEquals(3L, mined.height());
		assertEquals(1, mined.transfers());
		assertEquals(miner.toString(), mined.beneficiary());
		assertTrue(mined.pun().startsWith("watching is not protecting"), mined.pun());
		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals(4, chain.size(), "the block was written to the chain file");
		assertEquals(mined.pun(), chain.getLast().pun());
		ChainState state = stateOf(chain);
		assertEquals(Amount.parseLeth("1"), state.balanceOf(recipient));
		assertTrue(state.balanceOf(miner).compareTo(before.minus(Amount.parseLeth("1"))) > 0,
			"the miner pays 1 LETH out and is paid the reward and the fee back: " + before + " -> "
				+ state.balanceOf(miner));
		assertTrue(new ChainFile(chainFile).readPending().isEmpty());
		assertFalse(Files.exists(pendingFile()), "the waiting transfers went into the block");
	}

	@Test
	@DisplayName("with nothing waiting, the block carries no transfer and still pays the miner")
	void mine_withNothingWaiting_writesAnEmptyBlock() throws Exception
	{
		Amount before = stateOf(new ChainFile(chainFile).require()).balanceOf(miner);

		MinedBlock mined = MiningSupport.mine(order("an empty block", 10_000_000L),
			PASSWORD.toCharArray(), NOW);

		assertEquals(0, mined.transfers());
		ChainState state = stateOf(new ChainFile(chainFile).require());
		assertTrue(state.balanceOf(miner).compareTo(before) > 0,
			"the block reward goes to the miner: " + before + " -> " + state.balanceOf(miner));
	}

	@Test
	@DisplayName("mining where there is no chain yet writes its genesis block for the burn account, and the next block pays the miner (lethenon#148)")
	void mine_withoutAChain_writesTheGenesisBlockForTheBurnAccount() throws Exception
	{
		Files.delete(chainFile);

		MinedBlock genesis = MiningSupport.mine(order("in the beginning", 10_000_000L),
			PASSWORD.toCharArray(), NOW);
		MinedBlock first = MiningSupport.mine(order("block one", 10_000_000L),
			PASSWORD.toCharArray(), NOW + 120_000L);

		assertEquals(0L, genesis.height());
		assertEquals(Genesis.NOBODY.toString(), genesis.beneficiary());
		assertEquals(1L, first.height());
		assertEquals(miner.toString(), first.beneficiary());
		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals(Genesis.NOBODY, chain.getFirst().beneficiary());
		assertEquals(miner, chain.getLast().beneficiary());
		Replay.verify(chain);
	}

	@Test
	@DisplayName("where there is no chain yet, the test network starts a chain under the library's test identifier")
	void mine_withoutAChain_startsTheTestNetwork() throws Exception
	{
		Files.delete(chainFile);

		MinedBlock mined = MiningSupport.mine(
			order("in the beginning", 10_000_000L, ChainKind.TEST_NETWORK), PASSWORD.toCharArray(),
			NOW);

		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals(Chain.TEST_IDENTIFIER, chain.getFirst().chainIdentifier());
		assertEquals(Chain.TEST_IDENTIFIER, mined.chainIdentifier());
		Replay.verify(chain);
	}

	@Test
	@DisplayName("a new main chain is refused with the library's reason, which names 1.0.0, and nothing is written (#535)")
	void mine_withoutAChain_refusesTheMainChain_untilItHasItsAnchor() throws Exception
	{
		Files.delete(chainFile);
		char[] password = PASSWORD.toCharArray();

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> MiningSupport.mine(order("in the beginning", 10_000_000L, ChainKind.MAIN_CHAIN),
				password, NOW));

		assertTrue(refused.getMessage().contains(Genesis.NO_MAIN_CHAIN_WITHOUT_ITS_ANCHOR),
			refused.getMessage());
		assertFalse(Files.exists(chainFile), "no main chain file is written");
		assertArrayEquals(new char[password.length], password, "the password is wiped");
	}

	@ParameterizedTest(name = "choosing {0} on an existing test chain extends the test chain")
	@EnumSource(ChainKind.class)
	@DisplayName("on an existing chain its genesis block decides, whatever kind was chosen")
	void mine_onAnExistingChain_followsItsGenesis(final ChainKind kind) throws Exception
	{
		MinedBlock mined = MiningSupport.mine(order("its genesis decides", 10_000_000L, kind),
			PASSWORD.toCharArray(), NOW);

		List<BlockBody> chain = new ChainFile(chainFile).require();
		assertEquals(4, chain.size());
		assertEquals(Chain.TEST_IDENTIFIER, chain.getLast().chainIdentifier());
		assertEquals(Chain.TEST_IDENTIFIER, mined.chainIdentifier());
	}

	@Test
	@DisplayName("a chain that does not verify is refused, and neither file changes")
	void mine_onATamperedChain_writesNothing() throws Exception
	{
		Files.write(chainFile, CanonicalEncoding.encodeChain(aChainWithABrokenLink()));
		byte[] chainBefore = Files.readAllBytes(chainFile);

		assertThrows(ChainRejected.class, () -> MiningSupport
			.mine(order("never written", 10_000_000L), PASSWORD.toCharArray(), NOW));

		assertArrayEquals(chainBefore, Files.readAllBytes(chainFile));
		assertFalse(Files.exists(pendingFile()));
	}

	@Test
	@DisplayName("a chain that does not verify is refused before a single pun is tried")
	void mine_onATamperedChain_isRefusedBeforeMining() throws Exception
	{
		Files.write(chainFile, CanonicalEncoding.encodeChain(aChainWithABrokenLink()));

		// with no attempts at all, only a replay before mining can give the chain as the reason;
		// otherwise the reason would be the pun that was never tried
		assertThrows(ChainRejected.class,
			() -> MiningSupport.mine(order("never tried", 0L), PASSWORD.toCharArray(), NOW));
	}

	@Test
	@DisplayName("a waiting transfer the chain would refuse stops the block, and both files stay")
	void mine_withAWaitingTransferTheChainRefuses_writesNothing() throws Exception
	{
		new ChainFile(chainFile).writePending(List.of(wallet.sign(
			new TransactionBody(Chain.IDENTIFIER, 7L, miner, Destination.direct(aRecipient()),
				Amount.parseLeth("1"), Amount.ZERO, "a nonce from nowhere"),
			SignatureSuite.ED25519)));
		byte[] chainBefore = Files.readAllBytes(chainFile);
		byte[] pendingBefore = Files.readAllBytes(pendingFile());

		assertThrows(ChainRejected.class, () -> MiningSupport
			.mine(order("never written", 10_000_000L), PASSWORD.toCharArray(), NOW));

		assertArrayEquals(chainBefore, Files.readAllBytes(chainFile));
		assertArrayEquals(pendingBefore, Files.readAllBytes(pendingFile()));
	}

	@Test
	@DisplayName("running out of attempts is refused with the difficulty, and nothing is written")
	void mine_withoutEnoughAttempts_writesNothing() throws Exception
	{
		byte[] chainBefore = Files.readAllBytes(chainFile);

		IllegalStateException refused = assertThrows(IllegalStateException.class,
			() -> MiningSupport.mine(order("no luck", 0L), PASSWORD.toCharArray(), NOW));

		assertTrue(refused.getMessage().contains("difficulty"), refused.getMessage());
		assertArrayEquals(chainBefore, Files.readAllBytes(chainFile));
	}

	@Test
	@DisplayName("a wrong password is refused with the wallet file's name and without the password")
	void mine_withAWrongPassword_isRefusedWithoutRepeatingIt() throws Exception
	{
		byte[] chainBefore = Files.readAllBytes(chainFile);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> MiningSupport.mine(order("no", 10_000_000L), "not the password".toCharArray(),
				NOW));

		assertTrue(refused.getMessage().contains("wallet.lethenon-wallet"), refused.getMessage());
		assertFalse(refused.getMessage().contains("not the password"), refused.getMessage());
		assertArrayEquals(chainBefore, Files.readAllBytes(chainFile));
	}

	@Test
	@DisplayName("the password array is overwritten once the block is mined")
	void mine_wipesThePassword() throws Exception
	{
		char[] password = PASSWORD.toCharArray();

		MiningSupport.mine(order("wipe", 10_000_000L), password, NOW);

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	@Test
	@DisplayName("the password array is overwritten when mining is refused as well")
	void mine_wipesThePassword_whenRefused()
	{
		char[] password = PASSWORD.toCharArray();

		assertThrows(IllegalStateException.class,
			() -> MiningSupport.mine(order("wipe", 0L), password, NOW));

		assertArrayEquals(new char[PASSWORD.length()], password);
	}

	private MiningOrder order(final String pun, final long attempts)
	{
		return order(pun, attempts, ChainKind.TEST_NETWORK);
	}

	private MiningOrder order(final String pun, final long attempts, final ChainKind kind)
	{
		return new MiningOrder(chainFile, walletFile, kind, pun, attempts);
	}

	private Path pendingFile()
	{
		return Path.of(chainFile + ".pending");
	}

	private static ChainState stateOf(final List<BlockBody> chain)
	{
		return Replay.verify(chain).finalState();
	}

	private static Bytes aRecipient()
	{
		return TransactionSigner
			.asBytes(TransactionSigner.newKeyPair(SignatureSuite.ED25519).getPublic());
	}

	private List<BlockBody> aChainWithABrokenLink() throws Exception
	{
		BlockBody genesis = new ChainFile(chainFile).require().getFirst();
		BlockBody unlinked = Blocks.mine(new BlockBody(Chain.IDENTIFIER, 1L, Bytes.of(new byte[32]),
			miner, List.of(), 1_759_000_120_000L, 8, "a block that points nowhere"), 1_000_000L)
			.orElseThrow();
		return List.of(genesis, unlinked);
	}
}
