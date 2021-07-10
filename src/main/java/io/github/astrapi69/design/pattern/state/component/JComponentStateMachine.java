package io.github.astrapi69.design.pattern.state.component;

import javax.swing.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class JComponentStateMachine<T extends JComponent> extends ComponentStateMachine<T, SigninComponentState>
	implements
		SigninComponentState
{
	SigninComponentState current;
	boolean applicationFilePresent;
	boolean withMasterPassword;
	boolean masterPasswordLength;
	boolean withKeyFile;

	@Override
	public void onApplicationFileAdded(JComponentStateMachine context)
	{
		boolean applicationFilePresent = !isApplicationFilePresent();
		setApplicationFilePresent(applicationFilePresent);
		updateComponentState();
	}

	@Override
	public void onChangeWithMasterPassword(JComponentStateMachine context)
	{
		boolean withMasterPassword = !isWithMasterPassword();
		setWithMasterPassword(withMasterPassword);
		updateComponentState();
	}

	@Override
	public void onChangeWithKeyFile(JComponentStateMachine context)
	{
		boolean withKeyFile = !isWithKeyFile();
		setWithKeyFile(withKeyFile);
		updateComponentState();
	}

	@Override
	public void onChangeMasterPasswordLength(JComponentStateMachine context)
	{
		boolean masterPasswordLength = !isMasterPasswordLength();
		setMasterPasswordLength(masterPasswordLength);
		updateComponentState();
	}

	protected void updateComponentState()
	{
		if (!applicationFilePresent)
		{
			current = ComponentStateEnum.DISABLED;
			setEnabled(false);
		}
		if (applicationFilePresent && withMasterPassword && withKeyFile)
		{
			current = ComponentStateEnum.ENABLED;
			setEnabled(true);
		}
		else if (applicationFilePresent && withKeyFile)
		{
			current = ComponentStateEnum.DISABLED;
			setEnabled(false);
		}
	}

	public void setEnabled(boolean b)
	{
		component.setEnabled(b);
	}

}
