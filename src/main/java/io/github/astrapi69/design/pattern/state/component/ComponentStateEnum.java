package io.github.astrapi69.design.pattern.state.component;

public enum ComponentStateEnum implements SigninComponentState
{
	ENABLED {
		@Override
		public void onApplicationFileAdded(JComponentStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithMasterPassword(JComponentStateMachine context)
		{
			context.onChangeWithMasterPassword(context);
		}

		@Override
		public void onChangeMasterPasswordLength(JComponentStateMachine context)
		{
			context.onChangeWithMasterPassword(context);
		}

		@Override
		public void onChangeWithKeyFile(JComponentStateMachine context)
		{
			context.onChangeWithKeyFile(context);
		}
	},
	DISABLED {
		@Override
		public void onApplicationFileAdded(JComponentStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithMasterPassword(JComponentStateMachine context)
		{
			context.onChangeWithMasterPassword(context);
		}

		@Override
		public void onChangeMasterPasswordLength(JComponentStateMachine context)
		{
			context.onChangeWithMasterPassword(context);
		}

		@Override
		public void onChangeWithKeyFile(JComponentStateMachine context)
		{
			context.onChangeWithKeyFile(context);
		}

	}
}
