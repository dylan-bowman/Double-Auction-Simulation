/*
 * Author: Dylan Bowman
 * 
 * Limit Order
 * 
 * A limit order is placed in the limit order book by an Agent and specifies
 * the price the agent is willing to buy or sell at and the length the order
 * is active for.
 */

public class LimitOrder implements Comparable<LimitOrder> {
	
	private int	size;  // amount of shares represented in the order
	private double		price;  // price willing to sell or buy at
	private boolean	type;  // type of limit order: SELL = true; BUY = false
	private int			expiration;  // expiration round on order
	private Agent		player;  // player who made the order

	public LimitOrder(int size, double price, boolean type, int expiration,
							Agent p) {
		this.size = size;
		this.price = price;
		this.type = type;
		this.expiration = expiration;
		this.player = p;
	}

	// natural ordering for LimitOrder class done by price
	public int compareTo(LimitOrder that) {
		if (this.price < that.price) return -1;
		if (this.price > that.price) return 1;
		return 0;
	}

	// format the limit order nicely
	public String toString() {
		return String.format("Pla: %2d Size: %4d Price: %5.2f Exp: %3d",
				player.getPID(), size, price, expiration);
	}

	// returns the size of the limit order
	public int getSize() {
		return size;
	}

	// sets the size of the limit order
	public void setSize(int newSize) {
		size = newSize;
	}

	// returns the price of the limit order
	public double getPrice() {
		return price;
	}

	// returns the type of the limit order
	public boolean getType() {
		return type;
	}

	// returns the expiration on the limit order
	public int getExpiration() {
		return expiration;
	}

	// returns the player who made the limit order
	public Agent getPlayer() {
		return player;
	}

}