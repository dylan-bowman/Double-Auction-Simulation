/*
 * Author: Dylan Bowman
 * 
 * ChartistAgent class - implements Agent
 * Chartist agent is a specific type of agent that plays the market according to the history
 * of prices.  It either attempts to follow the trend of the prices in its bidding tendencies
 * or attempts to act contrarian to the history of prices. Please see my thesis for a more in depth 
 * explanation: http://dataspace.princeton.edu/jspui/handle/88435/dsp01tq57nr19m
 */

import java.util.Random;

public class ChartistAgent implements Agent {

	private double				money;
	private int					shares;
	private int					type;
	private LimitOrderBook	lob;  // reference to the simulation lob
	private int					pid;
	private DAS					das;  // reference to the simulation
	private int					history;  // how far back the Chartist agent looks
	private int					tradesCompleted;
	private boolean			chase;  // true = chasing chartist strategy, false = contrarian

	private double				lp;  // probability of limit order (prob. of market order is 1 - lp)
	private double				sp;  // probability of sell order (prob. of buy order is 1 - sp)
	private double				interval;  // price interval (used to get price of new order)
	private int					exp;  // # of rounds til expiration
	private Random				random;
	
	private int fundPrice;

	public ChartistAgent(double startingMoney, int startingShares,
								LimitOrderBook lob, int pid, double limitOrderProb,
								double sellProb, double interval, int exp,
								DASimulation das, int history) {
		this.money = startingMoney;
		this.shares = startingShares;
		this.type = 2;
		this.lob = lob;
		this.pid = pid;
		this.das = das;
		this.history = history;
		this.tradesCompleted = 0;
		this.chase = true;  // chartist agent types hard coded for this project for each run

		this.lp = limitOrderProb;
		this.sp = sellProb;
		this.interval = interval;
		this.exp = exp;
		this.random = new Random();
		
		this.fundPrice = 50;
	}
	
	// compare the two agent IDs for sorting purposes, returns 0 if equal
	public int compareTo(Agent that) {
		return this.getPID() - that.getPID();
	}

	// reutrns 2 for chartist type
	public int getType() {
		return type;
	}

	// get current money level
	public double getMoney() {
		return money;
	}

	// set current money level
	public boolean setMoney(double newMoney) {
		if (newMoney < 0) return false;  // can't owe money in this simulation
		money = newMoney;
		return true;
	}

	// get number of current shares
	public int getShares() {
		return shares;
	}

	// set number of current shares
	public boolean setShares(int newShares) {
		if (newShares < 0) return false;
		shares = newShares;
		return true;
	}

	// get the number of trades completed by this agent
	public int getTradesCompleted() {
		return tradesCompleted;
	}

	// get the agents id #
	public int getPID() {
		return pid;
	}

	// submit an order to the market according to this chartist agent's tendencies
	public boolean submitOrder(int round) {
		boolean success;
		
		// set a default history, and then get real history if the round > this agent's history level
		Double[] hist = { 1.0, 0.0, 2.0 };
		if (round > history) hist = das.getHistory(history); 

		// if price is rising, buy or sell based on type
		if (isAscending(hist)) {
			if (chase) success = lob.submitMarketOrder(false, 1, this);
			else success = lob.submitMarketOrder(true, 1, this);
			// if price is falling, buy or sell based on type
		} else if (isDescending(hist)) {
			if (chase) success = lob.submitMarketOrder(true, 1, this);
			else success = lob.submitMarketOrder(false, 1, this);
			// revert to Guo agent logic
		} else {
			double r = random.nextDouble();
			double s = random.nextDouble();
			// limit
			if (r < lp) {
				// sell
				if (s < sp) {
					// get the price to submit the bid at
					double newprice = random.nextDouble() * interval;
					if (lob.getBestBid() < 0) newprice += fundPrice;
					else newprice += lob.getBestBid();
					
					if (lob.areExpirationsOn()) {
						success = lob.submitLimitSellOrder(1, newprice, round + exp,
								this);
					} else {
						success = lob.submitLimitSellOrder(1, newprice, round, this);
					}
					// buy
				} else {
					// get the price to submit a bid at
					double newprice = random.nextDouble() * interval;
					if (lob.getBestAsk() < 0) newprice += (fundPrice - interval);
					else newprice += (lob.getBestAsk() - interval);
					
					if (lob.areExpirationsOn()) {
						success = lob.submitLimitBuyOrder(1, newprice, round + exp,
								this);
					} else {
						success = lob.submitLimitBuyOrder(1, newprice, round, this);
					}
				}
				// market
			} else {
				if (s < sp) success = lob.submitMarketOrder(true, 1, this);
				else success = lob.submitMarketOrder(false, 1, this);
			}
		}
		if (success) tradesCompleted++;
		return success;
	}

	// is this history of prices ascending?
	private boolean isAscending(Double[] list) {
		for (int i = 0; i < list.length - 1; i++) {
			if (list[i] >= list[i + 1]) return false;
			if (list[i] < 0 || list[i + 1] < 0) return false;
		}
		return true;
	}

	// is this history of prices descending?
	private boolean isDescending(Double[] list) {
		for (int i = 0; i < list.length - 1; i++) {
			if (list[i] <= list[i + 1]) return false;
			if (list[i] < 0 || list[i + 1] < 0) return false;
		}
		return true;
	}

	// submit a specific order to the market
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
				if (lob.getBestAsk() > 0 && price >= lob.getBestAsk()) success = lob
						.submitMarketOrder(false, size, this);
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

	// print final data
	public void printFinalData(double finalPrice) {
		System.out.printf("%3d, %2d, %8.2f, %5d, %8.2f", pid, type, money,
				shares, money + (shares * finalPrice));
		System.out.println();
	}

}
