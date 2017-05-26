/*
 * Author: Dylan Bowman
 * 
 * Double Auction Animator
 * 
 * Provides a (very) basic GUI for the user to interact with the DAS.
 * 
 * NOTE: some of this code was based off of COS402 class assignment
 * P3 at Princeton University... more specifically the Cat-Mouse Animator
 * I more or less used it as a learning tool, but some of the code is used 
 * directly as well.
 */

import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

@SuppressWarnings("serial")
public class DoubleAuctionAnimator extends JFrame {

	private DAS				das;
	private JPanel			topPanel;
	private JPanel			bottomPanel;
	private JList			buyList;
	private JList			sellList;
	private JList			orderChoice;
	private JList			typeChoice;
	private JScrollPane	buyScrollPane;
	private JScrollPane	sellScrollPane;
	private JTextField	sizeField;
	private JTextField	priceField;
	private JTextField	expirationField;
	private JButton		submitOrder;
	private JLabel			errorMessage;
	private JLabel			userShares;
	private JLabel			userMoney;
	private JLabel			roundNumber;

	// private static final Font font = new Font("SansSerif", Font.BOLD, 36);

	private Timer			timer;
	private JButton		startButton;
	private JButton		stopButton;
	private JButton		stepButton;
	private JSlider		slider;
	private int				sliderMax	= 10000;
	private int				delayMin		= 0;
	private int				delayMax		= 2000;
	private boolean		print;

	public DoubleAuctionAnimator(DAS das, boolean print) {
		this.das = das;
		this.print = print;

		setTitle("Double Auction Simulation");
		setSize(150, 300);
		setBackground(Color.CYAN); // not the best...

		Dimension dim = new Dimension(600, 600);
		setMinimumSize(dim);
		setResizable(false);

		createTopPanel();
		createBottomPanel();
	}

	// top half of the GUI
	private void createTopPanel() {
		topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout());
		getContentPane().add(topPanel, BorderLayout.CENTER);

		// intialize buy and sell list boxes and add them to the scroll panes
		buyList = new JList(getBuyListData());
		sellList = new JList(getSellListData());

		buyScrollPane = new JScrollPane();
		buyScrollPane.getViewport().add(buyList);
		topPanel.add(buyScrollPane);

		sellScrollPane = new JScrollPane();
		sellScrollPane.getViewport().add(sellList);
		topPanel.add(sellScrollPane);

		// add a panel at the top to display the current round status of the sim
		JPanel toptop = new JPanel();
		toptop.setLayout(new BorderLayout());
		JPanel toptopRound = new JPanel();
		roundNumber = new JLabel("Round 0 out of " + das.getNumRounds());
		toptopRound.add(roundNumber);
		toptop.add(toptopRound, BorderLayout.CENTER);

		// add labels to the top of the scroll panes
		JPanel topLabels = new JPanel();
		toptop.add(topLabels, BorderLayout.SOUTH);
		((FlowLayout) topLabels.getLayout()).setHgap(100);
		JLabel buyListLabel = new JLabel("Bids");
		JLabel sellListLabel = new JLabel("Asks");
		topLabels.add(buyListLabel);
		topLabels.add(sellListLabel);
		getContentPane().add(toptop, BorderLayout.NORTH);

		createControlsAndUserInformation();
	}

	// create user controls and information at the middle of the GUI (bottom of the top panel)
	private void createControlsAndUserInformation() {
		JPanel controls = new JPanel();
		controls.setLayout(new BorderLayout());
		topPanel.add(controls, BorderLayout.SOUTH);

		// create the buttons
		JPanel controlButtons = new JPanel();
		controls.add(controlButtons, BorderLayout.CENTER);

		// assign action listeners to the buttons waiting for a button press
		stepButton = new JButton("Step");
		stepButton.addActionListener(new StepActionListener());
		startButton = new JButton("Start");
		startButton.addActionListener(new StartActionListener());
		stopButton = new JButton("Stop");
		stopButton.addActionListener(new StopActionListener());
		
		// stop button is disabled at the start
		stopButton.setEnabled(false);

		controlButtons.add(startButton);
		controlButtons.add(stepButton);
		controlButtons.add(stopButton);
		
		// create the slider for controlling the speed of the animation
		slider = new JSlider(JSlider.HORIZONTAL, 0, sliderMax, sliderMax / 2);
		Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>();
		dict.put(new Integer(0), new JLabel("slow"));
		dict.put(new Integer(sliderMax), new JLabel("fast"));
		slider.setLabelTable(dict);
		slider.setPaintLabels(true);

		// add a change listener to the slider waiting for the value to be changed
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!slider.getValueIsAdjusting()) {
					setTimerDelay();
				}
			}
		});

		controls.add(slider, BorderLayout.NORTH);
		
		// timer acts as the "rounds" of the simulation
		timer = new Timer(1000, new StepActionListener());
		setTimerDelay();

		// create the user info panel
		JPanel userInfo = new JPanel();
		((FlowLayout) userInfo.getLayout()).setHgap(50);
		controls.add(userInfo, BorderLayout.SOUTH);

		userShares = new JLabel("UserShares: " + das.getUserShares());
		userMoney = new JLabel("User Money: " + das.getFormattedUserMoney());

		userInfo.add(userShares);
		userInfo.add(userMoney);
	}

	// bottom half of the GUI
	private void createBottomPanel() {
		bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		createOrderTextFields();

		createChoicesPanel();

		// create the submit order button at the very bottom and attach an action listener
		submitOrder = new JButton("Submit Order");
		submitOrder.addActionListener(new SubmitOrderActionListener());
		bottomPanel.add(submitOrder, BorderLayout.SOUTH);
	}
	
	// create panel to show size and price fields
	private void createOrderTextFields() {
		
		JPanel orderTextFields = new JPanel();
		orderTextFields.setLayout(new FlowLayout());
		bottomPanel.add(orderTextFields, BorderLayout.NORTH);

		JPanel sizePanel = new JPanel();
		sizePanel.setLayout(new FlowLayout());
		orderTextFields.add(sizePanel);

		JPanel pricePanel = new JPanel();
		pricePanel.setLayout(new FlowLayout());
		orderTextFields.add(pricePanel);

		JLabel sizeLabel = new JLabel("Size: ");
		sizeField = new JTextField("1", 5);
		sizePanel.add(sizeLabel);
		sizePanel.add(sizeField);

		JLabel priceLabel = new JLabel("Price: $");
		priceField = new JTextField("XXX.XX", 5);
		pricePanel.add(priceLabel);
		pricePanel.add(priceField);
	}

	// create panel to contain selection of market/limit and sell/buy
	private void createChoicesPanel() {
		JPanel choices = new JPanel();
		choices.setLayout(new BorderLayout());
		bottomPanel.add(choices, BorderLayout.CENTER);

		JPanel choicesLists = new JPanel();
		choicesLists.setLayout(new FlowLayout());
		choices.add(choicesLists, BorderLayout.CENTER);

		// initialize order and type choices and add them to the panels
		orderChoice = new JList(new String[] { "Limit", "Market" });
		orderChoice.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		orderChoice.setFixedCellWidth(50);
		orderChoice.setSelectedIndex(0);
		choicesLists.add(orderChoice);

		typeChoice = new JList(new String[] { "Sell", "Buy" });
		typeChoice.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		typeChoice.setFixedCellWidth(50);
		typeChoice.setSelectedIndex(0);
		choicesLists.add(typeChoice);

		// create expiration input text field
		JPanel expirationPanel = new JPanel();
		expirationPanel.setLayout(new FlowLayout());
		choices.add(expirationPanel, BorderLayout.SOUTH);

		JLabel expirationLabel1 = new JLabel("Limit Order will expire in: ");
		expirationPanel.add(expirationLabel1);
		expirationField = new JTextField("50", 5);
		expirationPanel.add(expirationField);
		JLabel expirationLabel2 = new JLabel(" rounds");
		expirationPanel.add(expirationLabel2);

		// error message displayed if fields are not entered correctly
		errorMessage = new JLabel("", SwingConstants.CENTER);
		errorMessage.setForeground(Color.RED);
		errorMessage.setVisible(false);
		choices.add(errorMessage, BorderLayout.NORTH);
	}

	// attach timer speed to the value of the slider
	private void setTimerDelay() {
		double sliderValue = slider.getValue() / ((double) sliderMax);
		int delay = ((int) (sliderValue * delayMin + (1.0 - sliderValue)
				* delayMax));
		timer.setDelay(delay);
	}

	// action listener for the Start button (starts the timer/simulation)
	private class StartActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			timer.start();
			stepButton.setEnabled(false);
			stopButton.setEnabled(true);
			startButton.setEnabled(false);
			submitOrder.setEnabled(false);
		}
	}

	// action listener for the step button (advances the timer/simulation one step)
	private class StepActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (das.isDone()) {
				timer.stop();
				stepButton.setEnabled(false);
				stopButton.setEnabled(false);
				startButton.setEnabled(false);
				submitOrder.setEnabled(false);
				das.printResults();
				das.printUserTrades();
			}
			if (print) printData();
			das.nextBid();
			refreshSimulation();
		}
	}

	// print data on the simulation to System.out
	private void printData() {
		System.out.printf("%5d %6.4f %6.4f", das.getCurrentRound(), das.getLOB()
				.getMidpointPrice(), das.getLOB().getMarketSpread());
		System.out.println();
		/*
		 * System.out.println(das.getCurrentRound() + " " +
		 * das.getLOB().getMidpointPrice() + " " +
		 * das.getLOB().getMarketSpread());
		 */
	}

	// action listener for the stop button (stops the timer/simulation)
	private class StopActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			timer.stop();
			stepButton.setEnabled(true);
			stopButton.setEnabled(false);
			startButton.setEnabled(true);
			submitOrder.setEnabled(true);
		}
	}

	// action listener for the submit order button - a user order
	private class SubmitOrderActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int size;
			double price;
			int expiration;
			boolean isLimit;
			boolean isSell;

			errorMessage.setVisible(false);

			// catch some potential errors by the user and show an error message
			try {
				size = Integer.parseInt(sizeField.getText());
				price = Double.parseDouble(priceField.getText());
				expiration = Integer.parseInt(expirationField.getText());
				isLimit = (orderChoice.getSelectedIndex() == 0) ? true : false;
				isSell = (typeChoice.getSelectedIndex() == 0) ? true : false;
			} catch (Exception exception) {
				errorMessage
						.setText("All fields must be entered, and entered correctly "
								+ "for order to process.");
				errorMessage.setVisible(true);
				return;
			}

			if (price <= 0) {
				errorMessage.setText("price must be positive");
				errorMessage.setVisible(true);
				return;
			}
			if (size <= 0) {
				errorMessage.setText("size must be positive");
				errorMessage.setVisible(true);
				return;
			}

			// print data if the option is selected
			if (print) printData();

			// print an error if the order was not succesful
			if (!das.submitUserOrder(isLimit, isSell, size, price,
					das.getCurrentRound() + expiration)) {
				errorMessage.setText("<html>Complete order did not go through, "
						+ "either because there was not enough liquidity<br> in the "
						+ "market, or because the trade did not pass the clearing"
						+ " house.<html>");
				errorMessage.setVisible(true);
				// return;
			}

			refreshSimulation();
		}
	}

	// refresh the GUI
	private void refreshSimulation() {
		sellList.setListData(getSellListData());
		buyList.setListData(getBuyListData());

		sellScrollPane.revalidate();
		sellScrollPane.repaint();
		buyScrollPane.revalidate();
		buyScrollPane.repaint();

		// update the user information and round
		userShares.setText("UserShares: " + das.getUserShares());
		userMoney.setText("User Money: " + das.getFormattedUserMoney());
		roundNumber.setText("Round " + das.getCurrentRound() + " of "
				+ das.getNumRounds());
	}

	// get the buy list data
	private LimitOrder[] getBuyListData() {
		return das.getLOB().getBuyBookAsArray();
	}

	// get the sell list data
	private LimitOrder[] getSellListData() {
		return das.getLOB().getSellBookAsArray();
	}

	// run this to run the GUI, feel free to play around with the hard coded values below
	public static void main(String[] args) {
		//int numRounds = 1000;
		boolean print = false;
		// param: #agents, #rounds, expOn, lp, sp, int, expLen
		// DAS das = new DASimulation(numRounds, 10, true, 0.9, 0.5, 3.49, 25);
		/*int T = 100;
		int numberRounds = 100 * T;
		int numAgents = 100;
		boolean interestOn = true;
		int tau = 1*T;
		double delta = 0.01;
		double lambda = 0.5;
		double pf = 50.0;
		double std1 = 1.0;
		double std2 = 1.4;
		double n0 = 1.0;
		int lmax = T;
		double kmax = 0.5;

		DAS das = new CISimulation(numberRounds, numAgents, interestOn, tau,
				delta, lambda, pf, std1, std2, n0, lmax, kmax);*/
		
		int numRounds = 10000;
		int numZero = 50;
		int numChartists = 50;
		boolean exp = true;
		int history = 3;
		double lp = 0.7;
		double sp = 0.5;
		double interval = 5;
		int expiration = 100;
		DASimulation das = new DASimulation(numRounds, numZero, numChartists,
				exp, history, lp, sp, interval, expiration);
		
		// param: das, #rounds, printOn
		DoubleAuctionAnimator daa = new DoubleAuctionAnimator(das, print);
		daa.setVisible(true);
		//if(das.isDone()) das.printResults();
		
	}
}
