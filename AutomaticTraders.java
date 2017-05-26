/*
 * Author: Dylan Bowman
 * 
 * Automatic Traders - class that represents the group of automatic traders/agents in the simulation
 */
import java.util.*;

public class AutomaticTraders {

	private int			N;
	private int			i;
	private Agent[]	agents;
	private Random		random;

	// initialize with the number of automatic traders/agents in the sim
	public AutomaticTraders(int number) {
		this.N = number;
		this.i = 0;
		this.agents = new Agent[number];
		this.random = new Random();
	}

	// get the number of automatic agents
	public int getNumberAgents() {
		return N;
	}

	// get number of agents of a certain type
	public int getNumberAgents(int type) {
		int count = 0;
		for (int i = 0; i < N; i++) {
			if (agents[i].getType() == type) count++;
		}
		return count;
	}

	// add an agent to the group
	public void addAgent(Agent a) {
		if (i >= N) throw new RuntimeException(
				"cant add anymore agents... what are you doing wrong?");
		agents[i++] = a;
	}

	// grab a random agents from the group
	public Agent randomAgent() {
		if (i < N) throw new RuntimeException(
				"shouldnt be choosing random agent when i < n");
		return agents[random.nextInt(N)];
	}

	// apply interest rate and dividends to the agents' money totals
	public void applyInterestRate(double ir, double div) {
		for (Agent a : agents) {
			a.setMoney(a.getMoney() * ir);
			a.setMoney(a.getMoney() + a.getShares() * div);
		}
	}

	// print the status of all the agents in the AutomaticTraders object
	public void print() {
		Arrays.sort(agents);

		System.out.println("+----------------------------+");
		System.out.println("|      Status of Agents      |");
		System.out.println("|----------------------------|");
		System.out.println("|PID|Tp| Money  |Shares| TC  |");
		System.out.println("|---|--|--------|------|-----|");
		for (int i = 0; i < agents.length; i++) {
			System.out.printf("|%-3d|%2d|%-8.2f|%-6d|%-5d|", agents[i].getPID(),
					agents[i].getType(), agents[i].getMoney(),
					agents[i].getShares(), agents[i].getTradesCompleted());
			System.out.println();
		}
		System.out.println("+----------------------------+");
		System.out.println();
	}

	// print the agent's final data
	public void printFinalData(double finalPrice) {
		Arrays.sort(agents);

		for (int i = 0; i < agents.length; i++) {
			agents[i].printFinalData(finalPrice);
		}
	}

	// print the status of all of the agents of a certain type
	public void printType(int type) {
		Arrays.sort(agents);

		System.out.println("+------------------------------+");
		System.out.println("|        Status of Agents      |");
		System.out.println("|------------------------------|");
		System.out.println("| PID |Tp| Money  |Shares| TC  |");
		System.out.println("|-----|--|--------|------|-----|");
		for (int i = 0; i < agents.length; i++) {
			if (agents[i].getType() == type) {
				System.out.printf("|%-5d|%2d|%-8.2f|%-6d|%-5d|",
						agents[i].getPID(), agents[i].getType(),
						agents[i].getMoney(), agents[i].getShares(),
						agents[i].getTradesCompleted());
				System.out.println();
			}
		}
		System.out.println("+--------------------------+");
		System.out.println();
	}
	
}
