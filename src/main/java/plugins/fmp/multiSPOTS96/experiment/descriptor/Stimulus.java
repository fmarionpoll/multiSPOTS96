package plugins.fmp.multiSPOTS96.experiment.descriptor;

import java.awt.Color;

public class Stimulus {
	private String name ="..";
	private Color color = Color.GREEN;
	 
    public Stimulus(String name) {
        super();
        this.name = name;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
    
    public Color getColor() {
    	return color;
    }
    
    public void setColor(Color color) {
    	this.color = color;
    }
     
    public String toString() {
        return this.name;
    }
     

}
