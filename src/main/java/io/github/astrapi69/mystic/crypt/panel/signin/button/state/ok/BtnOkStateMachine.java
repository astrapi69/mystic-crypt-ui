/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.signin.button.state.ok;

import javax.swing.*;

import io.github.astrapi69.design.pattern.state.component.AbstractJComponentStateMachine;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.app.file.xml.KeyFileOnDisk;
import io.github.astrapi69.mystic.crypt.lock.SecretComparison;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
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
public class BtnOkStateMachine extends AbstractJComponentStateMachine<JButton, BtnOkComponentState>
	implements
		BtnOkComponentState
{
	@ToString.Exclude
	MasterPwFileModelBean modelObject;

	/**
	 * Whether the master password counts only once its repeat is the same: true in "Create your
	 * master key", which asks for the password twice so that a typo is caught before the database
	 * is written with it (#879); false in the sign-in dialog, which has no repeat
	 */
	boolean repeatRequired;

	@Override
	protected void updateComponentState()
	{
		boolean applicationFilePresent = false;
		if (modelObject.getApplicationFileInfo() != null)
		{
			applicationFilePresent = FileInfo.toFile(modelObject.getApplicationFileInfo()).exists();
		}
		int minPasswordLength = modelObject.getMinPasswordLength();
		int passwordLength = modelObject.getMasterPw() != null
			? modelObject.getMasterPw().length
			: 0;
		boolean withKeyFile = modelObject.isWithKeyFile();
		boolean withMasterPw = modelObject.isWithMasterPw();
		boolean keyFilePresent = false;
		if (modelObject.getKeyFileInfo() != null)
		{
			// a key file this kind of database cannot read is no key file for it: with a master
			// password that is every file that is not PEM (#943)
			java.io.File keyFile = FileInfo.toFile(modelObject.getKeyFileInfo());
			keyFilePresent = keyFile.exists() && KeyFileOnDisk.takes(keyFile, withMasterPw);
		}
		boolean passwordConfirmed = !repeatRequired || SecretComparison
			.sameCharacters(modelObject.getMasterPw(), modelObject.getRepeatPw());
		if (applicationFilePresent && withMasterPw && minPasswordLength <= passwordLength
			&& passwordConfirmed && withKeyFile && keyFilePresent)
		{
			setCurrentState(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (applicationFilePresent && !withMasterPw && withKeyFile && keyFilePresent)
		{
			setCurrentState(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		else if (applicationFilePresent && !withKeyFile && withMasterPw
			&& minPasswordLength <= passwordLength && passwordConfirmed)
		{
			setCurrentState(BtnOkComponentStateEnum.ENABLED);
			setEnabled(true);
			return;
		}
		setCurrentState(BtnOkComponentStateEnum.DISABLED);
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
