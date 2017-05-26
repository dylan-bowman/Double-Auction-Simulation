/*
 * Author: Dylan Bowman
 * 
 * Double Auction Simulation - implements DAS
 * DASimulation implements a SFGK model of the double auction simulation.  This will be used
 * with Zero-intelligence and Chartist model agents.
 * Please see my thesis for a more in depth explanation: 
 * http://dataspace.princeton.edu/jspui/handle/88435/dsp01tq57nr19m
 */
import java.util.*;

public class DASimulation implements DAS {
	private int						rounds;
	private int						currentRound;
	private boolean				expirationOn;
	private double					interestRate;
	private double					dividend;
	private int						numZeroIntel;
	private int						numChartists;
	private int						interestPeriod;
	private int						type;

	private AutomaticTraders	at;
	private LimitOrderBook		lob;
	private Agent					ua;
	private ArrayList<Double>	priceHistory;

	private double					averageSpread;
	private double					averageBids;
	private double					averageAsks;

	private int						history; // history to look back on
	private double					lp; // probability of limit order (prob. of market order is 1 - lp)
	private double					sp; // probability of sell order (prob. of buy order is 1 - sp)
	private double					interval; // price interval (used to get price of new order)
	private int						exp; // # of rounds til expiration

	public DASimulation(int rounds, int zeroIntel, int chartists, boolean exp,
								int history, double lp, double sp, double interval,
								int ex) {
		this.averageSpread = 0;
		this.averageBids = 0;
		this.averageAsks = 0;
		this.history = history;
		this.lp = lp;
		this.sp = sp;
		this.interval = interval;
		this.exp = ex;

		this.rounds = rounds;
		this.expirationOn = exp;
		this.type = (chartists > 0) ? 1 : 0;
		this.interestRate = 1.03; // hard coded interest rate for this project
		this.dividend = 1.035; // hard coded dividend rate for this project
		this.numZeroIntel = zeroIntel;
		this.numChartists = chartists;
		this.interestPeriod = 1000; // hard coded interest period

		this.currentRound = 0;
		this.at = new AutomaticTraders(zeroIntel + chartists);
		this.lob = new LimitOrderBook(expirationOn);
		this.priceHistory = new ArrayList<Double>();

		double startingMoney = 1000.00;
		int startingShares = 20;

		this.ua = new UserAgent(startingMoney, startingShares, lob, 0);

		// initialize automated agents
		if (type == 0) {
			for (int i = 1; i <= zeroIntel; i++) {
				at.addAgent(new PracticeAgent(startingMoney, startingShares, lob,
						i, lp, sp, interval, ex));
			}
		} else if (type == 1) {
			for (int i = 1; i <= zeroIntel; i++) {
				at.addAgent(new PracticeAgent(startingMoney, startingShares, lob,
						i, lp, sp, interval, ex));
			}
			for (int i = 1; i <= chartists; i++) {
				at.addAgent(new ChartistAgent(startingMoney, startingShares, lob,
						i, lp, sp, interval, ex, this, history));
			}
		}
	}

	// used for bare min
	public DASimulation(int N, int rounds, boolean exp) {
		this(rounds, N, 0, exp, 0, 0.9, 0.5, 3.49, 50);
	}

	// used for type 0 simulations
	public DASimulation(int rounds, int N, boolean exp, double lp, double sp,
								double interval, int ex) {
		this(rounds, N, 0, exp, 0, lp, sp, interval, ex);
	}

	// get the type of the simulation
	public int getType() {
		return type;
	}

	// get the number of zero intelligence traders
	public int getNumZeroIntel() {
		return numZeroIntel;
	}

	// get the number of chartist traders
	public int getNumChartists() {
		return numChartists;
	}

	// get the total number of agents
	public int getNumAgents() {
		return numZeroIntel + numChartists;
	}

	// get the price history of the last rounds
	public Double[] getHistory(int rounds) {

		Double[] hist;
		hist = priceHistory.subList(currentRound - rounds, currentRound).toArray(
				new Double[rounds]);
		return hist;
	}

	// get the whole history
	public Double[] getHistory() {
		return priceHistory.toArray(new Double[priceHistory.size()]);
	}

	// get the limit order book
	public LimitOrderBook getLOB() {
		return lob;
	}

	// get the total number of rounds
	public int getNumRounds() {
		return rounds;
	}

	// get the current round
	public int getCurrentRound() {
		return currentRound;
	}

	// prompt a random agent to make a bid
	public boolean nextBid() {
		if (isDone()) return false;
		
		// process interest and dividend payments if enabled
		if ((currentRound % interestPeriod) == 0 && currentRound > 1) {
			at.applyInterestRate(interestRate, dividend);
			ua.setMoney(ua.getMoney() * interestRate);
			ua.setMoney(ua.getMoney() + ua.getShares() * dividend);
		}
		
		// add the midpoint price to the price history
		priceHistory.add(lob.getMidpointPrice());
		
		// calculate averages
		if (lob.getMarketSpread() > 0) averageSpread += lob.getMarketSpread();
		if (lob.getBuyBookSize() > 0) averageBids += lob.getBuyBookSize();
		if (lob.getSellBookSize() > 0) averageAsks += lob.getSellBookSize();
		
		// clear expired bids from the lob
		lob.clearExpiredBids(currentRound);
		
		// submit an order from a random agent
		return at.randomAgent().submitOrder(currentRound++);
	}

	// submit a user order, which acts as a round in the simulation
	public boolean submitUserOrder(boolean isLimit, boolean isSell, int size,
			double price, int expiration) {
		if (isDone()) return false;
		
		// process interest and dividend payments
		if ((currentRound % interestPeriod) == 0 && currentRound > 1) {
			at.applyInterestRate(interestRate, dividend);
			ua.setMoney(ua.getMoney() * interestRate);
			ua.setMoney(ua.getMoney() + ua.getShares() * dividend);
		}
		
		// add the midpoint to the price history
		priceHistory.add(lob.getMidpointPrice());
		
		// caclulate averages
		if (lob.getMarketSpread() > 0) averageSpread += lob.getMarketSpread();
		if (lob.getBuyBookSize() > 0) averageBids += lob.getBuyBookSize();
		if (lob.getSellBookSize() > 0) averageAsks += lob.getSellBookSize();
		
		// clear expired bids from the lob
		lob.clearExpiredBids(currentRound);
		
		// submit the users order
		return ua.submitOrder(isLimit, isSell, size, price, currentRound
				+ expiration);
	}

	// is the simulation done?
	public boolean isDone() {
		return (currentRound == rounds);
	}

	
	// get the current level of the user's money
	public double getUserMoney() {
		return ua.getMoney();
	}

	// get the current level of the user's money, formatted nicely
	public String getFormattedUserMoney() {
		return ((UserAgent) ua).getFormattedMoney();
	}

	// get the current number of user shares
	public int getUserShares() {
		return ua.getShares();
	}

	// print data on the simulation
	public void printData() {
		System.out.printf("%6.4f, %6.4f, %6d, %6d", lob.getMidpointPrice(),
				lob.getMarketSpread(), lob.getBuyBookSize(), lob.getSellBookSize());
		System.out.println();
	}
	
	// print the number of the user trades completed
	public void printUserTrades() {
		System.out.println("User Trades = " + ua.getTradesCompleted());
	}

	// print the average spread, with different options
	public void printAverageSpread(int i) {
		if (i == 0) {
			System.out.printf("%3.2f, %5.2f", (double) numChartists
					/ (numZeroIntel + numChartists), (averageSpread / rounds));
			System.out.println();
		} else if (i == 1) {
			System.out.printf("%3d, %5.2f", history, averageSpread / rounds);
			System.out.println();
		} else if (i == 2) {
			System.out.printf("%3.2f, %5.2f", lp, averageSpread / rounds);
			System.out.println();
		} else if (i == 3) {
			System.out.printf("%3.2f, %5.2f", sp, averageSpread / rounds);
			System.out.println();
		} else if (i == 4) {
			System.out.printf("%4.2f, %5.2f", interval, averageSpread / rounds);
			System.out.println();
		} else if (i == 5) {
			System.out.printf("%5d, %5.2f", exp, averageSpread / rounds);
			System.out.println();
		}
	}

	// print the results of the simulatoin
	public void printResults() {
		System.out.println("Final Price = " + lob.getMidpointPrice());
		ua.printFinalData(lob.getMidpointPrice());
		at.printFinalData(lob.getMidpointPrice());
	}

	public static void main(String[] args) {
		int numRounds = 1000000;
		int numZero = 100;
		int numChartists = 0;
		boolean exp = true;
		int history = 3;
		double lp = 0.7;
		double sp = 0.5;
		double interval = 5;
		int expiration = 1000;
		DASimulation das = new DASimulation(numRounds, numZero, numChartists,
				exp, history, lp, sp, interval, expiration);

		// System.out.println("Starting...");

		/*
		 * while (!das.isDone()) { das.nextBid(); das.printData(); }
		 */

		for (int i = 1; i <= 1000; i += 1) {
			das = new DASimulation(numRounds, numZero, numChartists, exp, history,
					lp, sp, interval, i);
			while (!das.isDone()) {
				das.nextBid();
			}
			das.printAverageSpread(5);
		}

		// System.out.println("End");
		// System.out.println();
		// das.printResults();
	}
}
