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

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.AttachmentPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesNewEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;
import io.github.astrapi69.swing.table.GenericJTable;

/**
 * Reusable steps for the "Edit Crypt Entry" dialog that "edit..." on an entry opens: the fields of
 * its "Main" tab and the properties of its "Properties" tab, and the two ways out of it. Same
 * interaction conventions as {@link CreateMasterKeySteps}
 */
final class EditCryptEntrySteps
{

	private static final int CLOSE_TIMEOUT_MILLIS = 10000;

	private final ApplicationSteps application;
	private final Robot robot;
	private final DialogFixture dialog;

	EditCryptEntrySteps(ApplicationSteps application, Robot robot, DialogFixture dialog)
	{
		this.application = application;
		this.robot = robot;
		this.dialog = dialog;
	}

	EditCryptEntrySteps typeTitle(String title)
	{
		GuiActionRunner.execute(() -> dialog.textBox("txtEntryName").target().setText(title));
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/** Types the password into "Password" and "Repeat" */
	EditCryptEntrySteps typePassword(String password)
	{
		GuiActionRunner.execute(() -> {
			dialog.textBox("txtPassword").target().setText(password);
			dialog.textBox("txtRepeat").target().setText(password);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		return this;
	}

	/**
	 * Adds a property on the "Properties" tab: "Add Property", key and value in the "New Property
	 * Value" dialog, OK
	 */
	EditCryptEntrySteps addProperty(String key, String value)
	{
		showTheTab("Properties");
		clickLater(dialog.button(JButtonMatcher.withText("Add Property")).target());
		answerThePropertyDialog("New Property Value", key, value);
		return this;
	}

	/**
	 * Changes the value of a property on the "Properties" tab: its row selected, "Edit", the new
	 * value in the "Edit Property Value" dialog, OK
	 */
	EditCryptEntrySteps changeProperty(String key, String value)
	{
		showTheTab("Properties");
		PropertiesPanel properties = robot.finder().findByType(dialog.target(),
			PropertiesPanel.class);
		GuiActionRunner.execute(() -> {
			var table = properties.getTblProperties();
			for (int row = 0; row < table.getRowCount(); row++)
			{
				KeyValuePair<String, String> property = table.getGenericTableModel().getData()
					.get(table.convertRowIndexToModel(row));
				if (key.equals(property.getKey()))
				{
					table.setRowSelectionInterval(row, row);
					return;
				}
			}
			throw new IllegalStateException("the entry has no property named '" + key + "'");
		});
		robot.waitForIdle();
		clickLater(dialog.button(JButtonMatcher.withText("Edit")).target());
		answerThePropertyDialog("Edit Property Value", key, value);
		return this;
	}

	/**
	 * Attaches a file on the "Attachments" tab: "Add File", the file picked in the file chooser the
	 * button opens
	 */
	EditCryptEntrySteps addAttachment(File file)
	{
		showTheTab("Attachments");
		clickLater(dialog.button(JButtonMatcher.withText("Add File")).target());
		pickInTheFileChooser(file);
		return this;
	}

	/**
	 * Shows the content of an attachment: its row selected on the "Attachments" tab, a double click
	 * on it, the text read from the dialog that opens, and the dialog closed with OK
	 *
	 * @return the text the dialog showed
	 */
	String showContentOf(String attachmentName)
	{
		selectTheAttachment(attachmentName);
		GenericJTable<?> table = attachmentTable();
		SwingUtilities.invokeLater(() -> {
			for (var listener : table.getMouseListeners())
			{
				listener.mouseClicked(new MouseEvent(table, MouseEvent.MOUSE_CLICKED,
					System.currentTimeMillis(), 0, 5, 5, 2, false, MouseEvent.BUTTON1));
			}
		});
		DialogFixture contentDialog = application.findDialogWithTitle(attachmentName);
		JTextArea content = robot.finder().findByName(contentDialog.target(),
			"txtAttachmentContent", JTextArea.class, false);
		String shown = GuiActionRunner.execute(() -> content.getText());
		clickLater(contentDialog.button(JButtonMatcher.withText("OK")).target());
		awaitClosed(contentDialog, "'" + attachmentName + "' content dialog");
		return shown;
	}

	/**
	 * Writes an attachment out: its row selected, "Save to", the file picked in the chooser
	 *
	 * @return the file name the chooser proposed before the target was set
	 */
	String saveAttachmentTo(String attachmentName, File target)
	{
		selectTheAttachment(attachmentName);
		clickLater(dialog.button(JButtonMatcher.withText("Save to")).target());
		JFileChooser chooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		String proposed = GuiActionRunner.execute(
			() -> chooser.getSelectedFile() == null ? null : chooser.getSelectedFile().getName());
		approve(chooser, target);
		return proposed;
	}

	private void selectTheAttachment(String attachmentName)
	{
		showTheTab("Attachments");
		GenericJTable<FileContentInfo> table = attachmentTable();
		GuiActionRunner.execute(() -> {
			for (int row = 0; row < table.getRowCount(); row++)
			{
				FileContentInfo attachment = table.getGenericTableModel().getData()
					.get(table.convertRowIndexToModel(row));
				if (attachmentName.equals(attachment.getName()))
				{
					table.setRowSelectionInterval(row, row);
					return;
				}
			}
			throw new IllegalStateException(
				"the entry has no attachment named '" + attachmentName + "'");
		});
		robot.waitForIdle();
	}

	@SuppressWarnings("unchecked")
	private <T> GenericJTable<T> attachmentTable()
	{
		AttachmentPanel panel = robot.finder().findByType(dialog.target(), AttachmentPanel.class);
		return robot.finder().findByType(panel, GenericJTable.class);
	}

	private void pickInTheFileChooser(File file)
	{
		JFileChooser chooser = JFileChooserFinder.findFileChooser()
			.withTimeout(10, TimeUnit.SECONDS).using(robot).target();
		UiTestSpeed.step();
		approve(chooser, file);
	}

	private void approve(JFileChooser chooser, File file)
	{
		SwingUtilities.invokeLater(() -> {
			chooser.setSelectedFile(file);
			chooser.approveSelection();
		});
		robot.waitForIdle();
		UiTestSpeed.step();
	}

	/** Closes the dialog with Cancel and waits until it is gone */
	void cancel()
	{
		close("Cancel");
	}

	/** Closes the dialog with OK and waits until it is gone */
	void ok()
	{
		close("OK");
	}

	private void showTheTab(String title)
	{
		JTabbedPane tabs = robot.finder().findByType(dialog.target(), JTabbedPane.class);
		GuiActionRunner.execute(() -> tabs.setSelectedIndex(tabs.indexOfTab(title)));
		robot.waitForIdle();
		UiTestSpeed.step();
	}

	private void answerThePropertyDialog(String title, String key, String value)
	{
		DialogFixture propertyDialog = application.findDialogWithTitle(title);
		PropertiesNewEntryPanel panel = robot.finder().findByType(propertyDialog.target(),
			PropertiesNewEntryPanel.class);
		GuiActionRunner.execute(() -> {
			panel.getTxtKey().setText(key);
			panel.getTxtValue().setText(value);
		});
		robot.waitForIdle();
		UiTestSpeed.step();
		clickLater(propertyDialog.button(JButtonMatcher.withText("OK")).target());
		awaitClosed(propertyDialog, "'" + title + "' dialog");
	}

	private void close(String buttonText)
	{
		clickLater(dialog.button(JButtonMatcher.withText(buttonText)).target());
		awaitClosed(dialog, "edit-crypt-entry dialog");
	}

	/**
	 * Clicks later on the event thread: the button opens or closes a modal dialog, and a click that
	 * waited for it would wait for the dialog's answer
	 */
	private static void clickLater(JButton button)
	{
		SwingUtilities.invokeLater(button::doClick);
		UiTestSpeed.step();
	}

	private static void awaitClosed(DialogFixture closing, String description)
	{
		Pause.pause(new Condition(description + " is closed")
		{
			@Override
			public boolean test()
			{
				return !closing.target().isShowing();
			}
		}, CLOSE_TIMEOUT_MILLIS);
	}
}
