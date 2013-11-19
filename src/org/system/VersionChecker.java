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

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;
import com.btr.proxy.util.PlatformUtil;
import com.btr.proxy.util.PlatformUtil.Platform;


	
public class VersionChecker extends Thread {

		static org.eclipse.swt.widgets.Shell _s = null;
		private boolean aborted=false;
		private InputStream ustream=null;

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
				URL u = new URL("https://github.com/Androxyde/Flashtool/raw/master/deploy-release.xml");
				ustream = u.openStream();
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
			catch (Exception e) {
				return "";
			}
		}
		
		public void run() {
			this.setName("Version Checker");
			String netrelease = "";
			int nbretry = 0;
			while (netrelease.length()==0 && !aborted) {
				netrelease = getLatestRelease();				
				if (netrelease.length()==0) {
					nbretry++;
					if (nbretry<10) {
						try {
							Thread.sleep(2000);
						} catch (Exception e1) {}
					} else aborted=true;
				}
			}
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
			aborted=true;
			if (ustream!=null)
				try {
					ustream.close();
				} catch (Exception e) {}
		}
}