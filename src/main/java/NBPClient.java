import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;


public class NBPClient {

	//Funkcja pobierajaca notowania kursu kupna i sprzedazy z JSON 
	private static void displayCurrency(JSONObject obj) {
		JSONArray str = obj.getJSONArray("rates");
		
		if(obj.getString("table").equals("C")) {
			for(int i = 0; i < str.length(); i++) {
				JSONObject rate = str.getJSONObject(i);
				String date = rate.getString("effectiveDate");
				double buy = rate.getDouble("bid");
				double sell = rate.getDouble("ask");
				
				if(i == 0) {
					display(date, buy, 0, sell, 0);
				}else {
					JSONObject rate2 = str.getJSONObject(i-1);
					double buy2 = rate2.getDouble("bid");
					double sell2 = rate2.getDouble("ask");
					display(date, buy, buy - buy2, sell, sell - sell2);
				}
				
			}
		}else { System.out.println("Format tabeli nie obs³ugiwany.");}
	}
	
	//Funkcja wyswietlajaca notowania kursow i roznice miedzy kursami w konsoli
	public static void display(String date, double buy, double diff_buy, double sell, double diff_sell) {
		DecimalFormat df = new DecimalFormat("0.00");
		String display = " ";
		if(diff_buy < 0)
			display = "";
		
		System.out.println(date + "   kurs kupna: " + df.format(buy) + " ró¿nica: " + display + df.format(diff_buy) + "   kurs sprzeda¿y " + df.format(sell) + " ró¿nica: " + display + df.format(diff_sell));
	}
	
	//Funkcja pobierajaca poczatkowa date od uzytkownika
	public static LocalDate getStartDate() {
		Scanner scan = new Scanner(System.in);
		System.out.println("Program wyœwietlaj¹cy notowania kursu kupna i sprzeda¿y USD od podanej daty do bie¿¹cej daty.");
		System.out.println("Oraz ro¿nicê wartoœci kursu kupna i sprzeda¿y pomiêdzy dniem obecnym i dniem poprzedzaj¹cym.\n");
		System.out.println("Wczytywanie daty pocz¹tkowej...\nPodaj rok: ");
		int year = scan.nextInt();
		System.out.println("Podaj miesi¹c:");
		int month = scan.nextInt();
		System.out.println("Podaj dzieñ:");
		int day = scan.nextInt();
		scan.close();
		LocalDate date = LocalDate.of(year, month, day);
		return date;
	}
	
	//Funkcja przetwarzajaca zapytanie url do uslugi sieciowej NBPapi z wczytana data z ominieciem limitu 367 dni na jedno zapytanie
	public static void parseUrl() {
		LocalDate startDate = getStartDate();
		LocalDate endDate = LocalDate.now();
		DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		if(startDate.isBefore(endDate)) {
			System.out.println("\nWyœwietlanie...\n");
			
			if(startDate.getYear() == endDate.getYear()) {
				nbpApiConnection(sdf.format(startDate), sdf.format(endDate));
			}else if(startDate.getYear() < endDate.getYear()) {
				int startYear = startDate.getYear();
				int endYear = endDate.getYear();
				for(int year = startYear; year <= endYear; year++) {
					LocalDate tmpEndDate = LocalDate.of(year, 12, 31);
					LocalDate tmpStartDate = LocalDate.of(year, 01, 01);
					if(year == startYear) {
						nbpApiConnection(sdf.format(startDate), sdf.format(tmpEndDate));
					}else if(year == endYear){
						nbpApiConnection(sdf.format(tmpStartDate), sdf.format(endDate));
					}else {
						nbpApiConnection(sdf.format(tmpStartDate), sdf.format(tmpEndDate));
					}
				}
			}
		}else {
			System.out.println("Notowania kursu USD od " + sdf.format(startDate) + " do " + sdf.format(endDate));
			System.out.println("B³¹d. Data pocz¹tkowa wiêksza od bie¿¹cej daty.");
		}
	}
	
	//Funkcja do nawiazania polaczenia z usluga sieciowa NBPapi
	private static void nbpApiConnection(String startDate, String endDate) {
		Client c = Client.create();
		String url = "http://api.nbp.pl/api/exchangerates/rates/c/usd/" + startDate + "/" + endDate;
		WebResource handler = c.resource(url);
		ClientResponse message = handler.accept("application/json").get(ClientResponse.class);				
		
		if(message.getStatus() != 200) {
			throw new RuntimeException("Error message from api NBP: " + message.getStatus());
		}
		
		String query = message.getEntity(String.class);
		displayCurrency(new JSONObject(query));
	}
	
	public static void main(String[] args) {
		try {
			parseUrl();
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
