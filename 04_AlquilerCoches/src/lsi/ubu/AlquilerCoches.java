package lsi.ubu;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.tests.Tests;
import lsi.ubu.util.ExecuteScript;

/**
 * AlquierCoches: Implementa la facturacion de un coche de alquiler segun el PDF de la carpeta enunciado
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jesus Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Raul Marticorena</a>
 * @author <a href="mailto:pgdiaz@ubu.es">Pablo García</a>
 * @author <a href="mailto:srarribas@ubu.es">Sandra Rodríguez</a>
 * @version 1.0
 * @since 1.0
 */
public class AlquilerCoches {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlquilerCoches.class);

	public static void main(String[] args) throws SQLException {

		LOGGER.info("Comienzo de los tests");

		// Crear las tablas y filas en base de datos para la prueba
		ExecuteScript.run("sql/alquiler_coches.sql");

		// Ejecutar los tests
		Tests tests = new Tests();
		tests.ejecutarTests();

		LOGGER.info("Fin de los tests");
	}
}
