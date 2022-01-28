package io.github.astrapi69.mystic.crypt.panel.signin;

import io.github.astrapi69.file.read.ReadFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

import java.io.File;

class MasterPwFileDialogTest {

    public static void main(final String[] a)
    {
        final MemoizedSigninModelBean memoizedSigninModelBean = MemoizedSigninModelBean.builder().build();

        MasterPwFileModelBean masterPwFileModelBean = MasterPwFileModelBean.builder()
                .minPasswordLength(6).withKeyFile(false).withMasterPw(false).showMasterPw(false)
                .build();
        masterPwFileModelBean.merge(memoizedSigninModelBean);
        IModel<MasterPwFileModelBean> model = BaseModel
                .<MasterPwFileModelBean> of(masterPwFileModelBean);
        MasterPwFileDialog dialog = new MasterPwFileDialog(null, "Enter your credentials", true,
            model);
        dialog.setSize(880, 380);
        dialog.setVisible(true);
    }
}
