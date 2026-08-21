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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Arrays;

import javax.swing.*;

/**
 * The "General" tab of the settings dialog: choose the Swing look and feel (applied live) and the
 * UI language (applied on the next start). Both are written straight into the shared
 * {@link MysticCryptSettings} so the dialog can persist them when it closes.
 */
public class GeneralSettingsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final transient MysticCryptSettings settings;

	public GeneralSettingsPanel(MysticCryptSettings settings)
	{
		super(new BorderLayout());
		this.settings = settings;

		String[] lookAndFeelNames = Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(UIManager.LookAndFeelInfo::getName).toArray(String[]::new);
		JComboBox<String> lookAndFeelCombo = new JComboBox<>(lookAndFeelNames);
		lookAndFeelCombo.setName("cmbLookAndFeel");
		lookAndFeelCombo.setSelectedItem(settings.getLookAndFeel());
		lookAndFeelCombo.addActionListener(e -> {
			String name = (String)lookAndFeelCombo.getSelectedItem();
			settings.setLookAndFeel(name);
			applyLookAndFeel(name);
		});

		JComboBox<String> languageCombo = new JComboBox<>(new String[] { "en", "de" });
		languageCombo.setName("cmbLanguage");
		languageCombo.setSelectedItem(settings.getLanguage());
		languageCombo
			.addActionListener(e -> settings.setLanguage((String)languageCombo.getSelectedItem()));

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
		form.add(new JLabel("Look and feel:"));
		form.add(lookAndFeelCombo);
		form.add(new JLabel("Language (applies after restart):"));
		form.add(languageCombo);
		add(form, BorderLayout.NORTH);
	}

	/**
	 * Applies the installed look and feel with the given name to every open window
	 *
	 * @param name
	 *            the look and feel name as reported by {@link UIManager.LookAndFeelInfo#getName()}
	 */
	public static void applyLookAndFeel(String name)
	{
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
		{
			if (info.getName().equals(name))
			{
				try
				{
					UIManager.setLookAndFeel(info.getClassName());
					for (Window window : Window.getWindows())
					{
						SwingUtilities.updateComponentTreeUI(window);
					}
				}
				catch (Exception exception)
				{
					// ignore - an unavailable look and feel is not worth failing the dialog for
				}
				return;
			}
		}
	}
}
