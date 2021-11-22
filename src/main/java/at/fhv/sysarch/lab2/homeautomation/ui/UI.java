package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.TemperatureSimulator;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.WeatherSimulator;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.util.Optional;
import java.util.Scanner;


// TODO Ist nicht für die Ausgabe zuständig (dies passiert über logging in Sensoren), ist dieser nur zuständig für User input handling?
// liest Commandozeile und gibt es an User weiter
// mögliche Commands: consume product, order product, play movie


public class UI extends AbstractBehavior<UI.UICommand> {

    public interface UICommand {}

    public static class InitiateUI implements UICommand {}

    private final ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    public static Behavior<UICommand> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, blinds, mediaStation, fridge));
    }

    private UI(ActorContext<UICommand> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.blinds = blinds;
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
        // TODO: Create Actor for UI Input-Handling
        Scanner scanner = new Scanner(System.in);
        String[] input = null;
        String reader = "";

        while (!reader.equalsIgnoreCase("quit") && scanner.hasNextLine()) {
            reader = scanner.nextLine();
            // TODO: change input handling
            String[] command = reader.split(" ");
            if (command[0].equals("t")) {
                this.tempSensor.tell(new TemperatureSensor.ReadTemperature(new Temperature(Double.parseDouble(command[1]), Temperature.Unit.CELSIUS)));
            }
            if (command[0].equals("a")) {
                this.airCondition.tell(new AirCondition.PowerAirCondition(Optional.of(Boolean.valueOf(command[1]))));
            }
            if (command[0].equals("m")) {
                this.mediaStation.tell(new MediaStation.ReadMediaStation(Optional.of(Boolean.valueOf(command[1]))));
            }
            if (command[0].equals("w")) {
                Weather weather = Weather.SUNNY;
                if (command[1].equals("cloudy")) {
                    weather = Weather.CLOUDY;
                }
                this.weatherSensor.tell(new WeatherSensor.ReadWeather(weather));
            }
            if (command[0].equals("f")) {
                if (command[1].equals("order")) {
                    Product product = new Product(command[2], 10, 0.3);
                    int amount = 1;
                    if (command.length >= 4) {
                        amount = Integer.parseInt(command[3]);
                    }
                    this.fridge.tell(new Fridge.OrderProduct(product, amount, fridge));
                }
                else if (command[1].equals("consume")) {
                    Product product = new Product(command[2], 10, 0.3);
                    int amount = 1;
                    if (command.length >= 4) {
                        amount = Integer.parseInt(command[3]);
                    }
                    this.fridge.tell(new Fridge.ConsumeProduct(product, amount));
                } else if (command[1].equals("products")) {
                    this.fridge.tell(new Fridge.QueryingStoredProducts());
                } else if (command[1].equals("orders")) {
                    this.fridge.tell(new Fridge.QueryingHistoryOfOrders());
                }
            }
            // TODO: process Input
        }
        getContext().getLog().info("UI done");
    }
}
