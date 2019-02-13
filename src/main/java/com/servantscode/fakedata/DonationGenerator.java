package com.servantscode.fakedata;

import com.servantscode.fakedata.client.DonationServiceClient;
import com.servantscode.fakedata.client.FamilyServiceClient;
import com.servantscode.fakedata.client.PledgeServiceClient;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.servantscode.fakedata.RandomSelector.rand;
import static com.servantscode.fakedata.RandomSelector.select;

public class DonationGenerator {

    private final DonationServiceClient donationClient;
    private final PledgeServiceClient pledgeClient;

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
        donationClient = new DonationServiceClient();
        pledgeClient = new PledgeServiceClient();
    }

    public static void generate(int numberOfDonors) throws IOException {
        int familyCount = new FamilyServiceClient().getFamilyCount();
        int[] donors = RandomSelector.randomNumbers(familyCount, numberOfDonors);

        for(int i=0; i<numberOfDonors; i++) {
            new DonationGenerator().generateDonationPattern(donors[i]);
        }
    }

    private void generateDonationPattern(int familyId) {
        Map<String, Object> pledge = generatePledge(familyId);

        String freq = (String) pledge.get("pledgeFrequency");
        int payments = Math.round(1f*LocalDate.now().getDayOfYear()/365*getPayments(freq));

        if(rand.nextInt(100) > 90) // 10% of pledges are ignored
            return;

        for(int i=0; i<payments; i++) {
            generateDonation(pledge, i);

            if(rand.nextInt(100) > 99) // 1% of donations are someone's last one
                return;
        }
    }

    private Map<String, Object> generatePledge(int familyId) {
        LocalDateTime now = LocalDateTime.now();

        String pledgeType = select(PLEDGE_TYPES);
        String pledgeFreq = select(PLEDGE_FREQ);
        float amount = (float) RandomSelector.rand.nextInt(1000);

        Map<String, Object> pledge = new HashMap<>();
        pledge.put("familyId", familyId);
        pledge.put("pledgeType", pledgeType);
        pledge.put("pledgeFrequency", pledgeFreq);
        pledge.put("pledgeAmount", amount);
        pledge.put("pledgeDate", convert(now.minusMonths(now.getMonthValue() + 1)));
        pledge.put("pledgeStart", convert(LocalDate.now().withDayOfYear(1).atStartOfDay()));
        pledge.put("pledgeEnd", convert(LocalDate.now().withDayOfYear(1).plusYears(1).atStartOfDay()));
        pledge.put("annualPledgeAmount", amount*getPayments(pledgeFreq));

        if(rand.nextInt(100) < 80) {// 20% of the time don't pledge, just give
            pledge = pledgeClient.createDonation(pledge);
            System.out.println("Created pledge for family: " + familyId);
        } else {
            System.out.println("Did not create pledge for family: " + familyId);
        }

        return pledge;
    }

    private void generateDonation(Map<String, Object> pledge, int paymentNumber) {
        if(rand.nextInt(100) > 95) {
            missedDonations++;
            return;
        }

        String freq = (String) pledge.get("pledgeFrequency");
        String pledgeType = (String) pledge.get("pledgeType");
        Date donationDate = getDonationDate(freq, pledgeType, paymentNumber);
        if(donationDate.after(new Date()))
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
        donation.put("donationDate", donationDate);
        donation.put("amount", amount);
        donation.put("donationType", donationType);
        if(donationType.equals("CHECK"))
            donation.put("checkNumber", rand.nextInt(10000));
        if(pledgeType.equals("EGIFT"))
            donation.put("transactionId", rand.nextLong());

        donationClient.createDonation(donation);
    }

    private Date getDonationDate(String freq, String pledgeType, int paymentNumber) {
        int totalPayments = getPayments(freq);
        float paymentSpan = 1f*365/totalPayments;
        int dayStart = Math.round(paymentNumber*paymentSpan)+7; //Make sure that we're into the time period in question

        LocalDate date = LocalDate.now().withDayOfYear(dayStart).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        if(pledgeType.equals("EGIFT"))
            date = date.plusDays(rand.nextInt(Math.round(paymentSpan))-7);

        return convert(date.atStartOfDay());
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

    private Date convert(LocalDateTime dt) {
        return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
