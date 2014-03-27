package gui.models;

public class CustIdItem {

	private String model;
	TableLine iddef;
	
	public CustIdItem(String pmodel, TableLine piddef) {
		model = pmodel;
		iddef = piddef;
	}
	
	public String getModel() {
		return model;
	}
	
	public TableLine getDef() {
		return iddef;
	}

}
