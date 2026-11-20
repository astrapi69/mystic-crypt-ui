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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * A {@code memoizedSignin.json} that cannot be read, a JSON cut short by a crash or another
 * program, does not stop the start, and it is kept rather than written over (#1082)
 */
class ARememberedSignInThatCannotBeReadUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	/** A remembered sign-in cut off in the middle of its first value */
	private static final byte[] CUT_SHORT = "{\"selectedApplicationFilePath\": \"/home/user/va"
		.getBytes(StandardCharsets.UTF_8);

	@Test
	@DisplayName("a remembered sign-in that cannot be read still opens the sign-in dialog, is kept aside after the sign-in, and the user is told")
	void aRememberedSignInThatCannotBeRead_isKeptAside_andTheUserIsTold() throws Exception
	{
		File databaseFile = new File(tempHome, "cut-short.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		File memoizedSigninFile = writeTheRememberedSignIn(CUT_SHORT);

		signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		DialogFixture told = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
		{
			@Override
			protected boolean isMatching(final Dialog dialog)
			{
				return "A file could not be read".equals(dialog.getTitle()) && dialog.isShowing();
			}
		}).withTimeout(15, TimeUnit.SECONDS).using(robot);
		String message = GuiActionRunner.execute(() -> textOf(told.target()));
		told.button(JButtonMatcher.withText("OK")).click();

		File[] keptAside = memoizedSigninFile.getParentFile()
			.listFiles((directory, name) -> name.startsWith(
				MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME + ".unreadable-"));
		assertEquals(1, keptAside.length, "the old remembered sign-in is kept aside");
		assertArrayEquals(CUT_SHORT, Files.readAllBytes(keptAside[0].toPath()),
			"with its bytes unchanged");
		assertTrue(
			message.contains(memoizedSigninFile.getAbsolutePath())
				&& message.contains(keptAside[0].getName()),
			"the message names the file and where it was kept: " + message);
		assertTrue(Files.readString(memoizedSigninFile.toPath()).contains("cut-short.mcrdb"),
			"the sign-in is remembered anew");
	}

	/** The texts of the labels in the given window, where a JOptionPane puts its message */
	private static String textOf(final java.awt.Container container)
	{
		StringBuilder text = new StringBuilder();
		for (java.awt.Component component : container.getComponents())
		{
			if (component instanceof javax.swing.JLabel label && label.getText() != null)
			{
				text.append(label.getText()).append(' ');
			}
			if (component instanceof java.awt.Container inner)
			{
				text.append(textOf(inner));
			}
		}
		return text.toString();
	}

	private File writeTheRememberedSignIn(final byte[] content) throws IOException
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		assertTrue(configurationDirectory.mkdirs() || configurationDirectory.isDirectory());
		File memoizedSigninFile = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		Files.write(memoizedSigninFile.toPath(), content);
		return memoizedSigninFile;
	}
}
