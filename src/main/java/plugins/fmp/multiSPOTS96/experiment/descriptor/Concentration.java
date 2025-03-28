package plugins.fmp.multiSPOTS96.experiment.descriptor;



public class Concentration {

	private String name = "..";
	 
    public Concentration(String name) {
        super();
        this.name = name;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
    
     
    public String toString() {
        return this.name;
    }
}
