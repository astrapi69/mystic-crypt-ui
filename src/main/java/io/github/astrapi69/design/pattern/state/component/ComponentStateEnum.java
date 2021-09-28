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
