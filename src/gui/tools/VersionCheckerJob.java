package gui.tools;

import gui.About;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import org.adb.AdbUtility;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.logger.MyLogger;
import org.system.DNSResolver;
import org.system.Devices;
import org.system.FTShell;
import org.system.GlobalConfig;
import org.system.OS;
import org.system.TextFile;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.PlatformUtil.Platform;

public class VersionCheckerJob extends Job {

	static org.eclipse.swt.widgets.Shell _s = null;
	private boolean aborted=false;
	private boolean ended = false;
	private InputStream ustream=null;
	private HttpURLConnection uconn=null;

	public VersionCheckerJob(String name) {
		super(name);
	}
	
	public void setMessageFrame(org.eclipse.swt.widgets.Shell s) {
		_s = s;
	}

	public String getLatestRelease() {
		try {
			ProxySearch proxySearch = new ProxySearch();
            
			if (PlatformUtil.getCurrentPlattform() == Platform.WIN) {
			  proxySearch.addStrategy(Strategy.IE);
			  proxySearch.addStrategy(Strategy.FIREFOX);
			  proxySearch.addStrategy(Strategy.JAVA);
			} else 
			if (PlatformUtil.getCurrentPlattform() == Platform.LINUX) {
			  proxySearch.addStrategy(Strategy.GNOME);
			  proxySearch.addStrategy(Strategy.KDE);
			  proxySearch.addStrategy(Strategy.FIREFOX);
			} else {
			  proxySearch.addStrategy(Strategy.OS_DEFAULT);
			}

			ProxySelector myProxySelector = proxySearch.getProxySelector();
			ProxySelector.setDefault(myProxySelector);

			SAXBuilder builder = new SAXBuilder();
			Document doc = null;
			MyLogger.getLogger().debug("Resolving github");
			DNSResolver tr = new DNSResolver("github.com");
			tr.start();
			tr.join(2000);
			tr.interrupt();
            if  (tr.get()!=null) {
            	MyLogger.getLogger().debug("Finished resolving github. Result : Success");
            	URL u;
            	if (About.build.contains("beta"))
            		u = new URL("https://github.com/Androxyde/Flashtool/raw/master/deploy-beta.xml");
            	else
            		u = new URL("https://github.com/Androxyde/Flashtool/raw/master/deploy-release.xml");
            	MyLogger.getLogger().debug("opening connection");
				if (!aborted)
					uconn = (HttpURLConnection) u.openConnection();
				if (!aborted)
					uconn.setConnectTimeout(5 * 1000);
				if (!aborted)
					uconn.setRequestMethod("GET");
				if (!aborted)
					uconn.connect();
			    
				MyLogger.getLogger().debug("Getting stream on connection");
				if (!aborted)
					ustream = uconn.getInputStream();
				if (ustream!=null) MyLogger.getLogger().debug("stream opened");
				doc = builder.build(ustream);
				Iterator<Element> mainitr = doc.getRootElement().getChildren().iterator();
				while (mainitr.hasNext()) {
					Element e = mainitr.next();
					if (e.getName().equals("property"))
						if (e.getAttributeValue("name").equals("version")) {
							ustream.close();
							return e.getAttributeValue("value");
						}
				}
				ustream.close();
				return "";
            }
            else {
            	MyLogger.getLogger().debug("Finished resolving github. Result : Failed");
            	return "";
            }
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	protected IStatus run(IProgressMonitor monitor) {
		String netrelease = "";
		int nbretry = 0;
		while (netrelease.length()==0 && !aborted) {
			MyLogger.getLogger().debug("Fetching latest release from github");
			netrelease = getLatestRelease();
			if (netrelease.length()==0) {
				if (!aborted)
					MyLogger.getLogger().debug("Url content not fetched. Retrying "+nbretry+" of 10");
				nbretry++;
				if (nbretry<10) {
					try {
						Thread.sleep(1000);
					} catch (Exception e1) {}
				}
				else
					aborted=true;
			}
		}
		MyLogger.getLogger().debug("out of loop");
		final String latest = netrelease;
		MyLogger.getLogger().debug("Latest : " + latest);
		MyLogger.getLogger().debug("Current build : "+About.build);
		ended = true;
		if (About.build!=null) {
			if (latest.length()>0 && !About.build.contains(latest)) {
				if (_s!=null) {
					Display.getDefault().syncExec(
							new Runnable() {
								public void run() {
		    		   				_s.setText(_s.getText()+"    --- New version "+latest+" available ---");
		    		   			}
		    		   		}
					);
		    	}
		    }
		}
		return Status.OK_STATUS;
   }

	public void done() {
		if (!ended) {
			ended = true;
			MyLogger.getLogger().debug("aborting job");
			aborted=true;
			if (uconn!=null)
			try {
				MyLogger.getLogger().debug("closing connection");
				uconn.disconnect();
			} catch (Exception e) {
				MyLogger.getLogger().debug("Error : "+e.getMessage());
			}
		}
	}

}