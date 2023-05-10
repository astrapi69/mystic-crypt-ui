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
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import lombok.Getter;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.properties.PropertiesPanel;
import io.github.astrapi69.swing.base.BasePanel;

@Getter
public class MysticCryptEntryTabbedPanel extends BasePanel<MysticCryptEntryModelBean>
{

	MysticCryptEntryPanel mysticCryptEntryPanel;
	AttachmentPanel attachmentPanel;
	PropertiesPanel propertiesPanel;
	private javax.swing.JTabbedPane tbpMysticCryptEntry;

	/**
	 * Creates new form MysticCryptEntryFormPanel
	 */
	public MysticCryptEntryTabbedPanel()
	{
		this(BaseModel.of(MysticCryptEntryModelBean.builder().build()));
	}

	/**
	 * Creates new form MysticCryptEntryFormPanel
	 */
	public MysticCryptEntryTabbedPanel(final IModel<MysticCryptEntryModelBean> model)
	{
		super(model);
	}


	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		tbpMysticCryptEntry = new javax.swing.JTabbedPane();
		mysticCryptEntryPanel = new MysticCryptEntryPanel(getModel());
		attachmentPanel = new AttachmentPanel(getModel());
		propertiesPanel = new PropertiesPanel(getModel());
		tbpMysticCryptEntry.add("Main", mysticCryptEntryPanel);
		tbpMysticCryptEntry.add("Attachments", attachmentPanel);
		tbpMysticCryptEntry.add("Properties", propertiesPanel);
	}


	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout
					.createSequentialGroup().addContainerGap().addComponent(tbpMysticCryptEntry,
						javax.swing.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE)
					.addContainerGap()));
		layout
			.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout
					.createSequentialGroup().addContainerGap().addComponent(tbpMysticCryptEntry,
						javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
					.addContainerGap()));
	}

}
