import java.util.Arrays;
import java.util.Random;

import com.navigation.GPSData;

public class CyclicBufferTest {
	
	private static int lastCurrents[] = new int[3];
	private static int lastCurrentsIndex = 0;
	
	public static void main(String[] args) {
		Random r = new Random();
		
		int current = 0;
		
		for(int i = 0; i < 10; i++) {
			int receivedData = r.nextInt() % 10 + 1;
			
			int effectiveIndex = lastCurrentsIndex % 3;
			lastCurrentsIndex++;
			lastCurrents[effectiveIndex] = receivedData;
			
			System.out.println("Index: " + lastCurrentsIndex + " Effective index: " + effectiveIndex);
			System.out.println("Content" + Arrays.toString(lastCurrents));
			
			if(lastCurrentsIndex < 3) {
				current = receivedData; // zastap aktualna pozycje daną z portu szeregowego
				System.out.println("Current: " + current);
			} else {
				current = average(lastCurrents);
				System.out.println("Average current: " + current);
			}
		}
	}
	
	private static int average(int[] lastCurrents) {
		int avg = 0;
		
		for(int lastCurrent : lastCurrents)
			avg += lastCurrent;

		avg /= lastCurrents.length;
		
		return avg;
	}
}
