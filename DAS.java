/*
 * Author: Dylan Bowman
 * 
 * Interface for DAS
 * 
 * Allows for standards to be placed around the different types of Double Auction Simulations.
 */

public interface DAS {
	// get the type of the simulation
	int getType();

	// get the number of zero intel agents involved
	int getNumZeroIntel();

	// get the number of chartist agents involved
	int getNumChartists();
	
	// get the totel number of agents involved
	int getNumAgents();

	// get the previous lenght of price history
	Double[] getHistory(int length);

	// get the entire price history
	Double[] getHistory();

	// get the limit book
	LimitOrderBook getLOB();

	// get the total number of rounds
	int getNumRounds();

	// get the current round
	int getCurrentRound();

	// process the next bid
	boolean nextBid();

	// process a user bid, which acts as its own round in the simulation
	boolean submitUserOrder(boolean isLimit, boolean isSell, int size,
			double price, int expiration);
	
	// is the simulation done?
	boolean isDone();
	
	// get the current level of the user's money
	double getUserMoney();
	
	// get the current level of the user's money, formatted nicely
	String getFormattedUserMoney();
	
	// get the current number of the user's shares
	int getUserShares();
	
	// print data from the simulation
	void printData();
	
	// print final results of the simulation
	void printResults();

	// print the average spread with different options
	void printAverageSpread(int i);
	
	// print the number of user trades
	void printUserTrades();
}
