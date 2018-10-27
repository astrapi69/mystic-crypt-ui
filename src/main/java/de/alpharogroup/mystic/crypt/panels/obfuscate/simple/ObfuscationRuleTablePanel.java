package de.alpharogroup.mystic.crypt.panels.obfuscate.simple;

import java.util.Map;

import com.thoughtworks.xstream.XStream;

import de.alpharogroup.collections.map.MapFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import de.alpharogroup.crypto.obfuscation.rule.ObfuscationRule;
import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;

public class ObfuscationRuleTablePanel extends BasePanel<ObfuscationModelBean>
{

	private static final long serialVersionUID = 1L;
	private Map<String, Class<?>> aliases;
	private javax.swing.JButton btnExport;
	private javax.swing.JButton btnImport;
	private javax.swing.JLabel lblKeyRules;
	private javax.swing.JScrollPane scpKeyRules;
	private javax.swing.JTable tblKeyRules;
	private XStream xStream;

	{
		xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.allowTypesByWildcard(new String[] { "de.alpharogroup.**" });
		aliases = MapFactory.newLinkedHashMap();
		aliases.put("KeyValuePair", KeyValuePair.class);
		aliases.put("ObfuscationRule", ObfuscationRule.class);
	}

	public ObfuscationRuleTablePanel()
	{
		this(BaseModel.of(ObfuscationModelBean.builder().build()));
	}

	public ObfuscationRuleTablePanel(final Model<ObfuscationModelBean> model)
	{
		super(model);
	}

	protected void onEditObfuscationRule(
		ObfuscationRule<Character, Character> selected)
	{
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblKeyRules = new javax.swing.JLabel();
		scpKeyRules = new javax.swing.JScrollPane();
		tblKeyRules = new javax.swing.JTable();
		btnImport = new javax.swing.JButton();
		btnExport = new javax.swing.JButton();

		lblKeyRules.setText("Table of key rules for obfuscate");

		tblKeyRules.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][] { { null, null }, { null, null }, { null, null }, { null, null } },
			new String[] { "Title 1", "Title 2" }));
		scpKeyRules.setViewportView(tblKeyRules);

		btnImport.setText("Import");

		btnExport.setText("Export");
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
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout
					.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(layout
						.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE)
						.addComponent(btnImport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
							javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGap(18, 18, 18).addComponent(
							btnExport, javax.swing.GroupLayout.PREFERRED_SIZE, 128,
							javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(javax.swing.GroupLayout.Alignment.LEADING,
						layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
							.addComponent(scpKeyRules, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.PREFERRED_SIZE, 1000,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addComponent(lblKeyRules, javax.swing.GroupLayout.Alignment.LEADING,
								javax.swing.GroupLayout.PREFERRED_SIZE, 280,
								javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGap(0, 0, Short.MAX_VALUE)))
					.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(29, 29, 29).addComponent(lblKeyRules)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(scpKeyRules, javax.swing.GroupLayout.PREFERRED_SIZE, 217,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(btnImport).addComponent(btnExport))
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
	}

}
