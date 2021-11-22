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


public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Fridge.FridgeCommand> fridge;
    private ActorRef<TemperatureSimulator.TemperatureSimulatorCommand> temperatureSimulator;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<TemperatureSimulator.TemperatureSimulatorCommand> temperatureSimulator) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, blinds, weatherSimulator, mediaStation, fridge, temperatureSimulator));
    }

    private UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator, ActorRef<MediaStation.MediaStationCommand> mediaStation, ActorRef<Fridge.FridgeCommand> fridge, ActorRef<TemperatureSimulator.TemperatureSimulatorCommand> temperatureSimulator) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.blinds = blinds;
        this.weatherSimulator = weatherSimulator;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        this.temperatureSimulator = temperatureSimulator;

        //TODO was anstatt Thread? -> laut Forum scheint dies der einzige Weg:
        // https://discuss.lightbend.com/t/interacting-with-root-actor-through-user-console-input/6511
        new Thread(() -> { this.runCommandLine(); }).start();

        getContext().getLog().info("UI started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
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
