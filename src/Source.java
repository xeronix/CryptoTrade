
public enum Source {
	USER,
	REFERRAL,
	TRADE_REWARD;
	
	public static Source getSource(String source) {
		switch(source) {
		case "user": return USER;
		case "referrer_refund": return REFERRAL;
		case "trade_reward": return TRADE_REWARD;
		}
		
		return null;
	}
}
