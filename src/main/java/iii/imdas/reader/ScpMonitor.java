//-----------------------------------------------------------------------------
package iii.imdas.reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Set;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import iii.imdas.model.state.LsInterval;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//-----------------------------------------------------------------------------
public class ScpMonitor extends Monitor {
    
    private static final Logger logger = LoggerFactory.getLogger(ScpMonitor.class);

  //---------------------------------------------------------------------------
  private Set<String> filesRead = new TreeSet<String>();
  //---------------------------------------------------------------------------
  private String prefix;
  private String postfix;
  private Vector<AccessPoint> accessPoints = new Vector<AccessPoint>();
  private Integer readbuffer;
  private ByteArrayOutputStream baos = new ByteArrayOutputStream();
  private String[] lsPostfixs;
  private int[] daysToFind = new int[]{1,2,3,4,5,6,7};

  //---------------------------------------------------------------------------
  public ScpMonitor(String prefix, SimpleDateFormat sdf, String postfix,int readbuffer) {
    super(sdf, LsInterval.DAY);
    this.prefix = prefix;
    this.postfix = postfix;
    this.readbuffer = readbuffer;
  }
  
//---------------------------------------------------------------------------
  public ScpMonitor(String prefix, SimpleDateFormat sdf, String postfix,int readbuffer, LsInterval interval) {
    super(sdf, interval);
    this.prefix = prefix;
    this.postfix = postfix;
    this.readbuffer = readbuffer;
  }
  //---------------------------------------------------------------------------
  public void fillFilesRead(int ... previous) {
	    filesRead.clear();
	    filesRead.addAll(lsFiles(previous).keySet());
  }
  //---------------------------------------------------------------------------
  public void add(AccessPoint accessPoint) {
    accessPoints.add(accessPoint);
  }
  //---------------------------------------------------------------------------
  public Map<String, byte[]> next() throws ParseException{
	    Map<String, byte[]> _next = new TreeMap<String, byte[]>();
	
	    Map<String, AccessPoint> historyFiles = lsFiles(0, -1);
	    if (filesRead.containsAll(historyFiles.keySet())) {
	      if (lsFiles(daysToFind).isEmpty())
	        return _next;
	
	      setFromDate(getDate(1));
	      return next();
	    }
	
	    //Map<String, AccessPoint> readingFiles = new TreeMap<String, AccessPoint>(historyFiles);
	    //readingFiles.keySet().removeAll(filesRead);
	    //filesRead = new TreeSet<String>(historyFiles.keySet());
	    //filesRead.removeAll(readingFiles.keySet());

	    filesRead.retainAll(historyFiles.keySet());
	    historyFiles.keySet().removeAll(filesRead);
	    
	    int buffersize = 0;
	    
	    Map<AccessPoint, Session> AccessPoint_Session_map = new HashMap<AccessPoint, Session>();
	    Set<AccessPoint> failureAccessPoints = new HashSet<AccessPoint>();
	    for (String readingFile : /*readingFiles*/historyFiles.keySet()) {
	    	AccessPoint accessPoint = /*readingFiles*/historyFiles.get(readingFile);
	    	if (failureAccessPoints.contains(accessPoint))
	    		continue;
	    	
	    	if (!AccessPoint_Session_map.containsKey(accessPoint)) {
				try {
					AccessPoint_Session_map.put(accessPoint, getSession(accessPoint));
				} catch (Exception e1) {
					System.out.println("Connection failure: " + accessPoint.url);
					failureAccessPoints.add(accessPoint);
					continue;
				}
	    	}
	    	
	    	Session session = AccessPoint_Session_map.get(accessPoint);
	    	try {
	    		_next.put(readingFile, retrieve(session, readingFile));
	    		System.out.println("retrieving done: " + accessPoint.url + ", " + readingFile);
                buffersize += _next.get(readingFile).length;
                filesRead.add(readingFile);
                logger.info("{}:{}",readingFile,_next.get(readingFile).length);
                if(buffersize > readbuffer)
                    break;
                
	    	} catch (Exception e1) {
	    		session.disconnect();
	    		AccessPoint_Session_map.remove(accessPoint);
	    		logger.error("{}:{}",accessPoint.url,e1.getMessage());
	    	}
	    }
	    
	    for (Session s : AccessPoint_Session_map.values())
        	s.disconnect();
	
	    return _next;
  }
  //---------------------------------------------------------------------------
  public Map<String, AccessPoint> lsFiles(int... aheads){
    Map<String, AccessPoint> files = new TreeMap<String, AccessPoint>();
    for (AccessPoint accessPoint : accessPoints)
      lsFiles(files, accessPoint, aheads);

    return files;
  }
  //---------------------------------------------------------------------------
  @SuppressWarnings({ "unused", "deprecation" })
private byte[] retrieve(
    Session session,
    String readingFile
  ) throws Exception {
    baos.reset();
//    try{
      String command = "scp -f " + readingFile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0] = 0; out.write(buf, 0, 1); out.flush();

      while(true){
        int c = checkAck(in);
        if (c!='C') {
          break;
        }

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize=0L;
        while(true){
          if(in.read(buf, 0, 1)<0){
            // error
            break; 
          }
          if(buf[0]==' ')break;
          filesize=filesize*10L+(long)(buf[0]-'0');
        }

        String file = null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
          }
        }

        //System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
          else foo=(int)filesize;
          foo=in.read(buf, 0, foo);
          if(foo<0){
            // error 
            break;
          }
          baos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0L) break;
        }
        baos.close();
        //baos = null;

        if(checkAck(in)!=0){
          //System.out.println("System.exit(0);");
          //System.exit(0);
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
          channel.disconnect();
          throw new Exception("ScpMonitor.java: method retrieve(): section checkAck(in)!=0");
        }

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }

      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
      channel.disconnect();
      return baos.toByteArray();
      //System.exit(0);
//    }
//    catch(Exception e){
//      //System.out.println(e);
//      e.printStackTrace();
//      try{if(baos!=null)baos.close();}catch(Exception ee){}
//    }
//    return new byte[]{};
  }
  //---------------------------------------------------------------------------
  private Session getSession(AccessPoint accessPoint) throws Exception{
    JSch jsch = new JSch();
    Session session = jsch.getSession(accessPoint.user, accessPoint.url, 22);
    session.setPassword(accessPoint.password);
    java.util.Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);
    session.connect();
    return session;
  }
  //---------------------------------------------------------------------------
  @SuppressWarnings("unchecked")
private void lsFiles(
    Map<String, AccessPoint> files, 
    AccessPoint accessPoint, 
    int... aheads
  ){
    Session session = null;
    ChannelSftp channelSftp = null;
    try {
        session = getSession(accessPoint);
        channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        
        for (String aDir : accessPoint.dir) {
        	try {channelSftp.cd(aDir);}catch(SftpException e){logger.error("{}:{}:{}",accessPoint.getUrl(),aDir,e.getMessage());continue;}
            //System.out.println("lsFiles, accessPoint.dir: " + aDir);
        
            Vector<ChannelSftp.LsEntry> entries = new Vector<ChannelSftp.LsEntry>();
            for (int ahead : aheads) {
            	Vector<ChannelSftp.LsEntry> tmpLsEntries = channelSftp.ls(prefix + getDate(ahead) + postfix);
            	if (this.lsPostfixs == null) {
            		entries.addAll(tmpLsEntries);
            	} else {
            		for (LsEntry tmpEntry : tmpLsEntries) {
            			boolean catchFile = false;
            			for (String pattern : this.lsPostfixs) {
            				if (tmpEntry.getFilename().endsWith(pattern)) {
            					catchFile = true;
            					break;
            				}
            			}
            			if (catchFile) {
            				entries.add(tmpEntry);
            			}
            		}
            	}
              
            }
            
            //System.out.println("entries: " + entries);
        
            for (ChannelSftp.LsEntry entry : entries)
              files.put(aDir+"/"+entry.getFilename(), accessPoint);
        }

        channelSftp.disconnect();
        session.disconnect();
    } catch (Exception e) {
        e.printStackTrace();
        logger.error("{}:{}",accessPoint.getUrl(),e.fillInStackTrace());
        
        if (channelSftp != null)
        	channelSftp.disconnect();
        
        if (session != null)
        	session.disconnect();
    }

  }
  //---------------------------------------------------------------------------
  static int checkAck(InputStream in) throws IOException{
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
	c=in.read();
	sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
	System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
	System.out.print(sb.toString());
      }
    }
    return b;
  }
  //---------------------------------------------------------------------------
  public static void main(String[] args) {
    ScpMonitor scpMonitor = new ScpMonitor("*~*~*~*~",  new SimpleDateFormat("yyyy-MMdd"), "*.zip", 50000000);
    scpMonitor.setFromDate("2016-0901");
    scpMonitor.add(new AccessPoint("user", "password",  "192.168.200.100",  "/home/upload/folder1" ));
    scpMonitor.add(new AccessPoint("user", "password",  "192.168.200.101",  "/home/upload2/folder2" ));
    try {
  	  Map<String, byte[]> remoteFiles = scpMonitor.next();
      System.out.println(remoteFiles.size());
      for(String zip : remoteFiles.keySet()){
      	System.out.println(zip + ": " + remoteFiles.get(zip).length);
      }
      
      remoteFiles = scpMonitor.next();
      System.out.println(remoteFiles.size());
      for(String zip : remoteFiles.keySet()){
      	System.out.println(zip + ": " + remoteFiles.get(zip).length);
      }
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
    };
  }
  //---------------------------------------------------------------------------

	public String[] getLsPostfixs() {
		return lsPostfixs;
	}
	
	public void setLsPostfixs(String... lsPostfixs) {
		this.lsPostfixs = lsPostfixs;
	}

	public Set<String> getFilesRead() {
		return filesRead;
	}

	public void setFilesRead(Set<String> filesRead) {
		this.filesRead = filesRead;
	}
	
	public void setDaysToFind(int[] days) {
		this.daysToFind = days;
	}
  
  
}
//-----------------------------------------------------------------------------
