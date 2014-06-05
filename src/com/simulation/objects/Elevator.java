package com.simulation.objects;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Elevator implements Runnable {
	private static ArrayList<Integer> elevatorQueue = new ArrayList<Integer>();	
	private static ArrayList<Integer> elevatorDequeue = new ArrayList<Integer>();
	private static ArrayList<Integer> stopQueue = new ArrayList<Integer>();
	private static ArrayList<TrackSem> semQueue = new ArrayList<TrackSem>();
	
	/* 
	 * Display a message, preceded by the name of the current thread
	 */
	static void threadMessage(String message) {
		String threadName = Thread.currentThread().getName();
		System.out.format("%s %s%n", threadName, message);
	}
	
	/*
	 * Class to keep track of Semaphores that are acquired when a Person gets on an elevator
	 * This semaphore is released when the elevator reaches tgtFloor
	 */
	private class TrackSem {
		private int floor;
		private Semaphore sem;
		
		public TrackSem(int destFloor, Semaphore tgtSem) {
			try {
				floor = destFloor;
				sem = tgtSem;
				sem.acquire();
			} catch (InterruptedException e) {
				System.out.println("InterruptedException in method toButton() in class Elevator");
			}
		}
		
		public int getFloor() {
			return floor;
		}
		
		public Semaphore getSemaphore() {
			return sem;
		}
	}
	
	/*
	 * Method called by a person to indicate what floor she/he is waiting on
	 */
	public void callButton(int onFloor) {
		threadMessage("waiting on floor " + onFloor);
		elevatorQueue.add(onFloor);
	}
	
	/* 
	 * Method called by a person to indicate what floor she/he wants to go to
	 * Invoking this method initially acquires the semaphore keepSem
	 * Semaphore variable keepSem is released when the floor toFloor is reached by the elevator
	 * and Person instance is let out 
	 */
	public void toButton(int toFloor, Semaphore keepSem) {
		threadMessage("wants to go to floor " + toFloor);
		elevatorDequeue.add(toFloor);
		semQueue.add(new TrackSem(toFloor, keepSem));
	}
	
	/* 
	 * Method to allow people to board the elevator at floor current floor
	 * Returns True if someone got on the elevator, otherwise False
	 */
	private Boolean getOn(int currFloor) {
		Boolean gotOn = false;
		if(elevatorQueue.size() > 0) {	//Someone is waiting to get on at the floor
			for(int j = 0; j < elevatorQueue.size();) {
				if(elevatorQueue.contains(currFloor)) {
					threadMessage("Person getting in the elevator");
					int pos = elevatorQueue.indexOf(currFloor);
					elevatorQueue.remove(pos);
					stopQueue.add(elevatorDequeue.remove(pos));
					gotOn = true;
				}
				else {
					j++;
				}
			}
		}
		return gotOn;
	}
	
	/* 
	 * Method to allow people to get off on a floor from the elevator
	 * Also release the semaphore(s) associated with the floor
	 * Returns true if someone got off, false otherwise
	 */
	private Boolean getOff(int currFloor) {
		Boolean gotOff = false;
		if(stopQueue.size() > 0) {	//People want to get off at this floor
			for(int j = 0; j < stopQueue.size();) {
				if(stopQueue.contains(currFloor)) {
					threadMessage("Person getting off from the elevator");
					int pos = stopQueue.indexOf(currFloor);
					stopQueue.remove(pos);
					gotOff = true;
				}
				else {
					j++;
				}
			}
			releaseSemaphores(currFloor);
		}
		return gotOff;
	}
	
	/* 
	 * Test if semQueue has an element whose floor state == currFloor
	 * If true, release the corresponding semaphore and remove that element
	 */
	private void releaseSemaphores(int currFloor) {		
		for(int i = 0; i < semQueue.size();) {
			TrackSem obj = semQueue.get(i);
			if(obj.getFloor()==currFloor) {	
				Semaphore targetSem = obj.getSemaphore();
				targetSem.release();	// Release associated semaphore
				semQueue.remove(i);		// Remove corresponding TrackSem instance record
			}
			else {
				i++;
			}
		}
	}
	
	/* 
	 * Move the elevator from floor N_floor to 1
	 */
	private void goDown() {
		for (int i = Driver.N_floors - 1; i > 1; i--) {
			Boolean open = false;
			try {
				Driver.buttonSem.acquire();
				threadMessage("Elevator is on floor " + i);
				if(getOn(i) || getOff(i)) {
					open = true;
				}
				Driver.buttonSem.release();
				if(open) {
					Thread.sleep(1000);
				}
				Thread.sleep(1000);	//Takes 1 second to travel between floors
			} catch (InterruptedException e) {
				System.out.println("Interrupt exception in method goDown(), class Elevator");
				return;
			}
		}		
	}
	
	/* 
	 * Move the elevator from floor 1 to N_floor
	 */
	private void goUp() {
		for (int i = 1; i <= Driver.N_floors; i++) {
			Boolean open = false;
			try {
				Driver.buttonSem.acquire();
				threadMessage("Elevator is on floor " + i);
				if(getOn(i) || getOff(i)) {
					open = true;
				}
				Driver.buttonSem.release();
				if(open) {
					Thread.sleep(1000);
				}
				Thread.sleep(1000);	//Takes 1 second to travel between floors
			} catch (InterruptedException e) {
				System.out.println("Interrupt exception in method goUp(), class Elevator");
				return;
			}
		}		
	}
	
	public void run() {
		while(Driver.startElevator.availablePermits()!=Driver.N_people);	// Wait till all Person instances have decided what floors they want to initially go to
		
		while(true) {	// Keep moving the elevator up and down
			goUp();
			goDown();
		}
	}
}
