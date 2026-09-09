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
	public static Choice askAndApply(final Component parent,
		final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null || !applicationModelBean.isDirty())
		{
			return Choice.DISCARDED;
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
