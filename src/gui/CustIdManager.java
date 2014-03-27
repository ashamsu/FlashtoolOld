package gui;

import gui.models.CustIdItem;
import gui.models.PropertiesFileContentProvider;
import gui.models.TableLine;
import gui.models.TableSorter;
import gui.models.VectorContentProvider;
import gui.models.VectorLabelProvider;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.system.DeviceEntry;
import org.system.Devices;
import org.system.PropertiesFile;
import org.system.TextFile;
import org.system.UpdateURL;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import gui.models.TableLine;

public class CustIdManager extends Dialog {

	protected Object result;
	protected Shell shlDeviceUpdateChecker;
	protected CTabFolder tabFolder;
	protected DeviceEntry _entry;
	protected Label lblInfo;
	protected HashMap models = new HashMap();

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public CustIdManager(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open(DeviceEntry entry) {
		_entry = entry;
		createContents();
		shlDeviceUpdateChecker.open();
		shlDeviceUpdateChecker.layout();
		Display display = getParent().getDisplay();
		while (!shlDeviceUpdateChecker.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shlDeviceUpdateChecker = new Shell(getParent(), getStyle());
		shlDeviceUpdateChecker.setSize(450, 336);
		shlDeviceUpdateChecker.setText("cdfID Manager");
		
		tabFolder = new CTabFolder(shlDeviceUpdateChecker, SWT.BORDER);
		tabFolder.setBounds(11, 43, 423, 223);
		tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));				
		
		Button btnNewButton = new Button(shlDeviceUpdateChecker, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shlDeviceUpdateChecker.dispose();
			}
		});
		btnNewButton.setBounds(359, 272, 75, 25);
		btnNewButton.setText("Close");
		
		lblInfo = new Label(shlDeviceUpdateChecker, SWT.NONE);
		lblInfo.setBounds(11, 244, 342, 15);
		
		Button btnAdd = new Button(shlDeviceUpdateChecker, SWT.NONE);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AddCustId add = new AddCustId(shlDeviceUpdateChecker,SWT.PRIMARY_MODAL | SWT.SHEET);
				CustIdItem item = (CustIdItem)add.open(_entry,models);
				if (item!=null) {
					new File(_entry.getDeviceDir()+File.separator+"updates"+File.separator+item.getModel()).mkdir();
					PropertiesFile pf = new PropertiesFile();
					pf.setProperty(item.getDef().getValueOf(0), item.getDef().getValueOf(1));
					pf.setFileName(_entry.getDeviceDir()+File.separator+"updates"+File.separator+item.getModel()+File.separator+"custlist.properties");
					models.put(item.getModel(), pf);
					addTab(item.getModel(), pf);
				}
			}
		});
		btnAdd.setBounds(10, 10, 75, 25);
		btnAdd.setText("Add Model");
		
		Button btnNewButton_1 = new Button(shlDeviceUpdateChecker, SWT.NONE);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Iterator i = models.keySet().iterator();
				while (i.hasNext()) {
					PropertiesFile pf = (PropertiesFile)models.get(i.next());
					pf.write("ISO8859-1");
				}
			}
		});
		btnNewButton_1.setBounds(279, 272, 75, 25);
		btnNewButton_1.setText("Apply");
		fillMap();
		parseMap();
	}

	public void fillMap() {
		File f = new File(_entry.getDeviceDir()+File.separator+"updates");
		File[] children = f.listFiles();
		int nbfolder = 0;
		for (int i=0;i<children.length;i++) {
			if (children[i].isDirectory()) {
				nbfolder++;
			}
		}
		if (nbfolder>0) {
			for (int i=0;i<children.length;i++) {
				if (children[i].isDirectory()) {
					addMap(children[i].getName());
				}
			}
		}
	}

	public void addMap(final String tabtitle) {
		PropertiesFile custlist = new PropertiesFile();
		String folder = tabtitle.length()>0?tabtitle+File.separator:"";
		custlist.open("", _entry.getDeviceDir()+File.separator+"updates"+File.separator+folder+"custlist.properties");
		models.put(tabtitle, custlist);
	}

	public void parseMap() {
		Iterator keys = models.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			PropertiesFile pf = (PropertiesFile)models.get(key);
			addTab(key, pf);
		}
	}
	
	public void addTab(final String tabtitle, final PropertiesFile pf) {
		final TableViewer tableViewer = new TableViewer(tabFolder,SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE);
		final CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
		Display.getDefault().asyncExec(
				new Runnable() {
					public void run() {
						tabItem.setText(tabtitle.length()>0?tabtitle:_entry.getId());
						
						tableViewer.setContentProvider(new PropertiesFileContentProvider());
						tableViewer.setLabelProvider(new VectorLabelProvider());
						// Create the popup menu
						  MenuManager menuMgr = new MenuManager();
						  Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
						  menuMgr.addMenuListener(new IMenuListener() {
						    @Override
						    public void menuAboutToShow(IMenuManager manager) {
						    	manager.add(new Action("Add") {
						            public void run() {
										AddCustId add = new AddCustId(shlDeviceUpdateChecker,SWT.PRIMARY_MODAL | SWT.SHEET);
										CustIdItem item = (CustIdItem)add.open(tabtitle,null);
										if (item != null) {
											pf.setProperty(item.getDef().getValueOf(0), item.getDef().getValueOf(1));
							            	tableViewer.refresh();
										}
						            }
						        });						    		
						    	if (!tableViewer.getSelection().isEmpty()) {
							    	manager.add(new Action("Edit") {
							            public void run() {
											AddCustId add = new AddCustId(shlDeviceUpdateChecker,SWT.PRIMARY_MODAL | SWT.SHEET);
											TableLine line = (TableLine)tableViewer.getTable().getSelection()[0].getData();
											pf.remove(line.getValueOf(0));
											CustIdItem item = (CustIdItem)add.open(tabtitle,line);
											if (item != null) {
												pf.setProperty(item.getDef().getValueOf(0), item.getDef().getValueOf(1));
								            	tableViewer.refresh();
											}
							            }
							        });
							    	manager.add(new Action("Delete") {
							            public void run() {
							            	pf.remove(((TableLine)tableViewer.getTable().getSelection()[0].getData()).getValueOf(0));
							            	tableViewer.refresh();
							            }
							        });
						    	}
						    }
						  });

						  menuMgr.setRemoveAllWhenShown(true);
						  tableViewer.getControl().setMenu(menu);
						Table tableDevice = tableViewer.getTable();
						TableColumn[] columns = new TableColumn[2];
						columns[0] = new TableColumn(tableDevice, SWT.NONE);
						columns[0].setText("Id");
						columns[1] = new TableColumn(tableDevice, SWT.NONE);
						columns[1].setText("Name");
						tableDevice.setHeaderVisible(true);
						tableDevice.setLinesVisible(true);
						TableSorter sort = new TableSorter(tableViewer);
						tableDevice.setSortColumn(tableDevice.getColumn(0));
						tableDevice.setSortDirection(SWT.UP);
						tableViewer.setInput(pf);
					}
				}
		);

		Display.getDefault().asyncExec(
				new Runnable() {
					public void run() {
						tableViewer.setInput(pf);
						for (int i = 0, n = tableViewer.getTable().getColumnCount(); i < n; i++) {
							tableViewer.getTable().getColumn(i).pack();
						}
						tableViewer.getTable().pack();
						tableViewer.refresh();
						tabItem.setControl(tableViewer.getTable());
					}
				}
		);
	}
}