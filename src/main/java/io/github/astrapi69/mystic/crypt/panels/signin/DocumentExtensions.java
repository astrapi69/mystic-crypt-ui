package io.github.astrapi69.mystic.crypt.panels.signin;

import javax.swing.event.DocumentEvent;

import lombok.NonNull;

public class DocumentExtensions
{

	public static void processDocumentLength(final @NonNull DocumentEvent documentEvent,
		final @NonNull BtnOkStateMachine btnOkStateMachine)
	{
		int currentLength = documentEvent.getDocument().getLength();
		btnOkStateMachine.onChangeMasterPasswordLength(btnOkStateMachine);
	}
}
