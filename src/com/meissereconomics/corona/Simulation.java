package com.meissereconomics.corona;

import java.util.ArrayList;

public class Simulation {

	public static final int SIZE = 1000000;

	private ArrayList<Population> models;

	public Simulation(long seed, double lockdownThreshold) {
		this.models = new ArrayList<Population>();
		this.models.add(new Population("traditional", seed, SIZE, 3.0, lockdownThreshold, false));
		this.models.add(new Population("network", seed, SIZE, 3.0, lockdownThreshold, true));
		this.models.add(new Population("network adjusted r0", seed, SIZE, 2.0, lockdownThreshold, true));
	}

	public void run() {
		int day = 0;
		System.out.print("Day\t");
		for (Population model : models) {
			System.out.print(model.labels());
		}
		System.out.println();
		printState(day);
		while (hasSick() && day < 500) {
			day++;
			tick();
			printState(day);
		}
	}

	private void tick() {
		for (Population m : models) {
			m.tick();
		}
	}

	private boolean hasSick() {
		for (Population m : models) {
			if (m.hasSick()) {
				return true;
			}
		}
		return false;
	}

	private void printState(int day) {
		System.out.print(day + "\t");
		for (Population model : models) {
			System.out.print(model + "\t");
		}
		System.out.println();
	}

	public static void main(String[] args) {
		long seed = 23;
//		double lockdownThreshold = 0.0; // set to 0.0 to disable lockdown and get the results for the first three charts
		double lockdownThreshold = 0.6; // use this line to simulate lockdown
		Simulation s1 = new Simulation(seed, lockdownThreshold);
		s1.run();
	}

}
