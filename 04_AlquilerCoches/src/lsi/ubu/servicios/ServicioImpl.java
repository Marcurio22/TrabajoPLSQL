package lsi.ubu.servicios;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

  @Override
  public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException {
    PoolDeConexiones pool = PoolDeConexiones.getInstance();

    Connection con = null;
    PreparedStatement st = null;
    PreparedStatement selectNIFClientes = null;
    PreparedStatement selectVehiculoModelo = null;
    PreparedStatement selectVehiculoOcupado = null;
    ResultSet rsCliente = null;
    ResultSet rsModelo = null;
    ResultSet rsOcupado = null;

    PreparedStatement insertFactura = null;
    PreparedStatement insertLineaAlquiler = null;
    PreparedStatement insertLineaDeposito = null;
    PreparedStatement selectCombustible = null;
    ResultSet rsCombustible = null;

    long diasDiff = DIAS_DE_ALQUILER;
    if (fechaFin != null) {
      diasDiff = TimeUnit.MILLISECONDS.toDays(fechaFin.getTime() - fechaIni.getTime());
      if (diasDiff < 1) {
        throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
      }
    }

    try {
      con = pool.getConnection();
      con.setAutoCommit(false);

      // Fechas SQL
      java.sql.Date sqlFechaIni = new java.sql.Date(fechaIni.getTime());
      java.sql.Date sqlFechaFin = (fechaFin != null)
          ? new java.sql.Date(fechaFin.getTime())
          : new java.sql.Date(fechaIni.getTime() + TimeUnit.DAYS.toMillis(DIAS_DE_ALQUILER));

      // 1. Verificar existencia del cliente
      selectNIFClientes = con.prepareStatement("SELECT NIF FROM Clientes WHERE NIF = ?");
      selectNIFClientes.setString(1, nifCliente);
      rsCliente = selectNIFClientes.executeQuery();
      if (!rsCliente.next()) {
        throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
      }

      // 2. Verificar existencia del vehículo y obtener id_modelo
      selectVehiculoModelo = con.prepareStatement("SELECT id_modelo FROM Vehiculos WHERE matricula = ?");
      selectVehiculoModelo.setString(1, matricula);
      rsModelo = selectVehiculoModelo.executeQuery();
      if (!rsModelo.next()) {
        throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
      }
      int idModelo = rsModelo.getInt(1);

      // 3. Comprobar solapamiento de reservas
      selectVehiculoOcupado = con.prepareStatement("SELECT idReserva FROM Reservas WHERE matricula = ? AND fecha_ini < ? AND fecha_fin >= ?");
      selectVehiculoOcupado.setString(1, matricula);
      selectVehiculoOcupado.setDate(2, sqlFechaFin);
      selectVehiculoOcupado.setDate(3, sqlFechaIni);
      rsOcupado = selectVehiculoOcupado.executeQuery();
      if (rsOcupado.next()) {
        throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);
      }

      // 4. Insertar reserva
      st = con.prepareStatement("INSERT INTO Reservas (idReserva, cliente, matricula, fecha_ini, fecha_fin) VALUES (seq_reservas.nextval, ?, ?, ?, ?)");
      st.setString(1, nifCliente);
      st.setString(2, matricula);
      st.setDate(3, sqlFechaIni);
      st.setDate(4, sqlFechaFin);
      st.executeUpdate();


      // 5. Obtener datos del modelo
      PreparedStatement selectDatosModelo = con.prepareStatement("SELECT nombre, precio_cada_dia, capacidad_deposito, tipo_combustible FROM Modelos WHERE id_modelo = ?");
      selectDatosModelo.setInt(1, idModelo);
      ResultSet rsDatos = selectDatosModelo.executeQuery();
      rsDatos.next();

      String nombreModelo = rsDatos.getString("nombre");
      BigDecimal precioDia = rsDatos.getBigDecimal("precio_cada_dia");
      BigDecimal capacidadDeposito = rsDatos.getBigDecimal("capacidad_deposito");
      String tipoCombustible = rsDatos.getString("tipo_combustible");
      rsDatos.close();
      selectDatosModelo.close();

      // 6. Obtener precio por litro
      selectCombustible = con.prepareStatement("SELECT precio_por_litro FROM Precio_Combustible WHERE tipo_combustible = ?");
      selectCombustible.setString(1, tipoCombustible);
      rsCombustible = selectCombustible.executeQuery();
      rsCombustible.next();
      BigDecimal precioLitro = rsCombustible.getBigDecimal(1);

      // 7. Calcular importes
      BigDecimal bdDias = BigDecimal.valueOf(diasDiff);
      BigDecimal importeAlquiler = precioDia.multiply(bdDias);
      BigDecimal importeDeposito = precioLitro.multiply(capacidadDeposito);
      BigDecimal totalFactura = importeAlquiler.add(importeDeposito);

      // 8. Insertar factura
      insertFactura = con.prepareStatement(
          "INSERT INTO Facturas (nroFactura, importe, cliente) VALUES (seq_facturas.nextval, ?, ?)",
        new String[] { "nroFactura" });
      insertFactura.setBigDecimal(1, totalFactura);
      insertFactura.setString(2, nifCliente);
      insertFactura.executeUpdate();

      // 9. Recuperar nº de factura generado
      ResultSet rsClave = insertFactura.getGeneratedKeys();
      rsClave.next();
      int nroFactura = rsClave.getInt(1);
      rsClave.close();

      // 10. Insertar líneas de factura
      insertLineaAlquiler = con.prepareStatement(
          "INSERT INTO Lineas_Factura (nroFactura, concepto, importe) VALUES (?, ?, ?)");
      insertLineaAlquiler.setInt(1, nroFactura);
      insertLineaAlquiler.setString(2,
          diasDiff + " dias de alquiler, vehiculo modelo " + nombreModelo);
      insertLineaAlquiler.setBigDecimal(3, importeAlquiler);
      insertLineaAlquiler.executeUpdate();

      insertLineaDeposito = con.prepareStatement("INSERT INTO Lineas_Factura (nroFactura, concepto, importe) VALUES (?, ?, ?)");
      insertLineaDeposito.setInt(1, nroFactura);
      insertLineaDeposito.setString(2,"Deposito lleno de " + capacidadDeposito.intValue()+ " litros de " + tipoCombustible);
      insertLineaDeposito.setBigDecimal(3, importeDeposito);
      insertLineaDeposito.executeUpdate();

      // 11. Confirmar transacción
      con.commit();

    } catch (SQLException e) {
      if (con != null) {
        con.rollback();
      }
      // Si es nuestra excepción, se propaga con su código y mensaje
      if (e instanceof AlquilerCochesException) {
        throw (AlquilerCochesException) e;
      }
      // Cualquier otro error se registra y se relanza
      LOGGER.debug(e.getMessage());
      throw e;
    } finally {
      // Liberar recursos
      if (rsCliente != null) {
    	  rsCliente.close();
      }
      if (rsModelo != null) {
    	  rsModelo.close();
      }
      if (rsOcupado != null) {
    	  rsOcupado.close();
      }
      if (selectNIFClientes != null) {
    	  selectNIFClientes.close();
      }
      if (selectVehiculoModelo != null) {
    	  selectVehiculoModelo.close();
      }
      if (selectVehiculoOcupado != null) {
    	  selectVehiculoOcupado.close();
      }
      if (st != null){
    	  st.close();
      }
      if (insertFactura != null) {
    	  insertFactura.close();
      }
      if (insertLineaAlquiler != null) {
    	  insertLineaAlquiler.close();
      }
      if (insertLineaDeposito != null) {
    	  insertLineaDeposito.close();
      }
      if (selectCombustible != null) {
    	  selectCombustible.close();
      }
      if (con != null) {
    	  con.close();
      }
    }
  }
}
