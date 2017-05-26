/*
 * Author: Dylan Bowman
 * 
 * CIAgent class - implements Agent
 * CI agent is a specific type of agent that plays the market according to the history
 * of prices + a specific algorithm based off the Chiarella and Iori agent-based model.
 * The algorithm uses rbar, rhat, and phat variables below.  Please see my thesis for a
 * more in depth explanation: http://dataspace.princeton.edu/jspui/handle/88435/dsp01tq57nr19m
 */

import java.util.Random;

public class CIAgent implements Agent {

	private double				money;
	private int					shares;
	private int					type;
	private LimitOrderBook	lob;  // reference to the simulation's lob
	private int					pid;
	private DAS					das;  // reference to the simulation
	private int					tradesCompleted;

	private double				fundval; // beginning fundamental price of the commodity
	private int					lifetime; // rounds until orders placed by this agent expire
	private double				fund; // weight placed on fundamentalist component
	private double				chart; // weight placed on chartist component
	private double				n; // weight placed on noise
	private int					li; // rounds of history to observe
	private double				ki; // percentage willing to bid of expected future price
	private double				tickSize;

	private Random				random;

	public CIAgent(double startingMoney, int startingShares, LimitOrderBook lob,
						int pid, DAS das, double val, int tau, double g1, double g2,
						double n, int Li, double ki, double delta) {
		this.money = startingMoney;
		this.shares = startingShares;
		this.type = 3;
		this.lob = lob;
		this.pid = pid;
		this.das = das;
		this.tradesCompleted = 0;

		this.fundval = val;
		this.lifetime = tau;
		this.fund = g1;
		this.chart = g2;
		this.n = n;
		this.li = Li;
		this.ki = ki;
		this.tickSize = delta;

		this.random = new Random();
	}
	
	// compare the two agent IDs for sorting purposes, returns 0 if equal
	public int compareTo(Agent that) {
		return this.getPID() - that.getPID();
	}

	// return 3 for CIAgent type
	public int getType() {
		return type;
	}

	// get current money level
	public double getMoney() {
		return money;
	}

	// set current money level
	public boolean setMoney(double newMoney) {
		if (newMoney < 0) return false;
		money = newMoney;
		return true;
	}

	// get current number of shares
	public int getShares() {
		return shares;
	}

	// set current number of shares
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

	// submit order based on this agent's tendencies
	public boolean submitOrder(int round) {
		// get the current fundamental value, p, of the commodity
		Double[] history = das.getHistory();
		double p;
		if (round == 0) p = fundval;
		else p = history[round - 1];

		// calculate spot return averaged over interval li, rbar
		double rbar = 0.0;
		if (round > history.length) {
			for (int i = 1; i <= li; i++) {
				rbar += (history[round - i] - history[round - i - 1])
						/ history[round - i - 1];
			}
			rbar = rbar / li;
		} else rbar = 0.0;

		// calculate rhat
		double e = random.nextGaussian() * 0.1;
		// double realfund = (round > 200) ? fund : 0.0;
		double realfund = fund;
		double rhat = (realfund * ((fundval - p) / p)) + (chart * rbar) + (n * e);

		// calculate future price expected, phat
		double realtau = (double) lifetime / 100.0;
		double phat = p * Math.exp(rhat * realtau);

		double price = 0.0;
		boolean success = true;
		// price is expected to increase
		if (phat >= p) {
			// calculate bid price
			price = phat * (1 - ki);
			price = price - (price % tickSize)
					+ ((price % tickSize < (tickSize / 2)) ? 0.0 : tickSize);
			// submit buy order
			success = lob.submitLimitBuyOrder(1, price, round + lifetime, this);
		} else {
			// calculate bid price
			price = phat * (1 + ki);
			price = price - (price % tickSize)
					+ ((price % tickSize < (tickSize / 2)) ? 0.0 : tickSize);
			// submit sell order
			success = lob.submitLimitSellOrder(1, price, round + lifetime, this);
		}
		return success;
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

	// print the final data for this agent
	public void printFinalData(double finalPrice) {
		System.out.printf("%3d, %2d, %8.2f, %5d, %8.2f", pid, type, money,
				shares, money + (shares * finalPrice));
		System.out.println();
	}

}
