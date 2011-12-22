/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation; 
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either versi�n 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.miniapplet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivilegedActionException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JApplet;
import javax.swing.UIManager;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOFormatFileException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.keystores.main.common.AOKeyStore;


/** MiniApplet de firma del proyecto Afirma. */
public final class MiniAfirmaApplet extends JApplet implements MiniAfirma {

	private static final long serialVersionUID = -4364574240099120486L;

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private static final String APPLET_PARAM_USER_AGENT = "userAgent"; //$NON-NLS-1$
	
	private static final String APPLET_PARAM_USER_KEYSTORE = "keystore"; //$NON-NLS-1$

	/** Identificador del navegador Web que carga el applet. */
	private String userAgent = null;

	/** Tipo de almac&eacute;n de claves del que extraer los certificados. */
	private AOKeyStore keystoreType = null;
	
	/** Ruta de la biblioteca o almac&eacute;n del que extraer los certificados. */
	private String keystoreLib = null;
	
	/** Ruta recomendada para la apertura de di&aacute;logos se seleccion y guardado de ficheros. */
	private File pathHint = new File(Platform.getUserHome());

	/** Mensaje del &uacute;ltimo error producido. */
	private String errorMessage = null;

	/** {@inheritDoc} */
	public String sign(final String dataB64, 
	                   final String algorithm, 
	                   final String format, 
	                   final String extraParams) throws AOFormatFileException, 
	                                                    PrivilegedActionException, 
	                                                    IOException {
		this.cleanErrorMessage();
		
        if (dataB64 == null) {
            final IllegalArgumentException e = new IllegalArgumentException("Se han introducido datos nulos para firmar"); //$NON-NLS-1$
            setErrorMessage(e);
            throw e;
        }

        final Properties params = ExtraParamsProcessor.convertToProperties(extraParams);
        
        try {
            return Base64.encodeBytes(AccessController.doPrivileged(new SignAction(
                 this.selectSigner((format == null || format.trim().length() < 1 ? null : format.trim()), null), 
                 Base64.decode(dataB64),
                 (algorithm == null || algorithm.trim().length() < 1 ? null : algorithm.trim()), 
                 this.selectPrivateKey(params), 
                 ExtraParamsProcessor.expandProperties(params)
            )));
        }
        catch (final AOFormatFileException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        }

	}

	/** {@inheritDoc} */
	public String coSign(final String signB64, 
	                     final String dataB64, 
	                     final String algorithm, 
	                     final String format, 
	                     final String extraParams) throws AOFormatFileException, 
	                                                      PrivilegedActionException, 
	                                                      IOException {
		this.cleanErrorMessage();

		if (signB64 == null) {
		    final IllegalArgumentException e = new IllegalArgumentException("Se ha introducido una firma nula para contrafirmar"); //$NON-NLS-1$
            setErrorMessage(e);
            throw e; 
		}

		final Properties params = ExtraParamsProcessor.convertToProperties(extraParams);

		try {
		    final byte[] sign = Base64.decode(signB64);
            return Base64.encodeBytes(AccessController.doPrivileged(new CoSignAction(
               this.selectSigner((format == null || format.trim().length() < 1 ? null : format.trim()), sign), 
               sign, 
               (dataB64 == null ? null : Base64.decode(dataB64)),
               (algorithm == null || algorithm.trim().length() < 1 ? null : algorithm.trim()), 
               this.selectPrivateKey(params), 
               ExtraParamsProcessor.expandProperties(params)
            )));
        }
        catch (final AOFormatFileException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        } 

	}

	/** {@inheritDoc} */
	public String counterSign(final String signB64, 
	                          final String algorithm, 
	                          final String format, 
	                          final String extraParams) throws AOFormatFileException, 
	                                                           PrivilegedActionException, 
	                                                           IOException {
		this.cleanErrorMessage();

		if (signB64 == null) {
            final IllegalArgumentException e = new IllegalArgumentException("Se ha introducido una firma nula para contrafirmar"); //$NON-NLS-1$
            setErrorMessage(e);
            throw e; 
		}

		final Properties params = ExtraParamsProcessor.convertToProperties(extraParams);

		try {
		    final byte[] sign = Base64.decode(signB64);
            return Base64.encodeBytes(AccessController.doPrivileged(new CounterSignAction(
               this.selectSigner((format == null || format.trim().length() < 1 ? null : format.trim()), sign), 
               sign,
               (algorithm == null || algorithm.trim().length() < 1 ? null : algorithm.trim()), 
               this.selectPrivateKey(params), 
               ExtraParamsProcessor.expandProperties(params)
            )));
        }
        catch (final AOFormatFileException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        } 

	}

	/** {@inheritDoc} */
	public String getSignersStructure(final String signB64) throws IOException, PrivilegedActionException, AOFormatFileException {

		this.cleanErrorMessage();

		if (signB64 == null) {
		    final IllegalArgumentException e = new IllegalArgumentException("Se ha introducido un firma nula para la extraccion de firmantes"); //$NON-NLS-1$
		    setErrorMessage(e);
		    throw e;
		}

		final byte[] sign;
		final AOSigner signer;
        try {
            sign = Base64.decode(signB64);
            signer = this.getSigner(sign);
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
		
		if (signer == null) {
		    final AOFormatFileException e = new AOFormatFileException("Los datos introducidos no se corresponden con una firma soportada"); //$NON-NLS-1$
		    setErrorMessage(e);
		    throw e;
		}

		return AOUtil.showTreeAsString(signer.getSignersStructure(sign, false), null, null);

	}

	/** {@inheritDoc} */
	public boolean saveDataToFile(final String data, 
	                              final String title, 
	                              final String fileName, 
	                              final String extension, 
	                              final String description) throws PrivilegedActionException, IOException {

		this.cleanErrorMessage();

		if (data == null) {
			LOGGER.warning("Se ha solicitado guardar en disco un contenido nulo, se ignorara la peticion"); //$NON-NLS-1$
			return false;
		}

		final String titleDialog = (title == null || title.trim().length() < 1 ? null : title.trim());

		final String[] extensions = (extension == null || extension.trim().length() < 1 ?
				null : new String[] { extension.trim() });

		final String descFiles = (extensions != null && description != null && description.trim().length() > 0 ?
				description.trim() : (extensions != null ? extension : null));

		final File fileHint = (fileName != null && fileName.trim().length() > 0 ?
				new File(this.pathHint, fileName) : this.pathHint);

		try {
			return AccessController.doPrivileged(
                 new SaveFileAction(
                        titleDialog, 
                        Base64.decode(data),
					    extensions, 
					    descFiles, 
					    fileHint, 
					    this
				 )).booleanValue();
		} 
		catch (final AOCancelledOperationException e) {
			return false;
		}
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        }
	}

	/** {@inheritDoc} */
	public String loadFilePath(final String title, final String extensions, final String description) throws PrivilegedActionException {

		this.cleanErrorMessage();

		final String titleDialog = (title == null || title.trim().length() < 1 ? null : title.trim());

		final String[] exts = (extensions == null || extensions.trim().length() < 1 ?
				null : new String[] { extensions.trim() });

		final String descFiles = (exts != null && description != null && description.trim().length() > 0 ?
				description.trim() : (exts != null ? extensions : null));

		try {
			return AccessController.doPrivileged(
                 new GetFilePathAction(
					titleDialog, 
					exts, 
					descFiles, 
					this
				 )
            );
		} 
		catch (final AOCancelledOperationException e) {
			return null;
		}
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
	}

	/** {@inheritDoc} */
	public String getFileContent(final String title, final String extensions, final String description) throws PrivilegedActionException {

		this.cleanErrorMessage();

		final String titleDialog = (title == null || title.trim().length() < 1 ? null : title.trim());

		final String[] exts = (extensions == null || extensions.trim().length() < 1 ?
				null : new String[] { extensions.trim() });

		final String descFiles = (exts != null && description != null && description.trim().length() > 0 ?
				description.trim() : (exts != null ? extensions : null));

		try {
			return Base64.encodeBytes(AccessController.doPrivileged(new GetFileContentAction(
					titleDialog, exts, descFiles, this)));
		} 
		catch (final AOCancelledOperationException e) {
			return null;
		}
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
	}

	/** {@inheritDoc} 
	 * @deprecated */
	@Deprecated
    public String getFileNameContentText(final String title, final String extensions, final String description) throws PrivilegedActionException {
		this.cleanErrorMessage();
		// Se llama a setError() desde getFileNameContent, no es necesario repetirlo aqui
		return this.getFileNameContent(title, extensions, description, false);
	}

	/** {@inheritDoc} */
	public String getFileNameContentBase64(final String title, final String extensions, final String description) throws PrivilegedActionException {
		this.cleanErrorMessage();
		// Se llama a setError() desde getFileNameContent, no es necesario repetirlo aqui
        return this.getFileNameContent(title, extensions, description, true);
	}

	private String getFileNameContent(final String title, final String extensions, final String description, final boolean asBase64) throws PrivilegedActionException {

		this.cleanErrorMessage();

		final String titleDialog = (title == null || title.trim().length() < 1 ? null : title.trim());
		final String[] exts = (extensions == null || extensions.trim().length() < 1 ?
				null : new String[] { extensions.trim() });

		final String descFiles = (exts != null && description != null && description.trim().length() > 0 ?
				description.trim() : (exts != null ? extensions : null));

		try { 
			return AccessController.doPrivileged(new GetFileNameContentAction(
					titleDialog, exts, descFiles, false, asBase64, this))[0]; 
		} 
		catch (final AOCancelledOperationException e) {
			return null;
		}
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
	}

	/** {@inheritDoc} */
	public String getTextFromBase64(final String base64Data, final String charset) throws IOException {

		this.cleanErrorMessage();

		if (base64Data == null) {
			return null;
		}
		try {
		    if (charset != null && charset.trim().length() > 0) {
	            return new String(Base64.decode(base64Data), charset);
	        }
            return new String(Base64.decode(base64Data));
        }
        catch (final IOException e) {
            setErrorMessage(e);
            throw e;
        }
	}

	/** {@inheritDoc} */
	public String getBase64FromText(final String plainText, final String charset) throws UnsupportedEncodingException {
		this.cleanErrorMessage();
		if (plainText == null) {
			return null;
		}
		if (charset != null && charset.trim().length() > 0) {
			try {
                return Base64.encodeBytes(plainText.getBytes(charset));
            }
            catch (final UnsupportedEncodingException e) {
                setErrorMessage(e);
                throw e;
            }
		}
		return Base64.encodeBytes(plainText.getBytes());
	}

    /**
     * Verifica los requisitos m&iacute;nimos de la plataforma en la que se ejecuta el applet.
     * Si no cumple los requisitos m&iacute;nimos lanza una excepci&oacute;n con la
     * descripci&oacute;n del problema.
     * @throws PrivilegedActionException Cuando ocurre un error de seguridad.
     * @throws InvalidExternalLibraryException Cuando se encuentra alguna incopatibilidad en el entorno de
     * ejecuci&oacute;n.
     * @deprecated Se externaliza las comprobaciones de entorno.
     */
	@Deprecated
	public void verifyPlatform() throws PrivilegedActionException {
		this.cleanErrorMessage();
		try {
            AccessController.doPrivileged(new VerifyPlatformAction(this.userAgent));
        }
        catch (final PrivilegedActionException e) {
            setErrorMessage(e);
            throw e;
        }
	}
	
	/**
	 * Recupera la version de Java en una cadena de la forma "JX". En donde 'X' es
	 * la versi&oacute;n principal de Java (J5, J6, J7...).
	 * @return Versi&oacute;n de la JVM.
	 * @deprecated Se externaliza las comprobaciones de entorno.
	 */
	@Deprecated
	public String getEcoJava() { 
		return AccessController.doPrivileged(new GetEcoJavaVersionAction()).toString();
	}
	
	@Override
	public void init() {
		this.userAgent = this.getParameter(APPLET_PARAM_USER_AGENT);
		
		String keystoreParam = this.getParameter(APPLET_PARAM_USER_KEYSTORE);
		if (keystoreParam != null && !keystoreParam.equals("null")) { //$NON-NLS-1$
			keystoreParam = keystoreParam.trim();
			int separatorPos = keystoreParam.indexOf(':');
			try {
				if (separatorPos == -1) {
					this.keystoreType = AOKeyStore.valueOf(keystoreParam);
				} else if (keystoreParam.length() > 1) {
					this.keystoreType = AOKeyStore.valueOf(keystoreParam.substring(0, separatorPos).trim());
					if (separatorPos < keystoreParam.length() -1 &&
							keystoreParam.substring(separatorPos + 1).trim().length() > 0) {
						this.keystoreLib = keystoreParam.substring(separatorPos + 1).trim();
					}
				}
			} catch (final Exception e) {
				LOGGER.warning(
						"Se ha intentado cargar un almacen de certificados no soportado, se cargara el por defecto: " + e); //$NON-NLS-1$
			}
		}

		this.configureLookAndFeel();
		LOGGER.info("Miniapplet Afirma"); //$NON-NLS-1$
	}

	/** {@inheritDoc} */
	public String getErrorMessage() {
		return this.errorMessage;
	}

	/**
	 * Establece el mensaje de error al indicado en la excepci&oacute;n recibida.
	 * @param e Excepci&oacute;n que produjo el error.
	 * @throws Exception Excepci&oacute;n recibida.
	 */
	 private void setErrorMessage(final Exception e) {
		 this.errorMessage = (e.getLocalizedMessage() != null ? e.getLocalizedMessage() :
			 (e.getMessage() != null ? e.getMessage() : e.toString()));
		 if (this.errorMessage.startsWith("java.security.PrivilegedActionException:")) { //$NON-NLS-1$
			 this.errorMessage = this.errorMessage.substring(
					 "java.security.PrivilegedActionException:".length()).trim(); //$NON-NLS-1$
		 }
		 final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 PrintWriter writer = new PrintWriter(baos);
		 e.printStackTrace(writer);
		 writer.flush();
		 writer.close();
		 
	     Logger.getLogger("es.gob.afirma.MiniAfirmaApplet").severe(new String(baos.toByteArray())); //$NON-NLS-1$
	 }

	 /** Elimina el mensaje de error establecido previamente. */
	 private void cleanErrorMessage() {
		 this.errorMessage = null;
	 }

	 /**
	  * Permite que el usuario seleccione un certificado del almac&eacute;n configurado, o
	  * el por defecto si no se indic&oacute;, y devuelve su clave privada.
	  * @param params Configuraci&oacute;n general establecida para la operaci&oacute;n.
	  * @return Clave privada asociada al certificado seleccionado.
	  * @throws PrivilegedActionException Cuando ocurre un error de seguridad.
	  * @throws AOCancelledOperationException Cuando se cancela la operaci&oacute;n.
	  */
	 private PrivateKeyEntry selectPrivateKey(final Properties params) throws PrivilegedActionException {
		 
		 final SelectPrivateKeyAction selectPrivateKeyAction;
		 if (this.keystoreType == null) {
			 selectPrivateKeyAction = new SelectPrivateKeyAction(
					 Platform.getOS(), 
					 Platform.getBrowser(this.userAgent), 
					 new CertFilterManager(params), 
					 this);
		 } 
		 else {
			 selectPrivateKeyAction = new SelectPrivateKeyAction(
					 this.keystoreType,
					 this.keystoreLib,
					 new CertFilterManager(params),
					 this);
		 }
		 return AccessController.doPrivileged(selectPrivateKeyAction);
	 }

	 /** Devuelve un manejador de firma compatible con un formato de firma o, de no establecerse, con
	  * una firma electr&oacute;nica concreta.
	  * @param format Formato de firma.
	  * @param sign Firma electr&oacute;nica.
	  * @return Manejador de firma.
	  * @throws AOFormatFileException Cuando el formato o la firma no estan soportados.
	  * @throws PrivilegedActionException Cuando ocurre un error de seguridad.
	  * @throws NullPointerException Cuando no se indica ni formato ni firma como par&aacute;nmetro.
	  */
	 private AOSigner selectSigner(final String format, final byte[] sign) throws AOFormatFileException, PrivilegedActionException {
		 final AOSigner signer;
		 if (format != null) {
			 signer = this.getSigner(format);
			 if (signer == null) {
			     throw new AOFormatFileException("El formato de firma indicado no esta soportado"); //$NON-NLS-1$
			 }
			 if (sign != null &&  !signer.isSign(sign)) {
			     throw new AOFormatFileException("La firma electronica no es compatible con el formato de firma indicado"); //$NON-NLS-1$
			 }
		 } 
		 else if (sign != null) {
			 signer = this.getSigner(sign);
			 if (signer == null) {
			     throw new IllegalArgumentException("Los datos introducidos no se corresponden con una firma soportada"); //$NON-NLS-1$
			 }
		 } 
		 else {
			 throw new IllegalArgumentException("No se ha indicado el formato ni la firma que se desea tratar"); //$NON-NLS-1$
		 }
		 return signer;
	 }

	 /** Recupera un manejador de firma compatible con el formato indicado. Si no se encuentra uno
	  * compatible, se devuelve {@code null}.
	  * @param format Nombre de un formato de firma.
	  * @return Manejador de firma.
	  * @throws PrivilegedActionException Cuando ocurre un problema de seguridad.
	  */
	 private AOSigner getSigner(final String format) throws PrivilegedActionException {
		 return AccessController.doPrivileged(new SelectSignerAction(format));
	 }

	 /** Recupera un manejador de firma compatible con la firma indicada. Si no se encuentra uno
	  * compatible, se devuelve {@code null}.
	  * @param signature Firma electr&oacute;nica.
	  * @return Manejador de firma.
	  * @throws PrivilegedActionException Cuando ocurre un problema de seguridad.
	  */
	 private AOSigner getSigner(final byte[] signature) throws PrivilegedActionException {
		 return AccessController.doPrivileged(new SelectSignerAction(signature));
	 }

	 /** Configura la apariencia de los di&aacute;logos Java siguiendo la configuraci&oacute;n
	  * establecida en el sistema. 
	  */
	 private void configureLookAndFeel() {
		 try {
			 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		 }
		 catch (final Exception e) {
			 LOGGER.warning("No se ha podido establecer el Look&Feel: " + e); //$NON-NLS-1$
		 }

		 // Nos aseguramos de que los dialogos salgan decorados
		 javax.swing.JDialog.setDefaultLookAndFeelDecorated(true);
		 javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
	 }

	 /** {@inheritDoc} 
	  * @deprecated */
	 @Deprecated
	 public String[] getMultiFileNameContentText(final String title, 
	                                             final String extensions, 
	                                             final String description) throws PrivilegedActionException {
	        return this.getMultiFileNameContent(title, extensions, description, false);
	 }
	 
	 /** {@inheritDoc} */
	 public String[] getMultiFileNameContentBase64(final String title, 
	                                               final String extensions, 
	                                               final String description) throws IOException, PrivilegedActionException {
	        return this.getMultiFileNameContent(title, extensions, description, true);
	 }
	 
	 private String[] getMultiFileNameContent(final String title, final String extensions, final String description, final boolean asBase64) throws PrivilegedActionException {
	        this.cleanErrorMessage();
	        final String titleDialog = (title == null || title.trim().length() < 1 ? null : title.trim());
	        final String[] exts = (extensions == null || extensions.trim().length() < 1 ?
	                null : new String[] { extensions.trim() });
	        final String descFiles = (exts != null && description != null && description.trim().length() > 0 ?
	                description.trim() : (exts != null ? extensions : null));
	        try { 
	            return AccessController.doPrivileged(new GetFileNameContentAction(
	                    titleDialog, exts, descFiles, true, asBase64, this)); 
	        } 
	        catch (final AOCancelledOperationException e) {
	            return null;
	        }
	        catch (final PrivilegedActionException e) {
	            setErrorMessage(e);
	            throw e;
	        }
     }


}
