package io.github.astrapi69.design.pattern.state.button;

public enum BtnOkComponentStateEnum implements BtnOkComponentState
{
	ENABLED {
		@Override
		public void onApplicationFileAdded(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithMasterPassword(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeMasterPasswordLength(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithKeyFile(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onSetKeyFile(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}
	},
	DISABLED {
		@Override
		public void onApplicationFileAdded(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithMasterPassword(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeMasterPasswordLength(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onChangeWithKeyFile(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}

		@Override
		public void onSetKeyFile(BtnOkStateMachine context)
		{
			context.onApplicationFileAdded(context);
		}
	}
}
