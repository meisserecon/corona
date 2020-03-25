package com.meissereconomics.corona;

public class Host {

	private static final int INCUBATION = Population.INCUBATION;
	private static final int DURATION = Population.DURATION;

	private int sick;
	private boolean immune;
	private double contactsPerPeriod;

	public Host(double contacts) {
		this.immune = false;
		this.sick = 0;
		this.contactsPerPeriod = contacts / DURATION / 2.0;
	}

	public double getInteractionsPerPeriod() {
		return contactsPerPeriod;
	}
	
	public void spread(IPopulation pop) {
		if (isSusceptible() || isInfectuous()) {
			double contacts = this.contactsPerPeriod;
			while (contacts >= 1.0) {
				contact(pop.getRandomHost());
				contacts -= 1.0;
			}
			if (pop.hasContact(contacts)) {
				contact(pop.getRandomHost());
			}
		}
	}

	private boolean isInfectuous() {
		return isSick() && sick <= DURATION;
	}

	public boolean isImmune() {
		return immune;
	}

	private void contact(Host other) {
		if (isInfectuous()) {
			other.infect();
		} else if (other.isSick()) {
			this.infect();
		}
	}

	public void infect() {
		if (isSusceptible()) {
			this.sick = -(DURATION + INCUBATION);
		}
	}

	public void tick() {
		if (sick > 0) {
			sick--;
			if (sick == 0) {
				immune = true;
			}
		} else if (sick < 0) {
			sick = -sick;
		}
	}

	public boolean isSusceptible() {
		return !isImmune() && !isSick();
	}

	public boolean isSick() {
		return sick > 0;
	}

}
