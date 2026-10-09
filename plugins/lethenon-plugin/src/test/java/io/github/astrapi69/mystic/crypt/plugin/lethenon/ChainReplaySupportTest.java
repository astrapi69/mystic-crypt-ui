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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.lethenon.Amount;
import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.Blocks;
import io.github.astrapi69.lethenon.Bytes;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Chain;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.Destination;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.lethenon.Mining;
import io.github.astrapi69.lethenon.SignatureSuite;
import io.github.astrapi69.lethenon.SignedTransaction;
import io.github.astrapi69.lethenon.TransactionBody;
import io.github.astrapi69.lethenon.TransactionSigner;

/**
 * The verifier behind the plugin's button, driven with real chain files rather than with mocks: the
 * chains below are built with the chain library's own encoder, which is what a user's chain file is
 * written by.
 * <p>
 * A refused chain never comes back as a report. Every refusal is an exception whose message names
 * the file and the reason, because the alternative - a report with a flag in it - is how a number
 * from blocks that did not verify reaches a user.
 */
class ChainReplaySupportTest
{

	@TempDir
	File directory;

	private final KeyPair holder = TransactionSigner.newKeyPair(SignatureSuite.ED25519);

	private final Bytes holderKey = TransactionSigner.asBytes(holder.getPublic());

	@Test
	@DisplayName("an accepted chain is reported with what was verified in it")
	void verify_reportsTheCounts_ofAnAcceptedChain() throws Exception
	{
		Path chainFile = write("chain.lethenon",
			CanonicalEncoding.encodeChain(aChainWithOneTransfer()));

		ChainReplayReport report = ChainReplaySupport.verify(chainFile);

		assertEquals(3L, report.blocks());
		assertEquals(1L, report.transactions());
		assertEquals(1L, report.signatures());
		assertTrue(report.summary().contains("replayed 3 blocks"), report.summary());
		assertTrue(report.summary().contains("which is the supply"),
			"the chain's own summary carries the supply invariant: " + report.summary());
	}

	@Test
	@DisplayName("a chain whose blocks do not line up is refused with the reason")
	void verify_refuses_aChainThatDoesNotReplay() throws Exception
	{
		List<BlockBody> chain = new ArrayList<>(aChainWithOneTransfer());
		BlockBody last = chain.getLast();
		chain.set(chain.size() - 1,
			new BlockBody(last.chainIdentifier(), last.height(), Bytes.of(new byte[32]),
				last.beneficiary(), last.transactions(), last.timestamp(), last.difficulty(),
				last.pun()));
		Path chainFile = write("tampered.lethenon", CanonicalEncoding.encodeChain(chain));

		ChainRejected refused = assertThrows(ChainRejected.class,
			() -> ChainReplaySupport.verify(chainFile));

		assertTrue(refused.getMessage().contains("previous hash"), refused.getMessage());
	}

	@Test
	@DisplayName("a truncated file is refused with its name and the chain library's reason")
	void verify_refuses_aTruncatedFile() throws Exception
	{
		byte[] encoded = CanonicalEncoding.encodeChain(aChainWithOneTransfer());
		byte[] truncated = java.util.Arrays.copyOf(encoded, encoded.length / 2);
		Path chainFile = write("truncated.lethenon", truncated);
		String reason = assertThrows(IllegalArgumentException.class,
			() -> CanonicalEncoding.readChain(truncated)).getMessage();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainReplaySupport.verify(chainFile));

		assertTrue(refused.getMessage().contains("truncated.lethenon"), refused.getMessage());
		assertTrue(refused.getMessage().contains(reason),
			"the library says why the bytes cannot be read, the plugin which file they are: "
				+ refused.getMessage());
	}

	@Test
	@DisplayName("a file that is not there is not an empty chain")
	void verify_refuses_aMissingFile()
	{
		Path missing = new File(directory, "nothing-here.lethenon").toPath();

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainReplaySupport.verify(missing));

		assertTrue(refused.getMessage().contains("there is no chain file at"),
			refused.getMessage());
		assertTrue(refused.getMessage().contains("nothing-here.lethenon"), refused.getMessage());
	}

	@Test
	@DisplayName("an empty file is refused rather than replayed as a chain of nothing")
	void verify_refuses_anEmptyFile() throws Exception
	{
		Path empty = write("empty.lethenon", new byte[0]);

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainReplaySupport.verify(empty));

		assertTrue(refused.getMessage().contains("is empty"), refused.getMessage());
	}

	@Test
	@DisplayName("a blank path is answered with what to pick, not with a stack trace")
	void verify_refuses_aBlankPath()
	{
		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainReplaySupport.verify(Path.of("")));

		assertTrue(refused.getMessage().contains("no chain file was named"), refused.getMessage());
	}

	@Test
	@DisplayName("the chain view lists every block with its pun and whom it paid")
	void blocks_listEveryBlock_withItsPunAndBeneficiary() throws Exception
	{
		List<BlockBody> mined = aChainWithOneTransfer();
		Path chainFile = write("chain.lethenon", CanonicalEncoding.encodeChain(mined));

		List<ChainBlockRow> rows = ChainReplaySupport.blocks(chainFile);

		assertEquals(3, rows.size());
		ChainBlockRow genesis = rows.get(0);
		assertEquals(0L, genesis.height());
		// mining varies the pun, so the row has to show the one the block was mined with
		assertEquals(mined.get(0).pun(), genesis.pun());
		assertTrue(genesis.pun().startsWith("in the beginning was the pun"), genesis.pun());
		assertEquals(Genesis.NOBODY.toString(), genesis.paidTo(),
			"a genesis block pays the burn account (lethenon#148)");
		assertEquals(0, genesis.transfers());
		assertEquals(1_759_000_000_000L, genesis.timestamp());
		assertEquals(8, genesis.difficulty());
		ChainBlockRow first = rows.get(1);
		assertEquals(1L, first.height());
		assertEquals(holderKey.toString(), first.paidTo());
		assertEquals(0, first.transfers());
		ChainBlockRow third = rows.get(2);
		assertEquals(2L, third.height());
		assertEquals(mined.get(2).pun(), third.pun());
		assertEquals(holderKey.toString(), third.paidTo());
		assertEquals(1, third.transfers());
		assertEquals(1_759_000_240_000L, third.timestamp());
	}

	@Test
	@DisplayName("the chain view shows no block of a chain that does not verify")
	void blocks_refuses_aChainThatDoesNotReplay() throws Exception
	{
		List<BlockBody> chain = new ArrayList<>(aChainWithOneTransfer());
		BlockBody last = chain.getLast();
		chain.set(chain.size() - 1,
			new BlockBody(last.chainIdentifier(), last.height(), Bytes.of(new byte[32]),
				last.beneficiary(), last.transactions(), last.timestamp(), last.difficulty(),
				last.pun()));
		Path chainFile = write("tampered.lethenon", CanonicalEncoding.encodeChain(chain));

		// a wrong previous hash, not a changed pun: at difficulty 8 a changed pun still meets the
		// difficulty once in 256 runs, which would make this test pass a chain it must refuse
		ChainRejected refused = assertThrows(ChainRejected.class,
			() -> ChainReplaySupport.blocks(chainFile));
		assertTrue(refused.getMessage().contains("previous hash"), refused.getMessage());
	}

	@Test
	@DisplayName("the chain view names a truncated file the same way the verifier does")
	void blocks_refuses_aTruncatedFile() throws Exception
	{
		byte[] encoded = CanonicalEncoding.encodeChain(aChainWithOneTransfer());
		Path chainFile = write("cut.lethenon",
			java.util.Arrays.copyOf(encoded, encoded.length / 2));

		IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
			() -> ChainReplaySupport.blocks(chainFile));

		assertTrue(refused.getMessage().contains("cut.lethenon"), refused.getMessage());
	}

	private Path write(final String name, final byte[] bytes) throws Exception
	{
		Path file = new File(directory, name).toPath();
		Files.write(file, bytes);
		return file;
	}

	/**
	 * A test chain in the shape lethenon 0.4.0 accepts: a genesis block for the burn account
	 * (lethenon#148), a block paying the holder its reward, and a third carrying one transfer out of
	 * it - mined at the minimum difficulty, which is what the chain's own tests use
	 */
	private List<BlockBody> aChainWithOneTransfer()
	{
		List<BlockBody> chain = new ArrayList<>(List.of(Genesis.candidate(Chain.TEST_IDENTIFIER,
			"in the beginning was the pun", 1_759_000_000_000L)));
		chain.add(Blocks.mine(Mining.nextBlock(chain, holderKey, List.of(), "the holder's block",
			1_759_000_120_000L), 1_000_000L).orElseThrow());
		SignedTransaction transfer = TransactionSigner.sign(new TransactionBody(Chain.TEST_IDENTIFIER,
			0L, holderKey, Destination.direct(Bytes.of(new byte[] { 7 })), Amount.ofLeth(3L),
			Amount.ZERO, "a protest in three lethe"), SignatureSuite.ED25519, holder.getPrivate());
		chain.add(Blocks.mine(Mining.nextBlock(chain, holderKey, List.of(transfer), "the third pun",
			1_759_000_240_000L), 1_000_000L).orElseThrow());
		return List.copyOf(chain);
	}
}
