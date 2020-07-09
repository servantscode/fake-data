package com.servantscode.fakedata.generator;

import org.servantscode.client.DonationServiceClient;
import org.servantscode.client.FamilyServiceClient;
import org.servantscode.client.FundServiceClient;
import org.servantscode.client.PledgeServiceClient;

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
import static java.time.temporal.ChronoUnit.DAYS;

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

        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = start.plusYears(1).minusDays(1);

        for(int i=0; i<numberOfDonors; i++) {
            int fundSelector = RandomSelector.rand.nextInt(100);
            if(fundSelector < 90)
                new DonationGenerator().generateDonationPattern(donors[i] + 1, generalFundId, start, end); // Never use 0
            if(fundSelector > 70)
                new DonationGenerator().generateDonationPattern(donors[i] + 1, buildingFundId, start, end); // Never use 0
        }
    }

    public static void generate(int numberOfDonors, LocalDate start, LocalDate end) throws IOException {
        int generalFundId = 1; //In db setup

        int familyCount = new FamilyServiceClient().getFamilyCount();
        int[] donors = RandomSelector.randomNumbers(familyCount, numberOfDonors);

        for(int i=0; i<numberOfDonors; i++) {
            new DonationGenerator().generateDonationPattern(donors[i] + 1, generalFundId, start, end); // Never use 0
        }
    }

    private static int generateBulidingFund() {
        Map<String, Object> fundData = new HashMap<>(4);
        fundData.put("name", "Building Fund");
        Map<String, Object> fund = fundClient.createFund(fundData);
        return (Integer)fund.get("id");
    }

    private void generateDonationPattern(int familyId, int fundId, LocalDate start, LocalDate end) {
        Map<String, Object> pledge = generatePledge(familyId, fundId, start, end);

        String freq = (String) pledge.get("pledgeFrequency");

        if(rand.nextInt(100) > 90) // 10% of pledges are ignored
            return;

//        int payments = Math.round(1f*LocalDate.now().getDayOfYear()/365*getPayments(freq));
        int payments = Math.round(1f*(start.until(end, DAYS))/365*getPayments(freq));

        for(int i=0; i<payments; i++) {
            generateDonation(pledge, i, fundId, start);

            if(rand.nextInt(100) > 99) // 1% of donations are someone's last one
                return;
        }
    }

    private Map<String, Object> generatePledge(int familyId, int fundId, LocalDate start, LocalDate end) {
        String pledgeType = select(PLEDGE_TYPES);
        String pledgeFreq = select(PLEDGE_FREQ);
        float amount = (float) RandomSelector.rand.nextInt(999) + 1;

        Map<String, Object> pledge = new HashMap<>();
        pledge.put("familyId", familyId);
        pledge.put("fundId", fundId);
        pledge.put("pledgeType", pledgeType);
        pledge.put("pledgeFrequency", pledgeFreq);
        pledge.put("pledgeAmount", amount);
        pledge.put("pledgeDate", LocalDate.now().withMonth(start.getMonthValue()));
        pledge.put("pledgeStart", start);
        pledge.put("pledgeEnd", end);
        pledge.put("annualPledgeAmount", amount*getPayments(pledgeFreq));

        if(rand.nextInt(100) < 80) {// 20% of the time don't pledge, just give
            pledge = pledgeClient.createDonation(pledge);
            System.out.println("Created pledge for family: " + familyId);
        } else {
            System.out.println("Did not create pledge for family: " + familyId);
        }

        return pledge;
    }

    private void generateDonation(Map<String, Object> pledge, int paymentNumber, int fundId, LocalDate start) {
        if(rand.nextInt(100) > 95) {
            missedDonations++;
            return;
        }

        String freq = (String) pledge.get("pledgeFrequency");
        String pledgeType = (String) pledge.get("pledgeType");
        LocalDate donationDate = getDonationDate(freq, pledgeType, paymentNumber, start);
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

    private LocalDate getDonationDate(String freq, String pledgeType, int paymentNumber, LocalDate start) {
        int totalPayments = getPayments(freq);
        float paymentSpan = 1f*365/totalPayments;
        int dayStart = Math.round(paymentNumber*paymentSpan)+7; //Make sure that we're into the time period in question

        LocalDate date = start.withDayOfYear(dayStart).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
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
