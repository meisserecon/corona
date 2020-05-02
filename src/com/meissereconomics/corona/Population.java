package com.meissereconomics.corona;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;

public class Population implements IPopulation {

	public static final int POWER_LAW = 3;
	public static final int INCUBATION = 5;
	public static final int DURATION = 10;
	public static final int INITIAL_INFECTIONS = 5;

	private Random rand;

	private final String name;
	private final boolean network;
	private final ArrayList<Host> hosts;
	private final double[] index;
	private final double r0;
	private double lockdownThreshold;
	private double totWeight;

	public Population(String name, long seed, int size, double r0, double lockdown, boolean network) {
		this.name = name;
		this.r0 = r0;
		this.lockdownThreshold = lockdown;
		this.rand = new Random(seed);
		this.hosts = new ArrayList<Host>();
		this.network = network;
		createPowerLawDistributedHosts(r0 * size, size);
//		createGeometricallyDistributedHosts(1.0 / r0, size, 1);
		this.index = new double[hosts.size()];
		this.totWeight = 0.0;
		int pos = 0;
		for (Host h : hosts) {
			totWeight += h.getInteractionsPerPeriod() * DURATION;
			index[pos++] = totWeight;
		}
//		System.out.println(totWeight / hosts.size() + " weight per host");
		for (int j = 0; j < INITIAL_INFECTIONS; j++) {
			Host random = getRandomHost();
			random.infect();
			random.tick();
		}
	}

	private void createGeometricallyDistributedHosts(double p, int size, double contacts) {
		if (size == 1) {
//			System.out.println("Creating " + 1 + " hosts with " + contacts);
			this.hosts.add(new Host(contacts));
		} else {
			int number = (int) Math.round(p * size);
			int next = size - number;
//			System.out.println("Creating " + number + " hosts with " + contacts);
			for (int i = 0; i < number; i++) {
				this.hosts.add(new Host(contacts));
			}
			createGeometricallyDistributedHosts(p, next, contacts + 1);
		}
	}

	private void createPowerLawDistributedHosts(double contacts, int size) {
		double average = contacts / size;
		double alpha = average / (average - 1);
		for (int i = 0; i < size; i++) {
			double uniform = rand.nextDouble();
			double x = 1.0 / Math.pow(1.0 - uniform, 1.0 / alpha);
			this.hosts.add(new Host(x / 2.0));
		}
	}

	/**
	 * Calculates the average aggregate "R", or the number of people the average host is expected to infect at average.
	 * 
	 * Since not every person is average and there is randomness involved, an R<1.0 does not suffices to prevent future outbreaks completely, but they will most likely be milder than the first
	 * outbreak.
	 */
	private double calculateR() {
		double susceptibleInteractions = 0.0;
		double totalInteractions = 0.0;
		for (Host h : hosts) {
			double interactions = h.getInteractionsPerPeriod();
			if (h.isSusceptible()) {
				susceptibleInteractions += interactions;
			}
			totalInteractions += interactions;
		}
		return susceptibleInteractions / totalInteractions * r0;
	}

	private int lockdown = -1;

	public void tick() {
		if (lockdownThreshold > 0.0) {
			getRandomHost().infect();
		}
		considerLockdown();
		if (isLockdown()) {
			lockdown--;
		} else {
			if (network) {
				for (Host host : hosts) {
					host.spread(this);
				}
			} else {
				double sick = count(h -> h.isSick()) * r0 / DURATION;
				for (int i = 0; i < sick; i++) {
					getRandomHost().infect();
				}
			}
		}
		for (Host host : hosts) {
			host.tick();
		}
	}

	private boolean isLockdown() {
		return lockdown > 0;
	}

	private void considerLockdown() {
		if (lockdown == -1 && calculateR() < lockdownThreshold) {
			lockdown = DURATION + INCUBATION;
		}
	}

	@Override
	public Host getRandomHost() {
		if (network) {
			double random = rand.nextDouble() * totWeight;
			int index = Arrays.binarySearch(this.index, random);
			if (index < 0) {
				index = -index - 1;
			}
			return hosts.get(index);
		} else {
			return hosts.get(rand.nextInt(hosts.size()));
		}
	}

	public int count(Predicate<Host> pred) {
		int immune = 0;
		for (Host h : hosts) {
			if (pred.test(h)) {
				immune++;
			}
		}
		return immune;
	}

	public int countImmune() {
		return count(h -> h.isImmune());
	}

	@Override
	public boolean hasContact(double probability) {
		return rand.nextDouble() <= probability;
	}

	@Override
	public String toString() {
		int immune = count(h -> h.isImmune());
		int sick = count(h -> h.isSick());
		return hosts.size() - immune - sick + "\t" + sick + "\t" + immune + "\t" + calculateR();
	}

	public boolean hasSick() {
		for (Host h : hosts) {
			if (h.isSick()) {
				return true;
			}
		}
		return false;
	}

	public String labels() {
		String postfix = " (" + name + ")\t";
		String labels = "Susceptible" + postfix;
		labels += "Infected" + postfix; // Exposed or Infectious
		labels += "Recovered" + postfix;
		labels += "R" + postfix;
		return labels;
	}

	public static void main(String[] args) {
		Population pop = new Population("test", 17l, 1000, 3.0, 0.0, true);
	}

}
