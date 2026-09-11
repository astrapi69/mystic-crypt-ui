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
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.lock.IdleLockDecision;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMSpinner;

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

	private final JMCheckBox chkTooltipsEnabled = new JMCheckBox("Show tooltips");

	/**
	 * The idle timeout in minutes, 0 meaning off. A spinner rather than a free text field so a
	 * settings value that cannot lock at all - a negative one, a typo - cannot be entered here
	 */
	private final JMCheckBox chkSaveWhenLocking = new JMCheckBox();

	private final JMSpinner<Integer> spnAutoLockMinutes = new JMSpinner<>(new SpinnerNumberModel(
		IdleLockDecision.DEFAULT_TIMEOUT_MINUTES, IdleLockDecision.OFF, 480, 1));

	/**
	 * After how many further minutes a locked vault is closed, 0 meaning off. Same shape as the
	 * spinner above and for the same reason: a value that cannot close at all must not be typeable
	 */
	private final JMSpinner<Integer> spnCloseLockedMinutes = new JMSpinner<>(new SpinnerNumberModel(
		IdleLockDecision.DEFAULT_CLOSE_LOCKED_MINUTES, IdleLockDecision.OFF, 480, 1));

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
		cmbViewMode.setToolTipText(Messages.getString("settings.general.tooltip.view.mode",
			"whether the application opens as a desktop-style window manager or a single panel"));
		chkTooltipsEnabled.setName("chkTooltipsEnabled");
		chkSaveWhenLocking.setName("chkSaveWhenLocking");
		chkSaveWhenLocking.setToolTipText(Messages.getString(
			"settings.general.tooltip.save.on.lock",
			"whether locking writes pending changes to the file; off by default, so a timer never "
				+ "commits a change you had not decided about"));
		spnAutoLockMinutes.setName("spnAutoLockMinutes");
		spnCloseLockedMinutes.setName("spnCloseLockedMinutes");
		spnCloseLockedMinutes.setToolTipText(Messages.getString(
			"settings.general.tooltip.close.locked",
			"after how many further minutes a locked database is closed, so its decrypted content "
				+ "leaves memory; 0 turns it off"));
		spnAutoLockMinutes.setToolTipText(Messages.getString("settings.general.tooltip.auto.lock",
			"after how many idle minutes the workspace locks itself; 0 turns it off"));
		bindComponents();
		// added after the binding on purpose: binding selects what the settings already hold, and
		// that must not switch the look and feel while the dialog is still being built
		cmbLookAndFeel.addActionListener(
			e -> applyLookAndFeel(GeneralSettingsPanel.this.settings.getLookAndFeel()));
		// same reasoning as the look-and-feel listener above: added after binding, so building the
		// panel does not itself flip the shared ToolTipManager
		chkTooltipsEnabled.addActionListener(
			e -> applyTooltipsEnabled(GeneralSettingsPanel.this.settings.isTooltipsEnabled()));

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
		form.add(new JLabel("Look and feel:"));
		form.add(cmbLookAndFeel);
		form.add(new JLabel("Language (applies after restart):"));
		form.add(cmbLanguage);
		form.add(new JLabel("View:"));
		form.add(cmbViewMode);
		form.add(new JLabel("Tooltips:"));
		form.add(chkTooltipsEnabled);
		form.add(new JLabel("Save when locking:"));
		form.add(chkSaveWhenLocking);
		form.add(new JLabel("Lock after idle minutes (0 = off):"));
		form.add(spnAutoLockMinutes);
		form.add(new JLabel("Close a locked database after minutes (0 = off):"));
		form.add(spnCloseLockedMinutes);
		add(form, BorderLayout.NORTH);
	}

	/**
	 * Binds every component to the settings object, so that a choice lands there as it is made and
	 * the components start on what the settings already hold
	 */
	private void bindComponents()
	{
		cmbLookAndFeel
			.setPropertyModel(LambdaModel.of(settings::getLookAndFeel, settings::setLookAndFeel));
		cmbLanguage.setPropertyModel(LambdaModel.of(settings::getLanguage, settings::setLanguage));
		cmbViewMode.setPropertyModel(LambdaModel.of(settings::getViewMode, settings::setViewMode));
		chkTooltipsEnabled.setPropertyModel(
			LambdaModel.of(settings::isTooltipsEnabled, settings::setTooltipsEnabled));
		chkSaveWhenLocking.setPropertyModel(
			LambdaModel.of(settings::isSaveWhenLocking, settings::setSaveWhenLocking));
		spnAutoLockMinutes.setPropertyModel(
			LambdaModel.of(settings::getAutoLockMinutes, settings::setAutoLockMinutes));
		spnCloseLockedMinutes.setPropertyModel(LambdaModel.of(settings::getCloseLockedAfterMinutes,
			settings::setCloseLockedAfterMinutes));
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
					updateEveryWindowOnTheEventDispatchThread();
				}
				catch (Exception exception)
				{
					// ignore - an unavailable look and feel is not worth failing the dialog for
				}
				return;
			}
		}
	}

	/**
	 * Turns tooltips on or off for the whole application - a single JDK-wide switch, so this needs
	 * no per-component work in any of the panels that call {@code setToolTipText(...)}
	 *
	 * @param enabled
	 *            whether tooltips are shown
	 */
	public static void applyTooltipsEnabled(boolean enabled)
	{
		ToolTipManager.sharedInstance().setEnabled(enabled);
	}

	/**
	 * Re-installs the look and feel on every open window, always on the event dispatch thread.
	 * <p>
	 * {@code updateComponentTreeUI} takes the AWT tree lock and then asks a text component for its
	 * preferred size, which needs that component's document. Called from another thread while the
	 * event dispatch thread is writing into such a document - the console tool does exactly that
	 * with the output it captures - the two block each other and the application hangs at startup.
	 * The application builds its frame outside the event dispatch thread
	 * ({@code StartMysticCryptApplication}), so this cannot be left to the caller.
	 */
	private static void updateEveryWindowOnTheEventDispatchThread()
	{
		Runnable updateAll = () -> {
			for (Window window : Window.getWindows())
			{
				SwingUtilities.updateComponentTreeUI(window);
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			updateAll.run();
			return;
		}
		try
		{
			SwingUtilities.invokeAndWait(updateAll);
		}
		catch (final InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
		catch (final java.lang.reflect.InvocationTargetException failed)
		{
			// same as above: an unavailable look and feel is not worth failing the caller for
		}
	}
}
