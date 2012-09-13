/*
 * Controlador Java de la Secretaría de Estado de Administraciones Públicas
 * para el DNI electrónico.
 *
 * El Controlador Java para el DNI electrónico es un proveedor de seguridad de JCA/JCE 
 * que permite el acceso y uso del DNI electrónico en aplicaciones Java de terceros 
 * para la realización de procesos de autenticación, firma electrónica y validación 
 * de firma. Para ello, se implementan las funcionalidades KeyStore y Signature para 
 * el acceso a los certificados y claves del DNI electrónico, así como la realización 
 * de operaciones criptográficas de firma con el DNI electrónico. El Controlador ha 
 * sido diseñado para su funcionamiento independiente del sistema operativo final.
 * 
 * Copyright (C) 2012 Dirección General de Modernización Administrativa, Procedimientos 
 * e Impulso de la Administración Electrónica
 * 
 * Este programa es software libre y utiliza un licenciamiento dual (LGPL 2.1+
 * o EUPL 1.1+), lo cual significa que los usuarios podrán elegir bajo cual de las
 * licencias desean utilizar el código fuente. Su elección deberá reflejarse 
 * en las aplicaciones que integren o distribuyan el Controlador, ya que determinará
 * su compatibilidad con otros componentes.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los términos de la 
 * Lesser GNU General Public License publicada por la Free Software Foundation, 
 * tanto en la versión 2.1 de la Licencia, o en una versión posterior.
 * 
 * El Controlador puede ser redistribuido y/o modificado bajo los términos de la 
 * European Union Public License publicada por la Comisión Europea, 
 * tanto en la versión 1.1 de la Licencia, o en una versión posterior.
 * 
 * Debería recibir una copia de la GNU Lesser General Public License, si aplica, junto
 * con este programa. Si no, consúltelo en <http://www.gnu.org/licenses/>.
 * 
 * Debería recibir una copia de la European Union Public License, si aplica, junto
 * con este programa. Si no, consúltelo en <http://joinup.ec.europa.eu/software/page/eupl>.
 *
 * Este programa es distribuido con la esperanza de que sea útil, pero
 * SIN NINGUNA GARANTÍA; incluso sin la garantía implícita de comercialización
 * o idoneidad para un propósito particular.
 */
package es.gob.jmulticard.asn1.der.pkcs15;

/** Tipo ASN.1 PKCS#15 <i>PrivateKeyObject</i>.
 * <pre>
 *  PrivateKeyObject {KeyAttributes} ::= PKCS15Object {
 *    CommonKeyAttributes,
 *    PrivateRsaKeyAttributes
 *  }
 * </pre>
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class PrivateKeyObject extends Pkcs15Object {

	/** Construye un objeto ASN.1 PKCS#15 <i>PrivateKeyObject</i> */
	public PrivateKeyObject() {
		super(
			CommonKeyAttributes.class,
			PrivateRsaKeyAttributesContextSpecific.class
		);
	}

	/** Obtiene el nombre de la clave privada.
	 * @return Nombre de la clave privada */
	String getKeyIdentifier() {
		return new String(((CommonKeyAttributes) this.getClassAttributes()).getIdentifier());
	}
	
	/** Obtiene el nombre de la clave privada.
	 * @return Nombre de la clave privada */
	String getKeyName() {
		return getCommonObjectAttributes().getLabel();
	}

	String getKeyPath() {
		return ((PrivateRsaKeyAttributesContextSpecific)getTypeAttributes()).getPath();
	}

    /** {@inheritDoc} */
    public String toString() {
    	return "Nombre de la clave privada: " + getCommonObjectAttributes().getLabel(); //$NON-NLS-1$
    }

}