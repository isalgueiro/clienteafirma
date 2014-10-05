package es.gob.afirma.test.pades;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.signers.pades.PdfHasUnregisteredSignaturesException;

/** Pruebas espec&iacute;ficas para el Bug 305907.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class Test305907 {

	private final static String TEST_FILE = "CONVENIO.pdf"; //$NON-NLS-1$
	private final static String TEST_FILE_2 = "CONVENIO_firmado_CF.pdf"; //$NON-NLS-1$

	private final static String DEFAULT_SIGNATURE_ALGORITHM = "SHA512withRSA"; //$NON-NLS-1$

	private final static String CERT_PATH = "ANF_PF_Activo.pfx"; //$NON-NLS-1$
	private final static String CERT_PASS = "12341234"; //$NON-NLS-1$
	private final static String CERT_ALIAS = "anf usuario activo"; //$NON-NLS-1$

	@SuppressWarnings("static-method")
	@Test
	public void testReadRaw() throws Exception {

		byte[] testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(TEST_FILE_2));

		final AOPDFSigner signer = new AOPDFSigner();
		Assert.assertTrue(signer.isSign(testPdf));

		testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(TEST_FILE));
		Assert.assertFalse(signer.isSign(testPdf));

        final KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
        ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
        final PrivateKeyEntry pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));

        boolean raises = false;
        try {
	        signer.sign(
	    		testPdf,
	    		DEFAULT_SIGNATURE_ALGORITHM,
	    		pke.getPrivateKey(),
	    		pke.getCertificateChain(),
	    		new Properties()
			);
        }
        catch (final PdfHasUnregisteredSignaturesException e) {
        	raises = true;
        }
        Assert.assertTrue(raises);

        final Properties p = new Properties();
        p.put("allowCosigningUnregisteredSignatures", "true"); //$NON-NLS-1$ //$NON-NLS-2$

        raises = false;
        try {
	        signer.sign(
	    		testPdf,
	    		DEFAULT_SIGNATURE_ALGORITHM,
	    		pke.getPrivateKey(),
	    		pke.getCertificateChain(),
	    		p
			);
        }
        catch (final PdfHasUnregisteredSignaturesException e) {
        	raises = true;
        }
        Assert.assertFalse(raises);

	}

}
