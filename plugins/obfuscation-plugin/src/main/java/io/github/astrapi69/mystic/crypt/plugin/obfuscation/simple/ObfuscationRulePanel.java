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
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ModeContext;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.document.RangeDocument;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

@Getter
public class ObfuscationRulePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JButton btnAdd;
	private javax.swing.JLabel lblObfuscationOperationRule;
	private javax.swing.JLabel lblOriginalChar;
	private javax.swing.JLabel lblReplaceWith;
	private JMTextField txtOriginalChar;
	private JMTextField txtRelpaceWith;

	/** What the user typed; both fields below write into it and the Add button reads it */
	private transient ObfuscationRulePanelModel ruleModelObject;

	public ObfuscationRulePanel()
	{
		this(BaseModel.of(ObfuscationModelBean.builder().build()));
	}

	public ObfuscationRulePanel(final IModel<ObfuscationModelBean> model)
	{
		super(model);
	}

	/**
	 * Gets the rule the two fields describe, as it stands at this moment
	 *
	 * @return the model the two fields of this panel write into
	 */
	public ObfuscationRulePanelModel getRuleModelObject()
	{
		return ruleModelObject;
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

		ruleModelObject = new ObfuscationRulePanelModel();

		lblOriginalChar = new javax.swing.JLabel();
		lblReplaceWith = new javax.swing.JLabel();
		// the document that limits a field to one character is passed to the constructor rather
		// than set afterwards: a model backed field listens to the document it is built with, and
		// a later setDocument would leave that listener behind on the discarded one
		txtOriginalChar = new JMTextField(new RangeDocument(0, 1), null, 0);
		txtRelpaceWith = new JMTextField(new RangeDocument(0, 1), null, 0);
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

		bindComponents();

		btnAdd.addActionListener(actionEvent -> onAdd(actionEvent));

	}

	/**
	 * Binds both fields to the model, so that every edit lands in the model and the model is what
	 * the Add button reads
	 */
	private void bindComponents()
	{
		txtOriginalChar.setPropertyModel(LambdaModel.of(ruleModelObject::getOriginalCharacter,
			ruleModelObject::setOriginalCharacter));
		txtRelpaceWith.setPropertyModel(
			LambdaModel.of(ruleModelObject::getReplaceWith, ruleModelObject::setReplaceWith));
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
