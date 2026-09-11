/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.action;

import java.awt.Component;

import javax.swing.JOptionPane;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.swing.dialog.JOptionPaneExtensions;
import io.github.astrapi69.swing.panel.label.LabelPanel;

/**
 * The "the database is modified, store your changes?" question, asked wherever an open vault is
 * about to go away.
 * <p>
 * It existed exactly once, inline in the window-closing listener, which made ending the application
 * the only way to reach it (#281). Closing a vault, replacing it with another one and ending the
 * application are three callers of one question, and a question re-typed per caller is a question
 * that drifts per caller.
 */
public final class SaveBeforeCloseConfirmation
{

	private SaveBeforeCloseConfirmation()
	{
	}

	/**
	 * What the user decided about the pending changes.
	 */
	public enum Choice
	{
		/** The changes were written to the vault's file */
		SAVED,

		/** The changes are to be dropped - or there were none */
		DISCARDED,

		/** The user does not want to go through with whatever asked; nothing was written */
		CANCELLED
	}

	/**
	 * Asks about unsaved changes and acts on the answer: saving writes the file here, so the caller
	 * only has to look at what came back.
	 * <p>
	 * A clean model answers {@link Choice#DISCARDED} without asking anything - there is nothing to
	 * write and nothing to decide. Dismissing the dialog with the window button counts as
	 * {@link Choice#CANCELLED}, not as "no": a caller that reads it as "no" throws away the changes
	 * of someone who only wanted the question to go away.
	 *
	 * @param parent
	 *            the component the dialog belongs to
	 * @param applicationModelBean
	 *            the model whose changes are in question
	 * @return what the user decided
	 */
	/**
	 * Asks whether to discard, because from a LOCKED workspace there is nothing else to offer.
	 * <p>
	 * Locking stopped writing with #304, and the master password left memory with #242, so "save"
	 * here is a button that cannot do what it says: it would reach a store with no password behind
	 * it. The two things this must never do are ending silently and offering that save - so the
	 * question names the loss and the way out of it, and cancelling stays in the application, where
	 * unlocking is one click away.
	 *
	 * @param parent
	 *            the component the dialog belongs to
	 * @return {@link Choice#DISCARDED} when the user accepts the loss, {@link Choice#CANCELLED}
	 *         otherwise
	 */
	private static Choice askWhetherToDiscardWhileLocked(final Component parent)
	{
		String defaultMessage = "<html><body>"
			+ "<div>This database is locked and has unsaved changes.</div>"
			+ "<div>They cannot be written while it is locked - the master password is not "
			+ "held in memory.</div>"
			+ "<div>Ending now discards them. Cancel, unlock and save to keep them.</div>"
			+ "</body></html>";
		LabelPanel panel = new LabelPanel(BaseModel
			.of(Messages.getString("dialog.confirm.discard.while.locked.message", defaultMessage)));
		int option = JOptionPaneExtensions.getSelectedOption(panel, JOptionPane.WARNING_MESSAGE,
			JOptionPane.YES_NO_OPTION, parent, Messages.getString(
				"dialog.confirm.discard.while.locked.title", "Discard the unsaved changes?"),
			null);
		return option == JOptionPane.YES_OPTION ? Choice.DISCARDED : Choice.CANCELLED;
	}

	public static Choice askAndApply(final Component parent,
		final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null || !applicationModelBean.isDirty())
		{
			return Choice.DISCARDED;
		}
		if (!applicationModelBean.isSignedIn())
		{
			return askWhetherToDiscardWhileLocked(parent);
		}
		String defaultMessage = "<html><body>" + "<div>The current database file is modified.</div>"
			+ "<div>Store your changes before finish application</div>" + "</body></html>";
		String confirmMessage = Messages.getString("dialog.confirm.save.before.close.message",
			defaultMessage);
		LabelPanel panel = new LabelPanel(BaseModel.of(confirmMessage));
		int option = JOptionPaneExtensions.getSelectedOption(
			panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, parent, Messages
				.getString("dialog.confirm.save.before.close.title", "Save Database Before Close."),
			null);
		if (option == JOptionPane.YES_OPTION)
		{
			ApplicationXmlFileStoreWorker.storeApplicationFile(applicationModelBean);
			return Choice.SAVED;
		}
		if (option == JOptionPane.NO_OPTION)
		{
			return Choice.DISCARDED;
		}
		return Choice.CANCELLED;
	}
}
