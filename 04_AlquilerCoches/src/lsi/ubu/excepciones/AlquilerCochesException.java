package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AlquierCochesException: Implementa las excepciones contextualizadas de la
 * transaccion de alquiler de coches
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jesus Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Raul Marticorena</a>
 * @author <a href="mailto:srarribas@ubu.es">Sandra Rodr�guez</a>
 * @version 1.2
 * @since 1.0
 */
public class AlquilerCochesException extends SQLException {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(AlquilerCochesException.class);

	public static final int CLIENTE_NO_EXIST = 1;
	public static final int VEHICULO_NO_EXIST = 2;
	public static final int SIN_DIAS = 3;
	public static final int VEHICULO_OCUPADO = 4;

	private int codigo; // = -1;
	private String mensaje;

	public AlquilerCochesException(int code) {

		codigo = code;
		switch(code) {
			case CLIENTE_NO_EXIST:
				mensaje = "Cliente inexistente";
				break;
			case VEHICULO_NO_EXIST:
				mensaje = "Vehículo inexistente";
				break;
			case SIN_DIAS:
				mensaje = "El número de días será mayor que cero";
				break;
			case VEHICULO_OCUPADO:
				mensaje = "El vehículo no está disponible";
				break;
		}

		LOGGER.debug(mensaje);

		// Traza_de_pila
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			LOGGER.debug(ste.toString());
		}
	}

	@Override
	public String getMessage() { // Redefinicion del metodo de la clase Exception
		return mensaje;
	}

	@Override
	public int getErrorCode() { // Redefinicion del metodo de la clase SQLException
		return codigo;
	}
}
