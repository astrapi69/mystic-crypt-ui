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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;

/**
 * A copied password does not sit on the clipboard forever, in the running application (#352).
 * <p>
 * The decision itself - when to clear, and the "only if nothing else was copied meanwhile"
 * comparison - is exhaustively unit tested in {@code ClipboardClearWatchdogTest}, without a
 * display. What can only be proven here is the wiring: that the real "Copy Password" action arms
 * the real watchdog the running frame started, reading the real setting from disk.
 * <p>
 * The interval is seeded to two seconds before sign-in - {@link MysticCryptSettings} is read fresh
 * when the watchdog starts, the same way every other seeded-setting test in this suite works - so
 * this does not wait out the twenty-second default
 */
class ClipboardClearsItselfAfterACopyUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();
	private static final String ENTRY_TITLE = "Clears Itself";
	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("a copied password is cleared again after the configured interval")
	void copiedPasswordIsClearedAfterTheInterval() throws Exception
	{
		seedClipboardClearSeconds(2);
		File databaseFile = new File(tempHome, "clipboard-clear-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.selectEntryRowByTitle(frame, ENTRY_TITLE);

		application.copyPasswordOfSelectedEntry(frame);
		assertEquals(ENTRY_PASSWORD, application.clipboardText(),
			"the precondition: the password really is on the clipboard before waiting for "
				+ "anything to clear it");

		Pause.pause(new Condition("the clipboard has been cleared")
		{
			@Override
			public boolean test()
			{
				return "".equals(application.clipboardText());
			}
		}, 15000);

		assertEquals("", application.clipboardText(),
			"the real 'Copy Password' action has to arm the real watchdog the running frame "
				+ "started, or nothing here ever clears");
	}

	@Test
	@DisplayName("copying something else before the interval survives the timer")
	void somethingCopiedAfterwardsIsNotDestroyed() throws Exception
	{
		seedClipboardClearSeconds(2);
		File databaseFile = new File(tempHome, "clipboard-no-clobber-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, ENTRY_TITLE, "someone", ENTRY_PASSWORD);
		application.selectEntryRowByTitle(frame, ENTRY_TITLE);
		application.copyPasswordOfSelectedEntry(frame);

		String somethingElse = "copied from somewhere else entirely, after the password";
		org.assertj.swing.edt.GuiActionRunner
			.execute(() -> io.github.astrapi69.awt.extension.ClipboardExtensions
				.copyToClipboard(somethingElse));

		// long enough for the armed timer to have fired at least once against the STALE armed
		// content, not long enough to be a slow test
		Pause.pause(3500);

		assertEquals(somethingElse, application.clipboardText(),
			"clearing unconditionally would destroy this - the whole reason the watchdog compares "
				+ "before clearing (#352)");
	}

	private void seedClipboardClearSeconds(final int seconds)
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		MysticCryptSettings settings = new MysticCryptSettings();
		settings.setClipboardClearSeconds(seconds);
		settings.save(configurationDirectory);
	}
}
