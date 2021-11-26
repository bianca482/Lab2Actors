package at.fhv.sysarch.lab2.homeautomation;

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
import at.fhv.sysarch.lab2.homeautomation.ui.UI;

public class HomeAutomationController extends AbstractBehavior<Void> {
    private ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private ActorRef<AirCondition.AirConditionCommand> airCondition;
    private ActorRef<Blinds.BlindsCommand> blinds;
    private ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private ActorRef<WeatherSimulator.WeatherSimulatorCommand> weatherSimulator;
    private ActorRef<MediaStation.MediaStationCommand> mediaStation;
    private ActorRef<Fridge.FridgeCommand> fridge;
    private ActorRef<TemperatureSimulator.TemperatureSimulatorCommand> temperatureSimulator;
    private ActorRef<UI.UICommand> ui;

    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationController::new);
    }

    private HomeAutomationController(ActorContext<Void> context) {
        super(context);

        this.airCondition = getContext().spawn(AirCondition.create("2", "1"), "AirCondition");
        this.tempSensor = getContext().spawn(TemperatureSensor.create(this.airCondition, "1", "1"), "TemperatureSensor");
        this.blinds = getContext().spawn(Blinds.create("6", "1"), "Blinds");
        this.weatherSensor = getContext().spawn(WeatherSensor.create(this.blinds, "3", "1"), "WeatherSensor");
        this.mediaStation = getContext().spawn(MediaStation.create(this.blinds, "4", "1"), "MediaStation");
        this.fridge = getContext().spawn(Fridge.create(100, 100, "5", "1"), "Fridge");
        this.weatherSimulator = getContext().spawn(WeatherSimulator.create(this.weatherSensor), "WeatherSimulator");
        this.temperatureSimulator = getContext().spawn(TemperatureSimulator.create(this.tempSensor), "TemperatureSimulator");
        this.ui = getContext().spawn(UI.create(this.tempSensor, this.airCondition, this.weatherSensor, this.mediaStation, this.fridge), "UI");

        //Tell UI Actor it should start the command line
        this.ui.tell(new UI.InitiateUI());

        getContext().getLog().info("HomeAutomation Application started");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().onSignal(PostStop.class, signal -> onPostStop()).build();
    }

    private HomeAutomationController onPostStop() {
        getContext().getLog().info("HomeAutomation Application stopped");
        return this;
    }
}
