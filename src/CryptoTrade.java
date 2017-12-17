import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

public class CryptoTrade {
	private static String tradingDataFilePath = System.getenv("KOINEX_TRADE_DATA_FILE_PATH");//"C:\\Users\\vmehta\\Dropbox\\Vipul\\Koinex_Trade.csv";
	private static String walletDataFilePath = System.getenv("KOINEX_WALLET_DATA_FILE_PATH");//"C:\\Users\\vmehta\\Dropbox\\Vipul\\Koinex_Wallet.csv";

	// currency -> tradingData map
	private final static Map<String, List<TradeData>> tradeDataMap = new HashMap<String, List<TradeData>>();
	private final static Map<String, List<WalletTransactionData>> walletTransactionDataMap = new HashMap<String, List<WalletTransactionData>>();

	private static BigDecimal targetProfitPercentage;
	
	private static String tradingStartDate = System.getenv("KOINEX_TRADE_START_DATE"); ;//"23-10-2017 00:00";
	
	private final static BigDecimal daysInTrade;
	
	private static BigDecimal totalSellingProfit = BigDecimal.ZERO;
	private static BigDecimal totalHoldingProfit = BigDecimal.ZERO;
	private static BigDecimal totalDeposit = BigDecimal.ZERO;
	private static BigDecimal totalWithdrawal = BigDecimal.ZERO;
	private static BigDecimal totalReferralEarning = BigDecimal.ZERO;
	private static BigDecimal totalInvestment = BigDecimal.ZERO;
	private static BigDecimal totalMarketValue = BigDecimal.ZERO;
	private static BigDecimal totalTradeReward = BigDecimal.ZERO;

	private static final DecimalFormat precision = new DecimalFormat("#0.0000");
	
	static {
		if (tradingDataFilePath == null) {
			tradingDataFilePath = "C:\\Users\\vmehta\\Dropbox\\Vipul\\Koinex_Trade.csv";
		}
		
		if (walletDataFilePath == null) {
			walletDataFilePath = "C:\\Users\\vmehta\\Dropbox\\Vipul\\Koinex_Wallet.csv";
		}
		
		if (tradingStartDate == null) {
			tradingStartDate = "23-10-2017 00:00";
		}
		
		String targetProfitPercentageString = System.getenv("KOINEX_TARGET_PROFIT");
		
		if (targetProfitPercentageString == null) {
			targetProfitPercentage = new BigDecimal("20");
		} else {
			targetProfitPercentage = new BigDecimal(targetProfitPercentageString);
		}
		
		Date dateOb = null;
		
		try {
			dateOb = TradeData.sdf.parse(tradingStartDate);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		long timePastTradeStartDate = System.currentTimeMillis() - dateOb.getTime();
		BigDecimal startTime = new BigDecimal(timePastTradeStartDate);
		BigDecimal secondsInDay = new BigDecimal(86400000);
		
		daysInTrade = startTime.divide(secondsInDay, 10, BigDecimal.ROUND_HALF_UP);
	}
	
	public static void main(String[] args) throws Exception {
		fetchKoinexData();
		
		fetchTradingData();
		analyzeTradingData();

		fetchWalletTransactionData();
		analyzeWalletTransactionData();
		
		System.out.println("Total Amount Deposited: INR:" + precision.format(totalDeposit));
		System.out.println("Total Amount Withdrawn: INR:" + precision.format(totalWithdrawal));
		System.out.println("Total Referral Earning: INR:" + precision.format(totalReferralEarning));
		System.out.println("Total Trade Reward: INR:" + precision.format(totalTradeReward));
		System.out.println("Total Selling Profit: INR: " + precision.format(totalSellingProfit));
		System.out.println("Total Holding Profit: INR:" + totalHoldingProfit);

		BigDecimal totalProfit = totalSellingProfit.add(totalTradeReward).add(totalReferralEarning);
		BigDecimal totalNetProfit = totalProfit.add(totalHoldingProfit);
		System.out.println("Total Net Profit = Total Selling Profit + Total Holding Profit: INR:"
				+ precision.format(totalNetProfit));
		
		System.out.println("Total Current Investment: INR:" + precision.format(totalInvestment));
		System.out.println("Total Market Value of Current Investment: INR:" + precision.format(totalMarketValue));
		
		BigDecimal totalWalletMoney = totalDeposit.subtract(totalWithdrawal).add(totalProfit).subtract(totalInvestment);
		
		System.out.println("Total Unused Wallet Money: INR:" + precision.format(totalWalletMoney));
		
		System.out.println("Total Amount Deposited + Total Amount Withdrawn + Total Net Profit = INR:"
				+ precision.format(totalDeposit.subtract(totalWithdrawal).add(totalNetProfit)));
		
		// double returnRate = totalNetProfit / totalInvestment * 100.0D;
		BigDecimal returnRate = totalNetProfit.divide(totalDeposit, 6, BigDecimal.ROUND_HALF_UP)
				.multiply(new BigDecimal("100"));
		
		System.out.println("Absolute Return : " + returnRate + "%");
		System.out.println("Estimated Yearly Return Without Compounding Factor: "
				+ (returnRate.divide(daysInTrade, 6, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("365"))).toString() + "%");
	}
	
	private static void fetchKoinexData() throws IOException {
		final String koinexUrl = "https://koinex.in/api/ticker";
		URL url =  new URL(koinexUrl);
		
		HttpURLConnection conn = null; 
		BufferedReader br = null;
		String jsonData = "";

		try {
			conn = (HttpURLConnection) url.openConnection();
			br =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			
			while ((line = br.readLine()) != null) {
				jsonData += line;
			} 
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			
			if (br != null) {
				br.close();
			}
		}
		
		if (jsonData.length() == 0) {
			throw new RuntimeException("Failed to fetch data from Koinex URL: " + koinexUrl);
		}
		
		JSONObject json = new JSONObject(jsonData);
		
		final String priceKey = "prices";
		
		JSONObject priceObject = json.getJSONObject(priceKey);
		
		Currency.bitcoinPrice = new BigDecimal(priceObject.getString("BTC"));
		Currency.bitcoinCashPrice = new BigDecimal(priceObject.getString("BCH"));
		Currency.etherPrice = new BigDecimal(priceObject.getString("ETH"));
		Currency.ripplePrice = new BigDecimal(priceObject.getString("XRP"));
		Currency.liteCoinPrice = new BigDecimal(priceObject.getString("LTC"));
	}
	
	private static void fetchWalletTransactionData() throws IOException {
		File file = new File(walletDataFilePath);
		
		String line = "";
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			
			//first line of csv file gives column names -> Timestamp,Currency,Type,Generated By,Status,Amount,Fees,Total Amount
			br.readLine(); 

			while ((line = br.readLine()) != null) {
				String[] lineSplits = line.split(",");
				final String date = lineSplits[0].trim();
				final String currency = lineSplits[1].trim();
				final String type = lineSplits[2].trim();
				final String source = lineSplits[3].trim();
				final String status = lineSplits[4].trim();
				
				if (!status.equalsIgnoreCase("completed")) {
					throw new RuntimeException("Failed to process wallet transaction data as one of the transaction is not in completed state.");
				}
				
				final BigDecimal amount = new BigDecimal(lineSplits[5].trim());
				final BigDecimal fees = new BigDecimal(lineSplits[6].trim());
				final BigDecimal totalAmount = new BigDecimal(lineSplits[7].trim());

				final WalletTransactionType transactionType = WalletTransactionType.getType(type);
				final Source sourceVal = Source.getSource(source);
				
				WalletTransactionData data = new WalletTransactionData(date, Currency.valueOf(currency),
						transactionType, sourceVal, amount, fees, totalAmount);

				if (walletTransactionDataMap.get(currency) == null) {
					walletTransactionDataMap.put(currency, new ArrayList<WalletTransactionData>());
				}

				walletTransactionDataMap.get(currency).add(data);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}		
	}
	
	private static void fetchTradingData() throws IOException {
		File file = new File(tradingDataFilePath);
		
		String line = "";
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			
			//first line of csv file gives column names -> Timestamp,Pair,Type,Quantity,Price per unit,Amount,Fees Percentage,Fees,Total Amount
			br.readLine(); 

			while ((line = br.readLine()) != null) {
				String[] lineSplits = line.split(",");
				final String date = lineSplits[0].trim();
				final String currency = lineSplits[1].trim();
				final String type = lineSplits[2].trim();
				final BigDecimal volume = new BigDecimal(lineSplits[3].trim());
				final BigDecimal pricePerUnit = new BigDecimal(lineSplits[4].trim());
				final BigDecimal amount = new BigDecimal(lineSplits[5].trim());
				final BigDecimal feesPercentage = new BigDecimal(lineSplits[6].trim());
				final BigDecimal fees = new BigDecimal(lineSplits[7].trim());
				final BigDecimal totalAmount = new BigDecimal(lineSplits[8].trim());
				Currency currencyVal  = Currency.getCurrency(currency);
				
				TradeData data = new TradeData(date, currencyVal, TradeType.valueOf(type), volume,
						pricePerUnit, amount, feesPercentage, fees, totalAmount);

				if (tradeDataMap.get(currencyVal.name()) == null) {
					tradeDataMap.put(currencyVal.name(), new ArrayList<TradeData>());
				}

				tradeDataMap.get(currencyVal.name()).add(data);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}		
	}

	/**
	 * Volume of any sell order is subtracted from a buy order having maximum
	 * effective price per unit
	 */
	private static void analyzeTradingData() {
		Iterator<Entry<String, List<TradeData>>> it = tradeDataMap.entrySet().iterator();
		DecimalFormat precision = new DecimalFormat("#0.0000");

		while (it.hasNext()) {
			Entry<String, List<TradeData>> entry = it.next();
			String currency = entry.getKey();
			System.out.println("<" + currency + ">:");

			BigDecimal volume = BigDecimal.ZERO;
			BigDecimal effectivePricePerUnit = null;
			BigDecimal profit = BigDecimal.ZERO;

			// process the data in increasing order of date
			List<TradeData> tradeDataList = entry.getValue();
			tradeDataList.sort(new Comparator<TradeData>() {

				@Override
				public int compare(TradeData o1, TradeData o2) {
					return o1.getTimestamp() < o2.getTimestamp() ? -1 : 1;
				}
			});
			
			for (TradeData data : tradeDataList) {
				if (!currency.equalsIgnoreCase(data.getCurrency().name())) {
					throw new RuntimeException("Inconsistent Trade Data : Currency name mismatch." + data.toString());
				}
				BigDecimal oldVolume = volume;

				if (data.getType() == TradeType.BUY) {
					volume = volume.add(data.getVolume());
					BigDecimal buyAmount = data.getTotalAmount();

					if (effectivePricePerUnit == null) {
						effectivePricePerUnit = buyAmount.divide(volume, 6, BigDecimal.ROUND_HALF_UP);
					} else {
						/*
						 * new effectivePricePerUnit =
						 * (effectivePricePerUnit*oldVolume +
						 * currentBuyTradeTotalAmount)/(new volume)
						 */
						effectivePricePerUnit = effectivePricePerUnit.multiply(oldVolume).add(data.getTotalAmount())
								.divide(volume, 6, BigDecimal.ROUND_HALF_UP);
					}
				} else if (data.getType() == TradeType.SELL) {
					BigDecimal volumeSold = data.getVolume();

					if (volume.compareTo(BigDecimal.ZERO) != 1) {
						throw new RuntimeException(
								"Inconsistent Trade Data : Volume Bought is less than volume sold. " + data.toString());
					}

					volume = volume.subtract(data.getVolume());

					BigDecimal soldAmount = data.getTotalAmount();
					/*
					 * profit = profit + currentSellTradeTotalAmount -
					 * volumeSold*effectivePricePerUnit
					 */
					profit = profit.add(soldAmount.subtract(volumeSold.multiply(effectivePricePerUnit)));
				}
			}

			BigDecimal currentPrice = Currency.getPrice(currency);
			System.out.println("Current Market Price : " + precision.format(currentPrice));

			System.out.println("Profit Earned By Selling Till Now: INR:" + precision.format(profit));
								
			if (volume.compareTo(BigDecimal.ZERO) == 0) {
				System.out.println("No Balance");
			} else {
				System.out.println("Balance Volume: " + volume);
				System.out.println("Effective Price Paid Per Unit For Balance Volume: INR:"
						+ precision.format(effectivePricePerUnit));
				
				BigDecimal effectiveInvestment = effectivePricePerUnit.multiply(volume);
				
				totalInvestment = totalInvestment.add(effectiveInvestment);
				
				System.out.println("Amount Paid For Balance Volume: INR:"
						+ precision.format(effectiveInvestment));
				
				BigDecimal currentMarketValue = currentPrice.multiply(volume);
				BigDecimal currentHoldingProfit = currentMarketValue.subtract(effectiveInvestment);

				totalMarketValue = totalMarketValue.add(currentMarketValue);
				
				System.out.println("Current Market Value of Volume: INR:" + currentMarketValue);

				System.out.println("Profit on Current Volume Held: INR: "+ precision.format(currentHoldingProfit));
				
				System.out.println("Net Profit = Profit Earned By Selling + Profit on Current Volume: INR: "
						+ precision.format(profit.add(currentHoldingProfit)));

				BigDecimal targetProfit = targetProfitPercentage.multiply(new BigDecimal("0.01")).multiply(effectiveInvestment);
						
				System.out.println("For Target Profit of " + targetProfitPercentage + "%, sell at "
						+ precision.format(targetProfit.add(effectiveInvestment).divide(volume, 4, BigDecimal.ROUND_HALF_UP)));
				
				totalHoldingProfit = totalHoldingProfit.add(currentHoldingProfit);
			}
			
			totalSellingProfit = totalSellingProfit.add(profit);
			
			System.out.println();
		}
	}

	private static void analyzeWalletTransactionData() {
		Iterator<Entry<String, List<WalletTransactionData>>> it = walletTransactionDataMap.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, List<WalletTransactionData>> entry = it.next();
			String currency = entry.getKey();
			System.out.println("<" + currency + ">:");

			for (WalletTransactionData data : entry.getValue()) {
				if (!currency.equalsIgnoreCase(data.getCurrency().name())) {
					throw new RuntimeException("Inconsistent Wallet Data : Currency name mismatch." + data.toString());
				}
				
				if (data.getType().equals(WalletTransactionType.DEPOSIT)) {					
					if (data.getSource().equals(Source.REFERRAL)) {
						totalReferralEarning = totalReferralEarning.add(data.getAmount());
					} else if (data.getSource().equals(Source.TRADE_REWARD)) {
						totalTradeReward = totalTradeReward.add(data.getAmount());
					} else{
						totalDeposit = totalDeposit.add(data.getAmount());
					}
				} else if (data.getType().equals(WalletTransactionType.WITHDRAWAL)) {
					if (!data.getSource().equals(Source.USER)) {
						throw new RuntimeException("Inconsistent Wallet Data :  Invalid source for withdrawal transaction. " + data.toString());
					}
					totalWithdrawal = totalWithdrawal.add(data.getAmount());
				} else {
					throw new RuntimeException("Inconsistent Wallet Data :  Invalid transaction type. " + data.toString());
				}
			}
		}		
	}
}
