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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.JInternalFrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.lethenon.BlockBody;
import io.github.astrapi69.lethenon.CanonicalEncoding;
import io.github.astrapi69.lethenon.ChainFile;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A chain file mystic-crypt-ui 8.8 wrote, opened in this version (#581). lethenon 0.6.0 keeps the
 * chain in a database, and the plugin converts a file of the old format only once the person agreed
 * to it: the question says what happens, "Cancel" leaves the file as it was, "Convert" converts it,
 * and the file 8.8 wrote stays beside the database byte for byte. The file is the one 8.8's own
 * code wrote (src/e2eTest/resources/lethenon/README.md), and what the conversion wrote is read back
 * with the chain library.
 */
class LethenonChainFileOf88UiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String FIXTURE = "/lethenon/written-by-8.8.lethenon";

	/** As the fixture's README records it */
	private static final String FIXTURE_SHA256 = "8ff65fe9faadac967ed8f24008244da363f06346dd6866f12be68c3f9497d31d";

	private static final byte[] SQLITE_HEADER = "SQLite format 3\0"
		.getBytes(StandardCharsets.US_ASCII);

	@Test
	@DisplayName("a chain file of 8.8 is converted only once the person agreed, keeps its blocks, and stays beside the database as it was (#581)")
	void aChainFileOf88_isConvertedOnlyOnceThePersonAgreed() throws Exception
	{
		installPluginRequiringItBuilt(LETHENON_ZIP);
		byte[] writtenBy88 = theFixture();
		Path chainFile = new File(tempHome, "written-by-8.8.lethenon").toPath();
		Files.write(chainFile, writtenBy88);
		Path legacy = new File(tempHome, "written-by-8.8.lethenon.legacy").toPath();
		File databaseFile = new File(tempHome, "lethenon-8.8.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.openPluginTool("Verify a Chain", "Verify a Chain");
		JInternalFrameFixture tool = new JInternalFrameFixture(robot,
			application.internalFrame("Verify a Chain"));
		GuiActionRunner
			.execute(() -> tool.textBox("txtChainFile").target().setText(chainFile.toString()));

		JOptionPaneFixture question = verifyAndAwaitTheQuestion(tool);
		String asked = GuiActionRunner
			.execute(() -> String.valueOf(question.target().getMessage()));
		assertTrue(asked.contains("written-by-8.8.lethenon.legacy"), asked);
		assertTrue(asked.contains("8.9 and earlier cannot read written-by-8.8.lethenon"), asked);
		question.buttonWithText("Cancel").click();
		awaitResult(tool, "nothing was done");

		assertArrayEquals(writtenBy88, Files.readAllBytes(chainFile),
			"declined: the file stays as it was");
		assertFalse(Files.exists(legacy), "declined: nothing was converted");

		verifyAndAwaitTheQuestion(tool).buttonWithText("Convert").click();
		awaitResult(tool, "accepted");

		String blocks = GuiActionRunner.execute(() -> tool.textBox("txtBlocks").target().getText());
		assertEquals("3 blocks", blocks);
		assertArrayEquals(writtenBy88, Files.readAllBytes(legacy),
			"the file 8.8 wrote stays beside the database, byte for byte");
		assertArrayEquals(SQLITE_HEADER, Arrays.copyOf(Files.readAllBytes(chainFile), 16),
			"the chain file is a database now");
		List<BlockBody> converted = new ChainFile(chainFile).read();
		assertEquals(CanonicalEncoding.readChain(writtenBy88), converted,
			"the database holds the blocks 8.8 wrote");
		assertEquals(List.of("written by 8.8 #40", "a second block from 8.8 #193",
			"a third block from 8.8 #68"), converted.stream().map(BlockBody::pun).toList());
	}

	/** The chain file 8.8 wrote, checked against the sha256 its README records */
	private static byte[] theFixture() throws Exception
	{
		byte[] bytes;
		try (InputStream in = LethenonChainFileOf88UiTest.class.getResourceAsStream(FIXTURE))
		{
			assertNotNull(in, FIXTURE + " is on the class path");
			bytes = in.readAllBytes();
		}
		assertEquals(FIXTURE_SHA256,
			HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
			"the fixture is the file its README describes");
		return bytes;
	}

	private JOptionPaneFixture verifyAndAwaitTheQuestion(final JInternalFrameFixture tool)
	{
		SwingUtilities.invokeLater(() -> tool.button("btnVerify").target().doClick());
		return JOptionPaneFinder.findOptionPane().withTimeout(20, TimeUnit.SECONDS).using(robot);
	}

	private static void awaitResult(final JInternalFrameFixture tool, final String expected)
	{
		Pause.pause(new Condition("the result line says '" + expected + "'")
		{
			@Override
			public boolean test()
			{
				return GuiActionRunner.execute(() -> tool.label("lblResult").target().getText())
					.contains(expected);
			}
		}, 30000);
	}
}
