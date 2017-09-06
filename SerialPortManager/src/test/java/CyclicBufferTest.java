import java.util.Arrays;
import java.util.Random;

public class CyclicBufferTest {
	
	private static float lastCurrents[] = new float[3];
	private static int lastCurrentsIndex = 0;
	
	public static void main(String[] args) {
		Random r = new Random();
		
		float current = 0;
		
		for(int i = 0; i < 10; i++) {
			float receivedData = r.nextFloat() * 10;
			
			int effectiveIndex = lastCurrentsIndex % 3;
			lastCurrentsIndex++;
			lastCurrents[effectiveIndex] = receivedData;
			
			System.out.println("Received data: " + receivedData + " Index: " + lastCurrentsIndex + " Effective index: " + effectiveIndex);
			System.out.println("Content" + Arrays.toString(lastCurrents));
			
			if(lastCurrentsIndex < 3) {
				current = receivedData; // zastap aktualna pozycje daną z portu szeregowego
				System.out.println("Current: " + current);
			} else {
				current = average(lastCurrents);
				System.out.println("Average current: " + current);
			}
			
			System.out.println();
		}
	}
	
	private static float average(float[] lastCurrents) {
		float avg = 0;
		
		for(float lastCurrent : lastCurrents)
			avg += lastCurrent;

		avg /= lastCurrents.length;
		
		return avg;
	}
}
