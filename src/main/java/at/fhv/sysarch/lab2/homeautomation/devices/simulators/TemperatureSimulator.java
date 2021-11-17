package at.fhv.sysarch.lab2.homeautomation.devices.simulators;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class TemperatureSimulator extends AbstractBehavior<TemperatureSimulator.TemperatureSimulatorCommand> {
    public interface TemperatureSimulatorCommand {}

    private final TimerScheduler<TemperatureSimulatorCommand> timers;

    private TemperatureSimulator(ActorContext<TemperatureSimulatorCommand> context, TimerScheduler<TemperatureSimulatorCommand> timers) {
        super(context);
        this.timers = timers;
    }

    public static Behavior<TemperatureSimulatorCommand> create() {
        return Behaviors.setup(context -> Behaviors.withTimers(timers -> new TemperatureSimulator(context, timers)));
    }

    @Override
    public Receive<TemperatureSimulatorCommand> createReceive() {
        return null;
    }
}
