package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.util.Optional;
import java.util.Scanner;


// Liest den Userinput in Commandozeile
public class UI extends AbstractBehavior<UI.UICommand> {

    public interface UICommand {}

    public static class InitiateUI implements UICommand {}

    private final ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<UICommand> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, mediaStation, fridge));
    }

    private UI(ActorContext<UICommand> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);

        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<UICommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitiateUI.class, this::onInitiateUI)
                .onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private Behavior<UICommand> onInitiateUI (InitiateUI message) {
        new Thread(this::runCommandLine).start();
        getContext().getLog().info("UI started CommandLine");
        return this;
    }

    private UI onPostStop() {
        getContext().getLog().info("UI stopped");
        return this;
    }

    public void runCommandLine() {
        Scanner scanner = new Scanner(System.in);
        String reader = "";

        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            String[] command = reader.split(" ");

            if (command.length >= 1) {
                switch (command[0]) {
                    case "t" -> this.tempSensor.tell(new TemperatureSensor.ReadTemperature(new Temperature(Double.parseDouble(command[1]), Temperature.Unit.CELSIUS)));
                    case "a" -> this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
                    case "m" -> this.mediaStation.tell(new MediaStation.ReadMediaStation(Optional.of(Boolean.valueOf(command[1]))));
                    case "w" -> {
                        Weather weather = Weather.SUNNY;
                        if (command.length >= 2 && command[1].equals("cloudy")) {
                            weather = Weather.CLOUDY;
                        }
                        this.weatherSensor.tell(new WeatherSensor.ReadWeather(weather));
                    }
                    //Kühlschrank
                    case "f" -> {
                        switch (command[1]) {
                            case "order" -> {
                                int amount = 1;
                                if (command.length >= 4) {
                                    amount = Integer.parseInt(command[3]);
                                }
                                this.fridge.tell(new Fridge.OrderProduct(command[2], amount, fridge));
                            }
                            case "consume" -> {
                                int amount = 1;
                                if (command.length >= 4) {
                                    amount = Integer.parseInt(command[3]);
                                }
                                this.fridge.tell(new Fridge.ConsumeProduct(command[2], amount));
                            }
                            case "products" -> this.fridge.tell(new Fridge.QueryingStoredProducts());
                            case "orders" -> this.fridge.tell(new Fridge.QueryingHistoryOfOrders());
                            case "catalog" -> {
                                System.out.println("Please enter '[name] [price] [weight]' of the product");
                                reader = scanner.nextLine();
                                command = reader.split(" ");
                                if (command.length >= 3) {
                                    this.fridge.tell(new Fridge.AddProductToCatalog(command[0], Double.parseDouble(command[1]), Double.parseDouble(command[2])));
                                } else {
                                    System.out.println("Could not add product to product catalog. Please provide name, price and weight of the product");
                                }
                            }
                        }
                    }
                }
            }
        }
        getContext().getLog().info("UI done");
    }
}
