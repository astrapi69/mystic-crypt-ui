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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Path;
import java.util.List;

import javax.swing.*;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextArea;

/**
 * Tool panel for viewing and editing the application menu as xml.
 * <p>
 * "Export" writes the menu bar that is currently in place into the editor, "Validate" checks the
 * edited xml without touching the application, "Apply" rebuilds the live menu bar from it and "Save"
 * persists it so it is applied on the next start. Every item keeps the action it already has,
 * because the layout only refers to action ids that are harvested from the running menu; unknown
 * ids produce a disabled item instead of breaking the menu. "Reset" removes a persisted layout
 * again.
 * <p>
 * The editor is bound to {@link MenuDesignerPanelModel}, so the xml a button works with is read
 * from the model rather than out of the widget.
 */
public class MenuDesignerPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The width the xml editor keeps even in a layout that is built from minimum sizes */
	private static final int MINIMUM_EDITOR_WIDTH = 320;

	/** The height the xml editor keeps even in a layout that is built from minimum sizes */
	private static final int MINIMUM_EDITOR_HEIGHT = 160;

	/** The xml being edited and the message of the last operation; the editor writes into it */
	private final transient MenuDesignerPanelModel modelObject = new MenuDesignerPanelModel();

	private final JMTextArea txtMenuXml = new JMTextArea(24, 90);
	private final JLabel lblResult = new JLabel(" ");

	public MenuDesignerPanel()
	{
		// one layout for every tool window, so this one looks like the one next to it: the xml
		// editor taking the height that is left, the buttons under what they act on, the result of
		// the last one of them on the line below
		super(ToolForm.newLayout());

		bindToModel();

		txtMenuXml.setName("txtMenuXml");
		txtMenuXml.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtMenuXml.setToolTipText(MenuDesignerMessages.getString("menudesigner.tooltip.menu.xml",
			"the xml layout of the application menu, in the schema Export produces"));
		lblResult.setName("lblResult");
		lblResult.setToolTipText(
			MenuDesignerMessages.getString("menudesigner.tooltip.result", "what the last operation did"));

		JButton btnExport = button("btnExport", "Export current menu", event -> onExport(),
			MenuDesignerMessages.getString("menudesigner.tooltip.export.button",
				"replaces the editor's content with the menu bar currently in place, so it can be reviewed or edited"));
		JButton btnValidate = button("btnValidate", "Validate", event -> onValidate(),
			MenuDesignerMessages.getString("menudesigner.tooltip.validate.button",
				"checks the edited xml without touching the running application"));
		JButton btnApply = button("btnApply", "Apply now", event -> onApply(),
			MenuDesignerMessages.getString("menudesigner.tooltip.apply.button",
				"rebuilds the live menu bar from the edited xml - not saved, lost on restart unless Save is used too"));
		JButton btnSave = button("btnSave", "Save as my menu", event -> onSave(),
			MenuDesignerMessages.getString("menudesigner.tooltip.save.button",
				"persists the edited xml so it is applied again on the next start"));
		JButton btnReset = button("btnReset", "Reset to standard", event -> onReset(),
			MenuDesignerMessages.getString("menudesigner.tooltip.reset.button",
				"removes the saved menu layout - destructive, the standard menu returns on the next start"));

		add(editorScrollPane(), ToolForm.GROWING);
		add(ToolForm.buttons(btnExport, btnValidate, btnApply, btnSave, btnReset),
			ToolForm.BUTTON_ROW);
		add(lblResult, ToolForm.RESULT_LINE);

		if (MenuDesignerSettingsContribution.exportOnOpen())
		{
			onExport();
		}
	}

	/**
	 * Binds the editor to the model, so that every edit lands in the model and the model is what
	 * the buttons read
	 */
	private void bindToModel()
	{
		txtMenuXml.setPropertyModel(
			LambdaModel.of(modelObject::getMenuXml, modelObject::setMenuXml));
	}

	/**
	 * Builds the scroll pane that carries the xml editor.
	 * <p>
	 * A viewport reports a minimum size of a few pixels, so without a stated minimum the scroll
	 * pane claims 22 x 22 px and any container that honours minimum sizes is free to squeeze the
	 * editor away entirely. The honest minimum belongs on the scroll pane rather than on the text
	 * area, whose own size is the size of the text it holds. The shared floor of a tool window is
	 * the one every other window uses; an xml document needs more room than a single line field, so
	 * this editor states a wider and taller one of its own.
	 *
	 * @return the scroll pane around the xml editor, with a minimum size that stays readable
	 */
	private JScrollPane editorScrollPane()
	{
		JScrollPane editor = ToolForm.scrolled(txtMenuXml);
		editor.setMinimumSize(new Dimension(MINIMUM_EDITOR_WIDTH, MINIMUM_EDITOR_HEIGHT));
		return editor;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

	/**
	 * Shows the given menu xml in the editor, which carries it into the model on its way
	 *
	 * @param menuXml
	 *            the menu xml to show
	 */
	private void showMenuXml(String menuXml)
	{
		txtMenuXml.setText(menuXml);
		txtMenuXml.setCaretPosition(0);
	}

	/**
	 * Reports what the last operation did, in the model and on the line below the buttons
	 *
	 * @param resultMessage
	 *            the message of the last operation
	 */
	private void showResult(String resultMessage)
	{
		modelObject.setResultMessage(resultMessage);
		lblResult.setText(resultMessage);
	}

	private void onExport()
	{
		try
		{
			showMenuXml(MysticCryptApplicationFrame.getInstance().exportCurrentMenuXml());
			showResult("exported the current menu");
		}
		catch (Exception exception)
		{
			showResult("export failed: " + exception.getMessage());
		}
	}

	private void onValidate()
	{
		try
		{
			List<String> errors = io.github.astrapi69.mystic.crypt.menu.MenuLayoutSupport
				.validate(modelObject.getMenuXml());
			showResult(
				errors.isEmpty() ? "valid" : errors.size() + " problem(s): " + errors.get(0));
		}
		catch (Exception exception)
		{
			showResult("invalid: " + exception.getMessage());
		}
	}

	private void onApply()
	{
		try
		{
			MysticCryptApplicationFrame.getInstance().applyMenuXml(modelObject.getMenuXml());
			showResult("applied to the running menu");
		}
		catch (Exception exception)
		{
			showResult("apply failed: " + exception.getMessage());
		}
	}

	private void onSave()
	{
		try
		{
			Path file = MysticCryptApplicationFrame.getInstance()
				.saveMenuLayout(modelObject.getMenuXml());
			showResult("saved to " + file);
		}
		catch (Exception exception)
		{
			showResult("save failed: " + exception.getMessage());
		}
	}

	private void onReset()
	{
		try
		{
			boolean removed = MysticCryptApplicationFrame.getInstance().resetMenuLayout();
			showResult(removed
				? "removed the saved menu, the standard menu returns on the next start"
				: "there was no saved menu");
		}
		catch (Exception exception)
		{
			showResult("reset failed: " + exception.getMessage());
		}
	}
}
