package com.meissereconomics.corona;

import java.util.ArrayList;
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
	private final double r0;
	private double lockdownThreshold;

	public Population(String name, long seed, int size, double r0, double lockdown, boolean network) {
		this.name = name;
		this.r0 = r0;
		this.lockdownThreshold = lockdown;
		this.rand = new Random(seed);
		this.hosts = new ArrayList<Host>();
		this.network = network;
		createPowerLawDistributedHosts(r0 * size, size);
		for (int j = 0; j < INITIAL_INFECTIONS; j++) {
			Host random = getRandomHost();
			random.infect();
			random.tick();
		}
	}

	private void createPowerLawDistributedHosts(double contacts, int size) {
		if (size < 100) {
			for (int i = 0; i < size; i++) {
				this.hosts.add(new Host(contacts / size));
			}
		} else {
			int superspreaders = size / POWER_LAW;
			int normies = size - superspreaders;
			double contactsForNormies = contacts / POWER_LAW;
			double contactsForSS = contacts - contactsForNormies;
			createPowerLawDistributedHosts(contactsForSS, superspreaders);
			createPowerLawDistributedHosts(contactsForNormies, normies);
		}
	}

	/**
	 * Calculates the average aggregate "R", or the number of people
	 * the average host is expected to infect at average.
	 * 
	 * Since not every person is average and there is randomness involved,
	 * an R<1.0 does not suffices to prevent future outbreaks completely,
	 * but they will most likely be milder than the first outbreak.
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
		getRandomHost().infect();
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
		return hosts.get(rand.nextInt(hosts.size()));
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
		labels += "Exposed or Infectious" + postfix;
		labels += "Recovered" + postfix;
		labels += "R" + postfix;
		return labels;
	}

}
