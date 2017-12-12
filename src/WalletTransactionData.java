import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WalletTransactionData {
	private WalletTransactionType type;
	private Currency currency;

	private BigDecimal fees;
	private String date;

	// amount without fees
	private BigDecimal amount;

	// amount with fees
	private BigDecimal totalAmount;

	private long timestamp;

	private Source source;

	public static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm");

	public WalletTransactionData(final String date, final Currency currency, final WalletTransactionType type, final Source source,
			final BigDecimal amount, final BigDecimal fees, final BigDecimal totalAmount) {
		this.type = type;
		this.currency = currency;

		this.fees = fees;
		this.date = date;
		this.amount = amount;
		this.totalAmount = totalAmount;
		this.source = source;
		Date dateOb = null;

		try {
			dateOb = sdf.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		setTimestamp(dateOb.getTime());

		if (type == WalletTransactionType.DEPOSIT) {
			if (totalAmount.compareTo(amount.add(fees)) != 0) {
				throw new RuntimeException(
						"Insonsistent Trade Data :  Total amount deposited does not match amount+fees. "
								+ this.toString());
			}
		} else if (type == WalletTransactionType.WITHDRAWAL) {
			if (totalAmount.compareTo(amount.subtract(fees)) != 0) {
				throw new RuntimeException(
						"Insonsistent Trade Data :  Total amount deposited does not match amount-fees. "
								+ this.toString());
			}
		} else {
			throw new RuntimeException(
					"Inconsistent Trade Data : Wallet Data can be only of type DEPOSIT/WITHDRAWAL." + this.toString());
		}
	}

	public BigDecimal getAmount() {
		return amount;
	}
	
	public Currency getCurrency() {
		return currency;
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

	public Source getSource() {
		return source;
	}
	
	public WalletTransactionType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return "[" + "Date:" + date + ", Currency:" + currency + ", Type:" + type + ", Source:" + source + ", Amount:"
				+ amount + ", fees:" + fees + ", TotalAmount:" + totalAmount + "]";
	}
}
