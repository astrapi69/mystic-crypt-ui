package io.github.astrapi69.design.pattern.state.button;

import java.io.File;

import javax.swing.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import io.github.astrapi69.design.pattern.state.component.AbstractJComponentStateMachine;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BtnOkStateMachine extends AbstractJComponentStateMachine<JButton, BtnOkComponentState>
	implements
		BtnOkComponentState
{
	String appDataFile;
	File keyFile;
	@Builder.Default
	int minPasswordLength = 6;
	int passwordLength;
	boolean withMasterPassword;
	boolean withKeyFile;

	@Override
	protected void updateComponentState()
	{
		boolean appDataFilePresent = appDataFile != null && 0 < appDataFile.length();
		if (appDataFilePresent
			&& withMasterPassword && minPasswordLength <= passwordLength
			&& withKeyFile && keyFile != null)
		{
			setCurrent(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (appDataFilePresent
			&& !withMasterPassword
			&& withKeyFile && keyFile != null)
		{
			setCurrent(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (appDataFilePresent
			&& !withKeyFile
			&& withMasterPassword && minPasswordLength <= passwordLength )
		{
			setCurrent(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		setCurrent(BtnOkComponentStateEnum.DISABLED);
		setEnabled(false);
	}

	@Override
	public void onApplicationFileAdded(BtnOkStateMachine context)
	{
		appDataFile = context.getAppDataFile();
		updateComponentState();
	}

	@Override
	public void onChangeWithMasterPassword(BtnOkStateMachine context)
	{
		setWithMasterPassword(withMasterPassword);
		updateComponentState();
	}

	@Override
	public void onChangeMasterPasswordLength(BtnOkStateMachine context)
	{
		passwordLength = context.getPasswordLength();
		updateComponentState();
	}

	@Override
	public void onChangeWithKeyFile(BtnOkStateMachine context)
	{
		updateComponentState();
	}

	@Override
	public void onSetKeyFile(BtnOkStateMachine context)
	{
		keyFile = context.getKeyFile();
		updateComponentState();
	}

}
