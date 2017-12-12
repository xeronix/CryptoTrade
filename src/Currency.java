public enum Currency {
	BITCOIN,
	BITCOINCASH,
	RIPPLE,
	ETHER,
	LITECOIN,
	INR;
	
	public static Currency getCurrency(String key) {
		switch(key) {
		case "BTC/INR" : return BITCOIN;
		case "BCH/INR" : return BITCOINCASH;
		case "XRP/INR" : return RIPPLE;
		case "ETH/INR" : return ETHER;
		case "LTC/INR" : return LITECOIN;
		}
		
		return null;
	}
}