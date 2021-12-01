//-----------------------------------------------------------------------------
package iii.imdas.reader;
//-----------------------------------------------------------------------------
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import iii.imdas.model.state.LsInterval;
//-----------------------------------------------------------------------------
abstract class Monitor {
  //---------------------------------------------------------------------------
  protected SimpleDateFormat sdf = null;
  protected String fromDate = null;
  //判斷ls file是以日或月做間隔, 預設為 DAY
  private LsInterval interval = LsInterval.DAY; 
  
//  protected boolean firstday = true;
  //---------------------------------------------------------------------------
//---------------------------------------------------------------------------
  protected Monitor(SimpleDateFormat sdf) {
    this.sdf = sdf;
  }
  
  protected Monitor(SimpleDateFormat sdf, LsInterval interval) {
    this.sdf = sdf;
    this.interval = interval;
  }
  //---------------------------------------------------------------------------
  public boolean isInit() {
    return fromDate != null;
  }
  //---------------------------------------------------------------------------
  public void setFromDate(String fromDate) {
//	firstday = fromDate.equals(this.fromDate);
    this.fromDate = fromDate;
  }
  //---------------------------------------------------------------------------
  public String getFromDate() {
    return fromDate;
  }
  //---------------------------------------------------------------------------
  public abstract Map<String, byte[]> next() throws Exception;
  //---------------------------------------------------------------------------
  protected String getDate(int ahead) throws ParseException {
    Calendar c = Calendar.getInstance();
    c.setTime(sdf.parse(fromDate));
    switch(interval) {
	    case MONTH:
	    	c.add(Calendar.MONTH, ahead);
	    	break;
	    case DAY: 
	    	c.add(Calendar.DATE, ahead);
	    	break;
	    case HOUR:
	    	c.add(Calendar.HOUR, ahead);
	    	break;
	    case QUARTER_HOUR:
	    	c.add(Calendar.MINUTE, (ahead * 15));
	    	break;
	    case MINUTE:
	    	c.add(Calendar.MINUTE, ahead);
	    	break;
	    default:
	    	throw new RuntimeException("scpmonitor unknow monitor interval type!!");	    
    }
    
    return sdf.format(c.getTime());
  } 
  //---------------------------------------------------------------------------
  public Date currentDate() throws ParseException {
	  Calendar c = Calendar.getInstance();
	  c.setTime(sdf.parse(fromDate));
	  return c.getTime();
  }
  //---------------------------------------------------------------------------
}
//-----------------------------------------------------------------------------
