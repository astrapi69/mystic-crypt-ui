package io.github.astrapi69.mystic.crypt.panels.signin;

import java.io.File;

import javax.swing.*;

import lombok.AllArgsConstructor;
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
	MasterPwFileModelBean modelObject;

	@Override
	protected void updateComponentState()
	{
		boolean applicationFilePresent = modelObject.getApplicationFile() != null && modelObject.getApplicationFile().exists();
		int minPasswordLength = modelObject.getMinPasswordLength();
		int passwordLength = modelObject.getMasterPw() != null ? modelObject.getMasterPw().length : 0;
		boolean withKeyFile = modelObject.isWithKeyFile();
		File keyFile = modelObject.getKeyFile();
		boolean withMasterPw = modelObject.isWithMasterPw();
		if (applicationFilePresent
			&& withMasterPw && minPasswordLength <= passwordLength
			&& withKeyFile && keyFile != null)
		{
			setCurrent(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (applicationFilePresent
			&& !withMasterPw
			&& withKeyFile && keyFile != null)
		{
			setCurrent(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (applicationFilePresent
			&& !withKeyFile
			&& withMasterPw && minPasswordLength <= passwordLength )
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
		updateComponentState();
	}

	@Override
	public void onChangeWithMasterPassword(BtnOkStateMachine context)
	{
		updateComponentState();
	}

	@Override
	public void onChangeMasterPasswordLength(BtnOkStateMachine context)
	{
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
		updateComponentState();
	}

}
