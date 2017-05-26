/*
 * Author: Dylan Bowman
 * 
 * CISimulation class - implements DAS
 * CISimulation implements a Chiarella and Iori version of an agent-based simulation model.
 * The CISimulation features CIAgents which have weights for fundamentaist, chartist and noise,
 * which they use to value the price and future price of the commodity in the market.
 * Please see my thesis for a more in depth explanation: 
 * http://dataspace.princeton.edu/jspui/handle/88435/dsp01tq57nr19m
 */

import java.util.ArrayList;
import java.util.Random;

public class CISimulation implements DAS {

	private int						numRounds;
	private int						currentRound;
	private int						numAgents;
	private boolean				interestOn;
	private double					interestRate;
	private double					dividend;
	private double					interestPeriod;
	private double					lambda; // probability of entering the market
	private double					pf; // fundamental value of the commodity

	private LimitOrderBook		lob;
	private AutomaticTraders	at;
	private Agent					ua;
	private ArrayList<Double>	priceHistory;

	private Random					random;

	private double					averageSpread;
	private double					averageBids;
	private double					averageAsks;

	private int						tau; // lifetime of all orders
	private double					delta; // tick size
	private double					std1; // weight of fundamentalist component
	private double					std2; // weight of chartist component
	private double					n0; // weight of noise
	private int						lmax; // max # of rounds looked back on for history by an agent
	private double					kmax; // max percentage of price expected to be bid

	public CISimulation(int numRounds, int numAgents, boolean interestOn,
								int tau, double delta, double lambda, double pf,
								double std1, double std2, double n0, int lmax,
								double kmax) {
		this.tau = tau;
		this.delta = delta;
		this.std1 = std1;
		this.std2 = std2;
		this.n0 = n0;
		this.lmax = lmax;
		this.kmax = kmax;

		this.averageSpread = 0;
		this.averageBids = 0;
		this.averageAsks = 0;

		this.numRounds = numRounds;
		this.currentRound = 0;
		this.numAgents = numAgents;
		this.interestOn = interestOn;
		this.interestRate = 1.03; // hard coded interest rate for this project
		this.dividend = 1.035;  // hard coded divident rate for this project
		this.interestPeriod = 1000; // hard coded interest period
		this.lambda = lambda;
		this.pf = pf;

		this.random = new Random();
		this.at = new AutomaticTraders(numAgents);
		this.lob = new LimitOrderBook(true);
		this.priceHistory = new ArrayList<Double>();

		double startingMoney = 1000.0;
		int startingShares = 20;

		this.ua = new UserAgent(startingMoney, startingShares, lob, 0);

		// initialize CIAgents
		for (int i = 1; i <= numAgents; i++) {
			double g1 = Math.abs(random.nextGaussian() * std1);
			double g2 = random.nextGaussian() * std2;
			double n = random.nextGaussian() * n0;
			int li = random.nextInt(lmax) + 1;
			double ki = random.nextDouble() * kmax;

			Agent a = new CIAgent(startingMoney, startingShares, lob, i, this, pf,
					tau, g1, g2, n, li, ki, delta);
			at.addAgent(a);
		}
	}

	// print # of user trades completed
	public void printUserTrades() {
		System.out.println("User Trades = " + ua.getTradesCompleted());
	}
	
	// get type of the simulation
	public int getType() {
		return 2;
	}

	// get number of zero intel agents present
	public int getNumZeroIntel() {
		return 0;
	}

	// get number of chartist agents present
	public int getNumChartists() {
		return 0;
	}

	// get number of agents total in this simulation
	public int getNumAgents() {
		return numAgents;
	}

	// get the last "length" rounds of price history for the simulation
	public Double[] getHistory(int length) {
		Double[] hist;
		hist = priceHistory.subList(currentRound - length, currentRound).toArray(
				new Double[length]);
		return hist;
	}

	// get the whole price history of the simulation
	public Double[] getHistory() {
		return priceHistory.toArray(new Double[currentRound]);
	}

	// get the simulation limit book
	public LimitOrderBook getLOB() {
		return lob;
	}

	// get the total number of rounds for this simulation
	public int getNumRounds() {
		return numRounds;
	}

	// get the current round of this simulation
	public int getCurrentRound() {
		return currentRound;
	}

	// prompt a random agent to make the next bid
	public boolean nextBid() {
		if (isDone()) return false;
		
		// process interest and dividend payments if enabled
		if (interestOn && (currentRound % interestPeriod) == 0
				&& currentRound > 1) {
			at.applyInterestRate(interestRate, dividend);
			ua.setMoney(ua.getMoney() * interestRate);
			ua.setMoney(ua.getMoney() + ua.getShares() * dividend);
		}

		// have a random agent submit a bid
		boolean success;
		double r1 = random.nextDouble();
		if (r1 < lambda) {
			success = at.randomAgent().submitOrder(currentRound);
		} else success = true;

		// add the transaction price (or midpoint price if no transaction) to the price history
		if (priceHistory.size() == 0) priceHistory.add(pf);
		else if (lob.transactionOccured()) priceHistory.add(lob
				.getLastTransactionPrice());
		else if (lob.getMidpointPrice() > 0) {
			priceHistory.add(lob.getMidpointPrice());
		} else { // if first couple rounds, just get the value from the last round
			priceHistory.add(priceHistory.get(currentRound - 1));
		}
		
		// calculate averages
		if (lob.getMarketSpread() > 0) averageSpread += lob.getMarketSpread();
		if (lob.getBuyBookSize() > 0) averageBids += lob.getBuyBookSize();
		if (lob.getSellBookSize() > 0) averageAsks += lob.getSellBookSize();
		
		// clear the expired bids in the lob
		lob.clearExpiredBids(currentRound++);
		
		return success;
	}
	
	// user wants to submit an order, which functions as its own round in the sim
	public boolean submitUserOrder(boolean isLimit, boolean isSell, int size,
			double price, int expiration) {
		if (isDone()) return false;
		
		// process interest and dividend payments if turned on
		if (interestOn && (currentRound % interestPeriod) == 0
				&& currentRound > 1) {
			at.applyInterestRate(interestRate, dividend);
			ua.setMoney(ua.getMoney() * interestRate);
			ua.setMoney(ua.getMoney() + ua.getShares() * dividend);
		}

		// submit the users order
		boolean success = ua.submitOrder(isLimit, isSell, size, price,
				currentRound + expiration);
		
		// add the transaction price (or midpoint price if no transaction) to the price history
		if (priceHistory.size() == 0) priceHistory.add(pf);
		if (lob.transactionOccured()) priceHistory.add(lob
				.getLastTransactionPrice());
		else if (lob.getMidpointPrice() > 0) priceHistory.add(lob
				.getMidpointPrice());
		else { // if first couple rounds, just get the value from the last round
			priceHistory.add(priceHistory.get(currentRound - 1));
		}
		
		// calculate averages
		if (lob.getMarketSpread() > 0) averageSpread += lob.getMarketSpread();
		if (lob.getBuyBookSize() > 0) averageBids += lob.getBuyBookSize();
		if (lob.getSellBookSize() > 0) averageAsks += lob.getSellBookSize();
		
		// clear expired bids from the lob
		lob.clearExpiredBids(currentRound++);
		
		return success;
	}

	// is the simulation done?
	public boolean isDone() {
		return currentRound == numRounds;
	}

	// get the current level of the user's money
	public double getUserMoney() {
		return ua.getMoney();
	}

	// get the current level of the user's money, formatted nicely
	public String getFormattedUserMoney() {
		return ((UserAgent) ua).getFormattedMoney();
	}

	// get the current number of the user's shares
	public int getUserShares() {
		return ua.getShares();
	}

	// print data on the simulation
	public void printData() {
		System.out.printf("%6.4f, %6.4f, %6d, %6d", lob.getMidpointPrice(),
				lob.getMarketSpread(), lob.getBuyBookSize(), lob.getSellBookSize());
		System.out.println();
	}
	
	// print the price history of the simulation
	public void printPriceHistory() {
		for(int i = 0; i < priceHistory.size(); i++) {
			System.out.printf("%6.4f", priceHistory.get(i));
			System.out.println();
		}
	}

	// print the average spread of the simulation, with different options
	public void printAverageSpread(int i) {
		if (i == 0) {
			System.out.printf("%3.2f, %5.2f", lambda, averageSpread / numRounds);
			System.out.println();
		} else if (i == 1) {
			System.out.printf("%3d, %5.2f", tau, averageSpread / numRounds);
			System.out.println();
		} else if (i == 2) {
			System.out.printf("%5.2f, %5.2f", delta, averageSpread / numRounds);
			System.out.println();
		} else if (i == 3) {
			System.out.printf("%3.2f, %5.2f", kmax, averageSpread / numRounds);
			System.out.println();
		} else if (i == 4) {
			System.out.printf("%4.2f, %5.2f", n0, averageSpread / numRounds);
			System.out.println();
		} else if (i == 5) {
			System.out.printf("%5d, %5.2f", lmax, averageSpread / numRounds);
			System.out.println();
		}
	}

	// print the final result of the simulation
	public void printResults() {
		System.out.println("Final Price = " + lob.getMidpointPrice());
		ua.printFinalData(lob.getMidpointPrice());
		at.printFinalData(lob.getMidpointPrice());
	}

	public static void main(String[] args) {
		int T = 100;
		int numRounds = 100 * T;
		int numAgents = 1000;
		boolean interestOn = false;
		int tau = 2 * T;
		double delta = 0.01;
		double lambda = 0.5;
		double pf = 1000.0;
		double std1 = 0.1;
		double std2 = 0.14;
		double n0 = 0.3;
		int lmax = T;
		double kmax = 0.5;

		CISimulation cis = new CISimulation(numRounds, numAgents, interestOn,
				tau, delta, lambda, pf, std1, std2, n0, lmax, kmax);

		//while (!cis.isDone()) {
			//cis.nextBid();
			//if (cis.getCurrentRound() % T == 0) cis.printData();
			//cis.printData();
		//}
		//cis.printPriceHistory();
		
		for (double i = 0.1; i <= 100; i += 0.1) {
			cis = new CISimulation(numRounds, numAgents, interestOn,
					tau, i, lambda, pf, std1, std2, n0, lmax, kmax);
			while (!cis.isDone()) {
				cis.nextBid();
			}
			cis.printAverageSpread(2);
		}
		
	}

}
