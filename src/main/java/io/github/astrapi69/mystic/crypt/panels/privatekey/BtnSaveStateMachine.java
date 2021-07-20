package io.github.astrapi69.mystic.crypt.panels.privatekey;

import javax.swing.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import org.springframework.util.ObjectUtils;

import io.github.astrapi69.design.pattern.state.component.AbstractJComponentStateMachine;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BtnSaveStateMachine
	extends AbstractJComponentStateMachine<JButton, BtnSaveComponentState>
	implements BtnSaveComponentState
{
	NewPrivateKeyModelBean modelObject;

	@Override
	protected void updateComponentState()
	{
		boolean filenameOfPrivateKeyPresent = !ObjectUtils.isEmpty(modelObject.getFilenameOfPrivateKey());
		boolean privateKeyDirectoryPresent = modelObject.getPrivateKeyDirectory() != null;
		boolean privateKeyObjectPresent = modelObject.getPrivateKey() != null;
		boolean keySizePresent = modelObject.getKeySize() != null;
		if (filenameOfPrivateKeyPresent
		&& privateKeyDirectoryPresent
			&& privateKeyObjectPresent
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

	@Override public void onChangeKeySize()
	{
		updateComponentState();
	}
}
