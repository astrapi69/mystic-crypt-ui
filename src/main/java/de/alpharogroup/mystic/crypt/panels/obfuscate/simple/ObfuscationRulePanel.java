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
package de.alpharogroup.mystic.crypt.panels.obfuscate.simple;

import java.awt.event.ActionEvent;

import de.alpharogroup.crypto.obfuscation.rule.ObfuscationRule;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.swing.document.RangeDocument;
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

	public ObfuscationRulePanel(final Model<ObfuscationModelBean> model)
	{
		super(model);
	}

	protected void onAdd(final ActionEvent actionEvent)
	{
	}

	protected void onEditObfuscationRule(ObfuscationRule<Character, Character> selected)
	{		
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

		// == custom edit ==
		txtOriginalChar.setDocument(new RangeDocument(0, 1));
		txtRelpaceWith.setDocument(new RangeDocument(0, 1));

		btnAdd.addActionListener(actionEvent -> onAdd(actionEvent));

	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(lblObfuscationOperationRule)
				.addGroup(layout
					.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(txtOriginalChar, javax.swing.GroupLayout.DEFAULT_SIZE,
								140, Short.MAX_VALUE)
							.addComponent(lblOriginalChar, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(txtRelpaceWith, javax.swing.GroupLayout.PREFERRED_SIZE,
								140, javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18).addComponent(btnAdd,
								javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addComponent(lblReplaceWith, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
							javax.swing.GroupLayout.PREFERRED_SIZE))))
				.addContainerGap(24, Short.MAX_VALUE)));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
					.addComponent(lblObfuscationOperationRule)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblOriginalChar, javax.swing.GroupLayout.PREFERRED_SIZE, 15,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblReplaceWith))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtOriginalChar, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtRelpaceWith, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnAdd))
					.addContainerGap(22, Short.MAX_VALUE)));

		getAccessibleContext().setAccessibleDescription("");
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

}
