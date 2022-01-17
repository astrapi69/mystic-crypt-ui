package io.github.astrapi69.mystic.crypt.panels.dbtree;

import javax.swing.*;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.Model;
import io.github.astrapi69.swing.base.BasePanel;
import lombok.Getter;

import java.awt.*;

@Getter
public class AddNodePanel extends BasePanel<AddNodeModelBean>
{
	JCheckBox cbxNode;
	JTextField txtName;
	JLabel lblName;
	JLabel lblNode;

	public AddNodePanel()
	{
		this(BaseModel.of(AddNodeModelBean.builder().build()));
	}

	public AddNodePanel(final Model<AddNodeModelBean> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblName = new JLabel("Enter name for node:");
		txtName = new JTextField();
		lblNode = new JLabel("Is leaf:");
		cbxNode = new JCheckBox();
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		GridLayout layout = new GridLayout(2, 2);
		this.setLayout(layout);
	}
}
