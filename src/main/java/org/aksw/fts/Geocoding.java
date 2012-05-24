package org.aksw.fts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.gson.Gson;

public class Geocoding {
	private static final Logger logger = LoggerFactory.getLogger(Geocoding.class);
	
	public static void main(String[] args) throws Exception {
		
		// Quick test how good it works
		File source = new File("google-geocode-requests.txt");
		File target = new File("google-geocode-responses.txt");
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
		PrintStream out = new PrintStream(target);
		
		final Geocoder geocoder = new Geocoder();
		Gson gson = new Gson();

		String line;
		try {
			int i = 0;
			while((line = reader.readLine()) != null) {
				line = line.trim();				
				if(line.isEmpty()) {
					continue;
				}
				
				++i;
				System.out.println("Processing line " + i + ": " + line);
				
				
				if(i % 2450 == 0) {
					System.out.println("Continuing tomorrow...");
					Thread.sleep(24 * 60 * 60 * 1000);
				}
				
				
				try {
					GeocoderRequest request = new GeocoderRequestBuilder().setAddress(line).setLanguage("en").getGeocoderRequest();
					GeocodeResponse response = geocoder.geocode(request);
					String json = gson.toJson(response);
					GeocodeResponse roundtrip = gson.fromJson(json, GeocodeResponse.class);
		
					if(!response.equals(roundtrip)) {
						logger.error("Roundtrip failed for: " + line);
					}
					
					out.println(line + "\t" + json);
					
					
				} catch(Exception e) {
					logger.error("An exception occured", e);
				}			
				
				
				Thread.sleep(1000);
			}
		} finally {
			out.flush();
			out.close();
		}
		
		
		
		 
		// convert java object to JSON format,
		// and returned as JSON formatted string
		
		//GeocodeResponse test = new GeocodeResponse();
		/*
		GeocodeResponse roundtrip = gson.fromJson(json, GeocodeResponse.class);
		System.out.println(response);
		System.out.println(roundtrip);
		System.out.println(json);
		*/		
	}
}
