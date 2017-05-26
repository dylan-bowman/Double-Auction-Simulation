/*
 * Author: Dylan Bowman
 * 
 * Interface for Agent
 * 
 * An agent can be any type of player in the double auction simulation, either automated
 * or user.
 */

public interface Agent extends Comparable<Agent> {
	// get the Agent's type... can be zero intel, etc.
	int getType();

	// get agent's money total
	double getMoney();

	// adjust agent's money total (to subtract, just enter a negative double)
	boolean setMoney(double newMoney);

	// get agent's shares total
	int getShares();

	// adjust agent's shares total (to subtract, just enter a negative int)
	boolean setShares(int newShares);

	// gets the number of trades completed by this agent
	int getTradesCompleted();

	// get agent's id
	int getPID();
	
	// submit an order into the system in the specified round of the simulation
	boolean submitOrder(int round);

	// submit an order into the simulation with specific details
	boolean submitOrder(boolean isLimit, boolean isSell, int size, double price,
			int expiration);
	
	// print final data for the simulation
	void printFinalData(double finalPrice);
}