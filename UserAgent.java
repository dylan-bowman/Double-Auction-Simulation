/*
 * Author: Dylan Bowman
 * 
 * User Agent implements Agent
 * The user agent that registers the user actions from the GUI and stores its current state.
 */

public class UserAgent implements Agent {

	private double	money;
	private int	shares;
	private int type;
	private int	pid;
	private int	tradesCompleted;
	private LimitOrderBook	lob;

	public UserAgent(double startingMoney, int startingShares,
							LimitOrderBook lob, int pid) {
		this.money = startingMoney;
		this.shares = startingShares;
		this.type = 1;
		this.pid = pid;
		this.tradesCompleted = 0;
		this.lob = lob;
	}

	// compare by agent ids
	public int compareTo(Agent that) {
		return this.getPID() - that.getPID();
	}

	// returns 1 for user type
	public int getType() {
		return type;
	}

	// get the user's current money level
	public double getMoney() {
		return money;
	}
	
	// get the user's current money level, formatted nicely
	public String getFormattedMoney() {
		return String.format("%6.2f", money);
	}

	// set the user's current money level
	public boolean setMoney(double newMoney) {
		if (newMoney < 0) return false;
		money = newMoney;
		return true;
	}

	// get the current number of the user's shares
	public int getShares() {
		return shares;
	}

	// set the current number of the user's shares
	public boolean setShares(int newShares) {
		if (newShares < 0) return false;
		shares = newShares;
		return true;
	}

	// get the number of trades completed by the user
	public int getTradesCompleted() {
		return tradesCompleted;
	}

	// get the id #
	public int getPID() {
		return pid;
	}

	// cannot submit an order automatically based on user tendencies like other agents
	public boolean submitOrder(int round) {
		return false;
	}

	// submit an order based on the user's inputs
	public boolean submitOrder(boolean isLimit, boolean isSell, int size,
			double price, int expiration) {
		boolean success;
		if (isLimit) {
			if (isSell) {
				if (price <= lob.getBestBid()) success = lob.submitMarketOrder(
						true, size, this);
				else success = lob.submitLimitSellOrder(size, price, expiration,
						this);
			} else {
				if (lob.getBestAsk() > 0 && price >= lob.getBestAsk()) success = lob.submitMarketOrder(
						false, size, this);
				else success = lob.submitLimitBuyOrder(size, price, expiration,
						this);
			}
		} else {
			if (isSell) success = lob.submitMarketOrder(true, size, this);
			else success = lob.submitMarketOrder(false, size, this);
		}

		if (success) tradesCompleted++;
		return success;
	}
	
	// print the user's final data
	public void printFinalData(double finalPrice) {
		System.out.printf("%3d, %2d, %8.2f, %5d, %8.2f", pid, type, money,
				shares, money + (shares * finalPrice));
		System.out.println();
	}

}
