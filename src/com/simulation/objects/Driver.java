package com.simulation.objects;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Driver {

	// Display a message, preceded by the name of the current thread
	static void threadMessage(String message) {
		String threadName = Thread.currentThread().getName();
		System.out.format("%s: %s%n", threadName, message);
	}

	/*
	 * Default values in case command line parameters are not specified
	 */
	public static int N_floors = 5;
	public static int maxWander = 11;
	public static int N_people = 10;
	private static int runTime = 300;
	
	private static long startTime = System.currentTimeMillis();

	public static Elevator elevator = new Elevator();
	private static ArrayList<Thread> peopleQueue = new ArrayList<Thread>();

	/* 
	 * Create N_people semaphores
	 * They will initially be acquired by each Person thread in its constructor
	 * When the Person thread is started, it will release the semaphore it is holding
	 * The Elevator thread can only start its run() hook method AFTER all semaphores have been released
	 */
	public static Semaphore startElevator;
	
	/* 
	 * A binary semaphore to allow only one person to operate
	 * the elevator's button
	 */
	public static Semaphore buttonSem = new Semaphore(1, true);
	
	/*
	 * Method to start all Person instances
	 * followed by the Elevator thread
	 */
	static void startThreads(Thread t) {	
		for(int i = 0; i < peopleQueue.size(); i++) {
			Thread tmp = peopleQueue.get(i);
			threadMessage("Staring Person thread");
			tmp.start();
		}
		threadMessage("Starting Elevator thread");
		t.start();
	}

	/* 
	 * Create N_people Person threads
	 * and append it to the ListArray peopleQueue
	 * which will be started later in the code
	 */
	static void createPeople(){
		for(int i = 0; i < N_people; i++) {
			Person person = new Person(elevator);
			Thread tmp = new Thread(person);
			peopleQueue.add(tmp);
		}
	}
	
	static void parseInput(String args[]) {
		Options options = new Options();
		options.addOption("p", true, "Number of people in the building");
		options.addOption("w", true, "Maximum person wandering time on a floor");
		options.addOption("f", true, "Number of floors in the building");
		options.addOption("R", true, "Simulation running time");
		
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			if(cmd.hasOption("p")){
				N_people = Integer.parseInt(cmd.getOptionValue("p"));		//defaults to 5
			} else if(cmd.hasOption("w")) {
				maxWander = Integer.parseInt(cmd.getOptionValue("w"));	//defaults to 11 s
			} else if(cmd.hasOption("f")) {
				N_floors = Integer.parseInt(cmd.getOptionValue("f"));		//defaults to 10
			} else if(cmd.hasOption("R")) {
				runTime = Integer.parseInt(cmd.getOptionValue("R")); 	//defaults to 300 s
			}
		} catch (ParseException e) {
			System.out.println("Error parsing command line arguments");
			return;
		}
		
	}

	public static void main(String args[]) throws InterruptedException {
		//parse command line arguments to determine the different values
		parseInput(args);
		
		/* 
		 * This needs to be initialized before creating Person thread(s) because
		 * each Person thread acquires a semaphore in its constructor
		 */
		startElevator = new Semaphore(N_people, true);
	
		Thread t = new Thread(elevator);
		createPeople();
		
		buttonSem.acquire();
		startThreads(t);	//start people threads and elevator thread
		buttonSem.release();
		
		/*
		 * Loop until Elevator thread exits after runTime
		 */
		while(t.isAlive()) {
			//Wait maximum of 1 second for elevator thread to finish
			t.join(1000);
			if(((System.currentTimeMillis() - startTime) > runTime*1000) && t.isAlive()) {
				threadMessage("Specifid run time limit reached!");
				t.interrupt();
				t.join();
			}
		}
		threadMessage("Finally!");
	}
}
