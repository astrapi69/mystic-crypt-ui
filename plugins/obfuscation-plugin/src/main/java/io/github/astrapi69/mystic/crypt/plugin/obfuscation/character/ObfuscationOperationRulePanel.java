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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation.character;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import io.github.astrapi69.collection.array.ArrayExtensions;
import io.github.astrapi69.collection.list.ListFactory;
import io.github.astrapi69.crypt.api.obfuscation.rule.Operation;
import io.github.astrapi69.crypt.data.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ModeContext;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.ObfuscationMessages;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.document.NumberValuesDocument;
import io.github.astrapi69.swing.document.RangeDocument;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

@Getter
public class ObfuscationOperationRulePanel extends BasePanel<ObfuscationOperationModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JButton btnAdd;
	private JMComboBox<Operation, EnumComboBoxModel<Operation>> cmbOperation;
	private javax.swing.JLabel lblIndexes;
	private javax.swing.JLabel lblObfuscationOperationRule;
	private javax.swing.JLabel lblOperation;
	private javax.swing.JLabel lblOriginalChar;
	private javax.swing.JLabel lblReplaceWith;
	private JMTextField txtIndexes;
	private JMTextField txtOriginalChar;
	private JMTextField txtRelpaceWith;

	/** What the user typed or chose; every component below writes into it */
	private transient ObfuscationOperationRulePanelModel ruleModelObject;

	public ObfuscationOperationRulePanel(final IModel<ObfuscationOperationModelBean> model)
	{
		super(model);
	}

	/**
	 * Gets the rule the fields and the combo box describe, as it stands at this moment
	 *
	 * @return the model the components of this panel write into
	 */
	public ObfuscationOperationRulePanelModel getRuleModelObject()
	{
		return ruleModelObject;
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
	}


	public void onEditObfuscationOperationRule(
		ObfuscationOperationRule<Character, Character> selected)
	{
		String indexes = selected.getIndexes().toString();
		if (indexes != null && 2 < indexes.length())
		{
			indexes = indexes.substring(1, indexes.length() - 1);
			List<String> result = ListFactory.newArrayList();

			List<String> splitted = ArrayExtensions.toList(indexes.split(","));
			splitted.stream().forEach(s -> result.add(s.trim()));
			indexes = result.stream().collect(Collectors.joining(","));

		}
		getModelObject().setSelected(selected);
		getModelObject().setProccessMode(ModeContext.UPDATE);
		txtOriginalChar.setText(selected.getCharacter().toString());
		txtRelpaceWith.setText(selected.getReplaceWith().toString());
		txtIndexes.setText(indexes);
		cmbOperation.setSelectedItem(selected.getOperation());
	}


	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		ruleModelObject = new ObfuscationOperationRulePanelModel();

		lblOriginalChar = new javax.swing.JLabel();
		lblReplaceWith = new javax.swing.JLabel();
		// the documents that limit a field to one character, or to numbers, are passed to the
		// constructor rather than set afterwards: a model backed field listens to the document it
		// is built with, and a later setDocument would leave that listener behind on the discarded
		// one
		txtOriginalChar = new JMTextField(new RangeDocument(0, 1), null, 0);
		txtRelpaceWith = new JMTextField(new RangeDocument(0, 1), null, 0);
		btnAdd = new javax.swing.JButton();
		lblObfuscationOperationRule = new javax.swing.JLabel();
		lblIndexes = new javax.swing.JLabel();
		txtIndexes = new JMTextField(new NumberValuesDocument(), null, 0);

		lblOperation = new javax.swing.JLabel();
		cmbOperation = new JMComboBox<>(new EnumComboBoxModel<>(Operation.class));

		lblOriginalChar.setText("Original Character");

		lblReplaceWith.setText("Replace with");

		btnAdd.setText("Add");

		lblObfuscationOperationRule.setFont(new java.awt.Font("Ubuntu", 1, 12)); // NOI18N
		lblObfuscationOperationRule.setText("Obfuscation Operation Rule");

		lblIndexes.setText("Indexes");

		lblOperation.setText("Operation");

		txtOriginalChar.setToolTipText(ObfuscationMessages.getString(
			"obfuscation.operation.rule.tooltip.original.char",
			"the character to replace - one character only"));
		txtRelpaceWith.setToolTipText(ObfuscationMessages.getString(
			"obfuscation.operation.rule.tooltip.replace.with",
			"the character it is replaced with - one character only"));
		txtIndexes.setToolTipText(ObfuscationMessages.getString("obfuscation.operation.rule.tooltip.indexes",
			"comma separated positions in the text where the chosen operation is applied instead of the plain replacement"));
		cmbOperation.setToolTipText(ObfuscationMessages.getString("obfuscation.operation.rule.tooltip.operation",
			"the transformation applied to the original character at the named indexes"));
		btnAdd.setToolTipText(ObfuscationMessages.getString("obfuscation.operation.rule.tooltip.add.button",
			"adds this rule to the table below"));

		bindComponents();

		btnAdd.addActionListener(actionEvent -> onAdd(actionEvent));

	}

	/**
	 * Binds every component to the model, so that each edit lands in the model and the model is
	 * what the Add button reads. The operation the combo box already shows is put into the model
	 * first, so that binding it does not clear the selection the user sees
	 */
	private void bindComponents()
	{
		if (cmbOperation.getSelectedItem() instanceof Operation preselected)
		{
			ruleModelObject.setOperation(preselected);
		}
		txtOriginalChar.setPropertyModel(LambdaModel.of(ruleModelObject::getOriginalCharacter,
			ruleModelObject::setOriginalCharacter));
		txtRelpaceWith.setPropertyModel(
			LambdaModel.of(ruleModelObject::getReplaceWith, ruleModelObject::setReplaceWith));
		txtIndexes.setPropertyModel(
			LambdaModel.of(ruleModelObject::getIndexes, ruleModelObject::setIndexes));
		cmbOperation.setPropertyModel(
			LambdaModel.of(ruleModelObject::getOperation, ruleModelObject::setOperation));
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

		add(lblIndexes);
		add(ToolForm.sized(txtIndexes), ToolForm.FIELD);

		add(lblOperation);
		add(cmbOperation, ToolForm.FIELD);

		add(ToolForm.buttons(btnAdd), ToolForm.BUTTON_ROW);
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeToolFormLayout();
	}

}
