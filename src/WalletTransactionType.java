
public enum WalletTransactionType {
	DEPOSIT,
	WITHDRAWAL;
	
	public static WalletTransactionType getType(String type) {
		switch(type) {
			case "deposit": return DEPOSIT;
			case "withdrawal": return WITHDRAWAL;
		}
		
		return null;
	}
}
