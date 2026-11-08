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

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.transport.PeerAddress;

/**
 * Which node of this program serves a chain file (#814): found however the file's path is spelled,
 * forgotten once its node stopped, and never confused with a node started on the same file since
 */
class ServedChainFilesTest
{

	@TempDir
	Path directory;

	/**
	 * One way to write the path of a chain file
	 *
	 * @param spelling
	 *            what it looks like, in words
	 * @param of
	 *            writes the file's absolute path that way
	 */
	record Spelling(String spelling, UnaryOperator<Path> of)
	{
		@Override
		public String toString()
		{
			return spelling;
		}
	}

	static Stream<Arguments> spellings()
	{
		return Stream.of(Arguments.of(new Spelling("as the node was started on it", path -> path)),
			Arguments.of(new Spelling("with a dot in it",
				path -> path.getParent().resolve(".").resolve(path.getFileName()))),
			Arguments.of(new Spelling("through its parent and back",
				path -> path.getParent().resolve("..").resolve(path.getParent().getFileName())
					.resolve(path.getFileName()))),
			Arguments.of(new Spelling("relative to the working directory",
				path -> Path.of("").toAbsolutePath().relativize(path))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("spellings")
	void aServedFile_isFound_howeverItsPathIsSpelled(final Spelling spelling)
	{
		Path chainFile = directory.resolve("served.lethenon");
		ServedChainFiles.serve(chainFile, 18480);
		try
		{
			assertEquals(Optional.of(new PeerAddress("127.0.0.1", 18480)),
				ServedChainFiles.nodeFor(spelling.of().apply(chainFile)));
		}
		finally
		{
			ServedChainFiles.release(chainFile, 18480);
		}
	}

	@Test
	void aFileNoNodeOfThisProgramServes_hasNoNode()
	{
		assertEquals(Optional.empty(),
			ServedChainFiles.nodeFor(directory.resolve("not-served.lethenon")));
	}

	@Test
	void aStoppedNode_isForgotten()
	{
		Path chainFile = directory.resolve("stopped.lethenon");
		ServedChainFiles.serve(chainFile, 18480);

		ServedChainFiles.release(chainFile, 18480);

		assertEquals(Optional.empty(), ServedChainFiles.nodeFor(chainFile));
	}

	@Test
	void anOlderNodeStopping_leavesTheNodeStartedSinceOnTheSameFile()
	{
		Path chainFile = directory.resolve("restarted.lethenon");
		ServedChainFiles.serve(chainFile, 18480);
		ServedChainFiles.serve(chainFile, 18481);

		ServedChainFiles.release(chainFile, 18480);

		try
		{
			assertEquals(Optional.of(new PeerAddress("127.0.0.1", 18481)),
				ServedChainFiles.nodeFor(chainFile));
		}
		finally
		{
			ServedChainFiles.release(chainFile, 18481);
		}
	}
}
