package es.gob.afirma.envelopers.cms;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;

import org.junit.Assert;
import org.junit.Test;

import es.gob.afirma.core.misc.AOUtil;

/** Pruebas de sobre digitales.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class TestEnvelopes {

    private static final String CERT_PATH = "PFActivoFirSHA256.pfx"; //$NON-NLS-1$
    private static final String CERT_PASS = "12341234"; //$NON-NLS-1$
    private static final String CERT_ALIAS = "fisico activo prueba"; //$NON-NLS-1$

	/** Prueba de apertura de sobre.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void openEnvelope() throws Exception {

		final KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
		ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
		final PrivateKeyEntry pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));

		final byte[] envelope = AOUtil.getDataFromInputStream(
			TestEnvelopes.class.getResourceAsStream("/sample.enveloped") //$NON-NLS-1$
		);

		final byte[] originalData = AOUtil.getDataFromInputStream(
				TestEnvelopes.class.getResourceAsStream("/sample") //$NON-NLS-1$
		);

		final byte[] recoveredData = new AOCMSEnveloper().recoverData(envelope, pke);

		Assert.assertArrayEquals(originalData, recoveredData);

	}

}
