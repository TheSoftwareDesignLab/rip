package oldModel;

public class OldContext {
	
	private double cpu;
	private double memory;
	private int battery;
	private double temperature;
	private boolean wifi;
	private boolean airplaneMode;
	
	public double getCpu() {
		return cpu;
	}
	public void setCpu(double cpu) {
		this.cpu = cpu;
	}
	public double getMemory() {
		return memory;
	}
	public void setMemory(double memory) {
		this.memory = memory;
	}
	public int getBattery() {
		return battery;
	}
	public void setBattery(int battery) {
		this.battery = battery;
	}
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	public boolean isWifi() {
		return wifi;
	}
	public void setWifi(boolean wifi) {
		this.wifi = wifi;
	}
	public boolean isAirplaneMode() {
		return airplaneMode;
	}
	public void setAirplaneMode(boolean airplaneMode) {
		this.airplaneMode = airplaneMode;
	}
}
