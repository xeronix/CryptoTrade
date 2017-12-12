import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TradeData {
	private TradeType type;
	private Currency currency;
	private BigDecimal volume;

	private BigDecimal pricePerUnit;

	private BigDecimal fees;
	private String date;
	private BigDecimal effectivePricePerUnit;

	// amount without fees
	private BigDecimal amount;

	// amount with fees
	private BigDecimal totalAmount;

	private BigDecimal feesPercentage;

	private long timestamp;

	public static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm");

	public TradeData(final String date, final Currency currency, final TradeType type, final BigDecimal volume,
			final BigDecimal pricePerUnit, final BigDecimal amount, final BigDecimal feesPercentage,
			final BigDecimal fees, final BigDecimal totalAmount) {
		this.type = type;
		this.currency = currency;
		this.volume = volume;
		this.pricePerUnit = pricePerUnit;
		this.fees = fees;
		this.date = date;
		this.amount = amount;
		this.feesPercentage = feesPercentage;
		this.totalAmount = totalAmount;

		Date dateOb = null;

		BigDecimal percentFactor = new BigDecimal("0.01");
		
		try {
			dateOb = sdf.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		setTimestamp(dateOb.getTime());

		if (fees.compareTo(amount.multiply(feesPercentage).multiply(percentFactor)) != 0) {
			throw new RuntimeException(
					"Insonsistent Trade Data :  Total fees paid does not match amount*feesPercentage. "
							+ this.toString());
		}

		if (type == TradeType.BUY) {
			if (volume.multiply(pricePerUnit).add(fees).compareTo(totalAmount) != 0) {
				throw new RuntimeException(
						"Insonsistent Trade Data :  Total amount paid does not match volume*pricePerUnit + fees. "
								+ this.toString());
			}

			this.effectivePricePerUnit = totalAmount.divide(volume, 6, BigDecimal.ROUND_HALF_UP);
		} else if (type == TradeType.SELL) {
			if (volume.multiply(pricePerUnit).subtract(fees).compareTo(totalAmount) != 0) {
				throw new RuntimeException(
						"Insonsistent Trade Data :  Total amount recevied does not match volume*pricePerUnit - fees. "
								+ this.toString());
			}

			this.effectivePricePerUnit = totalAmount.divide(volume, 6, BigDecimal.ROUND_HALF_UP);
		}
	}

	public void setVolume(BigDecimal volume) {
		this.volume = volume;
	}

	public void setEffectivePricePerUnit(BigDecimal effectivePricePerUnit) {
		this.effectivePricePerUnit = effectivePricePerUnit;
	}

	public BigDecimal getEffectivePricePerUnit() {
		return effectivePricePerUnit;
	}

	public TradeType getType() {
		return type;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getVolume() {
		return volume;
	}

	public BigDecimal getPricePerUnit() {
		return pricePerUnit;
	}

	public BigDecimal getFees() {
		return fees;
	}

	public String getDate() {
		return date;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getTotalAmount(){
		return totalAmount;
	}
	
	@Override
	public String toString() {
		return "[" + "Date:" + date + ", Currency:" + currency + ", Type:" + type + ", Volume:" + volume
				+ ", PricePerUnit:" + pricePerUnit + ", Amount:" + amount + ", FeesPercentage:" + feesPercentage
				+ ", fees:" + fees + ", TotalAmount:" + totalAmount + "]";
	}
}
