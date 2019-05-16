package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.DonationServiceClient;
import com.servantscode.fakedata.client.FamilyServiceClient;
import com.servantscode.fakedata.client.FundServiceClient;
import com.servantscode.fakedata.client.PledgeServiceClient;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.servantscode.fakedata.generator.RandomSelector.rand;
import static com.servantscode.fakedata.generator.RandomSelector.select;

public class DonationGenerator {

    private static final DonationServiceClient donationClient = new DonationServiceClient();
    private static final PledgeServiceClient pledgeClient = new PledgeServiceClient();
    private static final FundServiceClient fundClient = new FundServiceClient();

    private static final List<String> PLEDGE_TYPES = Arrays.asList("EGIFT", "BASKET");
    private static final Map<String, List<String>> DONATION_TYPES;
    private static final List<String> PLEDGE_FREQ = Arrays.asList("WEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY");

    static {
        DONATION_TYPES = new HashMap<>();
        DONATION_TYPES.put("EGIFT", Arrays.asList("EGIFT", "CREDIT_CARD"));
        DONATION_TYPES.put("BASKET", Arrays.asList("CASH", "CHECK"));
    }

    private int missedDonations = 0;

    public DonationGenerator() {
    }

    public static void generate(int numberOfDonors) throws IOException {
        int generalFundId = 1; //In db setup
        int buildingFundId = generateBulidingFund();

        int familyCount = new FamilyServiceClient().getFamilyCount();
        int[] donors = RandomSelector.randomNumbers(familyCount, numberOfDonors);

        for(int i=0; i<numberOfDonors; i++) {
            int fundSelector = RandomSelector.rand.nextInt(100);
            if(fundSelector < 90)
                new DonationGenerator().generateDonationPattern(donors[i] + 1, generalFundId); // Never use 0
            if(fundSelector > 70)
                new DonationGenerator().generateDonationPattern(donors[i] + 1, buildingFundId); // Never use 0
        }
    }

    private static int generateBulidingFund() {
        Map<String, Object> fundData = new HashMap<>(4);
        fundData.put("name", "Building Fund");
        Map<String, Object> fund = fundClient.createFund(fundData);
        return (Integer)fund.get("id");
    }

    private void generateDonationPattern(int familyId, int fundId) {
        Map<String, Object> pledge = generatePledge(familyId, fundId);

        String freq = (String) pledge.get("pledgeFrequency");
        int payments = Math.round(1f*LocalDate.now().getDayOfYear()/365*getPayments(freq));

        if(rand.nextInt(100) > 90) // 10% of pledges are ignored
            return;

        for(int i=0; i<payments; i++) {
            generateDonation(pledge, i, fundId);

            if(rand.nextInt(100) > 99) // 1% of donations are someone's last one
                return;
        }
    }

    private Map<String, Object> generatePledge(int familyId, int fundId) {
        LocalDate now = LocalDate.now();

        String pledgeType = select(PLEDGE_TYPES);
        String pledgeFreq = select(PLEDGE_FREQ);
        float amount = (float) RandomSelector.rand.nextInt(1000);

        Map<String, Object> pledge = new HashMap<>();
        pledge.put("familyId", familyId);
        pledge.put("fundId", fundId);
        pledge.put("pledgeType", pledgeType);
        pledge.put("pledgeFrequency", pledgeFreq);
        pledge.put("pledgeAmount", amount);
        pledge.put("pledgeDate", now.minusMonths(now.getMonthValue() + 1));
        pledge.put("pledgeStart", LocalDate.now().withDayOfYear(1));
        pledge.put("pledgeEnd", LocalDate.now().withDayOfYear(1).plusYears(1));
        pledge.put("annualPledgeAmount", amount*getPayments(pledgeFreq));

        if(rand.nextInt(100) < 80) {// 20% of the time don't pledge, just give
            pledge = pledgeClient.createDonation(pledge);
            System.out.println("Created pledge for family: " + familyId);
        } else {
            System.out.println("Did not create pledge for family: " + familyId);
        }

        return pledge;
    }

    private void generateDonation(Map<String, Object> pledge, int paymentNumber, int fundId) {
        if(rand.nextInt(100) > 95) {
            missedDonations++;
            return;
        }

        String freq = (String) pledge.get("pledgeFrequency");
        String pledgeType = (String) pledge.get("pledgeType");
        LocalDate donationDate = getDonationDate(freq, pledgeType, paymentNumber);
        if(donationDate.isAfter(LocalDate.now()))
            return;

        float pledgeAmount = (float)pledge.get("pledgeAmount");
        String donationType = select(DONATION_TYPES.get(pledgeType));

        float amount = pledgeAmount;
        if(missedDonations > 0 && rand.nextInt(100) > 25)
            amount = pledgeAmount*(missedDonations+1);
        if(rand.nextInt(100) > 95)
            amount = (rand.nextFloat()+.5f)*amount;

        amount = (float) (Math.floor(amount*100f)/100f); //Round out any partial cents
        missedDonations = 0;

        Map<String, Object> donation = new HashMap<>();
        donation.put("familyId", pledge.get("familyId"));
        donation.put("fundId", fundId);
        donation.put("donationDate", donationDate);
        donation.put("amount", amount);
        donation.put("donationType", donationType);
        if(donationType.equals("CHECK"))
            donation.put("checkNumber", rand.nextInt(10000));
        if(pledgeType.equals("EGIFT"))
            donation.put("transactionId", rand.nextLong());

        donationClient.createDonation(donation);
    }

    private LocalDate getDonationDate(String freq, String pledgeType, int paymentNumber) {
        int totalPayments = getPayments(freq);
        float paymentSpan = 1f*365/totalPayments;
        int dayStart = Math.round(paymentNumber*paymentSpan)+7; //Make sure that we're into the time period in question

        LocalDate date = LocalDate.now().withDayOfYear(dayStart).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        if(pledgeType.equals("EGIFT"))
            date = date.plusDays(rand.nextInt(Math.round(paymentSpan))-7);

        return date;
    }

    private int getPayments(String pledgeFreq) {
        switch (pledgeFreq) {
            case "ANNUALLY":
                return 1;
            case "QUARTERLY":
                return 4;
            case "MONTHLY":
                return 12;
            case "WEEKLY":
                return 52;
            default:
                System.out.println("Can't get payment period for: " + pledgeFreq);
                throw new IllegalArgumentException();
        }
    }
}
