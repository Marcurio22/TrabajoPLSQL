package lsi.ubu.util.exceptions;

import java.sql.SQLException;

/**
 * Utilidad para el tratamiento de errores en bases de datos.
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jes�s Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Ra�l Marticorena</a>
 * @version 1.0
 * @since 1.0
 */
public interface SGBDErrorUtil {

	/**
	 * Traduce el c�digo de error num�rico al valor de error correspondiente en el
	 * SGBD que se est� utilizando.
	 * 
	 * @param errorSGBD n�mero devuelto por el SGBD
	 * @return el tipo de error correspondiente
	 */
	SGBDError translate(int errorSGBD);

	/**
	 * Comprueba si la excepci�n contiene un cierto c�digo de error.
	 * 
	 * @param ex    excepci�n con c�digo interno de la base de datos
	 * @param error error en la base de datos
	 * @return true si coinciden, false en caso contrario
	 */
	boolean checkExceptionToCode(SQLException ex, SGBDError error);
}