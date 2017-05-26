/*
 * Author: Dylan Bowman
 * 
 * Limit Order Book
 * An object that keeps track of the limit orders in the DAS.  Consists of a buy book, sell
 * book, and expiration book.  Please see my thesis for a more in depth explanation: 
 * http://dataspace.princeton.edu/jspui/handle/88435/dsp01tq57nr19m
 */
import java.util.*;

public class LimitOrderBook {
	private PriorityQueue<LimitOrder>	sellbook;  // contains limit sell orders ordered by lowest price first
	private PriorityQueue<LimitOrder>	buybook;  // contains limit buy orders ordered by highest price first
	private PriorityQueue<LimitOrder>	expbook;  // contains all limit orders ordered by expiration round
	private boolean  limitOrdersExpire; // whether limit orders expiring is enabled or not
	private ClearingHouse  ch  = new ClearingHouse(); // clearingHouse to clear trades
	
	private double lastTransactionPrice;
	private boolean transactionOccured;

	// static to determine which type of order it is
	private static final boolean	SELL	= true;
	private static final boolean	BUY	= false;
	
	private boolean same;

	public LimitOrderBook(boolean expirationOn) {
		// ordered by lowest sell price first
		this.sellbook = new PriorityQueue<LimitOrder>();
		// ordered by highest buy price first
		this.buybook = new PriorityQueue<LimitOrder>(10,
				Collections.reverseOrder());
		// ordered by lowest expiration round first
		if (expirationOn) {
			this.expbook = new PriorityQueue<LimitOrder>(10,
					ExpirationComparator.getInstance());
		} else {
			this.expbook = null;
		}
		this.limitOrdersExpire = expirationOn;
		this.lastTransactionPrice = 0.0;
		this.transactionOccured = false;
		
		this.same = false;
	}

	// returns whether limit orders can expire or not
	public boolean areExpirationsOn() {
		return limitOrdersExpire;
	}

	// get the buy book as an array
	public LimitOrder[] getBuyBookAsArray() {
		LimitOrder[] bb = buybook.toArray(new LimitOrder[0]);
		Arrays.sort(bb, Collections.reverseOrder());
		return bb;
	}

	// get the sell book as an array
	public LimitOrder[] getSellBookAsArray() {
		LimitOrder[] sb = sellbook.toArray(new LimitOrder[0]);
		Arrays.sort(sb);
		return sb;
	}

	// get the best (lowest) asking (selling) price
	public double getBestAsk() {
		if (sellbook.peek() != null) return sellbook.peek().getPrice();
		else return -1;
	}

	// get the best (highest) bidding (buying) price
	public double getBestBid() {
		if (buybook.peek() != null) return buybook.peek().getPrice();
		else return -1;
	}

	// get the market spread (lowest selling price - highest buying price)
	public double getMarketSpread() {
		if (buybook.peek() == null || sellbook.peek() == null) return -1;
		else return (sellbook.peek().getPrice() - buybook.peek().getPrice());
	}

	// get the midpoint of the lowest selling price and highest buying price
	public double getMidpointPrice() {
		if (buybook.peek() == null || sellbook.peek() == null) return -1;
		else return (sellbook.peek().getPrice() + buybook.peek().getPrice()) / 2.0;
	}
	
	// get the size of buy book
	public int getBuyBookSize() {
		return buybook.size();
	}
	
	// get the size of the sell book
	public int getSellBookSize() {
		return sellbook.size();
	}
	
	// get the price of the last transaction that took place
	public double getLastTransactionPrice() {
		return lastTransactionPrice;
	}
	
	// did a transaction occur in the last round?
	public boolean transactionOccured() {
		return transactionOccured;
	}

	// print the sell, buy and expiration books
	public void print(int round) {
		// System.out.println("Limit Order Book at round " + round + ":");
		printSellBook();
		printBuyBook();
		if (limitOrdersExpire) printExpirationBook();
	}

	// print just the sell book
	public void printSellBook() {
		// print sell book
		LimitOrder[] sb = getSellBookAsArray();

		System.out.println("+--------------------------+");
		System.out.println("|     Limit Sell Orders    |");
		System.out.println("|--------------------------|");
		System.out.println("|Exp  |Pla | Size | Price  |");
		System.out.println("|-----|----|------|--------|");
		for (int i = 0; i < sb.length; i++) {
			System.out.printf("|%-5d|%-4d|%-6d|%-8.2f|", sb[i].getExpiration(),
					sb[i].getPlayer().getPID(), sb[i].getSize(), sb[i].getPrice());
			System.out.println();
		}
		System.out.println("+--------------------------+");
		System.out.println();
	}

	// print just the buy book
	public void printBuyBook() {
		// print buy book
		LimitOrder[] bb = getBuyBookAsArray();

		System.out.println("+--------------------------+");
		System.out.println("|     Limit Buy Orders     |");
		System.out.println("|--------------------------|");
		System.out.println("|Exp  |Pla | Size | Price  |");
		System.out.println("|-----|----|------|--------|");
		for (int i = 0; i < bb.length; i++) {
			System.out.printf("|%-5d|%-4d|%-6d|%-8.2f|", bb[i].getExpiration(),
					bb[i].getPlayer().getPID(), bb[i].getSize(), bb[i].getPrice());
			System.out.println();
		}
		System.out.println("+--------------------------+");
		System.out.println();
	}

	// print just the expiration book
	public void printExpirationBook() {
		// print expiration book
		LimitOrder[] eb = expbook.toArray(new LimitOrder[0]);
		Arrays.sort(eb, ExpirationComparator.getInstance());

		System.out.println("+--------------------------+");
		System.out.println("|      Expiration Book     |");
		System.out.println("|--------------------------|");
		System.out.println("|Exp  |Pla | Size | Price  |");
		System.out.println("|-----|----|------|--------|");
		for (int i = 0; i < eb.length; i++) {
			System.out.printf("|%-5d|%-4d|%-6d|%-8.2f|", eb[i].getExpiration(),
					eb[i].getPlayer().getPID(), eb[i].getSize(), eb[i].getPrice());
			System.out.println();
		}
		System.out.println("+--------------------------+");
		System.out.println();
	}

	// submits a limit buy order to the limit order book
	// the order is from agent p and has size, price, and expiration round
	public boolean submitLimitBuyOrder(int size, double price, int exp, Agent p) {
		if (price < 0) return false;
		boolean success;

		if (sellbook.size() > 0) {
			// if the buying price of this order is above the lowest selling price, buy it at that price instead
			LimitOrder lowestSell = sellbook.peek();
			if (price >= lowestSell.getPrice()) {
				return submitMarketOrder(false, size, p);
			}
		}

		// add the limit order to the buybook and expiration book
		LimitOrder newOrder = new LimitOrder(size, price, BUY, exp, p);
		success = buybook.add(newOrder);
		if (limitOrdersExpire) success = success && expbook.add(newOrder);
		transactionOccured = false;

		return success;
	}

	// submits a limit sell order to the limit order book
	// the order is from agent p and has size, price, and expiration round
	public boolean submitLimitSellOrder(int size, double price, int exp, Agent p) {
		if (price < 0) return false;
		boolean success;

		if (buybook.size() > 0) {
			// if the selling price of this order is below the highest selling price, sell it at that price instead
			LimitOrder highestBuy = buybook.peek();
			if (price <= highestBuy.getPrice()) {
				return submitMarketOrder(true, size, p);
			}
		}
		
	// add the limit order to the sellbook and expiration book
		LimitOrder newOrder = new LimitOrder(size, price, SELL, exp, p);
		success = sellbook.add(newOrder);
		if (limitOrdersExpire) success = success && expbook.add(newOrder);
		transactionOccured = false;
		
		return success;
	}

	// submits and handles a market buy order
	// immediatly buys or sells *size* amounts of shares for agent *buyer*
	// at the lowest/highest price available in the limit order book
	public boolean submitMarketOrder(boolean sell, int size, Agent agent1) {
		boolean s = true;

		LimitOrder lo = null;
		Agent buyer = null;
		Agent seller = null;
		PriorityQueue<LimitOrder> book;

		// determine type of market order
		if (sell) {
			seller = agent1;
			book = buybook;
		} else {
			buyer = agent1;
			book = sellbook;
		}
		
		// while there are still more shares to be bought
		while (size > 0 && s) {
			// get the top limit order in the book
			lo = book.poll();

			// if no limit orders left, the trade was not finished completely, so
			// we return false
			// NOTE that if simulations are run with a max order size of 1, this
			// works fine...
			// it will return null if the order wasnt able to process because of no
			// limit orders
			// but if max order size > 1, this will return false EVEN IF some of
			// the market order
			// went through and processed.
			if (lo == null) {
				s = false;
				break;
			}

			if (sell) buyer = lo.getPlayer();
			else seller = lo.getPlayer();
			
			// players shouldnt be allowed to buy shares from themselves...
			// skip to the next lo in the queue
			if (buyer == seller && same) { continue; } 

			// if the orders are equal in size... simple
			if (lo.getSize() == size) {
				// verify both the seller and buyer sides in the clearinghouse
				if (ch.checkSeller(seller, size)) {
					if (ch.checkBuyer(buyer, size, lo.getPrice())) {
						// execute transaction
						ch.tradeClears(buyer, seller, size, lo.getPrice());
						if (limitOrdersExpire) expbook.remove(lo);
						size = 0;
					} else if (sell) continue;
					else s = false;
				} else if (sell) s = false;
				else continue;

				// if the seller's size is larger than the buyer's, need to add
				// the limit order back into the sellbook with the adjusted size
			} else if (lo.getSize() > size) {
				// verify both the seller and buyer sides in the clearinghouse
				if (ch.checkSeller(seller, size)) {
					if (ch.checkBuyer(buyer, size, lo.getPrice())) {
						// execute transaction
						ch.tradeClears(buyer, seller, size, lo.getPrice());
						if (limitOrdersExpire) expbook.remove(lo);
						
						// replace limit order with new size
						lo.setSize(lo.getSize() - size);
						book.add(lo);
						if (limitOrdersExpire) expbook.add(lo);
						size = 0;
					} else if (sell) continue;
					else s = false;
				} else if (sell) s = false;
				else continue;
			// if the buyer's size is larger than the seller's, subtract
			// the seller's size from the buyer's and continue in the
			// while loop
			} else {
			// verify both the seller and buyer sides in the clearinghouse
				if (ch.checkSeller(seller, lo.getSize())) {
					if (ch.checkBuyer(buyer, lo.getSize(), lo.getPrice())) {
						// execute transaction
						ch.tradeClears(buyer, seller, lo.getSize(), lo.getPrice());
						
						// set new size of market order and continue
						size -= lo.getSize();
						if (limitOrdersExpire) expbook.remove(lo);
					} else if (sell) continue;
					else s = false;
				} else if (sell) s = false;
				else continue;
			}
		}
		// if s is false and lo is not null, didnt pass clearinghouse, add limit order
		// back onto book
		if (s == false && lo != null) {
			book.add(lo);
		}
		
		return s;
	}

	// clear expired bids from the lob
	public void clearExpiredBids(int round) {
		if (!limitOrdersExpire) return;
		if (expbook.size() <= 0) return;

		LimitOrder top = expbook.peek();
		if (top == null) return;
		
		// keep clearing the orders until there aren'y anymore that are expired
		while (top.getExpiration() <= round && top != null) {
			expbook.remove();
			if (top.getType() == true) sellbook.remove(top);
			else buybook.remove(top);
			top = expbook.peek();
			if (top == null) return;
		}
	}

	// clear the limit order book
	private void clearBooks() {
		sellbook.clear();
		buybook.clear();
		expbook.clear();
	}

	// the ClearingHouse makes sure the buyer has the necessary funds
	// and that the seller has the necessary shares to make the transaction
	private class ClearingHouse {

		
		public ClearingHouse() {
		}

		// check that the buyer has the necessary funds
		public boolean checkBuyer(Agent buyer, int size, double price) {
			return buyer.getMoney() - (size * price) >= 0;
		}

		// check that the seller has the necessary shares
		public boolean checkSeller(Agent seller, int size) {
			return seller.getShares() - size >= 0;
		}

		// only call this once you've checked the buyer AND the seller
		// future work: really should make all three of these functions one function
		public void tradeClears(Agent buyer, Agent seller, int size, double price) {
			// execute the transaction and trigger the transactionOccured boolean
			double cost = size * price;
			seller.setShares(seller.getShares() - size);
			seller.setMoney(seller.getMoney() + cost);
			buyer.setShares(buyer.getShares() + size);
			buyer.setMoney(buyer.getMoney() - cost);
			lastTransactionPrice = price;
			transactionOccured = true;
		}
	}

	// main for testing functionality and corner cases of limit order book
	public static void main(String[] args) {
		LimitOrderBook lob = new LimitOrderBook(true);
		Agent agent1 = new PracticeAgent(100, 100, lob, 1);
		Agent agent2 = new PracticeAgent(100, 100, lob, 2);

		System.out.println("Beginning systematic tests of limit book:");
		// test limit buy
		System.out.println("-----------------------------------------");
		System.out.println("Test 1: limit buy order");
		boolean success = lob.submitLimitBuyOrder(1, 1.00, 50, agent2);
		success = success && lob.submitLimitBuyOrder(1, 0.90, 45, agent1);
		success = success && lob.submitLimitBuyOrder(2, 0.95, 61, agent1);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		// test limit sell
		System.out.println("-----------------------------------------");
		System.out.println("Test 2: limit sell order");
		success = lob.submitLimitSellOrder(1, 1.50, 40, agent2);
		success = success && lob.submitLimitSellOrder(2, 1.45, 72, agent1);
		success = success && lob.submitLimitSellOrder(1, 1.48, 63, agent1);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		// test market buy (different sizes)
		System.out.println("-----------------------------------------");
		System.out.println("Test 3a: MBO - p2 limit order should clear");
		success = lob.submitMarketOrder(BUY, 1, agent1);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		System.out
				.println("Test 3b: MBO - should return true, no p2 limit orders");
		success = lob.submitMarketOrder(BUY, 1, agent1);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		System.out.println("Test 3c: MBO - should clear top order");
		success = lob.submitMarketOrder(BUY, 1, agent2);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		System.out.println("Test 3d: MBO - should clear rest of orders");
		success = lob.submitMarketOrder(BUY, 2, agent2);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		System.out
				.println("Test 3e: MBO - should be empty and return true (only one share bought)");
		lob.submitLimitSellOrder(1, 1.36, 63, agent1);
		success = lob.submitMarketOrder(BUY, 2, agent2);
		System.out.println("Successful? " + success);
		lob.printSellBook();

		System.out.println("Test 3f: quick glimpse of expiration book");
		System.out.println("should only have limit buy orders in there");
		lob.printExpirationBook();

		// test market sell (different sizes)
		System.out.println("-----------------------------------------");
		System.out.println("Test 4a: MSO - p2 limit order should clear");
		success = lob.submitMarketOrder(SELL, 1, agent1);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		System.out
				.println("Test 4b: MSO - should return true, no p2 limit orders");
		success = lob.submitMarketOrder(SELL, 1, agent1);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		System.out.println("Test 4c: MSO - should clear top order");
		success = lob.submitMarketOrder(SELL, 1, agent2);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		System.out.println("Test 4d: MSO - should clear rest of orders");
		success = lob.submitMarketOrder(SELL, 2, agent2);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		System.out
				.println("Test 4e: MSO - should be empty but return true (only one share bought)");
		lob.submitLimitBuyOrder(1, 1.00, 63, agent1);
		success = lob.submitMarketOrder(SELL, 2, agent2);
		System.out.println("Successful? " + success);
		lob.printBuyBook();

		System.out.println("Test 4f: quick glimpse of expiration book");
		System.out.println("should have nothing in here");
		lob.printExpirationBook();

		// test clearinghouse
		System.out.println("-----------------------------------------");
		System.out
				.println("Test 5a: Clearinghouse - agent1 doesnt have enough shares");
		agent1 = new PracticeAgent(0, 0, lob, 1);
		agent2 = new PracticeAgent(100, 100, lob, 2);
		System.out.println("Before:");
		lob.submitLimitSellOrder(1, 1.00, 25, agent1);
		lob.printSellBook();
		System.out.println("After:");
		success = lob.submitMarketOrder(BUY, 1, agent2);
		lob.printSellBook();
		System.out.println("Successful? " + success);
		System.out.println("Should be true, and sb clear");

		System.out
				.println("Test 5b: Clearinghouse - agent1 doesnt have enough money");
		System.out.println("Before:");
		lob.submitLimitBuyOrder(1, 1.00, 25, agent1);
		lob.printBuyBook();
		System.out.println("After:");
		success = lob.submitMarketOrder(SELL, 1, agent2);
		lob.printSellBook();
		System.out.println("Successful? " + success);
		System.out.println("Should be true, and bb clear");

		System.out
				.println("Test 5c: Clearinghouse - agent2 doesnt have enough money");
		agent1 = new PracticeAgent(100, 100, lob, 1);
		agent2 = new PracticeAgent(0, 0, lob, 2);
		System.out.println("Before:");
		lob.submitLimitSellOrder(1, 1.00, 25, agent1);
		lob.printSellBook();
		System.out.println("After:");
		success = lob.submitMarketOrder(BUY, 1, agent2);
		lob.printSellBook();
		System.out.println("Successful? " + success);
		System.out.println("Should be false, and sb have one order");

		System.out
				.println("Test 5d: Clearinghouse - agent2 doesnt have enough shares");
		System.out.println("Before:");
		lob.submitLimitBuyOrder(1, 1.00, 25, agent1);
		lob.printBuyBook();
		System.out.println("After:");
		success = lob.submitMarketOrder(SELL, 1, agent2);
		lob.printSellBook();
		System.out.println("Successful? " + success);
		System.out.println("Should be false, and bb have one order");
		lob.clearBooks();

		// test expiration stuff?
		System.out.println("-----------------------------------------");
		System.out.println("Test 6a: expiration process");
		agent1 = new PracticeAgent(100, 100, lob, 1);
		agent2 = new PracticeAgent(100, 100, lob, 2);
		System.out.println("Before:");
		lob.submitLimitSellOrder(1, 1.10, 1, agent1);
		lob.submitLimitBuyOrder(1, 1.00, 2, agent2);
		lob.submitLimitBuyOrder(1, 1.01, 1, agent2);
		lob.submitLimitBuyOrder(1, 0.99, 0, agent1);
		lob.print(0);
		System.out.println("After:");
		lob.clearExpiredBids(1);
		lob.print(1);
		System.out.println("Sellbook should be clear, bb and eb have one bid");

		// corner cases?

	}
}