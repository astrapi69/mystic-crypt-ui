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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The window derives the key off the event dispatch thread, so it keeps answering while it works,
 * says that it works and offers Cancel (#1053). A cancelled file operation writes nothing, and a
 * cancelled text operation shows nothing. The work here runs on a worker the test holds, so each
 * state between the press and the answer can be looked at
 */
class FileCryptPanelWorksOffTheEventThreadTest
{

	static
	{
		System.setProperty("java.awt.headless", "true");
	}

	private static final String PASSPHRASE = "throwaway-" + java.util.UUID.randomUUID();

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/** A worker that keeps what it was given until the test lets it run */
	private static final class HeldWork implements Executor
	{
		private final List<Runnable> held = new ArrayList<>();

		@Override
		public void execute(final Runnable command)
		{
			held.add(command);
		}

		void runAll()
		{
			List<Runnable> waiting = new ArrayList<>(held);
			held.clear();
			waiting.forEach(Runnable::run);
		}
	}

	private final HeldWork work = new HeldWork();

	private FileCryptPanel panelOverTheHeldWork()
	{
		return new FileCryptPanel(work, Runnable::run);
	}

	@Test
	@DisplayName("while the work runs, the window says so, offers Cancel and takes no second press (#1053)")
	void whileTheWorkRuns_theWindowSaysSo_andOffersCancel()
	{
		FileCryptPanel panel = panelOverTheHeldWork();
		type(panel, "txtPlainText", "a note to myself");
		type(panel, "pwdText", PASSPHRASE);
		type(panel, "pwdTextRepeated", PASSPHRASE);

		click(panel, "btnEncryptText");

		assertEquals(
			FileCryptMessages.getString("filecrypt.result.working",
				"working: the key is being derived from the passphrase ..."),
			panel.getResultText());
		assertTrue(find(panel, "btnCancelFileCrypt").isVisible(), "nothing offers to cancel");
		assertFalse(find(panel, "btnEncryptText").isEnabled(), "a second press is taken");
		assertFalse(find(panel, "btnDecryptFile").isEnabled(), "a second press is taken");

		work.runAll();

		assertEquals("encrypted 16 characters", panel.getResultText());
		assertFalse(find(panel, "btnCancelFileCrypt").isVisible(), "Cancel stays after the work");
		assertTrue(find(panel, "btnEncryptText").isEnabled(), "the buttons stay off");
		assertFalse(textOf(panel, "txtEncryptedText").isEmpty(), "the encrypted text is missing");
	}

	@Test
	@DisplayName("a file encryption cancelled while the key is derived writes no file (#1053)")
	void aFileEncryptionCancelledWhileTheKeyIsDerived_writesNoFile(@TempDir final File directory)
		throws Exception
	{
		File source = new File(directory, "secret.txt");
		Files.writeString(source.toPath(), "content");
		File target = new File(directory, "secret.txt.mcenc");
		FileCryptPanel panel = panelOverTheHeldWork();
		type(panel, "txtSourceFile", source.getAbsolutePath());
		type(panel, "txtTargetFile", target.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);
		type(panel, "pwdFileRepeated", PASSPHRASE);

		click(panel, "btnEncryptFile");
		click(panel, "btnCancelFileCrypt");
		work.runAll();

		assertFalse(target.exists(), "the cancelled encryption wrote the file");
		assertEquals("not encrypted: " + FileCryptMessages.getString("filecrypt.result.cancelled",
			"cancelled before anything was written"), panel.getResultText());
		assertTrue(source.exists(), "the source went");
	}

	@Test
	@DisplayName("a cancel that arrives after the file was written reports the file, which is there (#1053)")
	void aCancelAfterTheFileWasWritten_reportsTheFile(@TempDir final File directory)
		throws Exception
	{
		File source = new File(directory, "secret.txt");
		Files.writeString(source.toPath(), "content");
		File target = new File(directory, "secret.txt.mcenc");
		HeldWork screen = new HeldWork();
		FileCryptPanel panel = new FileCryptPanel(work, screen);
		type(panel, "txtSourceFile", source.getAbsolutePath());
		type(panel, "txtTargetFile", target.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);
		type(panel, "pwdFileRepeated", PASSPHRASE);

		click(panel, "btnEncryptFile");
		work.runAll();
		click(panel, "btnCancelFileCrypt");
		screen.runAll();

		assertTrue(target.exists(), "the file was not written before the cancel");
		assertEquals("encrypted to " + target.getName(), panel.getResultText(),
			"the window says nothing was written while the file is there");
		assertEquals(target.getAbsolutePath(), textOf(panel, "txtTargetFile"));
	}

	@Test
	@DisplayName("a text encryption cancelled while the key is derived shows no text (#1053)")
	void aTextEncryptionCancelled_showsNoText()
	{
		FileCryptPanel panel = panelOverTheHeldWork();
		type(panel, "txtPlainText", "a note to myself");
		type(panel, "pwdText", PASSPHRASE);
		type(panel, "pwdTextRepeated", PASSPHRASE);

		click(panel, "btnEncryptText");
		click(panel, "btnCancelFileCrypt");
		work.runAll();

		assertTrue(textOf(panel, "txtEncryptedText").isEmpty(), "the cancelled text shows");
		assertEquals("not encrypted: " + FileCryptMessages.getString("filecrypt.result.cancelled",
			"cancelled before anything was written"), panel.getResultText());
	}

	@Test
	@DisplayName("a file too large to read is refused with an answer, and the window takes presses again (#1053)")
	void aFileTooLargeToRead_isRefusedWithAnAnswer(@TempDir final File directory) throws Exception
	{
		File source = new File(directory, "larger-than-an-array.bin");
		try (RandomAccessFile sparse = new RandomAccessFile(source, "rw"))
		{
			// sparse: the length is there, the disk space is not taken
			sparse.setLength(Integer.MAX_VALUE + 1L);
		}
		File target = new File(directory, "larger-than-an-array.bin.mcenc");
		FileCryptPanel panel = panelOverTheHeldWork();
		type(panel, "txtSourceFile", source.getAbsolutePath());
		type(panel, "txtTargetFile", target.getAbsolutePath());
		type(panel, "pwdFile", PASSPHRASE);
		type(panel, "pwdFileRepeated", PASSPHRASE);

		click(panel, "btnEncryptFile");
		work.runAll();

		assertTrue(panel.getResultText().startsWith("not encrypted: "),
			"the window waits for an answer: " + panel.getResultText());
		assertTrue(find(panel, "btnEncryptFile").isEnabled(), "the buttons stay off");
		assertFalse(find(panel, "btnCancelFileCrypt").isVisible(), "Cancel stays after the work");
		assertFalse(target.exists(), "a file was written");
	}

	@Test
	@DisplayName("the window of the application does not do the work on the thread that pressed the button (#1053)")
	void theApplicationsWindow_doesNotWorkOnThePressingThread() throws Exception
	{
		FileCryptPanel panel = onTheEventThread(FileCryptPanel::new);
		SwingUtilities.invokeAndWait(() -> {
			type(panel, "txtPlainText", "a note to myself");
			type(panel, "pwdText", PASSPHRASE);
			type(panel, "pwdTextRepeated", PASSPHRASE);
			click(panel, "btnEncryptText");
		});

		assertEquals(
			FileCryptMessages.getString("filecrypt.result.working",
				"working: the key is being derived from the passphrase ..."),
			onTheEventThread(panel::getResultText),
			"the press returned with the answer, so the work ran on the event dispatch thread");
		long deadline = System.currentTimeMillis() + 30_000;
		while (!"encrypted 16 characters".equals(onTheEventThread(panel::getResultText))
			&& System.currentTimeMillis() < deadline)
		{
			Thread.sleep(20);
		}
		assertEquals("encrypted 16 characters", onTheEventThread(panel::getResultText));
		assertTrue(onTheEventThread(() -> find(panel, "btnEncryptText").isEnabled()),
			"the buttons stay off");
	}

	/** What the given read returns when it runs on the event dispatch thread */
	private static <T> T onTheEventThread(final java.util.function.Supplier<T> read)
		throws Exception
	{
		AtomicReference<T> value = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> value.set(read.get()));
		return value.get();
	}

	private void type(final Container panel, final String name, final String text)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((JTextComponent)component).setText(text);
	}

	private String textOf(final Container panel, final String name)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		return component instanceof JPasswordField password
			? new String(password.getPassword())
			: ((JTextComponent)component).getText();
	}

	private void click(final Container panel, final String name)
	{
		Component component = find(panel, name);
		assertNotNull(component, "no component named " + name);
		((AbstractButton)component).doClick();
	}

	private Component find(final Container container, final String name)
	{
		for (Component component : container.getComponents())
		{
			if (name.equals(component.getName()))
			{
				return component;
			}
			if (component instanceof Container child)
			{
				Component found = find(child, name);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
