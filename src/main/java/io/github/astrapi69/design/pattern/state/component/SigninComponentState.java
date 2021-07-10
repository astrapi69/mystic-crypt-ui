package io.github.astrapi69.design.pattern.state.component;

public interface SigninComponentState extends ComponentState
{
	void onApplicationFileAdded(JComponentStateMachine context);
	void onChangeWithMasterPassword(JComponentStateMachine context);
	void onChangeMasterPasswordLength(JComponentStateMachine context);
	void onChangeWithKeyFile(JComponentStateMachine context);

}
