package model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class HybridError {
	
	private LocalDateTime currentTime;
	private String description;
	private String type;
	
	public HybridError(String description, String type) {
		
		currentTime = LocalDateTime.now();
		this.description = description;
		this.type = type;
		
	}

}
