package domain;

import java.util.Random;

public enum Shelf {
	A1, A2, B1, B2, C1, C2, D1, D2, E1, E2, E3, F1, F2, F3;

	private static Random random = new Random();

	public static Shelf getRandomShelf() {
		return values()[random.nextInt(values().length)];
	}
}
