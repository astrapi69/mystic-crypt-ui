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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.Replay;

/**
 * Replays a chain file through the chain library and hands back what was verified.
 * <p>
 * No chain logic of its own: {@link ChainFiles#read} reads the file - since lethenon 0.6.0 a
 * database, which a file of 0.5.0 is migrated into once it verified (#581) - and
 * {@link Replay#verify} checks every signature, every state transition, every block hash and the
 * supply invariant. What this class adds is what a panel cannot do: naming the file in every
 * refusal. The chain library says why bytes cannot be read; which file they came from, only the
 * caller knows.
 * <p>
 * It takes a {@link Path} and returns a record, so it is testable without a display
 * (architecture.md layer 2: no Swing types in the support layer).
 */
public final class ChainReplaySupport
{

	private ChainReplaySupport()
	{
	}

	/**
	 * Replays the chain in the given file
	 *
	 * @param chainFile
	 *            the file to replay
	 * @return what the replay verified
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws IllegalArgumentException
	 *             when no file was named, nothing is there, or what is there is not a chain file
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain is a chain and does not verify, with the reason in the message
	 */
	public static ChainReplayReport verify(final Path chainFile) throws IOException
	{
		Replay replay = replayed(chainFile).replay();
		return new ChainReplayReport(replay.blocks(), replay.transactions(), replay.signatures(),
			replay.describe());
	}

	/**
	 * Lists the blocks of the chain in the given file, for the chain view. The whole chain is
	 * replayed first, exactly as {@link #verify(Path)} does, so a block is listed only when every
	 * block before and after it verified as well.
	 *
	 * @param chainFile
	 *            the file to read
	 * @return one row per block, genesis first
	 * @throws IOException
	 *             when the file cannot be read
	 * @throws IllegalArgumentException
	 *             when no file was named, nothing is there, or what is there is not a chain file
	 * @throws io.github.astrapi69.lethenon.ChainRejected
	 *             when the chain is a chain and does not verify, with the reason in the message
	 */
	public static List<ChainBlockRow> blocks(final Path chainFile) throws IOException
	{
		return replayed(chainFile).chain().stream()
			.map(block -> new ChainBlockRow(block.height(), block.pun(),
				block.beneficiary().toString(), block.transactions().size(), block.timestamp(),
				block.difficulty()))
			.toList();
	}

	/**
	 * A chain read from its file, and the replay that accepted it
	 *
	 * @param chain
	 *            the blocks, genesis first
	 * @param replay
	 *            what the replay verified, final state included
	 */
	record Replayed(List<BlockBody> chain, Replay replay) {
	}

	/**
	 * Reads and replays a chain file: the one path every tool of this plugin reads a chain through,
	 * so none of them can show anything from a chain the others would refuse
	 */
	static Replayed replayed(final Path chainFile) throws IOException
	{
		if (chainFile == null || chainFile.toString().isBlank())
		{
			throw new IllegalArgumentException("no chain file was named: pick the file a lethenon "
				+ "command line wrote, usually chain.lethenon");
		}
		if (!Files.isRegularFile(chainFile))
		{
			throw new IllegalArgumentException("there is no chain file at "
				+ chainFile.toAbsolutePath() + "; a chain starts when its genesis block is mined");
		}
		if (Files.size(chainFile) == 0)
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath()
				+ " is empty, so it holds no genesis block to replay from");
		}
		List<BlockBody> chain = ChainFiles.read(chainFile);
		return new Replayed(chain, replay(chainFile, chain));
	}

	/**
	 * Replays a chain read from a file, and puts the file in front of a refusal, as
	 * {@code readChain} does for bytes it cannot read (#532): the library's reason cannot name the
	 * file, and a refusal without it leaves open which file was meant - a chain an earlier lethenon
	 * wrote, refused by name since lethenon 0.4.0, among them (#535)
	 *
	 * @param chainFile
	 *            the file the chain was read from
	 * @param chain
	 *            the blocks, genesis first
	 * @return what the replay verified
	 * @throws ChainRejected
	 *             with the file's absolute path in front of the library's reason
	 */
	static Replay replay(final Path chainFile, final List<BlockBody> chain)
	{
		try
		{
			return Replay.verify(chain);
		}
		catch (ChainRejected refused)
		{
			throw new ChainRejected(chainFile.toAbsolutePath() + ": " + refused.getMessage());
		}
	}
}
