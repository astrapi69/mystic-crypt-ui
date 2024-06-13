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
package io.github.astrapi69.mystic.crypt.panel.table;

import java.awt.*;

import io.github.astrapi69.swing.model.component.JMLabel;
import lombok.Getter;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextField;
import io.github.astrapi69.swing.model.label.LabelModel;

@Getter
public class NewTableEntryPanel extends BasePanel<NewTableEntryModel>
{

	JMTextField txtName;
	JMLabel lblName;

	public NewTableEntryPanel()
	{
		this(BaseModel.of(NewTableEntryModel.builder().build()));
	}

	public NewTableEntryPanel(final IModel<NewTableEntryModel> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		NewTableEntryModel modelObject = getModelObject();
		LabelModel labelModelName = modelObject.getLabelModelName();
		if (labelModelName != null)
		{
			lblName = new JMLabel(BaseModel.of(labelModelName));
		}
		else
		{
			lblName = new JMLabel("Enter name:");
		}
		txtName = new JMTextField();

		txtName.setPropertyModel(LambdaModel.of(modelObject::getName, modelObject::setName));
		lblName.setPropertyModel(
			LambdaModel.of(modelObject::getLabelModelName, modelObject::setLabelModelName));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
	}

	protected void onInitializeGridLayout()
	{
		GridLayout layout = new GridLayout(2, 2);
		this.setLayout(layout);

		add(lblName);
		add(txtName);
	}

	protected void onInitializeMigLayout()
	{
		MigLayout layout = new MigLayout(new LC().fillX().wrapAfter(2),
			new AC().align("left").gap("10").grow().fill(), new AC().fill().gap("10"));
		this.setLayout(layout);

		add(lblName);
		add(txtName, new CC().grow().width("120px"));
	}
}
