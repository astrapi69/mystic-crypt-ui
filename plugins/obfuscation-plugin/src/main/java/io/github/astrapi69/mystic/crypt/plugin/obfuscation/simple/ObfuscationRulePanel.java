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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple;

import java.awt.event.ActionEvent;

import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationRule;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ModeContext;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.document.RangeDocument;
import lombok.Getter;

@Getter
public class ObfuscationRulePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JButton btnAdd;
	private javax.swing.JLabel lblObfuscationOperationRule;
	private javax.swing.JLabel lblOriginalChar;
	private javax.swing.JLabel lblReplaceWith;
	private javax.swing.JTextField txtOriginalChar;
	private javax.swing.JTextField txtRelpaceWith;

	public ObfuscationRulePanel()
	{
		this(BaseModel.of(ObfuscationModelBean.builder().build()));
	}

	public ObfuscationRulePanel(final IModel<ObfuscationModelBean> model)
	{
		super(model);
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
	}

	protected void onEditObfuscationRule(ObfuscationRule<Character, Character> selected)
	{
		getModelObject().setSelected(selected);
		getModelObject().setProccessMode(ModeContext.UPDATE);
		txtOriginalChar.setText(selected.getCharacter().toString());
		txtRelpaceWith.setText(selected.getReplaceWith().toString());
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblOriginalChar = new javax.swing.JLabel();
		lblReplaceWith = new javax.swing.JLabel();
		txtOriginalChar = new javax.swing.JTextField();
		txtRelpaceWith = new javax.swing.JTextField();
		btnAdd = new javax.swing.JButton();
		lblObfuscationOperationRule = new javax.swing.JLabel();

		lblOriginalChar.setText("Original Character");

		lblReplaceWith.setText("Replace with");

		btnAdd.setText("Add");

		lblObfuscationOperationRule.setFont(new java.awt.Font("Ubuntu", 1, 12)); // NOI18N
		lblObfuscationOperationRule.setText("Obfuscation Rule");

		// stable component names for UI tests (AssertJ-Swing lookups by name)
		txtOriginalChar.setName("txtOriginalChar");
		txtRelpaceWith.setName("txtRelpaceWith");
		btnAdd.setName("btnAddRule");

		// == custom edit ==
		txtOriginalChar.setDocument(new RangeDocument(0, 1));
		txtRelpaceWith.setDocument(new RangeDocument(0, 1));

		btnAdd.addActionListener(actionEvent -> onAdd(actionEvent));

	}

	/**
	 * Lays this panel out with the shared tool window form: the heading over the whole width, a
	 * right aligned label next to every field, and the button under the fields it acts on
	 */
	protected void onInitializeToolFormLayout()
	{
		setLayout(ToolForm.newLayout());

		add(lblObfuscationOperationRule, ToolForm.WIDE);

		add(lblOriginalChar);
		add(ToolForm.sized(txtOriginalChar), ToolForm.FIELD);

		add(lblReplaceWith);
		add(ToolForm.sized(txtRelpaceWith), ToolForm.FIELD);

		add(ToolForm.buttons(btnAdd), ToolForm.BUTTON_ROW);

		getAccessibleContext().setAccessibleDescription("");
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeToolFormLayout();
	}

}
