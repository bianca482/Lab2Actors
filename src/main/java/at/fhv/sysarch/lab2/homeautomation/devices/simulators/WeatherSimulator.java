package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;
import at.fhv.sysarch.lab2.homeautomation.devices.WeatherSensor;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class WeatherSimulator extends AbstractBehavior<WeatherSimulator.WeatherSimulatorCommand> {
    public interface WeatherSimulatorCommand {}

    public class WeatherSimulatorCommandImpl implements WeatherSimulatorCommand {}

    private enum Timeout implements WeatherSimulatorCommand {
        INSTANCE
    }

    private final TimerScheduler<WeatherSimulatorCommand> timers;
    private static final Object TIMER_KEY = new Object();
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;

    public static Behavior<WeatherSimulatorCommand> create(ActorRef<WeatherSensor.WeatherCommand> weatherSensor) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new WeatherSimulator(context, weatherSensor, timers)));
    }

    public WeatherSimulator(ActorContext<WeatherSimulatorCommand> context, ActorRef<WeatherSensor.WeatherCommand> weatherSensor, TimerScheduler<WeatherSimulatorCommand> timers) {
        super(context);
        this.timers = timers;
        this.weatherSensor = weatherSensor;
        this.timers.startTimerAtFixedRate(new WeatherSimulatorCommandImpl(), Duration.ofSeconds(15));

        getContext().getLog().info("WeatherSimulator started");
    }

    @Override
    public Receive<WeatherSimulatorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Timeout.INSTANCE, this::onTimeout)
                .onMessage(WeatherSimulatorCommand.class, this::onCommand)
                .build();
    }

    private Behavior<WeatherSimulatorCommand> onCommand(WeatherSimulatorCommand message) {
        getContext().getLog().info("WeatherSimulatorCommand received");
        Duration delay = Duration.ofSeconds(1);
        timers.startSingleTimer(TIMER_KEY, Timeout.INSTANCE, delay);
        return this;
    }

    private Behavior<WeatherSimulatorCommand> onTimeout() {
        getContext().getLog().info("Timeout received");
        // Save all possible weather values in list
        List<Weather> values = List.of(Weather.values());
        int size = values.size();
        Random random = new Random();

        // Send message to weather sensor
        this.weatherSensor.tell(new WeatherSensor.ReadWeather(values.get(random.nextInt(size))));
        return this;
    }
}
