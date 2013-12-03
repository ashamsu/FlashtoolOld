package org.system;

import gui.About;

import java.net.*;
import java.util.Iterator;
import java.util.Scanner;
import java.io.*;

import org.eclipse.swt.widgets.Display;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.logger.MyLogger;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.PlatformUtil.Platform;


	
public class VersionChecker extends Thread {

		static org.eclipse.swt.widgets.Shell _s = null;
		private boolean aborted=false;
		private InputStream ustream=null;
		private HttpURLConnection uconn=null;

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
				System.out.println("resolving host");
				DNSResolver dnsRes = new DNSResolver("github.com");
                Thread t = new Thread(dnsRes);
                t.start();
                t.join(1000);
                System.out.println("finished resolving host");
                if  (dnsRes.get()!=null) {
				URL u = new URL("https://github.com/Androxyde/Flashtool/raw/master/deploy-release.xml");
				System.out.println("opening connection");
				uconn = (HttpURLConnection) u.openConnection();
			    uconn.setConnectTimeout(5 * 1000);
			    uconn.setRequestMethod("GET");
			    uconn.connect();
				System.out.println("Getting stream on connection");
				ustream = uconn.getInputStream();
				if (ustream!=null) System.out.println("stream opened");
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
                } else return "";
			}
			catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}
		
		public void run() {
			this.setName("Version Checker");
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
							Thread.sleep(2000);
						} catch (Exception e1) {}
					}
					else aborted=true;
				}
			}
			System.out.println("out of loop");
			final String latest = netrelease;
			if (latest.length()>0 && !About.build.contains(latest) && !About.build.contains("beta")) {
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

		public void done() {
			System.out.println("aborting job");
			aborted=true;
			if (uconn!=null)
			try {
				System.out.println("closing connection");
				uconn.disconnect();
			} catch (Exception e) {
				System.out.println("Error : "+e.getMessage());
			}
		}
}