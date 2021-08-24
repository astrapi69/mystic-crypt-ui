package io.github.astrapi69.mystic.crypt.panels.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.security.PrivateKey;
import java.security.Security;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collections.list.ListFactory;
import io.github.astrapi69.crypto.key.PrivateKeyDecryptor;
import io.github.astrapi69.crypto.key.PrivateKeyGenericDecryptor;
import io.github.astrapi69.crypto.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypto.model.CryptModel;
import io.github.astrapi69.delete.DeleteFileExtensions;
import io.github.astrapi69.gson.JsonStringToObjectExtensions;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.read.ReadFileExtensions;
import io.github.astrapi69.search.PathFinder;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class ApplicationFileWithKeyFactoryTest {

    PrivateKey derPrivateKey;
    PrivateKey pemPrivateKey;

    File derDir;
    File pemDir;

    File privateKeyDerFile;
    File privateKeyPemFile;

    File applicationFile;
    String selectedApplicationFilePath;
    File decryptedApplicationFile;
    CryptModel<Cipher, PrivateKey, byte[]> decryptModel;
    PrivateKeyDecryptor decryptor;
    PrivateKeyGenericDecryptor<String> genericDecryptor;

    @BeforeEach
    void setUp() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        applicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(),
                "empty-db-with-key" + ApplicationFileFactory.MCRDB_FILE_EXTENSION);
        selectedApplicationFilePath = applicationFile.getAbsolutePath();
        decryptedApplicationFile = PathFinder.getRelativePath(PathFinder.getSrcTestResourcesDir(), "empty-db-with-key.json");

        pemDir = new File(PathFinder.getSrcTestResourcesDir(), "pem");
        privateKeyPemFile = new File(pemDir, "private.pem");
        pemPrivateKey = RuntimeExceptionDecorator
                .decorate(() -> PrivateKeyReader.readPemPrivateKey(privateKeyPemFile));
        decryptModel = CryptModel.<Cipher, PrivateKey, byte[]>builder().key(pemPrivateKey)
                .build();
        decryptor = RuntimeExceptionDecorator
                .decorate(() -> new PrivateKeyDecryptor(decryptModel));
        genericDecryptor = new PrivateKeyGenericDecryptor<>(decryptor);

        derDir = new File(PathFinder.getSrcTestResourcesDir(), "der");
        privateKeyDerFile = new File(derDir, "private.der");
        derPrivateKey = RuntimeExceptionDecorator
                .decorate(() -> PrivateKeyReader.readPrivateKey(privateKeyDerFile));

    }

    @AfterEach
    void tearDown() {
        RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(applicationFile));
        RuntimeExceptionDecorator.decorate(() -> DeleteFileExtensions.delete(decryptedApplicationFile));
        selectedApplicationFilePath = null;
        decryptModel = null;
    }

    @Test
    void newApplicationFileWithPrivateKey() throws Exception {
        // define parameter for the unit test
        File actualEncryptedFile;
        MasterPwFileModelBean modelObject;
        ApplicationModelBean applicationModelBean;
        // create test data
        modelObject = MasterPwFileModelBean.builder()
                .applicationFile(applicationFile)
                .privateKey(pemPrivateKey)
                .keyFile(privateKeyPemFile)
                .applicationFilePaths(ListFactory.newArrayList(""))
                .keyFilePaths(ListFactory.newArrayList(""))
                .build();
        // test the actual method
        actualEncryptedFile = ApplicationFileFactory.newApplicationFileWithPrivateKey(modelObject);

        // proof that method is working as expected
        byte[] encryptedBytes = ReadFileExtensions.readFileToBytearray(actualEncryptedFile);
        String json = genericDecryptor.decrypt(encryptedBytes);
        applicationModelBean = JsonStringToObjectExtensions.toObject(json,
                ApplicationModelBean.class);
        assertNotNull(applicationModelBean);
        MasterPwFileModelBean masterPwFileModelBean = applicationModelBean.getMasterPwFileModelBean();
        assertEquals(modelObject, masterPwFileModelBean);

        ApplicationModelBean modelBeanReaded = ApplicationFileReader.readApplicationFileWithPrivateKey(
                modelObject);
        assertNotNull(modelBeanReaded);
        assertEquals(applicationModelBean, modelBeanReaded);
    }

}
