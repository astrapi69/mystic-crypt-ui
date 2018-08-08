package de.alpharogroup.mystic.crypt.panels.signin;

import java.awt.event.ActionEvent;

import de.alpharogroup.model.BaseModel;
import de.alpharogroup.model.api.Model;
import de.alpharogroup.swing.base.BasePanel;
import lombok.Getter;

/**
 * The class {@link MasterPwFilePanel}
 */
@Getter
public class MasterPwFilePanel extends BasePanel<MasterPwFileModelBean>
{

	private static final long serialVersionUID = 1L;

	private javax.swing.JButton btnCancel;
	private javax.swing.JButton btnHelp;
	private javax.swing.JButton btnKeyFileChooser;
	private javax.swing.JButton btnMasterPw;
	private javax.swing.JButton btnOk;
	private javax.swing.JCheckBox cbxKeyFile;
	private javax.swing.JCheckBox cbxMasterPw;
	private javax.swing.JLabel lblImageHeader;
	private javax.swing.JTextField txtKeyFile;
	private javax.swing.JPasswordField txtMasterPw;

	/**
	 * Instantiates a new {@link MasterPwFilePanel}
	 */
	public MasterPwFilePanel()
	{
		this(BaseModel.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder().build()));
	}

	/**
	 * Instantiates a new {@link MasterPwFilePanel}
	 *
	 * @param model the model
	 */
	public MasterPwFilePanel(final Model<MasterPwFileModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblImageHeader = new javax.swing.JLabel();
		cbxMasterPw = new javax.swing.JCheckBox();
		cbxKeyFile = new javax.swing.JCheckBox();
		txtMasterPw = new javax.swing.JPasswordField();
		btnMasterPw = new javax.swing.JButton();
		txtKeyFile = new javax.swing.JTextField();
		btnKeyFileChooser = new javax.swing.JButton();
		btnHelp = new javax.swing.JButton();
		btnOk = new javax.swing.JButton();
		btnCancel = new javax.swing.JButton();

		setPreferredSize(new java.awt.Dimension(820, 380));

		lblImageHeader.setText("Enter Master Key");

		cbxMasterPw.setText("Master Password:");
		cbxMasterPw.addActionListener(actionEvent -> onCheckMasterPw(actionEvent));

		cbxKeyFile.setText("Key File:");
		cbxKeyFile.addActionListener(actionEvent -> onCheckKeyFile(actionEvent));

		txtMasterPw.setText("");

		btnMasterPw.setText("***");
		btnMasterPw.addActionListener(actionEvent -> onShowMasterPw(actionEvent));

		txtKeyFile.setText("");

		btnKeyFileChooser.setText("File");

		btnHelp.setText("Help");

		btnOk.setText("OK");

		btnCancel.setText("Cancel");
	}

	protected void onShowMasterPw(final ActionEvent actionEvent)
	{
		if (getModelObject().isShowMasterPw())
		{
			getTxtMasterPw().setEchoChar('*');
		}
		else
		{
			char zero = 0;
			getTxtMasterPw().setEchoChar(zero);
		}
		getModelObject().setShowMasterPw(!getModelObject().isShowMasterPw());
	}

	protected void onCheckKeyFile(final ActionEvent actionEvent)
	{
		getModelObject().setWithKeyFile(!getModelObject().isWithKeyFile());
	}

	protected void onCheckMasterPw(final ActionEvent actionEvent)
	{
		getModelObject().setWithMasterPw(!getModelObject().isWithMasterPw());
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
				.addComponent(lblImageHeader, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(btnHelp, javax.swing.GroupLayout.PREFERRED_SIZE, 122,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18)
							.addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 141,
								javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup().addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addComponent(cbxMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE, 201,
								Short.MAX_VALUE)
							.addComponent(cbxKeyFile, javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
									false)
								.addComponent(txtMasterPw)
								.addComponent(txtKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
									520, javax.swing.GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
							.addGroup(layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
									false)
								.addComponent(btnMasterPw, javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(btnKeyFileChooser,
									javax.swing.GroupLayout.PREFERRED_SIZE, 70,
									javax.swing.GroupLayout.PREFERRED_SIZE))))
					.addGap(12, 12, 12)))
				.addContainerGap()));
		layout.setVerticalGroup(layout
			.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup().addContainerGap()
				.addComponent(lblImageHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 80,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup().addComponent(cbxMasterPw)
						.addGap(18, 18, 18).addComponent(cbxKeyFile))
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout
							.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
							.addComponent(txtMasterPw, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.PREFERRED_SIZE)
							.addComponent(btnMasterPw))
						.addGap(18, 18, 18).addGroup(
							layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(txtKeyFile, javax.swing.GroupLayout.PREFERRED_SIZE,
									javax.swing.GroupLayout.DEFAULT_SIZE,
									javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(btnKeyFileChooser))))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 128,
					Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(btnHelp).addComponent(btnOk).addComponent(btnCancel))
				.addGap(42, 42, 42)));
	}

}
