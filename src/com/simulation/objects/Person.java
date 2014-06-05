package com.simulation.objects;

import java.util.*;
import java.util.concurrent.Semaphore;

public class Person implements Runnable {
	private static Elevator elevator;
	private Semaphore destReached;	// Semaphore to block the Person instance from choosing another floor after it has already pressed the toButton
	private int onFloor;
	private int toFloor;
	private Boolean init = true;

	public Person(Elevator el) {
		onFloor = 1;	//Initially everyone gets on at 1st level
		elevator = el;
		destReached = new Semaphore(1, true);
		Driver.startElevator.acquireUninterruptibly();
	}			

	public void run() {
		try {
			Random ranGen = new Random();
			while(true) {
				if(init) {	// Person instance just reached the building and is waiting to get inside the elevator
					//Randomly choose a floor to go to, except 1
					toFloor = 1 + ranGen.nextInt(Driver.N_floors);
					while(toFloor == 1) {
						toFloor = 1 + ranGen.nextInt(Driver.N_floors);
					}
					
					Driver.buttonSem.acquire();
					elevator.callButton(onFloor);
					elevator.toButton(toFloor, destReached);					
					Driver.startElevator.release();		//Allow the elevator thread to start
					Driver.buttonSem.release();
					
					init = false;
				} else {
					if(destReached.availablePermits() == 1) {	// Person instance reached its previous target floor
						onFloor = toFloor;	// Update onFloor state
						
						int wander = 1 + ranGen.nextInt(Driver.maxWander)*1000;
						Thread.sleep(wander);	//Wander on the floor for x time
						
						toFloor = 1 + ranGen.nextInt(Driver.N_floors);	//Randomly choose a floor to go to

						Driver.buttonSem.acquire();
						elevator.callButton(onFloor);
						elevator.toButton(toFloor, destReached);
						Driver.buttonSem.release();
					}
				}
			}
		} catch (InterruptedException e) {
			return;
		}
	}
}
