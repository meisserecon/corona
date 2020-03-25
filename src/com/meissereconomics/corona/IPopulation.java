package com.meissereconomics.corona;

public interface IPopulation {

	Host getRandomHost();

	boolean hasContact(double probability);

}
