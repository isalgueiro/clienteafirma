//In latest Java 8, there are some changes with respect to the source code structure. Now you need to use below command to compile the source code to remove the errors ad warnings:
//javac -XDignore.symbol.file
//When javac is compiling code it doesn't link against rt.jar by default in latest Java 8. Instead it uses special symbol file lib/ct.sym with class stubs. The option -XDignore.symbol.file is to ignore the symbol file so that it will link against rt.jar.

package es.gob.afirma.standalone.configurator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509ExtensionUtils;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcDigestCalculatorProvider;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

/** Utilidades para la creaci&oacute;n de certificados X.509.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
final class CertUtil {

	private static final String AF_ROOT_SUBJECT_PRINCIPAL = "CN=AutoFirma ROOT"; //$NON-NLS-1$

	private static final int KEY_SIZE = 2048;

	private static final String PROVIDER = "SC"; //$NON-NLS-1$

	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA"; //$NON-NLS-1$

	static class CertPack {

		private final Certificate sslCert;
		private final PrivateKey prK;
		private final String alias;
		private final char[] password;
		private byte[] p12 = null;
		private final Certificate caCert;

		CertPack(final Certificate caCertificate,
				 final PrivateKeyEntry sslCertificatePrivateKeyEntry,
				 final String sslCertificateAlias,
				 final char[] storePassword) {

			this.sslCert = sslCertificatePrivateKeyEntry.getCertificate();
			this.caCert = caCertificate;
			this.prK = sslCertificatePrivateKeyEntry.getPrivateKey();
			this.alias = sslCertificateAlias;
			this.password = storePassword;
		}

		Certificate getSslCertificate() {
			return this.sslCert;
		}

		Certificate getCaCertificate() {
			return this.caCert;
		}

		byte[] getPkcs12() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			if (this.p12 == null) {
				final KeyStore keyStore = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
				keyStore.load(null, null);
				keyStore.setKeyEntry(
					this.alias,
					this.prK,
					this.password,
					new Certificate[] { this.sslCert, this.caCert }
				);
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				keyStore.store(baos, this.password);
				this.p12 = baos.toByteArray();
			}
			return this.p12;
		}
	}

	/**
	 * Genera una tupla con un certificado de CA y un certificado SSL emitido por el para la comunicaci&oacute;n con el
	 * equipo local.
	 * @param sslCertificateAlias Alias con el que identificar el certificado SSL
	 * @param storePassword
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 */
	static CertPack getCertPackForLocalhostSsl(final String sslCertificateAlias, final String storePassword) throws NoSuchAlgorithmException, CertificateException, IOException {

		Security.addProvider(new BouncyCastleProvider());
		final PrivateKeyEntry caCertificatePrivateKeyEntry = generateCaCertificate(
			AF_ROOT_SUBJECT_PRINCIPAL
		);
		final PrivateKeyEntry sslCertificatePrivateKeyEntry = generateSslCertificate(
			"127.0.0.1", //$NON-NLS-1$
			caCertificatePrivateKeyEntry
		);
		return new CertPack(
			caCertificatePrivateKeyEntry.getCertificate(),
			sslCertificatePrivateKeyEntry,
			sslCertificateAlias,
			storePassword.toCharArray()
		);

	}

	/**
	 * Genera un certificado de CA apto para la emisi&oacute;n de un certificado SSL.
	 * @param subjectPrincipal Principal del subject del certificado.
	 * @return Entrada con el certificado y su conjunto de claves.
	 * @throws NoSuchAlgorithmException Cuando no se reconoce el algoritmo de generaci&oacute;n de claves.
	 * @throws CertificateException Cuando ocurre un error en la codificacion del certificado.
	 * @throws IOException Cuando ocurre un error al generar el certificado.
	 */
	private static PrivateKeyEntry generateCaCertificate(final String subjectPrincipal) throws NoSuchAlgorithmException,
                                                                                               CertificateException,
                                                                                               IOException {
		// Generamos el par de claves...
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA"); //$NON-NLS-1$
		keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
		final KeyPair keyPair = keyPairGenerator.generateKeyPair();

		//Creamos el generador de certificados
		final Date expirationDate = new Date();
		expirationDate.setTime(new Date().getTime()+(long)10*365*24*3600*1000);
		final X509v3CertificateBuilder generator = new JcaX509v3CertificateBuilder(
			new X500Name(subjectPrincipal),
			BigInteger.valueOf(new Random().nextInt()),
    		new Date(),
    		expirationDate,
    		new X500Name(subjectPrincipal),
    		keyPair.getPublic()
		);

		//Se incluyen los atributos del certificado CA
		DigestCalculator digCalc = null;
		try {
			digCalc = new BcDigestCalculatorProvider()
			        .get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
		}
		catch (final OperatorCreationException e) {
			throw new IOException("No se ha podido inicializar el operador de cifrado: " + e, e); //$NON-NLS-1$
		}
        final X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(digCalc);

        final byte[] encoded = keyPair.getPublic().getEncoded();
        final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded));
		generator.addExtension(Extension.subjectKeyIdentifier, false,
				x509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo));
	    generator.addExtension(Extension.basicConstraints, true,
	            new BasicConstraints(true));

	    final KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign
	    		| KeyUsage.digitalSignature | KeyUsage.keyEncipherment
	    		| KeyUsage.dataEncipherment | KeyUsage.cRLSign);

	    generator.addExtension(Extension.keyUsage, false, usage);

	    final ASN1EncodableVector purposes = new ASN1EncodableVector();
	    purposes.add(KeyPurposeId.id_kp_serverAuth);
	    purposes.add(KeyPurposeId.id_kp_clientAuth);
	    purposes.add(KeyPurposeId.anyExtendedKeyUsage);
	    generator.addExtension(Extension.extendedKeyUsage, false,
	    		new DERSequence(purposes));

	    //Firma del CA con su propia clave privada (autofirmado)
	    X509Certificate cert = null;
		try {
			cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(
				generator.build(
					new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER).build(
						keyPair.getPrivate()
					)
				)
			);
		}
		catch (final OperatorCreationException e) {
			throw new CertificateException("Error durante la construccion del certificado CA: " + e, e); //$NON-NLS-1$
		}

        //Definicion de propiedades del certificado
        return new PrivateKeyEntry(
        		keyPair.getPrivate(),
			new Certificate[] {
				cert
			}
		);
	}

	private static PrivateKeyEntry generateSslCertificate(final String cn,
			                                              final PrivateKeyEntry issuerKeyEntry) {

		// Generamos las claves...
		X509Certificate cert;
		KeyPair pair;
		try {
			final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA"); //$NON-NLS-1$
			keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
			pair = keyPairGenerator.generateKeyPair();

			//Generamos el generador de certificados
			final Date expirationDate = new Date();
			expirationDate.setTime(new Date().getTime()+(long)10*365*24*3600*1000);
			final X500Name issuerDN = new JcaX509CertificateHolder((X509Certificate) issuerKeyEntry.getCertificate()).getSubject();

			final X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
				issuerDN,
				BigInteger.valueOf(new Random().nextInt()),
				new Date(),
	    		expirationDate,
	    		new X500Name("CN="+cn), //$NON-NLS-1$
				pair.getPublic()
			);

			//Incluimos los atributos del certifiado
			final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
			certBuilder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(pair.getPublic()));
			certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
			certBuilder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(issuerKeyEntry.getCertificate().getPublicKey()));

			final List<GeneralName> altNames = new ArrayList<>();
			altNames.add(new GeneralName(GeneralName.iPAddress, cn));
			if (altNames.size() > 0) {
				final GeneralNames subjectAltName = new GeneralNames(altNames.toArray(new GeneralName [altNames.size()]));
				certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
			}

			//Firma del certificado SSL con la clave privada del CA
			final ContentSigner caSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER).build(
				issuerKeyEntry.getPrivateKey()
			);
			cert = new JcaX509CertificateConverter().setProvider(PROVIDER)
					.getCertificate(certBuilder.build(caSigner));
		}
		catch (final Exception | Error t) {
			throw new RuntimeException("Error al generar el certificado SSL: " + t, t); //$NON-NLS-1$
		}
        return new PrivateKeyEntry(
    		pair.getPrivate(),
    		new Certificate[] {
				cert
    		}
		);

	}
}
