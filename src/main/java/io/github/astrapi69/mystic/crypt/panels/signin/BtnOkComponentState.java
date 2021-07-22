package io.github.astrapi69.mystic.crypt.panels.signin;

public interface BtnOkComponentState
{
	void onApplicationFileAdded(BtnOkStateMachine context);
	void onChangeWithMasterPassword(BtnOkStateMachine context);
	void onChangeMasterPasswordLength(BtnOkStateMachine context);
	void onChangeWithKeyFile(BtnOkStateMachine context);
	void onSetKeyFile(BtnOkStateMachine context);
}
