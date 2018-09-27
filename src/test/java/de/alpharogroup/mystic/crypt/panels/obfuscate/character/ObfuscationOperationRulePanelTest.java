package de.alpharogroup.mystic.crypt.panels.obfuscate.character;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.alpharogroup.layout.CloseWindow;
import de.alpharogroup.model.BaseModel;

public class ObfuscationOperationRulePanelTest
{

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException
	{
		final JFrame frame = new JFrame();
		frame.addWindowListener(new CloseWindow());
		frame.setTitle("RulePanel");

		final JPanel panel = new ObfuscationOperationRuleFormPanel();
			new ObfuscationOperationRulePanel(BaseModel.<ObfuscationOperationModelBean> of(ObfuscationOperationModelBean.builder()
			.tableModel(EditableCharacterObfuscationOperationRulesTableModel.builder().build()).build()));
		frame.add(panel);
		frame.setBounds(0, 0, 1280, 650);
		frame.setVisible(true);
	}
}
