package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;

/*
Weather Sensor measures the weather condition.
-------------------------------------------------
Rules:
If the weather is sunny the blinds will close.
If the weather is not sunny the blinds will open (unless a movie is playing).
 */
public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand{}

    public static final class ReadWeather implements WeatherSensor.WeatherCommand {
        final Weather weather;

        public ReadWeather(Weather weather) {
            this.weather = weather;
        }
    }

    private ActorRef<Blinds.BlindsCommand> blinds;
    private final String groupId;
    private final String deviceId;

    public static Behavior<WeatherSensor.WeatherCommand> create(ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new WeatherSensor(context, blinds, groupId, deviceId));
    }

    public WeatherSensor(ActorContext<WeatherCommand> context, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        super(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;
        getContext().getLog().info("WeatherSensor started");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather w) {
        getContext().getLog().info("WeatherSensor received {}", w.weather);
        this.blinds.tell(new Blinds.ControlBlinds(w.weather));
        return this;
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor {}--{} stopped", groupId, deviceId);
        return this;
    }
}
