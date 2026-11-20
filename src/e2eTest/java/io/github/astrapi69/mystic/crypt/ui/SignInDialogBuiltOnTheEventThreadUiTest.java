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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.UIManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.clipboard.ClipboardClearDecision;
import io.github.astrapi69.mystic.crypt.lock.IdleLockDecision;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileDialog;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * The sign-in dialog is built and shown on the event dispatch thread, with the persisted look and
 * feel applied before its first component exists (#1072).
 * <p>
 * It used to be built on the start thread, then the look was switched on that same thread. Swing
 * components are created and used on the event dispatch thread, and a look switch beside another
 * thread's component construction is what killed a start with {@code ClassCastException:
 * FlatLightLaf cannot be cast to NimbusLookAndFeel} at {@code NimbusStyle.validate}. The test
 * watches the look switch of one start: on which thread it happens, and whether a sign-in dialog
 * already exists when it does.
 */
class SignInDialogBuiltOnTheEventThreadUiTest extends AbstractUiTest
{

	/** What one change of the look and feel looked like when it happened */
	private record LookChange(String thread, boolean signInDialogExisted) {
	}

	@Test
	@DisplayName("the persisted look is switched on the event thread before any sign-in dialog exists, and the event thread holds the dialog's show")
	void theLookIsSwitchedOnTheEventThreadBeforeTheDialogExists()
	{
		File configurationDirectory = new File(tempHome, ".config/mystic-crypt-ui");
		configurationDirectory.mkdirs();
		new MysticCryptSettings("FlatLaf Dark", "en", FrameMode.APPLICATION_PANEL, true,
			IdleLockDecision.DEFAULT_TIMEOUT_MINUTES, false,
			IdleLockDecision.DEFAULT_CLOSE_LOCKED_MINUTES,
			ClipboardClearDecision.DEFAULT_CLEAR_AFTER_SECONDS, true).save(configurationDirectory);
		List<LookChange> changes = new CopyOnWriteArrayList<>();
		PropertyChangeListener watcher = event -> {
			if ("lookAndFeel".equals(event.getPropertyName()))
			{
				boolean dialogExisted = false;
				for (Window window : Window.getWindows())
				{
					dialogExisted |= window instanceof MasterPwFileDialog;
				}
				changes.add(new LookChange(Thread.currentThread().getName(), dialogExisted));
			}
		};
		UIManager.addPropertyChangeListener(watcher);
		try
		{
			launchApplication();
		}
		finally
		{
			UIManager.removePropertyChangeListener(watcher);
		}

		assertFalse(changes.isEmpty(), "the persisted look was applied at the start");
		for (LookChange change : changes)
		{
			assertTrue(change.thread().startsWith("AWT-EventQueue"),
				"the look is switched on the event thread, not on " + change.thread());
			assertFalse(change.signInDialogExisted(),
				"the look is switched before the first component of the sign-in dialog is built");
		}
		assertTrue(eventThreadHoldsTheDialogShow(),
			"the sign-in dialog is shown from the event thread, which the start thread waits on");
	}

	private static boolean eventThreadHoldsTheDialogShow()
	{
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
		{
			if (entry.getKey().getName().startsWith("AWT-EventQueue"))
			{
				for (StackTraceElement frame : entry.getValue())
				{
					if (frame.getClassName()
						.equals("io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame"))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}
