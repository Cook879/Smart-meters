import java.io.Serializable;

/**
 * Helper class, stores detail of a power company's tariff
 */
public class Tariff implements Serializable {

	private static final long serialVersionUID = -5851392807798356425L;
	
	private int dayCost, nightCost, discount, threshold;
	
	/**
	 * Constructor 
	 * 
	 * @param dayCost int
	 * @param nightCost int
	 * @param discount int
	 * @param threshold int
	 */
	public Tariff(int dayCost, int nightCost, int discount, int threshold) {
		this.dayCost = dayCost;
		this.nightCost = nightCost;
		this.discount = discount;
		this.threshold = threshold;
	}
	
	/**
	 * Returns the tariff details as a String
	 * 
	 * @Override
	 * @return String
	 */
	public String toString() {
		return "Day cost: " + dayCost + "p per unit; Night cost: " + nightCost + "p  per unit; Discount of "
				+ (discount*100) + "% after using " + threshold + " units.";  
	}
	
	/**
	 * Accessor for the dayCost variable
	 * 
	 * @return int
	 */
	public int getDayCost() {
		return dayCost;
	}
	
	/**
	 * Accessor for the nightCost variable
	 * 
	 * @return int
	 */
	public int getNightCost() {
		return nightCost;
	}
	
	/**
	 * Accessor for the discount variable
	 * 
	 * @return int
	 */
	public int getDiscount() {
		return discount;
	}
	
	/**
	 * Accessor for the threshold variable
	 * 
	 * @return int
	 */
	public int getThreshold() {
		return threshold;
	}
}