package main.java.mpinspector;

/*
 * discard
 */
public class Test {
	 public static void main( String[] args ) {
		transfer(23570.481);
		transfer(8984.854);
		transfer(19917.282);
		transfer(34709.788);
		transfer(27986.133);
//		transfer(36155.346);
//		transfer(80505.046);
		transfer(1970.755);
		transfer(13664.141);
		transfer(14843.213);
		transfer(17593.363);
		transfer(2022.947);
		transfer(18671.264);
		//transfer(11609.89);
		//transfer(15069.478);
		//transfer(57747.735);
		transfer(216369);
		transfer(16643.769);
		transfer(80505.046);
		transfer(34709.788);
		transfer((80505.046+34709.788)/2);
	 }
	 
	 static void transfer(double d) {
		// time

		 int HH = (int) (d /  3600);

		 // minute

		 int mm = (int) (d %  3600 / 60);

		 // second

		 int SS = (int) (d %  60);
		 
		 System.out.println(HH + ":"+ mm);
	 }
}
