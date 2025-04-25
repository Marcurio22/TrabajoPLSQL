package lsi.ubu.servicios;

import java.math.BigDecimal;
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
    
      PreparedStatement insertFactura = null;
      PreparedStatement insertLinea = null;
      PreparedStatement insertLinea2 = null;
      PreparedStatement selectModelo = null;
      PreparedStatement selectCombustible = null;
      ResultSet rsModelo = null;

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
      
      con.setAutoCommit(false);
      
      // Calcula fecha inicio y fecha fin
          java.sql.Date sqlFechaIni = new java.sql.Date(fechaIni.getTime());
          java.sql.Date sqlFechaFin;


          if (fechaFin != null) {
              sqlFechaFin = new java.sql.Date(fechaFin.getTime());
          } else {
        	  sqlFechaFin = null;
            //sqlFechaFin = new java.sql.Date(fechaIni.getTime() + TimeUnit.DAYS.toMillis(DIAS_DE_ALQUILER));
          }
          
      // Verifica existencia del cliente
          selectNIFClientes = con.prepareStatement("SELECT NIF FROM Clientes WHERE NIF = ?");
          selectNIFClientes.setString(1, nifCliente);
          rs = selectNIFClientes.executeQuery();
          if (!rs.next()) {
              throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
          }
      
          //Verificar existencia del vehículo
      selectMatricula = con.prepareStatement("SELECT id_modelo FROM vehiculos WHERE matricula = ?");  
      selectMatricula.setString(1, matricula);
      rsMatricula = selectMatricula.executeQuery();
      if (!rsMatricula.next()) {
        throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
      }
      
      int idModelo = rsMatricula.getInt("id_modelo");
      
      
          
      //Verifica que el vehículo está ocupado
      selectVehiculo= con.prepareStatement("SELECT 1 FROM reservas WHERE matricula = ? AND fecha_ini < ? AND fecha_fin >= ?");
      //selectVehiculo.setString(1, nifCliente);
      selectVehiculo.setString(1, matricula);
      selectVehiculo.setDate(2, sqlFechaFin);
      selectVehiculo.setDate(3, sqlFechaIni);
      rsVehiculo = selectVehiculo.executeQuery();
      if (rsVehiculo.next()) {
        throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);
      }
      
      // Inserta la reserva
          st = con.prepareStatement(
              "INSERT INTO reservas (idReserva, cliente, matricula, fecha_ini, fecha_fin) VALUES (seq_reservas.nextval, ?, ?, ?, ?)");
          st.setString(1, nifCliente);
          st.setString(2, matricula);
          st.setDate(3, sqlFechaIni);
          st.setDate(4, sqlFechaFin);
          int Filas = st.executeUpdate();
          if (Filas == 0) {        
            String mensaje = "La fila no se ha insertado correctamente.";
            LOGGER.debug(mensaje);      
      }
          
          // Obtener datos del modelo
          selectModelo = con.prepareStatement(
              "SELECT nombre, precio_cada_dia, capacidad_deposito, tipo_combustible FROM Modelos WHERE id_modelo = ?");
          selectModelo.setInt(1, idModelo);
          rsModelo = selectModelo.executeQuery();
          rsModelo.next();

          String nombreModelo = String.valueOf(idModelo);
          BigDecimal precioDia = rsModelo.getBigDecimal("precio_cada_dia");
          BigDecimal capacidadDeposito = rsModelo.getBigDecimal("capacidad_deposito");
          String tipoCombustible = rsModelo.getString("tipo_combustible");
          
          // Obtener precio por litro
          selectCombustible = con.prepareStatement(
              "SELECT precio_por_litro FROM precio_combustible WHERE tipo_combustible = ?");
          selectCombustible.setString(1, tipoCombustible);
          ResultSet rsCombustible = selectCombustible.executeQuery();
          rsCombustible.next();
          BigDecimal precioLitro = rsCombustible.getBigDecimal("precio_por_litro");


          BigDecimal importeAlquiler = precioDia.multiply(BigDecimal.valueOf(diasDiff));
          BigDecimal importeDeposito = precioLitro.multiply(capacidadDeposito);
          BigDecimal total = importeAlquiler.add(importeDeposito);
          
          // Insert factura
          insertFactura = con.prepareStatement(
              "INSERT INTO facturas (nroFactura, importe, cliente) VALUES (seq_num_fact.nextval, ?, ?)",
              new String[] { "nroFactura" });
          insertFactura.setBigDecimal(1, total);
          insertFactura.setString(2, nifCliente);
          insertFactura.executeUpdate();
          
          // Obtener nroFactura generado
          ResultSet rsClave = insertFactura.getGeneratedKeys();
          rsClave.next();
          int nroFactura = rsClave.getInt(1);
          
          String conceptoAlquiler = diasDiff + " dias de alquiler, vehiculo modelo " + nombreModelo;
          if (conceptoAlquiler.length() > 40) {
        	    conceptoAlquiler = conceptoAlquiler.substring(0, 40);
        	}
          String conceptoDeposito = "Deposito lleno de " + capacidadDeposito.intValue() + " litros de " + tipoCombustible;
          if (conceptoDeposito.length() > 40) {
        	    conceptoDeposito = conceptoDeposito.substring(0, 40);
        	}
          // Insert líneas de factura
          insertLinea = con.prepareStatement(
              "INSERT INTO lineas_factura (nroFactura, concepto, importe) VALUES (?, ?, ?)");
          insertLinea.setInt(1, nroFactura);
          insertLinea.setString(2, conceptoAlquiler);
          insertLinea.setBigDecimal(3, importeAlquiler);
          insertLinea.executeUpdate();
          
          
          insertLinea2 = con.prepareStatement(
              "INSERT INTO lineas_factura (nroFactura, concepto, importe) VALUES (?, ?, ?)");
          insertLinea2.setInt(1, nroFactura);
          insertLinea2.setString(2, conceptoDeposito);
          insertLinea2.setBigDecimal(3, importeDeposito);
          insertLinea2.executeUpdate();
      
      con.commit();

    } catch (SQLException e) {
      if(null != con) {
        con.rollback();
      }
      if (e instanceof AlquilerCochesException) {        
                throw (AlquilerCochesException) e;      
            }    
      

      if (new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {  
        
                throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);      
            }
      
      if (new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {      
        
                throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);      
            }
            
      LOGGER.debug(e.getMessage());

      throw e;

    } finally {
      /* A rellenar por el alumnado*/
      if (rs != null) rs.close();
          if (rsMatricula != null) rsMatricula.close();
          if (rsVehiculo != null) rsVehiculo.close();
          if (rsModelo != null) rsModelo.close();
          if (selectCombustible != null) selectCombustible.close();
          if (rs != null) rs.close();
          if (st != null) st.close();
          if (selectNIFClientes != null) selectNIFClientes.close();
          if (selectMatricula != null) selectMatricula.close();
          if (selectVehiculo != null) selectVehiculo.close();
          if (insertFactura != null) insertFactura.close();
          if (insertLinea != null) insertLinea.close();
          if (insertLinea2 != null) insertLinea2.close();
          if (selectModelo != null) selectModelo.close();
          if (con != null) con.close();
    }
  }
}
