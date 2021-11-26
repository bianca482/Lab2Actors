package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.TemperatureSensor;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;

import java.time.Duration;
import java.util.Random;

public class TemperatureSimulator extends AbstractBehavior<TemperatureSimulator.TemperatureSimulatorCommand> {
    public interface TemperatureSimulatorCommand {}

    public static class TemperatureSimulatorCommandImpl implements TemperatureSimulatorCommand {}

    private enum Timeout implements TemperatureSimulatorCommand {
        INSTANCE
    }

    private final TimerScheduler<TemperatureSimulatorCommand> timers;
    private static final Object TIMER_KEY = new Object();
    private final ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor;
    private final Temperature currentTemperature;

    private TemperatureSimulator(ActorContext<TemperatureSimulatorCommand> context, ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor, TimerScheduler<TemperatureSimulatorCommand> timers) {
        super(context);
        this.timers = timers;
        this.temperatureSensor = temperatureSensor;
        this.currentTemperature = new Temperature(23, Temperature.Unit.GRAD_CELSIUS);
        this.timers.startTimerAtFixedRate(new TemperatureSimulatorCommandImpl(), Duration.ofSeconds(10));

        getContext().getLog().info("TemperatureSimulator started");
    }

    public static Behavior<TemperatureSimulatorCommand> create(ActorRef<TemperatureSensor.TemperatureCommand> temperatureSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new TemperatureSimulator(context, temperatureSensor, timers)));
    }

    @Override
    public Receive<TemperatureSimulatorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(TemperatureSimulator.Timeout.INSTANCE, this::onTimeout)
                .onMessage(TemperatureSimulatorCommand.class, this::onCommand)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureSimulator.TemperatureSimulatorCommand> onCommand(TemperatureSimulatorCommand message) {
        getContext().getLog().info("TemperatureSimulatorCommand received");

        Duration delay = Duration.ofSeconds(1);
        timers.startSingleTimer(TIMER_KEY, TemperatureSimulator.Timeout.INSTANCE, delay);
        return this;
    }

    private Behavior<TemperatureSimulator.TemperatureSimulatorCommand> onTimeout() {
        getContext().getLog().info("Timeout received");

        Random random = new Random();
        double randomValue = random.doubles(-1, 1).limit(1).findFirst().getAsDouble();
        randomValue = Math.round(100.0 * randomValue) / 100.0;
        currentTemperature.setValue(currentTemperature.getValue() + randomValue);

        // Send message to weather sensor
        this.temperatureSensor.tell(new TemperatureSensor.ReadTemperature(new Temperature(currentTemperature.getValue(), currentTemperature.getUnit())));
        return this;
    }

    private TemperatureSimulator onPostStop() {
        getContext().getLog().info("TemperatureSimulator actor stopped");

        return this;
    }
}
