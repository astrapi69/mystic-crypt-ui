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

import java.awt.GridLayout;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.keepass.EntryExportChoice;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import lombok.Getter;

/**
 * The dialog of the export of chosen entries (#1118): where to and with which credentials, as the
 * export of the whole vault asks it, and which fields and whether the attachments go into the file.
 * It says what a file given away means for the passwords in it, and how the recipient takes in a
 * later export without getting every entry twice. The lines of the plugins follow the choice of
 * fields, because what of a plugin's entries goes into the file depends on it
 */
@Getter
public class ExportSelectedEntriesPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The file, the password and the key file, as in the export of the whole vault */
	private final ExportKeePassPanel destination;

	private final transient ExportSelectedEntriesModel choiceModel = new ExportSelectedEntriesModel();

	private final transient Function<EntryExportChoice, List<String>> noticesFor;

	private final JTextComponent txtSelectedExportNotices;

	/**
	 * The dialog for the given number of chosen entries
	 *
	 * @param entryCount
	 *            how many entries were chosen
	 * @param lastFilePath
	 *            the file the last export wrote, or {@code null}
	 * @param lastKeyFilePath
	 *            the key file the last export used, or {@code null}
	 * @param noticesFor
	 *            the lines of the plugins for a choice of fields
	 */
	public ExportSelectedEntriesPanel(final int entryCount, final String lastFilePath,
		final String lastKeyFilePath, final Function<EntryExportChoice, List<String>> noticesFor)
	{
		super(ToolForm.newLayout());
		this.noticesFor = noticesFor;
		destination = new ExportKeePassPanel(lastFilePath, lastKeyFilePath, List.of());
		txtSelectedExportNotices = ToolForm.intro("");
		txtSelectedExportNotices.setName("txtSelectedExportNotices");

		JTextComponent intro = ToolForm.intro(introFor(entryCount));
		intro.setName("txtSelectedExportIntro");
		add(intro, ToolForm.INTRO_ROW);
		add(destination, ToolForm.WIDE);
		add(new JLabel(Messages.getString("keepass.export.selected.fields", "Goes into the file:")),
			"aligny top");
		add(choices(), ToolForm.FIELD);
		add(txtSelectedExportNotices, ToolForm.INTRO_ROW);
		showTheNotices();
	}

	/**
	 * What of each entry goes into the file, as the user chose it
	 *
	 * @return the choice
	 */
	public EntryExportChoice choice()
	{
		return choiceModel.choice();
	}

	private static String introFor(final int entryCount)
	{
		return Messages.getString("keepass.export.selected.intro.count",
			"Exports the chosen entries into a KeePass file of their own: ")
			+ entryCount + "\n"
			+ Messages.getString("keepass.export.selected.intro.password",
				"Send the password of the file another way than the file itself.")
			+ "\n"
			+ Messages.getString("keepass.export.selected.intro.given.away",
				"A file given away cannot be taken back: only changing the passwords of the entries in it helps.")
			+ "\n" + Messages.getString("keepass.export.selected.intro.merge",
				"The recipient takes in a later export with KeePassXC's Database > Merge From Database, or with KeePass's Synchronize: each entry keeps its identifier, so it is updated rather than added twice.");
	}

	private JPanel choices()
	{
		JPanel choices = new JPanel(new GridLayout(0, 3));
		choices.add(checkBox("cbxExportTitle", "keepass.export.selected.field.title", "Title",
			choiceModel::isTitle, choiceModel::setTitle));
		choices.add(checkBox("cbxExportUserName", "keepass.export.selected.field.user.name",
			"User name", choiceModel::isUserName, choiceModel::setUserName));
		choices.add(checkBox("cbxExportPassword", "keepass.export.selected.field.password",
			"Password", choiceModel::isPassword, choiceModel::setPassword));
		choices.add(checkBox("cbxExportUrl", "keepass.export.selected.field.url", "URL",
			choiceModel::isUrl, choiceModel::setUrl));
		choices.add(checkBox("cbxExportNotes", "keepass.export.selected.field.notes", "Notes",
			choiceModel::isNotes, choiceModel::setNotes));
		choices.add(checkBox("cbxExportCustomFields", "keepass.export.selected.field.custom",
			"Custom fields", choiceModel::isCustomFields, choiceModel::setCustomFields));
		choices.add(checkBox("cbxExportAttachments", "keepass.export.selected.field.attachments",
			"Attachments", choiceModel::isAttachments, choiceModel::setAttachments));
		return choices;
	}

	private JMCheckBox checkBox(final String name, final String key, final String text,
		final BooleanSupplier chosen, final Consumer<Boolean> choose)
	{
		JMCheckBox checkBox = new JMCheckBox(Messages.getString(key, text));
		checkBox.setName(name);
		checkBox.setPropertyModel(LambdaModel.of(chosen::getAsBoolean, choose::accept));
		checkBox.addActionListener(event -> showTheNotices());
		return checkBox;
	}

	/** Shows the lines of the plugins for the choice as it stands */
	private void showTheNotices()
	{
		List<String> notices = noticesFor.apply(choice());
		txtSelectedExportNotices.setText(String.join("\n", notices));
		txtSelectedExportNotices.setVisible(!notices.isEmpty());
	}
}
