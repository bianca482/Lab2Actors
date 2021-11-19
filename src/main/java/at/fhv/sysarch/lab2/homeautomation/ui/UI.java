package at.fhv.sysarch.lab2.homeautomation.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.*;
import at.fhv.sysarch.lab2.homeautomation.devices.simulators.WeatherSimulator;

import java.util.Optional;
import java.util.Scanner;

public class UI extends AbstractBehavior<Void> {

    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;

    public static Behavior<Void> create(ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator, ActorRef<MediaStation.MediaStationCommand> mediaStation) {
        return Behaviors.setup(context -> new UI(context, tempSensor, airCondition, weatherSensor, blinds, weatherSimulator, mediaStation));
    }

    private UI(ActorContext<Void> context, ActorRef<TemperatureSensor.TemperatureCommand> tempSensor, ActorRef<AirCondition.AirConditionCommand> airCondition, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, ActorRef<Blinds.BlindsCommand> blinds, ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator, ActorRef<MediaStation.MediaStationCommand> mediaStation) {
        super(context);
        // TODO: implement actor and behavior as needed
        // TODO: move UI initialization to appropriate place
        this.airCondition = airCondition;
        this.tempSensor = tempSensor;
        this.weatherSensor = weatherSensor;
        this.blinds = blinds;
        this.weatherSimulator = weatherSimulator;
        this.mediaStation = mediaStation;
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
            // TODO: process Input
        }
        getContext().getLog().info("UI done");
    }
}
