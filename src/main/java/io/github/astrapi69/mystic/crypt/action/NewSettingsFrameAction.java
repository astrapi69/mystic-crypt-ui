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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.app.file.KeyFilePathsNotRemovedException;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.RememberedKeyFileSupport;
import io.github.astrapi69.mystic.crypt.settings.LookAndFeelChoice;
import io.github.astrapi69.mystic.crypt.settings.MysticCryptSettings;
import io.github.astrapi69.mystic.crypt.settings.SettingsPanel;

/**
 * Opens the modal settings dialog (Plugins, Plugin settings and General tabs). The settings are
 * persisted to the configuration directory when the dialog is closed; plugin enable/disable state
 * is persisted by pf4j itself as it happens.
 */
public class NewSettingsFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	public NewSettingsFrameAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		MysticCryptSettings settings = LookAndFeelChoice
			.settingsForTheDialog(frame.getConfigurationDirectory());
		final boolean wasRemembering = settings.isRememberKeyFiles();
		SettingsPanel settingsPanel = new SettingsPanel(settings, frame.getPluginManager(),
			frame::refreshPluginsMenu, frame.getConfigurationDirectory(),
			frame.getModelObject().isSignedIn());

		JDialog dialog = new JDialog(frame, "Settings", true);
		dialog.setName("dlgSettings");
		dialog.getContentPane().add(settingsPanel, BorderLayout.CENTER);

		JButton closeButton = new JButton("Close");
		closeButton.setName("btnCloseSettings");
		closeButton.addActionListener(event -> {
			keepAndApply(frame, settings, wasRemembering);
			dialog.dispose();
		});
		// closing with the window button is closing too: a setting that is silently thrown away
		// because the dialog was dismissed the other way is worse than no setting
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent windowEvent)
			{
				keepAndApply(frame, settings, wasRemembering);
			}
		});
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		south.add(closeButton);
		dialog.getContentPane().add(south, BorderLayout.SOUTH);

		dialog.setSize(640, 420);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	/**
	 * Writes the settings down and puts the frame into the view they now ask for.
	 * <p>
	 * Applying the view on every close and not only on a change is deliberate: a plugin switches
	 * the frame to the desktop view to open its window, and choosing the same value again fires no
	 * event, so without this there would be no way back to the panel view.
	 *
	 * @param frame
	 *            the application frame
	 * @param settings
	 *            the settings as the dialog left them
	 * @param wasRemembering
	 *            whether "Remember key files" was on when the dialog opened: the key file paths are
	 *            removed, and the user told about what stays, when it is switched off, not on every
	 *            close while it is off
	 */
	private static void keepAndApply(final MysticCryptApplicationFrame frame,
		final MysticCryptSettings settings, final boolean wasRemembering)
	{
		settings.save(frame.getConfigurationDirectory());
		if (wasRemembering && !settings.isRememberKeyFiles())
		{
			forgetRememberedKeyFiles(frame);
		}
		frame.applyViewMode(settings.getViewMode());
	}

	/**
	 * Removes the key file paths from the remembered sign-in and from the remembered KeePass paths
	 * at once, since the setting that keeps them was switched off (#997). A file that cannot be
	 * read or written leaves the paths on disk, and the user is told so.
	 */
	private static void forgetRememberedKeyFiles(final MysticCryptApplicationFrame frame)
	{
		File configurationDirectory = frame.getConfigurationDirectory();
		File memoizedSigninFile = new File(configurationDirectory,
			MysticCryptApplicationFrame.MEMOIZED_SIGNIN_JSON_FILENAME);
		java.util.List<String> notRemoved = new java.util.ArrayList<>();
		try
		{
			RememberedKeyFileSupport.forgetKeyFiles(memoizedSigninFile);
		}
		catch (final java.io.IOException exception)
		{
			notRemoved.add(describe(exception));
			Logger.getLogger(NewSettingsFrameAction.class.getName()).log(Level.WARNING,
				"the key file paths could not be removed from " + memoizedSigninFile, exception);
		}
		try
		{
			MemoizedKeePassModelBean.forgetKeyFiles(configurationDirectory);
		}
		catch (final java.io.IOException exception)
		{
			notRemoved.add(describe(exception));
			Logger.getLogger(NewSettingsFrameAction.class.getName()).log(Level.WARNING,
				"the key file paths could not be removed from the remembered KeePass paths",
				exception);
		}
		java.util.List<java.io.File> keptAside = RememberedKeyFileSupport
			.keptAsideCopies(configurationDirectory);
		if (notRemoved.isEmpty() && keptAside.isEmpty())
		{
			return;
		}
		StringBuilder notice = new StringBuilder();
		if (!notRemoved.isEmpty())
		{
			notice
				.append(Messages.getString("settings.general.remember.key.files.not.removed",
					"The key file paths could not be removed from these files, which are left as "
						+ "they are:"))
				.append('\n').append(String.join("\n", notRemoved)).append('\n');
		}
		if (!keptAside.isEmpty())
		{
			notice.append(Messages.getString("settings.general.remember.key.files.kept.aside",
				"These copies of files that could not be read keep their key file paths, you can "
					+ "delete them:"));
			keptAside.forEach(copy -> notice.append('\n').append(copy.getAbsolutePath()));
		}
		JOptionPane.showMessageDialog(frame, notice.toString().strip(),
			Messages.getString("settings.general.remember.key.files.not.removed.title",
				"Key files still remembered"),
			JOptionPane.WARNING_MESSAGE);
	}

	/** The line of the notice for a file whose key file paths stay: its path and the reason */
	private static String describe(final java.io.IOException exception)
	{
		if (exception instanceof KeyFilePathsNotRemovedException notRemoved)
		{
			String reason = notRemoved
				.getReason() == KeyFilePathsNotRemovedException.Reason.UNREADABLE
					? Messages.getString("settings.general.remember.key.files.reason.unreadable",
						"the file cannot be read")
					: Messages.getString("settings.general.remember.key.files.reason.not.writable",
						"the file cannot be written");
			return notRemoved.getFile().getAbsolutePath() + ": " + reason;
		}
		return exception.getMessage();
	}
}
