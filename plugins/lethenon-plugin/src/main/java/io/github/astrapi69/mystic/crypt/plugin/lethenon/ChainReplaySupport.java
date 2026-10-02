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
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.Replay;

/**
 * Replays a chain file through the chain library and hands back what was verified.
 * <p>
 * No chain logic of its own: {@link CanonicalEncoding#readChain} reads the bytes and
 * {@link Replay#verify} checks every signature, every state transition, every block hash and the
 * supply invariant. What this class adds is the two things a panel cannot do - naming the file in
 * the message, and turning a truncated file into a sentence instead of a
 * {@link BufferUnderflowException} out of a byte buffer.
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

	/** A chain read from its file, and the replay that accepted it */
	private record Replayed(List<BlockBody> chain, Replay replay) {
	}

	private static Replayed replayed(final Path chainFile) throws IOException
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
		byte[] encoded = Files.readAllBytes(chainFile);
		if (encoded.length == 0)
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath()
				+ " is empty, so it holds no genesis block to replay from");
		}
		List<BlockBody> chain = readChain(chainFile, encoded);
		return new Replayed(chain, Replay.verify(chain));
	}

	/**
	 * A file that stops in the middle of a block comes out of the buffer as a
	 * {@link BufferUnderflowException}, whose message is null - useless in a dialog. The file is
	 * named here instead, which is what makes a bug report about it actionable.
	 */
	private static List<BlockBody> readChain(final Path chainFile, final byte[] encoded)
	{
		try
		{
			return CanonicalEncoding.readChain(encoded);
		}
		catch (BufferUnderflowException truncated)
		{
			throw new IllegalArgumentException(chainFile.toAbsolutePath() + " is " + encoded.length
				+ " bytes and ends in the middle of a block: it is truncated, or it is not a "
				+ "lethenon chain file", truncated);
		}
	}
}
