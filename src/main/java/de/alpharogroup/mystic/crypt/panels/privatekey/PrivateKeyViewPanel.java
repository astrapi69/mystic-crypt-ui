package de.alpharogroup.mystic.crypt.panels.privatekey;

import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;

import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;


/**
 * The class {@link PrivateKeyViewPanel}.
 */
@Getter
public class PrivateKeyViewPanel extends BasePanel<PrivateKeyModelBean>
{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	private JLabel lblKeySize;
	private JLabel lblKeySizeDisplay;
	private JLabel lblPrivateKey;
	private JLabel lblPublicKey;
	private JScrollPane scpPrivateKey;
	private JScrollPane scpPublicKey;
	private JTextArea txtPrivateKey;
	private JTextArea txtPublicKey;

	public PrivateKeyViewPanel()
	{
		this(BaseModel.<PrivateKeyModelBean> of(PrivateKeyModelBean.builder().build()));
	}

	public PrivateKeyViewPanel(final Model<PrivateKeyModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblKeySize = new JLabel();
		lblKeySizeDisplay = new JLabel();
		lblPrivateKey = new JLabel();
		scpPrivateKey = new JScrollPane();
		txtPrivateKey = new JTextArea();
		lblPublicKey = new JLabel();
		scpPublicKey = new JScrollPane();
		txtPublicKey = new JTextArea();

		lblKeySize.setText("Keysize");

		lblKeySizeDisplay.setText("");

		lblPrivateKey.setText("Private key");

		txtPrivateKey.setColumns(20);
		txtPrivateKey.setRows(5);
		scpPrivateKey.setViewportView(txtPrivateKey);

		lblPublicKey.setText("Public key");

		txtPublicKey.setColumns(20);
		txtPublicKey.setRows(5);
		scpPublicKey.setViewportView(txtPublicKey);

		txtPrivateKey.setEditable(false);
		txtPublicKey.setEditable(false);

		txtPrivateKey.setFont(new Font("monospaced", Font.PLAIN, 12));
		txtPublicKey.setFont(new Font("monospaced", Font.PLAIN, 12));
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
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addGap(46, 46, 46)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
					.addComponent(lblKeySize, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
						Short.MAX_VALUE)
					.addComponent(lblKeySizeDisplay, GroupLayout.DEFAULT_SIZE, 128,
						Short.MAX_VALUE))
				.addGap(33, 33, 33)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(lblPrivateKey, GroupLayout.PREFERRED_SIZE, 179,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(scpPrivateKey, GroupLayout.PREFERRED_SIZE, 480,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(scpPublicKey, GroupLayout.PREFERRED_SIZE, 480,
						GroupLayout.PREFERRED_SIZE)
					.addComponent(lblPublicKey, GroupLayout.PREFERRED_SIZE, 165,
						GroupLayout.PREFERRED_SIZE))
				.addGap(48, 48, 48)));
		layout
			.setVerticalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addGap(43, 43, 43)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(lblKeySize).addComponent(lblPrivateKey)
							.addComponent(lblPublicKey))
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
							.addComponent(lblKeySizeDisplay)
							.addComponent(scpPrivateKey, GroupLayout.DEFAULT_SIZE, 520,
								Short.MAX_VALUE)
							.addComponent(scpPublicKey))
						.addContainerGap(40, Short.MAX_VALUE)));
	}
}
