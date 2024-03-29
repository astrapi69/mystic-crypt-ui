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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

import javax.swing.JButton;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import io.github.astrapi69.design.pattern.state.component.AbstractJComponentStateMachine;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BtnSaveStateMachine
	extends
		AbstractJComponentStateMachine<JButton, BtnSaveComponentState>
	implements
		BtnSaveComponentState
{
	NewPrivateKeyModelBean modelObject;

	@Override
	protected void updateComponentState()
	{
		boolean filenameOfPrivateKeyPresent = StringUtils
			.isNotEmpty(modelObject.getFilenameOfPrivateKey());
		boolean privateKeyDirectoryPresent = modelObject.getPrivateKeyDirectory() != null;
		boolean privateKeyObjectPresent = modelObject.getPrivateKeyInfo() != null;
		boolean keySizePresent = modelObject.getKeySize() != null;
		if (filenameOfPrivateKeyPresent && privateKeyDirectoryPresent && privateKeyObjectPresent
			&& keySizePresent)
		{
			setEnabled(true);
			return;
		}
		setEnabled(false);
	}


	@Override
	public void onGenerate()
	{
		updateComponentState();
	}

	@Override
	public void onClear()
	{
		updateComponentState();
	}

	@Override
	public void onChangeFilename()
	{
		updateComponentState();
	}

	@Override
	public void onChangeDirectory()
	{
		updateComponentState();
	}

	@Override
	public void onChangeKeySize()
	{
		updateComponentState();
	}
}
