package lsi.ubu.tests;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AlquilerCochesException;
import lsi.ubu.servicios.Servicio;
import lsi.ubu.servicios.ServicioImpl;
import lsi.ubu.util.PoolDeConexiones;

public class Tests {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Tests.class);

	public void ejecutarTests() throws SQLException {

		Servicio servicio = new ServicioImpl();

		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		SimpleDateFormat formatoFechas = new SimpleDateFormat("dd-MM-yyyy");

		// caso 1 nro dias negativo
		{
			try {
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DATE, -1);

				Date ayer = calendar.getTime();

				servicio.alquilar("12345678A", "1234-ABC", new Date(), ayer);
				LOGGER.info("Nro de dias insuficiente MAL no da excepcion");
			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.SIN_DIAS) {
					LOGGER.info("Nro de dias insuficiente OK");
				} else {
					LOGGER.info("Nro de dias insuficiente MAL");
				}
			}
		}

		/* Creamos fechas adecuadas */
		Date fechaIni = null;
		Date fechaFin = null;

		try {
			fechaIni = formatoFechas.parse("20-03-2013");
			fechaFin = formatoFechas.parse("22-03-2013");
		} catch (ParseException e1) {
			LOGGER.error("Error en el test al parsear la fechas desde cadena.");
		}

		// caso 2 vehiculo inexistente
		{
			try {
				servicio.alquilar("87654321Z", "9999-ZZZ", fechaIni, fechaFin);
				LOGGER.info("Alquilar vehiculo inexistente MAL no da excepcion");
			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.VEHICULO_NO_EXIST) {
					LOGGER.info("Alquilar vehiculo inexistente OK");
				} else {
					LOGGER.info("Alquilar vehiculo inexistente MAL");
				}
			}
		}

		// caso 3 cliente inexistente
		{
			try {
				servicio.alquilar("87654321Z", "1234-ABC", fechaIni, fechaFin);
				LOGGER.info("Alquilar a cliente inexistente MAL no da excepcion");
			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.CLIENTE_NO_EXIST) {
					LOGGER.info("Alquilar  a cliente inexistente OK");
				} else {
					LOGGER.info("Alquilar a cliente inexistente MAL");
				}
			}
		}

		// caso 4 Todo correcto pero NO especifico la fecha final
		{
			Connection con = null;
			PreparedStatement st = null;
			ResultSet rs = null;
			CallableStatement cst = null;

			try {
				// Reinicio filas
				con = pool.getConnection();
				cst = con.prepareCall("{call inicializa_test}");
				cst.execute();

				fechaIni = formatoFechas.parse("11-3-2013");

				servicio.alquilar("12345678A", "1234-ABC", fechaIni, null);

				String query = "";
				query += " SELECT listAgg(matricula||TO_CHAR(fecha_ini, 'DD-MM-YYYY')||TO_CHAR(fecha_fin, 'DD-MM-YYYY')||facturas.importe||cliente||concepto||lineas_factura.importe, '#') ";
				query += " within group (order by nroFactura, concepto) ";
				query += " FROM facturas join lineas_factura using(NroFactura) ";
				query += " join reservas using(cliente) ";

				st = con.prepareStatement(query);

				rs = st.executeQuery();
				rs.next();

				String resultado = rs.getString(1);
				if (rs.wasNull()) {
					resultado = ""; // El join esta vacio
				}

				LOGGER.info("Caso alquiler correcto pero NO especifico la fecha final --------------");

				String resultadoPrevisto = "1234-ABC11-03-201313512345678A4 dias de alquiler, vehiculo modelo 1   60#1234-ABC11-03-201313512345678ADeposito lleno de 50 litros de Gasolina 75";

				if (resultado.equals(resultadoPrevisto)) {
					LOGGER.info("SI Coinciden la factura y las linea de factura  OK");
				} else {
					LOGGER.info("NO Coinciden la factura y las linea de factura  MAL");
					LOGGER.info("Se obtiene...*" + resultado + "*");
					LOGGER.info("Y deberia ser*" + resultadoPrevisto + "*");
				}

				rs.close();

			} catch (SQLException e) {
				LOGGER.error(e.getMessage());
			} catch (ParseException e) {
				LOGGER.error("Error en el test al parsear la fechas desde cadena.");
			} finally {
				if (cst != null) {
					cst.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
				if (con != null) {
					con.close();
				}
			}
		}

		// caso 5 Intentar alquilar un coche ya alquilado
		// --5.1 la fecha ini del alquiler esta dentro de una reserva
		{
			// Reservo del 2013-3-10 al 12
			Connection con = null;
			PreparedStatement st = null;
			CallableStatement cst = null;

			try {
				// Reinicio filas
				con = pool.getConnection();
				cst = con.prepareCall("{call inicializa_test}");
				cst.execute();

				String query = "";
				query += " insert into reservas values ";
				query += " (seq_reservas.NEXTVAL, '11111111B', '1234-ABC', date '2013-3-11'-1, date '2013-3-11'+1) ";

				st = con.prepareStatement(query);
				st.executeUpdate();

				con.commit();

				fechaIni = new java.sql.Date(formatoFechas.parse("11-03-2013").getTime());
				fechaFin = new java.sql.Date(formatoFechas.parse("13-03-2013").getTime());

				servicio.alquilar("12345678A", "1234-ABC", fechaIni, fechaFin);
				LOGGER.info("MAL Caso vehiculo ocupado solape de fechaIni no levanta excepcion");

			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.VEHICULO_OCUPADO) {
					LOGGER.info("OK Caso vehiculo ocupado solape de fechaIni correcto");
				} else {
					LOGGER.info("MAL Caso vehiculo ocupado solape de fechaIni levanta excepcion " + e.getMessage());
				}
			} catch (ParseException e1) {
				LOGGER.error("Error en el test al parsear la fechas desde cadena.");
			} finally {
				if (cst != null) {
					cst.close();
				}
				if (st != null) {
					st.close();
				}
				if (con != null) {
					con.close();
				}
			}
		}

		// --5.2 la fecha fin del alquiler esta dentro de una reserva
		{
			// Reservo del 2013-3-10 al 12
			Connection con = null;
			PreparedStatement st = null;
			CallableStatement cst = null;

			try {
				// Reinicio filas
				con = pool.getConnection();
				cst = con.prepareCall("{call inicializa_test}");
				cst.execute();

				String query = "";
				query += " insert into reservas values ";
				query += " (seq_reservas.NEXTVAL, '11111111B', '1234-ABC', date '2013-3-11'-1, date '2013-3-11'+1) ";

				st = con.prepareStatement(query);
				st.executeUpdate();

				con.commit();

				fechaIni = new java.sql.Date(formatoFechas.parse("07-03-2013").getTime());
				fechaFin = new java.sql.Date(formatoFechas.parse("11-03-2013").getTime());

				servicio.alquilar("12345678A", "1234-ABC", fechaIni, fechaFin);
				LOGGER.info("MAL Caso vehiculo ocupado solape de fechaFin no levanta excepcion");

			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.VEHICULO_OCUPADO) {
					LOGGER.info("OK Caso vehiculo ocupado solape de fechaFin correcto");
				} else {
					LOGGER.info("MAL Caso vehiculo ocupado solape de fechaFin levanta excepcion " + e.getMessage());
				}

			} catch (ParseException e1) {
				LOGGER.error("Error en el test al parsear la fechas desde cadena.");
			} finally {
				if (cst != null) {
					cst.close();
				}
				if (st != null) {
					st.close();
				}
				if (con != null) {
					con.close();
				}
			}
		}

		// --5.3 la el intervalo del alquiler esta dentro de una reserva
		{
			// Reservo del Reservo del 2013-3-9 al 13
			Connection con = null;
			PreparedStatement st = null;
			CallableStatement cst = null;

			try {
				// Reinicio filas
				con = pool.getConnection();
				cst = con.prepareCall("{call inicializa_test}");
				cst.execute();

				String query = "";
				query += " insert into reservas values ";
				query += " (seq_reservas.NEXTVAL, '11111111B', '1234-ABC', date '2013-3-11'-2, date '2013-3-11'+2) ";

				st = con.prepareStatement(query);
				st.executeUpdate();

				con.commit();

				java.sql.Date dateIni = new java.sql.Date(formatoFechas.parse("04-03-2013").getTime());
				java.sql.Date dateFin = new java.sql.Date(formatoFechas.parse("19-03-2013").getTime());

				servicio.alquilar("12345678A", "1234-ABC", dateIni, dateFin);
				LOGGER.info(
						"MAL Caso vehiculo ocupado intervalo del alquiler esta dentro de una reserva no levanta excepcion");

			} catch (SQLException e) {
				if (e.getErrorCode() == AlquilerCochesException.VEHICULO_OCUPADO) {
					LOGGER.info("OK Caso vehiculo ocupado intervalo del alquiler esta dentro de una reserva correcto");
				} else {
					LOGGER.info(
							"MAL Caso vehiculo ocupado intervalo del alquiler esta dentro de una reserva levanta excepcion levanta excepcion "
									+ e.getMessage());
				}
			} catch (ParseException e1) {
				LOGGER.error("Error en el test al parsear la fechas desde cadena.");
			} finally {
				if (cst != null) {
					cst.close();
				}
				if (st != null) {
					st.close();
				}
				if (con != null) {
					con.close();
				}
			}
		}

		// caso 6 Todo correcto pero SI especifico la fecha final
		{
			Connection con = null;
			PreparedStatement st = null;
			CallableStatement cst = null;
			ResultSet rs = null;

			try {
				// Reinicio filas
				con = pool.getConnection();
				cst = con.prepareCall("{call inicializa_test}");
				cst.execute();

				fechaIni = formatoFechas.parse("11-3-2013");
				fechaFin = formatoFechas.parse("13-3-2013");

				servicio.alquilar("12345678A", "2222-ABC", fechaIni, fechaFin);

				String query = "";
				query += " SELECT listAgg(nroFactura||matricula||TO_CHAR(fecha_ini, 'DD-MM-YYYY')||TO_CHAR(fecha_fin, 'DD-MM-YYYY')||facturas.importe||cliente||concepto||lineas_factura.importe, '#') ";
				query += " within group (order by nroFactura, concepto) ";
				query += " FROM facturas join lineas_factura using(NroFactura) ";
				query += " join reservas using(cliente) ";

				st = con.prepareStatement(query);

				rs = st.executeQuery();

				rs.next();
				String resultado = rs.getString(1);
				if (rs.wasNull()) {
					resultado = ""; // El join esta vacio
				}

				LOGGER.info("Caso alquiler correcto pero SI especifico la fecha final --------------");

				String resultadoPrevisto = "12222-ABC11-03-201313-03-201310212345678A2 dias de alquiler, vehiculo modelo 2   32#12222-ABC11-03-201313-03-201310212345678ADeposito lleno de 50 litros de Gasoil   70";

				if (resultado.equals(resultadoPrevisto)) {
					LOGGER.info("SI Coinciden la factura y las linea de factura OK");
				} else {
					LOGGER.info("NO Coinciden la factura y las linea de factura MAL");
					LOGGER.info("Se obtiene...*" + resultado + "*");
					LOGGER.info("Y deberia ser*" + resultadoPrevisto + "*");
				}

			} catch (SQLException e) {
				LOGGER.error(e.getMessage());
			} catch (ParseException e) {
				LOGGER.error("Error en el test al parsear la fechas desde cadena.");
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
				if (cst != null) {
					cst.close();
				}
				if (con != null) {
					con.close();
				}
			}
		}
	}
}
