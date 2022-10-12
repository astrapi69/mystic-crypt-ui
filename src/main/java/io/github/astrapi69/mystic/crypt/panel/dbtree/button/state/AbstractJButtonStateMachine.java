package io.github.astrapi69.mystic.crypt.panel.dbtree.button.state;

import io.github.astrapi69.design.pattern.state.button.ButtonStateMachine;

import javax.swing.JButton;

public abstract class AbstractJButtonStateMachine<ST> extends ButtonStateMachine<JButton, ST> {

	public void setEnabled(final boolean enabled)
	{
		getButton().setEnabled(enabled);
	}
}
