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
package io.github.astrapi69.mystic.crypt.panel.keepass;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.swing.JOptionPane;

import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.action.ExportKeePassDatabaseAction;
import io.github.astrapi69.mystic.crypt.keepass.EntryExportChoice;
import io.github.astrapi69.mystic.crypt.keepass.KeePassCredentials;
import io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter;
import io.github.astrapi69.mystic.crypt.keepass.MemoizedKeePassModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.ui.screen.OwnedDialogs;
import io.github.astrapi69.mystic.crypt.write.DataClass;
import io.github.astrapi69.mystic.crypt.write.GuardedSave;
import io.github.astrapi69.mystic.crypt.write.OverwriteConfirmation;

/**
 * Exports the entries chosen in the entry table into a KeePass file of their own, with a password
 * of its own (#1118). Only those entries go into the file, of each only the fields the user chose,
 * without the history and with the attachments only on request; each keeps its identifier, so the
 * recipient merges a later export instead of getting every entry twice.
 * <p>
 * Identifiers given when the vault was opened are kept only from the next save on (#272), so a
 * vault with such identifiers is saved first, after asking: an export without that save would hand
 * out identifiers the next open replaces.
 * <p>
 * Not an action: the popup of the entry table starts it with the rows chosen there, as it starts
 * copy, edit and delete, and a locked workspace shows no entry table. So it sits next to its panel
 * and not in the action package, whose classes the lock invariant fires one by one.
 */
public final class ExportSelectedEntriesToKeePass
{

	private ExportSelectedEntriesToKeePass()
	{
	}

	/**
	 * Asks where to, with which credentials and what of the entries, and writes the file
	 *
	 * @param caller
	 *            the component that asks, whose window owns the dialogs
	 * @param entries
	 *            the chosen entries of the vault, which are left as they are
	 */
	public static void export(final Component caller, final List<MysticCryptEntryModelBean> entries)
	{
		MysticCryptApplicationFrame frame = MysticCryptApplicationFrame.getInstance();
		if (entries.isEmpty() || !identifiersAreKept(caller, frame.getModelObject()))
		{
			return;
		}
		MemoizedKeePassModelBean memoized = MemoizedKeePassModelBean
			.load(frame.getConfigurationDirectory());
		ExportSelectedEntriesPanel panel = new ExportSelectedEntriesPanel(entries.size(),
			memoized.getLastExportFilePath(), memoized.getLastExportKeyFilePath(),
			choice -> noticesOf(frame, entries, choice));
		int option = OwnedDialogs.selectedOption(caller, panel, JOptionPane.PLAIN_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, title(), panel.getDestination().getTxtFile());
		if (option != JOptionPane.OK_OPTION)
		{
			return;
		}
		File file = panel.getDestination().getSelectedFile();
		if (file == null)
		{
			JOptionPane.showMessageDialog(OwnedDialogs.ownerOf(caller), Messages
				.getString("keepass.export.selected.no.file", "No destination file selected."),
				title(), JOptionPane.ERROR_MESSAGE);
			return;
		}
		remember(frame, memoized, file, panel.getDestination().getSelectedKeyFile());
		// a file that is there is not replaced without asking (#300)
		if (OverwriteConfirmation.allowsWriting(OwnedDialogs.ownerOf(caller), file,
			DataClass.DERIVED))
		{
			write(caller, frame, entries, panel, file);
		}
	}

	/**
	 * Whether the identifiers of the vault are in its file, saving it first after asking when the
	 * open gave identifiers that are not
	 */
	private static boolean identifiersAreKept(final Component caller,
		final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null || !applicationModelBean.isIdentifiersAwaitASave())
		{
			return true;
		}
		int option = OwnedDialogs.confirm(caller, title(), Messages.getString(
			"keepass.export.selected.save.first",
			"<div width='450'>The entries of this vault were given identifiers when it was opened, and the vault keeps them only once it is saved. "
				+ "Without that save a later export of the same entries would carry other identifiers, and the recipient would get them twice.<br><br>"
				+ "Save the vault now and go on with the export?</div>"),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		return option == JOptionPane.OK_OPTION
			&& GuardedSave.writeOrTell(caller, applicationModelBean);
	}

	private static List<String> noticesOf(final MysticCryptApplicationFrame frame,
		final List<MysticCryptEntryModelBean> entries, final EntryExportChoice choice)
	{
		return frame.getPluginVaultConnections() == null
			? List.of()
			: frame.getPluginVaultConnections().selectedEntriesExportNotices(entries, choice);
	}

	/**
	 * Remembers the chosen paths whether or not the export succeeds, as the export of the whole
	 * vault does
	 */
	private static void remember(final MysticCryptApplicationFrame frame,
		final MemoizedKeePassModelBean memoized, final File file, final File keyFile)
	{
		memoized.setLastExportFilePath(file.getAbsolutePath());
		memoized.setLastExportKeyFilePath(keyFile != null ? keyFile.getAbsolutePath() : null);
		memoized.save(frame.getConfigurationDirectory());
	}

	private static void write(final Component caller, final MysticCryptApplicationFrame frame,
		final List<MysticCryptEntryModelBean> entries, final ExportSelectedEntriesPanel panel,
		final File file)
	{
		try
		{
			KdbxDatabase database = new KdbxDatabase();
			database.setName(ExportKeePassDatabaseAction.vaultNameOf(frame));
			database.setDescription(Messages.getString("keepass.export.database.description",
				"Exported from mystic-crypt-ui"));
			database.getRootGroup().setName(ExportKeePassDatabaseAction.vaultNameOf(frame));
			KeePassTreeConverter.fillDatabaseWith(database, entries, panel.choice());
			try (OutputStream outputStream = new FileOutputStream(file))
			{
				database.write(KeePassCredentials.of(panel.getDestination().getPassword(),
					panel.getDestination().getSelectedKeyFile()), outputStream);
			}
			JOptionPane.showMessageDialog(OwnedDialogs.ownerOf(caller),
				Messages.getString("keepass.export.selected.done", "Entries exported: ")
					+ entries.size() + " - " + file.getName(),
				Messages.getString("keepass.export.selected.done.title", "Export successful"),
				JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(OwnedDialogs.ownerOf(caller),
				Messages.getString("keepass.export.selected.failed",
					"Could not export the KeePass file: ") + exception.getMessage(),
				title(), JOptionPane.ERROR_MESSAGE);
		}
	}

	private static String title()
	{
		return Messages.getString("keepass.export.selected.title", "Export selected to KeePass");
	}
}
