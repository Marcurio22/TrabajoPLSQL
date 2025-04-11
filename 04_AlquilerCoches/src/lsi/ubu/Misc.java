package lsi.ubu;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Misc {
	public static final int MANY_DAYS = 100;//1000;Para hacer pruebas ponemos nºs pequeños
	public static final int MAX_BEGIN = 50; //100;
	public static final int DEFAULT_INVOICE_DAYS = 4;
	
	public static Date truncDate( Date d ) {
		
		if (d!=null) {
		    Calendar calendar = Calendar.getInstance();
		    calendar.setTime(d);
		    calendar.set(Calendar.HOUR_OF_DAY, 0);
		    calendar.set(Calendar.MINUTE, 0);
		    calendar.set(Calendar.SECOND, 0);
		    calendar.set(Calendar.MILLISECOND, 0);
		 
		    return calendar.getTime();
		} 	else
			return null;
	}
	
	public static Date getCurrentDate() {
	    Calendar calendar = Calendar.getInstance();
	    calendar.set(Calendar.HOUR_OF_DAY, 0);
	    calendar.set(Calendar.MINUTE, 0);
	    calendar.set(Calendar.SECOND, 0);
	    calendar.set(Calendar.MILLISECOND, 0);
	 
	    return calendar.getTime();
	}
	
	public static Date addDays( Date arg_fecha, int dias) {
		
		Date d=truncDate(arg_fecha);
	
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.DAY_OF_YEAR,dias);
	
		return new Date(cal.getTimeInMillis());
	}
	
	public static int howManyDaysBetween( Date fechaReciente, Date fechaAntigua) {
			Date d1=truncDate(fechaReciente);
			Date d2=truncDate(fechaAntigua);
		
		   long diff = d1.getTime() - d2.getTime();
		   return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}
}
