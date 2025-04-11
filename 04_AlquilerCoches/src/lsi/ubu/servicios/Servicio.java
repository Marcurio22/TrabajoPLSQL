package lsi.ubu.servicios;

import java.sql.SQLException;
import java.util.Date;

public interface Servicio {

	public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException;
}
