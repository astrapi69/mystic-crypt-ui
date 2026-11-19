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

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;

import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.mystic.crypt.panel.dbtree.AttachmentPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesNewEntryPanel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;
import io.github.astrapi69.swing.table.GenericJTable;

/**
 * Reusable steps for the "Edit Crypt Entry" dialog that "edit..." on an entry opens: the fields of
 * its "Main" tab, the properties of its "Properties" tab, the attachments of its "Attachments" tab
 * (Add File, the content a double click shows, Save to), and the two ways out of it. Same
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
		selectPropertyRow(key);
		clickLater(dialog.button(JButtonMatcher.withText("Edit")).target());
		answerThePropertyDialog("Edit Property Value", key, value);
		return this;
	}

	/**
	 * What the "Properties" table shows in the value column of the property's row, as the user sees
	 * it: the text of the cell the table renders, not the model's answer
	 *
	 * @param key
	 *            the property's name
	 * @return the rendered text of the value cell
	 */
	String valueShownInTheTable(String key)
	{
		PropertiesPanel properties = propertiesPanel();
		return GuiActionRunner.execute(() -> {
			var table = properties.getTblProperties();
			int row = rowOf(properties, key);
			Component rendered = table.prepareRenderer(table.getCellRenderer(row, 1), row, 1);
			return ((JLabel)rendered).getText();
		});
	}

	/**
	 * What the "Properties" table's model answers for the value column of the property's row: what
	 * a copy of the row reads
	 *
	 * @param key
	 *            the property's name
	 * @return the model's value of the cell
	 */
	Object valueTheTableModelGives(String key)
	{
		PropertiesPanel properties = propertiesPanel();
		return GuiActionRunner
			.execute(() -> properties.getTblProperties().getValueAt(rowOf(properties, key), 1));
	}

	/**
	 * What a copy of the property's row puts on a clipboard of the test's own: the row selected,
	 * then the table's own transfer handler asked to copy it
	 *
	 * @param key
	 *            the property's name
	 * @return the text on the clipboard
	 */
	String copiedFromTheRow(String key)
	{
		PropertiesPanel properties = propertiesPanel();
		return GuiActionRunner.execute(() -> {
			var table = properties.getTblProperties();
			int row = rowOf(properties, key);
			table.setRowSelectionInterval(row, row);
			Clipboard clipboard = new Clipboard("the test's own");
			table.getTransferHandler().exportToClipboard(table, clipboard, TransferHandler.COPY);
			return (String)clipboard.getData(DataFlavor.stringFlavor);
		});
	}

	/**
	 * Opens the property for editing: its row selected, "Edit", and the "Edit Property Value"
	 * dialog left open for the caller to look at
	 *
	 * @param key
	 *            the property's name
	 * @return the steps for the open property dialog
	 */
	PropertyDialogSteps openPropertyForEditing(String key)
	{
		selectPropertyRow(key);
		clickLater(dialog.button(JButtonMatcher.withText("Edit")).target());
		DialogFixture propertyDialog = application.findDialogWithTitle("Edit Property Value");
		return new PropertyDialogSteps(propertyDialog,
			robot.finder().findByType(propertyDialog.target(), PropertiesNewEntryPanel.class));
	}

	/**
	 * Renames a property on the "Properties" tab: its row selected, "Edit", the new name in the
	 * "Edit Property Value" dialog, OK. The value is left as it is
	 *
	 * @param key
	 *            the property's name
	 * @param newKey
	 *            the name it gets
	 * @return these steps
	 */
	EditCryptEntrySteps renameProperty(String key, String newKey)
	{
		PropertyDialogSteps property = openPropertyForEditing(key);
		property.typeKey(newKey);
		property.ok();
		return this;
	}

	/** The steps for the "Edit Property Value" dialog, which the Edit button of a row opens */
	final class PropertyDialogSteps
	{
		private final DialogFixture propertyDialog;
		private final PropertiesNewEntryPanel panel;

		private PropertyDialogSteps(DialogFixture propertyDialog, PropertiesNewEntryPanel panel)
		{
			this.propertyDialog = propertyDialog;
			this.panel = panel;
		}

		/** Whether the value field hides what it holds */
		boolean valueIsMasked()
		{
			return GuiActionRunner.execute(() -> panel.getTxtValue().getEchoChar() != 0);
		}

		/** Whether the button that shows the value is visible on screen */
		boolean showButtonIsShowing()
		{
			return GuiActionRunner.execute(() -> panel.getBtnShowValue().isShowing());
		}

		/** The value the field holds, whether it shows it or not */
		String value()
		{
			return GuiActionRunner.execute(() -> String.valueOf(panel.getTxtValue().getPassword()));
		}

		/** Presses the button that shows the value, or masks it again */
		void pressShow()
		{
			GuiActionRunner.execute(() -> panel.getBtnShowValue().doClick());
			robot.waitForIdle();
			UiTestSpeed.step();
		}

		/** Types a new name into the key field */
		void typeKey(String key)
		{
			GuiActionRunner.execute(() -> panel.getTxtKey().setText(key));
			robot.waitForIdle();
			UiTestSpeed.step();
		}

		/** Closes the property dialog with OK and waits until it is gone */
		void ok()
		{
			clickLater(propertyDialog.button(JButtonMatcher.withText("OK")).target());
			awaitClosed(propertyDialog, "'Edit Property Value' dialog");
		}

		/** Closes the property dialog with Cancel and waits until it is gone */
		void cancel()
		{
			clickLater(propertyDialog.button(JButtonMatcher.withText("Cancel")).target());
			awaitClosed(propertyDialog, "'Edit Property Value' dialog");
		}
	}

	private PropertiesPanel propertiesPanel()
	{
		showTheTab("Properties");
		return robot.finder().findByType(dialog.target(), PropertiesPanel.class);
	}

	private void selectPropertyRow(String key)
	{
		PropertiesPanel properties = propertiesPanel();
		GuiActionRunner.execute(() -> {
			int row = rowOf(properties, key);
			properties.getTblProperties().setRowSelectionInterval(row, row);
		});
		robot.waitForIdle();
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

	/** The view row of the property with the given name; on the event thread */
	private static int rowOf(PropertiesPanel properties, String key)
	{
		var table = properties.getTblProperties();
		for (int row = 0; row < table.getRowCount(); row++)
		{
			Map.Entry<String, String> property = table.getGenericTableModel().getData()
				.get(table.convertRowIndexToModel(row));
			if (key.equals(property.getKey()))
			{
				return row;
			}
		}
		throw new IllegalStateException("the entry has no property named '" + key + "'");
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
