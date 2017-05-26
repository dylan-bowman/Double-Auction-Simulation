/*
 * Author: Dylan Bowman
 * 
 * Practice Agent
 * 
 * A simple automatic agent for original testing
 */
import java.util.*;

public class PracticeAgent implements Agent, Comparable<Agent> {
	private double				money;
	private int					shares;
	private LimitOrderBook	lob;
	private int					pid;					// might not need this..
	private Random				random;
	private double				a;
	private double				b;
	private double				interval;
	private int					type	= 0;
	private int					tradesCompleted;
	private int					exp;
	
	private int fundPrice;

	public PracticeAgent(double startingmoney, int startingshares,
								LimitOrderBook limitOrderBook, int playerid,
								double limitOrderProb, double sellProb,
								double interval, int exp) {
		this.money = startingmoney;
		this.shares = startingshares;
		this.lob = limitOrderBook;
		this.pid = playerid;
		this.random = new Random();
		this.a = limitOrderProb;
		this.b = sellProb;
		this.interval = interval;
		this.tradesCompleted = 0;
		this.exp = exp;
		
		this.fundPrice = 50;
	}

	public PracticeAgent(double startingmoney, int startingshares,
								LimitOrderBook limitOrderBook, int playerid) {
		this(startingmoney, startingshares, limitOrderBook, playerid, 0.9, 0.5,
				3.49, 100);
	}

	public int compareTo(Agent that) {
		return this.getPID() - that.getPID();
	}

	public int getType() {
		return type;
	}

	public double getMoney() {
		return money;
	}

	public boolean setMoney(double newMoney) {
		if (newMoney < 0) return false;
		money = newMoney;
		return true;
	}

	public int getShares() {
		return shares;
	}

	public boolean setShares(int newShares) {
		if (shares < 0) return false;
		shares = newShares;
		return true;
	}

	public int getTradesCompleted() {
		return tradesCompleted;
	}

	public int getPID() {
		return pid;
	}

	// submits a limit order with probability *a* and a market
	// order with probability 1-*a*; order will be a sell order
	// with probability *b* and a buy order with probability 1-*b*
	public boolean submitOrder(int round) {
		double rand1 = random.nextDouble();
		double rand2 = random.nextDouble();
		boolean success;

		// limit order
		if (rand1 < a) {
			// sell
			if (rand2 < b) {
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
				double newprice = random.nextDouble() * interval;
				if (lob.getBestAsk() < 0) newprice += (fundPrice - interval);
				else newprice += (lob.getBestAsk() - interval);
				if (lob.areExpirationsOn()) {
					success = lob
							.submitLimitBuyOrder(1, newprice, round + exp, this);
				} else {
					success = lob.submitLimitBuyOrder(1, newprice, round, this);
				}
			}
		} else {
			if (rand2 < b) success = lob.submitMarketOrder(true, 1, this);
			else success = lob.submitMarketOrder(false, 1, this);
		}
		if (success) tradesCompleted++;
		return success;
	}

	public boolean submitOrder(boolean isLimit, boolean isSell, int size,
			double price, int expiration) {
		boolean success;
		if (isLimit) {
			if (isSell) success = lob.submitLimitSellOrder(size, price,
					expiration, this);
			else success = lob.submitLimitBuyOrder(size, price, expiration, this);
		} else {
			if (isSell) success = lob.submitMarketOrder(true, size, this);
			else success = lob.submitMarketOrder(false, size, this);
		}

		if (success) tradesCompleted++;
		return success;
	}

	public void printFinalData(double finalPrice) {
		System.out.printf("%3d, %2d, %8.2f, %5d, %8.2f", pid, type, money,
				shares, money + (shares * finalPrice));
		System.out.println();
	}

	public static void main(String[] args) {
		int N = Integer.parseInt(args[0]);
		int rounds = Integer.parseInt(args[1]);
		boolean exp = Boolean.parseBoolean(args[2]);
		LimitOrderBook lob = new LimitOrderBook(exp);
		AutomaticTraders at = new AutomaticTraders(N);
		double limitProb;
		double sellProb;
		double interval;
		int expiration;

		if (args.length > 3) {
			limitProb = Double.parseDouble(args[3]);
			sellProb = Double.parseDouble(args[4]);
			interval = Double.parseDouble(args[5]);
			expiration = Integer.parseInt(args[6]);

			for (int i = 0; i < N; i++) {
				at.addAgent(new PracticeAgent(100, 100, lob, i, limitProb,
						sellProb, interval, expiration));
			}
		} else {
			limitProb = 0.9;
			sellProb = 0.5;
			interval = 3.49;
			expiration = 100;
			for (int i = 0; i < N; i++) {
				at.addAgent(new PracticeAgent(100, 100, lob, i));
			}
		}

		System.out.println("Beginning basic practice agent test");
		System.out.println("Number of Agents = " + N);
		System.out.println("Rounds = " + rounds);
		System.out.println("Expirations on? = " + exp);
		if (exp) System.out.println("Expiration time = " + expiration);
		System.out.println("Sell prob = " + sellProb);
		System.out.println("Interval = " + interval);
		System.out.println("-----------------------------------------");
		boolean s = false;
		for (int i = 0; i < rounds; i++) {
			System.out.println("Round " + i);
			s = at.randomAgent().submitOrder(i);
			System.out.println("Successful order made? " + s);
			lob.print(i);
			System.out.println("Market Spread = " + lob.getMarketSpread());
			System.out.println("Midpoint Price = " + lob.getMidpointPrice());
			System.out.println("-----------------------------------------");
		}
		System.out.println();
		at.print();

	}
}