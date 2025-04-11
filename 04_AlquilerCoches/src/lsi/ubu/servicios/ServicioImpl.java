package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AlquilerCochesException;
import lsi.ubu.util.PoolDeConexiones;
import lsi.ubu.util.exceptions.SGBDError;
import lsi.ubu.util.exceptions.oracle.OracleSGBDErrorUtil;

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	private static final int DIAS_DE_ALQUILER = 4;

	public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement st = null;
		PreparedStatement selectNIFClientes = null;
		PreparedStatement selectMatricula = null;
		PreparedStatement selectVehiculo = null;
		ResultSet rs = null;
		ResultSet rsMatricula = null;
		ResultSet rsVehiculo = null;

		/*
		 * El calculo de los dias se da hecho
		 */
		long diasDiff = DIAS_DE_ALQUILER;
		if (fechaFin != null) {
			diasDiff = TimeUnit.MILLISECONDS.toDays(fechaFin.getTime() - fechaIni.getTime());

			if (diasDiff < 1) {
				throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
			}
		}

		try {
			con = pool.getConnection();

			/* A completar por el alumnado... */

			/* ================================= AYUDA R�PIDA ===========================*/
			/*
			 * Algunas de las columnas utilizan tipo numeric en SQL, lo que se traduce en
			 * BigDecimal para Java.
			 * 
			 * Convertir un entero en BigDecimal: new BigDecimal(diasDiff)
			 * 
			 * Sumar 2 BigDecimals: usar metodo "add" de la clase BigDecimal
			 * 
			 * Multiplicar 2 BigDecimals: usar metodo "multiply" de la clase BigDecimal
			 *
			 * 
			 * Paso de util.Date a sql.Date java.sql.Date sqlFechaIni = new
			 * java.sql.Date(sqlFechaIni.getTime());
			 *
			 *
			 * Recuerda que hay casos donde la fecha fin es nula, por lo que se debe de
			 * calcular sumando los dias de alquiler (ver variable DIAS_DE_ALQUILER) a la
			 * fecha ini.
			 */
			
			 con=pool.getConnection();
			 
			 st= con.prepareStatement("INSERT INTO Reservas (idReserva, nifCliente, matricula, fechaIni, fechaFin) VALUES (seq_reservas.nextval, ?, ?, ?, ?)");
			 
			 st.setString(1,nifCliente);
			 st.setString(2, matricula);
			 
			 java.sql.Date FechaIni = new java.sql.Date(fechaIni.getTime());
			 st.setDate(3, FechaIni);
			 if (fechaFin != null) {
					java.sql.Date FechaFin = new java.sql.Date(fechaFin.getTime());
					st.setDate(4, FechaFin);
				}
			 
			 selectNIFClientes = con.prepareStatement(				
					 "SELECT NIF FROM clientes WHERE NIF= ?"			
					);	
			 selectNIFClientes.setString(1, nifCliente);
			 rs = selectNIFClientes.executeQuery();
			 if (!rs.next()) {
				 throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
			 }
			 selectMatricula = con.prepareStatement(				
					 "SELECT matricula FROM vehiculos WHERE matricula= ?"			
					);	
			 selectMatricula.setString(1, matricula);
			 rsMatricula = selectMatricula.executeQuery();
			 if (!rsMatricula.next()) {
				 throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
			 }
			 selectVehiculo= con.prepareStatement(
					 "SELECT matricula FROM reservas WHERE matricula= ?");
			 selectVehiculo.setString(1, matricula);
			 rsVehiculo = selectMatricula.executeQuery();
			 if (rsVehiculo.next()) {
				 throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);
			 }
			 
			 con.commit();

		} catch (SQLException e) {
			if(null != con) {
				con.rollback();
			}
			if (new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {				
                throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);			
            }		
			if (new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {				
                throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);			
            }	
			if (new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {				
                throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);			
            }
            if (e instanceof AlquilerCochesException) {				
                throw (AlquilerCochesException) e;			
            }

			LOGGER.debug(e.getMessage());

			throw e;

		} finally {
			/* A rellenar por el alumnado*/
			if(st != null) {
				st.close();
			}
			if(selectNIFClientes !=null) {
				selectNIFClientes.close();
			}
			if(selectMatricula !=null) {
				selectMatricula.close();
			}
			if(selectVehiculo !=null) {
				selectVehiculo.close();
			}
			if(con !=null) {
				con.close();
			}
		}
	}
}
