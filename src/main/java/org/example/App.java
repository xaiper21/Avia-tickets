package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class App {

    public static void main(String[] args) {
        String filePath;
        if (args.length == 0) {
            filePath = "src/main/resources/tickets.json";
        } else {
            filePath = args[0];
        }

        ObjectMapper mapper = new ObjectMapper();

        try {

            TicketsList ticketList = mapper.readValue(new File(filePath), TicketsList.class);

            List<Ticket> filteredTickets = ticketList.getTickets().stream()
                    .filter(t -> "VVO".equals(t.getOrigin()) && "TLV".equals(t.getDestination()))
                    .collect(Collectors.toList());

            System.out.println("--- Минимальное время полета между Владивостоком и Тель-Авивом ---");
            calculateMinFlightTime(filteredTickets);

            System.out.println();

            System.out.println("--- Разница между средней ценой и медианой ---");
            calculatePriceDifference(filteredTickets);

        } catch (Exception e) {
            System.err.println("Error processing the file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void calculateMinFlightTime(List<Ticket> tickets) {

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        Map<String, List<Ticket>> ticketsByCarrier = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCarrier));

        ticketsByCarrier.forEach((carrier, ticketList) -> {
            Duration minDuration = null;

            for (Ticket ticket : ticketList) {

                LocalTime departureTime = LocalTime.parse(ticket.getDepartureTime(), timeFormatter);
                LocalTime arrivalTime = LocalTime.parse(ticket.getArrivalTime(), timeFormatter);

                Duration duration = Duration.between(departureTime, arrivalTime);

                if (duration.isNegative()) {
                    duration = duration.plus(24, ChronoUnit.HOURS);
                }

                if (minDuration == null || duration.compareTo(minDuration) < 0) {
                    minDuration = duration;
                }
            }

            if (minDuration != null) {
                long hours = minDuration.toHours();
                long minutes = minDuration.toMinutes() % 60;
                System.out.printf("Авиаперевозчик %s: %d часов %d минут%n", carrier, hours, minutes);
            }
        });
    }

    private static void calculatePriceDifference(List<Ticket> tickets) {

        List<Integer> prices = tickets.stream()
                .map(Ticket::getPrice)
                .collect(Collectors.toList());

        double averagePrice = prices.stream()
                .mapToDouble(p -> p)
                .average()
                .orElse(0.0);

        Collections.sort(prices);

        double medianPrice;
        int size = prices.size();

        if (size % 2 == 1) {
            medianPrice = prices.get(size / 2);
        } else {
            medianPrice = (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        }

        double difference = averagePrice - medianPrice;
        System.out.printf("Средняя цена: %.2f ₽%n", averagePrice);
        System.out.printf("Медиана цен: %.2f ₽%n", medianPrice);
        System.out.printf("Разница: %.2f ₽%n", difference);
    }
}
