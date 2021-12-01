//-----------------------------------------------------------------------------
package iii.imdas.reader;
import java.util.List;
//-----------------------------------------------------------------------------
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

//-----------------------------------------------------------------------------
public class AccessPoint {

//---------------------------------------------------------------------------
  String user;
  String password;
  String url;
  String[] dir;
  private boolean isDone = false;
  
  public AccessPoint(){	  
  }
  
  //---------------------------------------------------------------------------
  public AccessPoint(String user, String password, String url, String... dir) {
    this.user = user;
    this.password = password;
    this.url = url;
    this.dir = dir;
  }
  //---------------------------------------------------------------------------
  public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String[] getDir() {
		return dir;
	}
	public void setDir(String...dir) {
		this.dir = dir;
	}
	@Override
	public String toString() {
		return "AccessPoint [user=" + user + ", password=" + password
				+ ", url=" + url + ", dir=" + dir + "]";
	}
	
	@SuppressWarnings("unchecked")
	public void setConfiguration(Map<String, Object> config) {
	  if (!isDone) {
		  this.user = MapUtils.getString(config, "user");
		  this.password = MapUtils.getString(config, "password");
		  this.url = MapUtils.getString(config, "ip");
		  List<String> dirList = (List<String>) MapUtils.getObject(config, "dir");
		  this.dir = dirList.toArray(new String[dirList.size()]);  			
		  isDone = true;
	  }	  
  }
	
}
//-----------------------------------------------------------------------------
