package com.navigation.algorithm;

public enum Command {
	V("V"), // predkosc (w kmh)
	T("T"); // skret
	
	private final String mnemonic;
	
	private Command(String mnemonic) {
		this.mnemonic = mnemonic;
	}
	
	public String getMemonic() {
		return mnemonic;
	}
}
