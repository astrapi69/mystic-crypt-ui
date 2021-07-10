package io.github.astrapi69.mystic.crypt.panels.signin;

import javax.swing.event.DocumentEvent;

import lombok.NonNull;
import io.github.astrapi69.design.pattern.state.button.BtnOkStateMachine;

public class DocumentExtensions
{

	public static void processDocumentLength(final @NonNull DocumentEvent e,
		final @NonNull BtnOkStateMachine btnOkStateMachine)
	{
		int currentLength = e.getOffset();
		if (e.getType().equals(DocumentEvent.EventType.INSERT))
		{
			currentLength++;
		}
		if (e.getType().equals(DocumentEvent.EventType.REMOVE))
		{
			currentLength--;
		}
		btnOkStateMachine.setPasswordLength(currentLength);
		btnOkStateMachine.onChangeMasterPasswordLength(btnOkStateMachine);
	}
}
