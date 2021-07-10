package io.github.astrapi69.design.pattern.state.button;

public interface BtnOkComponentState
{
	void onApplicationFileAdded(BtnOkStateMachine context);
	void onChangeWithMasterPassword(BtnOkStateMachine context);
	void onChangeMasterPasswordLength(BtnOkStateMachine context);
	void onChangeWithKeyFile(BtnOkStateMachine context);
	void onSetKeyFile(BtnOkStateMachine context);
}
