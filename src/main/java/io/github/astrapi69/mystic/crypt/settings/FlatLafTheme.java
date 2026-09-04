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
package io.github.astrapi69.mystic.crypt.settings;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import lombok.Getter;

/**
 * FlatLaf's (github.com/JFormDesigner/FlatLaf) four bundled themes, offered alongside the
 * JDK-bundled look and feels. {@link #installAll()} registers them with {@link UIManager} by name,
 * which is what makes them appear in {@link GeneralSettingsPanel}'s model-backed look-and-feel
 * dropdown and lets a choice survive a restart the same way any JDK-bundled look and feel does
 * (#125) - without that registration they were only ever known to the "Look and Feel" menu, which
 * builds its items from the classes directly and does not persist a choice.
 */
@Getter
public enum FlatLafTheme
{
	LIGHT("FlatLaf Light", FlatLightLaf.class), DARK("FlatLaf Dark", FlatDarkLaf.class), INTELLIJ(
		"FlatLaf IntelliJ",
		FlatIntelliJLaf.class), DARCULA("FlatLaf Darcula", FlatDarculaLaf.class);

	private final String label;

	private final Class<? extends LookAndFeel> lookAndFeelClass;

	FlatLafTheme(final String label, final Class<? extends LookAndFeel> lookAndFeelClass)
	{
		this.label = label;
		this.lookAndFeelClass = lookAndFeelClass;
	}

	/**
	 * Registers every theme with {@link UIManager} under its label, so it is found by
	 * {@link UIManager#getInstalledLookAndFeels()} wherever the application already looks up
	 * installed look and feels by name. Must run before the settings dropdown is built and before a
	 * persisted look-and-feel choice is replayed on startup.
	 * <p>
	 * Also turns on FlatLaf's built-in reveal-password button for every
	 * {@link javax.swing.JPasswordField} in the application, one {@link UIManager} default instead
	 * of a change to every panel that has a password field (#198). It has to run this early because
	 * the sign-in dialog - the very first password field shown - is created before anything else.
	 * The default is inert under a non-FlatLaf look and feel (Nimbus and the other JDK-bundled ones
	 * stay selectable in this application): nothing reads it, so nothing changes.
	 */
	public static void installAll()
	{
		for (final FlatLafTheme theme : values())
		{
			UIManager.installLookAndFeel(
				new UIManager.LookAndFeelInfo(theme.label, theme.lookAndFeelClass.getName()));
		}
		UIManager.put("PasswordField.showRevealButton", true);
	}
}
