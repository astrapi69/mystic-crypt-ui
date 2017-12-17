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
package de.alpharogroup.mystic.crypt.panels.obfuscate;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.file.read.ReadFileExtensions;
import de.alpharogroup.file.write.WriteFileExtensions;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.GenericJTable;
import de.alpharogroup.swing.base.BasePanel;
import de.alpharogroup.xml.json.JsonTransformer;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;


@Getter
@Slf4j
public class SimpleRuleTablePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;
	private JLabel lblKeyRules;
	private JScrollPane scpKeyRules;
	private GenericJTable<KeyValuePair<String, String>> tblKeyRules;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private JFileChooser fileChooser;

	public SimpleRuleTablePanel()
	{
		this(BaseModel.<ObfuscationModelBean> of(ObfuscationModelBean.builder().build()));

	}

	public SimpleRuleTablePanel(@NonNull final Model<ObfuscationModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		fileChooser = new JFileChooser();

		scpKeyRules = new JScrollPane();
		tblKeyRules = new GenericJTable<>(getModelObject().getKeyRulesTableModel());
		lblKeyRules = new JLabel();

		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		scpKeyRules.setViewportView(tblKeyRules);

		lblKeyRules.setText("Table of key rules for obfuscate");

		btnImport.setText("Import");
		btnExport.setText("Export");

		btnImport.addActionListener(actionEvent -> onImport(actionEvent));
		btnExport.addActionListener(actionEvent -> onExport(actionEvent));
	}

	@SuppressWarnings("unchecked")
	protected void onImport(final ActionEvent actionEvent)
	{
		final int returnVal = fileChooser.showOpenDialog(SimpleRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File obfuscationRules = fileChooser.getSelectedFile();
			try {
				final String fromFile = ReadFileExtensions.readFromFile(obfuscationRules);
				final Map<String, String> map = JsonTransformer.toObject(fromFile, Map.class);
				getModelObject().getKeyRulesTableModel().setData(KeyValuePair.toKeyValuePairs(map));
				getModelObject().getKeyRulesTableModel().fireTableDataChanged();
			} catch (final IOException e) {
				log.error("IOException ", e);
			}
		}
	}

	protected void onExport(final ActionEvent actionEvent)
	{
		final Map<String, String> map = getModelObject().getKeyRulesTableModel().toMap();
		final int returnVal = fileChooser.showSaveDialog(SimpleRuleTablePanel.this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			final File obfuscationRules = fileChooser.getSelectedFile();
			String json;
			try
			{
				json = JsonTransformer.toJson(map);
				WriteFileExtensions.writeStringToFile(obfuscationRules, json, "UTF-8");
			}
			catch (final JsonProcessingException e)
			{
				log.error("JsonProcessingException", e);
			}
		}
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeGroupLayout();
	}

	protected void onInitializeGroupLayout()
	{
		final GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(lblKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 280,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 1188,
						javax.swing.GroupLayout.PREFERRED_SIZE)
					.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup()
							.addComponent(btnImport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addComponent(btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(3, 3, 3)))
				.addContainerGap(31, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(29, 29, 29).addComponent(lblKeyRules)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 217,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(btnImport).addComponent(btnExport))
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}

}
