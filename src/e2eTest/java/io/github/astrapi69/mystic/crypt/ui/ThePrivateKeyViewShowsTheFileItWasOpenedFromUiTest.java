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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JInternalFrame;
import javax.swing.JTextArea;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.exception.WaitTimedOutError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What the private key viewer shows is the PEM file it was opened from.
 * <p>
 * {@code OpenPrivateKeyAction} reads a PEM file with file-worker's
 * {@code ReadFileExtensions.fromFile}, which decodes strictly since 21.0. The smoke test of the
 * viewer only checks that its internal frame opens; this one reads the text the frame shows and
 * compares it with the file's own content, so a read that returns nothing, a part, or a replaced
 * character is seen.
 */
class ThePrivateKeyViewShowsTheFileItWasOpenedFromUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("the private key view shows the text of the PEM file it was opened from")
	void thePrivateKeyView_showsThePemFileText() throws Exception
	{
		File databaseFile = new File(tempHome, "private-key-view.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		File privateKeyPemFile = new File(tempHome, "viewed-private-key.pem");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyPemFile);
		String pemText = Files.readString(privateKeyPemFile.toPath());
		assertTrue(pemText.startsWith("-----BEGIN"), "precondition: a PEM file: " + pemText);

		application.openPrivateKeyViaMenu(privateKeyPemFile);

		JInternalFrame view = application.requireInternalFrame("Private key view");
		List<String> shown = GuiActionRunner.execute(
			() -> robot.finder().findAll(view, new GenericTypeMatcher<JTextArea>(JTextArea.class)
			{
				@Override
				protected boolean isMatching(final JTextArea textArea)
				{
					return true;
				}
			}).stream().map(JTextArea::getText).collect(Collectors.toList()));
		assertTrue(shown.contains(pemText),
			"one of the text areas of the view holds the PEM file's text, the areas hold: "
				+ shown);

		application.closeInternalFrame("Private key view");
	}

	/** A line after the key, in Windows-1252: what a PEM file from a tool or a person may carry */
	private static final String WINDOWS_1252_LINE = "Schl\u00fcssel von J\u00fcrgen\n";

	@Test
	@DisplayName("a PEM file with a line in Windows-1252 does not open the private key view (today, #1081)")
	void aPemFileWithAWindows1252Line_doesNotOpenTheView() throws Exception
	{
		File databaseFile = new File(tempHome, "private-key-view-1252.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		File privateKeyPemFile = new File(tempHome, "viewed-private-key-1252.pem");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyPemFile);
		appendInWindows1252(privateKeyPemFile, WINDOWS_1252_LINE);

		assertThrows(WaitTimedOutError.class,
			() -> application.openPrivateKeyViaMenu(privateKeyPemFile),
			"file-worker 21 reads the PEM file strictly, the action gives up and opens no view");
	}

	private static void appendInWindows1252(final File file, final String line) throws Exception
	{
		byte[] pem = Files.readAllBytes(file.toPath());
		assertTrue(new String(pem, StandardCharsets.US_ASCII).startsWith("-----BEGIN"),
			"precondition: a PEM file");
		byte[] appended = line.getBytes(Charset.forName("windows-1252"));
		byte[] all = new byte[pem.length + appended.length];
		System.arraycopy(pem, 0, all, 0, pem.length);
		System.arraycopy(appended, 0, all, pem.length, appended.length);
		Files.write(file.toPath(), all);
	}
}
