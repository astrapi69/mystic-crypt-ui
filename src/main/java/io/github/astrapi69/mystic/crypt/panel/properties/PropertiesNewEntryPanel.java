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
package io.github.astrapi69.mystic.crypt.panel.properties;

import java.awt.event.ActionEvent;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextField;
import lombok.Getter;

/**
 * The dialog of one custom property, for adding and for editing. The value of a protected property
 * opens masked, like the password of an entry, and {@link #getBtnShowValue()} shows it in clear on
 * request; any other value is shown as it is
 */
@Getter
public class PropertiesNewEntryPanel extends BasePanel<KeyValuePair<String, String>>
{

	private static final char MASKING = '*';

	private static final char CLEAR = 0;

	private javax.swing.JLabel lblCreateNewEntry;
	private javax.swing.JLabel lblKey;
	private javax.swing.JLabel lblValue;
	private JMTextField txtKey;
	private JMPasswordField txtValue;
	private javax.swing.JButton btnShowValue;

	/**
	 * Creates the dialog of a new property
	 */
	public PropertiesNewEntryPanel()
	{
		this(BaseModel.of(KeyValuePair.<String, String> builder().build()));
	}

	/**
	 * Creates the dialog of a property that is not protected
	 *
	 * @param model
	 *            the property the dialog edits
	 */
	public PropertiesNewEntryPanel(final IModel<KeyValuePair<String, String>> model)
	{
		this(model, false);
	}

	/**
	 * Creates the dialog of a property
	 *
	 * @param model
	 *            the property the dialog edits
	 * @param masked
	 *            whether the property is protected, so that its value opens masked
	 */
	public PropertiesNewEntryPanel(final IModel<KeyValuePair<String, String>> model,
		final boolean masked)
	{
		super(model);
		if (masked)
		{
			txtValue.setEchoChar(MASKING);
			btnShowValue.setVisible(true);
		}
	}

	/**
	 * Shows a masked value in clear, or masks it again
	 *
	 * @param actionEvent
	 *            the press of {@link #getBtnShowValue()}
	 */
	protected void onShowValue(final ActionEvent actionEvent)
	{
		txtValue.setEchoChar(txtValue.echoCharIsSet() ? CLEAR : MASKING);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblKey = new javax.swing.JLabel();
		lblValue = new javax.swing.JLabel();
		txtKey = new JMTextField();
		txtValue = new JMPasswordField();
		btnShowValue = new javax.swing.JButton();
		lblCreateNewEntry = new javax.swing.JLabel();

		lblKey.setText("Key");

		lblValue.setText("Value");

		lblCreateNewEntry.setText("Create new entry");
		txtValue.setEchoChar(CLEAR);
		btnShowValue.setText("***");
		btnShowValue.setToolTipText(Messages.getString("dialog.crypt.properties.tooltip.show.value",
			"shows or hides the protected value"));
		btnShowValue.addActionListener(this::onShowValue);
		btnShowValue.setVisible(false);
		// bind with model
		KeyValuePair<String, String> modelObject = getModelObject();
		txtKey.setPropertyModel(LambdaModel.of(modelObject::getKey, modelObject::setKey));
		txtValue.setPropertyModel(LambdaModel.of(this::readValue, this::writeValue));
	}

	/**
	 * The value as the password field holds it: characters, while the property keeps a String,
	 * because the type of the properties is part of the vault format (#335)
	 */
	private char[] readValue()
	{
		String value = getModelObject().getValue();
		return value == null ? null : value.toCharArray();
	}

	private void writeValue(final char[] characters)
	{
		getModelObject().setValue(String.valueOf(characters));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
						.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
						.addComponent(txtKey, javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
							layout.createSequentialGroup()
								.addComponent(txtValue, javax.swing.GroupLayout.DEFAULT_SIZE, 363,
									Short.MAX_VALUE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(btnShowValue)))
					.addGap(20, 20, 20))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lblCreateNewEntry, javax.swing.GroupLayout.DEFAULT_SIZE,
						javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGap(401, 401, 401)))));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(lblCreateNewEntry).addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(txtKey, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(lblKey, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGap(18, 18, 18)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 34,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(txtValue, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(btnShowValue))
					.addContainerGap()));
	}

}
