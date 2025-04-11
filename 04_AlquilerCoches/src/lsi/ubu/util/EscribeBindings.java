package lsi.ubu.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase de utilidad para reconfigurar el pool de conexiones a la bases de
 * datos.
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jesus Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Raul Marticorena</a>
 */
public class EscribeBindings {
	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(EscribeBindings.class);

	/**
	 * Principal.
	 * 
	 * @param args argumentos (se ignoran)
	 */
	public static void main(String[] args) {
		try {
			PoolDeConexiones.reconfigurarPool();
			LOGGER.info("Pool reconfigurado con exito.");
		} catch (Exception e) {
			LOGGER.error("Error reconfigurando el pool de conexiones.");
			LOGGER.error(e.getMessage());
		}
	}

}
