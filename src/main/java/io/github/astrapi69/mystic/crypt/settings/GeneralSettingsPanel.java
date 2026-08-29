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

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;

/**
 * The "General" tab of the settings dialog: choose the Swing look and feel (applied live) and the
 * UI language (applied on the next start). Both are written straight into the shared
 * {@link MysticCryptSettings} so the dialog can persist them when it closes.
 * <p>
 * That settings object is the model of this panel: both combo boxes are bound to it, so a choice is
 * in the model the moment it is made, and the look-and-feel switch reads the name from there
 * instead of asking a combo box what it currently shows.
 */
public class GeneralSettingsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The languages the user interface is offered in, as language tags */
	private static final String[] LANGUAGES = { "en", "de" };

	/** The settings this panel edits; both combo boxes below write into it */
	private final transient MysticCryptSettings settings;

	private final JMComboBox<String, ?> cmbLookAndFeel = new JMComboBox<>(
		installedLookAndFeelNames());

	private final JMComboBox<String, ?> cmbLanguage = new JMComboBox<>(LANGUAGES);

	private final JMComboBox<FrameMode, EnumComboBoxModel<FrameMode>> cmbViewMode = new JMComboBox<>(
		new EnumComboBoxModel<>(FrameMode.class));

	/**
	 * Instantiates a new {@link GeneralSettingsPanel} over the settings it edits
	 *
	 * @param settings
	 *            the settings this panel reads from and writes into
	 */
	public GeneralSettingsPanel(MysticCryptSettings settings)
	{
		super(new BorderLayout());
		this.settings = settings;

		cmbLookAndFeel.setName("cmbLookAndFeel");
		cmbLanguage.setName("cmbLanguage");
		cmbViewMode.setName("cmbViewMode");
		bindComponents();
		// added after the binding on purpose: binding selects what the settings already hold, and
		// that must not switch the look and feel while the dialog is still being built
		cmbLookAndFeel.addActionListener(
			e -> applyLookAndFeel(GeneralSettingsPanel.this.settings.getLookAndFeel()));

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
		form.add(new JLabel("Look and feel:"));
		form.add(cmbLookAndFeel);
		form.add(new JLabel("Language (applies after restart):"));
		form.add(cmbLanguage);
		form.add(new JLabel("View:"));
		form.add(cmbViewMode);
		add(form, BorderLayout.NORTH);
	}

	/**
	 * Binds both combo boxes to the settings object, so that a choice lands there as it is made and
	 * the combo boxes start on what the settings already hold
	 */
	private void bindComponents()
	{
		cmbLookAndFeel
			.setPropertyModel(LambdaModel.of(settings::getLookAndFeel, settings::setLookAndFeel));
		cmbLanguage.setPropertyModel(LambdaModel.of(settings::getLanguage, settings::setLanguage));
		cmbViewMode.setPropertyModel(LambdaModel.of(settings::getViewMode, settings::setViewMode));
	}

	/**
	 * The names of the look and feels this Java runtime has installed
	 *
	 * @return the names of the installed look and feels
	 */
	private static String[] installedLookAndFeelNames()
	{
		return Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(UIManager.LookAndFeelInfo::getName).toArray(String[]::new);
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
				LookAndFeel current = UIManager.getLookAndFeel();
				if (current != null && current.getClass().getName().equals(info.getClassName()))
				{
					// already the active look and feel - skip the needless updateComponentTreeUI
					// pass (it re-installs every menu's UI and spams KeyboardManager warnings)
					return;
				}
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
