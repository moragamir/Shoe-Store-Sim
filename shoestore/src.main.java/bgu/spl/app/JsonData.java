package bgu.spl.app;

import com.google.gson.annotations.SerializedName;

public class JsonData {
	
	@SerializedName("initialStorage")
		public ShoeStorageInfo[] initialStorage;
	@SerializedName("amount")
		private int amountOnStorage;

	@SerializedName("services")
		public JsonService services;

	public class JsonService{

		public WebsiteClientService[] customers;
		public ManagementService manager;
		public TimeService time;
		public int factories;
		public int sellers;
	}
	
	

}

