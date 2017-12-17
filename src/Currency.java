import java.math.BigDecimal;

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
		
		throw new RuntimeException("Inconsistent Wallet Data : Invalid Currency Key: " + key);
	}
	
	public static BigDecimal getPrice(String currencyName) {
		switch(currencyName) {
		case "BITCOIN":
			return bitcoinPrice;
		case "BITCOINCASH":
			return bitcoinCashPrice;
		case "RIPPLE":
			return ripplePrice;
		case "ETHER":
			return etherPrice;
		case "LITECOIN":
			return liteCoinPrice;
		}
		
		throw new RuntimeException("Inconsistent Wallet Data : Invalid Currency Name: " + currencyName);
	}
	
	public static BigDecimal bitcoinPrice;
	public static BigDecimal bitcoinCashPrice;
	public static BigDecimal etherPrice;
	public static BigDecimal ripplePrice;
	public static BigDecimal liteCoinPrice;
}