package io.github.astrapi69.mystic.crypt.panel.mousemove;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Frame;

import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.astrapi69.junit.jupiter.callback.before.test.IgnoreHeadlessExceptionExtension;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.window.adapter.CloseWindow;

public class MouseMoveSettingsPanelTest
{

	private FrameFixture underTest;
	private MouseMoveSettingsPanel panel;

	@ExtendWith(IgnoreHeadlessExceptionExtension.class)
	@Test
	public void test()
	{
		String selectedItem;
		final Frame frame = new Frame("TestFrame");
		IModel<MouseMoveSettingsModelBean> model = BaseModel
			.of(MouseMoveSettingsModelBean.builder().build());
		panel = new MouseMoveSettingsPanel(model);
		frame.add(panel);
		frame.addWindowListener(new CloseWindow());
		frame.setSize(400, 220);
		frame.setVisible(true);
		underTest = new FrameFixture(frame);
		selectedItem = underTest.comboBox("cmbVariableX").selectedItem();
		assertEquals(selectedItem, "1");
		underTest.comboBox("cmbVariableX").selectItem(2);
		selectedItem = underTest.comboBox("cmbVariableX").selectedItem();
		assertEquals(selectedItem, "3");
	}
}
