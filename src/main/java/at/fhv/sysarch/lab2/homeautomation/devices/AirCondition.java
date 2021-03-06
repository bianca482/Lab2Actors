package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Temperature;

/**
 * This class shows ONE way to switch behaviors in object-oriented style. Another approach is the use of static
 * methods for each behavior.
 *
 * The switching of behaviors is not strictly necessary for this example, but is rather used for demonstration
 * purpose only.
 *
 * For an example with functional-style please refer to: {@link https://doc.akka.io/docs/akka/current/typed/style-guide.html#functional-versus-object-oriented-style}
 *
 */
import java.util.Optional;

/*
AC (Air Conditioning) regulates the AC depending on the measured temperature.
------------------------------------------
Rules:
If the temperature is above 20°C the AC starts cooling. DONE
If the temperature is below 20°C the AC turns off. DONE
 */
public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {
    public interface AirConditionCommand {}

    public static final class PowerAirCondition implements AirConditionCommand {
        final Optional<Boolean> value;

        public PowerAirCondition(Optional<Boolean> value) {
            this.value = value;
        }
    }

    public static final class EnrichedTemperature implements AirConditionCommand {
        Temperature temperature;

        public EnrichedTemperature(Temperature temperature) {
            this.temperature = temperature;
        }
    }

    private final String groupId;
    private final String deviceId;
    private boolean active;
    private boolean poweredOn;

    public AirCondition(ActorContext<AirConditionCommand> context, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.active = false;
        this.poweredOn = true;

        getContext().getLog().info("AirCondition started");
    }

    public static Behavior<AirConditionCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new AirCondition(context, groupId, deviceId));
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                .onMessage(PowerAirCondition.class, this::onPowerAirConditionOff)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperature r) {
        getContext().getLog().info("Aircondition reading {}", r.temperature.getValue());

        if (r.temperature.getValue() >= 20) {
            getContext().getLog().info("Aircondition actived");
            this.active = true;
        }
        else {
            getContext().getLog().info("Aircondition deactived");
            this.active =  false;
        }

        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOff(PowerAirCondition r) {
        getContext().getLog().info("Turning Aircondition to {}", r.value);

        // Wenn AC ausgeschaltet werden soll
        if (r.value.isPresent() && !r.value.get()) {
            return this.powerOff();
        }
        return this;
    }

    private Behavior<AirConditionCommand> onPowerAirConditionOn(PowerAirCondition r) {
        getContext().getLog().info("Turning Aircondition to {}", r.value);

        //Wenn AC eingeschalten werden soll
        if (r.value.isPresent() && r.value.get()) {
            return Behaviors.receive(AirConditionCommand.class)
                    .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                    .onMessage(PowerAirCondition.class, this::onPowerAirConditionOff)
                    .onSignal(PostStop.class, signal -> onPostStop())
                    .build();
        }
        return this;
    }

    private Behavior<AirConditionCommand> powerOff() {
        this.poweredOn = false;
        //Neues Verhalten, wenn AC ausgeschalten ist
        return Behaviors.receive(AirConditionCommand.class)
                .onMessage(PowerAirCondition.class, this::onPowerAirConditionOn)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("AirCondition actor {}-{} stopped", groupId, deviceId);
        return this;
    }
}
