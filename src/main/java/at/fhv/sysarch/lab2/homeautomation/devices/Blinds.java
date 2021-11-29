package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Weather;

import java.util.Optional;

/*
Blinds regulates the blinds depending on the measured weather condition, e.g., closes the blinds if the weather is sunny.
----------------------------------------------
Rules:
If the weather is sunny the blinds will close.
If the weather is not sunny the blinds will open (unless a movie is playing).
If a movie is playing the blinds are closed.
 */
public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {
    public interface BlindsCommand {}

    public static final class ControlBlindsMovie implements BlindsCommand {
        boolean isPlayingMovie;

        public ControlBlindsMovie(boolean isPlayingMovie) {
            this.isPlayingMovie = isPlayingMovie;
        }
    }

    public static final class ControlBlindsWeather implements BlindsCommand {
        Weather weather;

        public ControlBlindsWeather(Weather weather) {
            this.weather = weather;
        }
    }

    private boolean isOpen;
    private final String groupId;
    private final String deviceId;
    private Optional<Weather> currentWeather;
    private Optional<Boolean> isPlayingMovie;

    public Blinds(ActorContext<BlindsCommand> context, String groupId, String deviceId) {
        super(context);
        this.isOpen = false;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.currentWeather = Optional.empty();
        this.isPlayingMovie = Optional.empty();

        getContext().getLog().info("Blinds Actor started");
    }

    public static Behavior<BlindsCommand> create(String groupId, String deviceId) {
        return Behaviors.setup(context -> new Blinds(context, groupId, deviceId));
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ControlBlindsWeather.class, this::onControlBlindsWeather)
                .onMessage(ControlBlindsMovie.class, this::onControlBlindsMovie)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onControlBlinds() {
        getContext().getLog().info("Blinds reading the weather is {}", currentWeather);

        // If the weather is sunny the blinds will close.
        if (currentWeather.isPresent()) {
            if (currentWeather.get().equals(Weather.SUNNY)) {
                isOpen = false;
                getContext().getLog().info("Blinds reading a movie is running is {}", isPlayingMovie);
                getContext().getLog().info("Blinds closed");
            }
            // If the weather is not sunny the blinds will open (unless a movie is playing).
            else if (currentWeather.get().equals(Weather.CLOUDY)) {
                // Movie is running: Close blinds
                if (isPlayingMovie.isPresent() && isPlayingMovie.get()) {
                    isOpen = false;
                    getContext().getLog().info("Blinds reading movie is running");
                    getContext().getLog().info("Blinds closed");
                // No movie is running: Open blinds
                } else {
                    isOpen = true;
                    getContext().getLog().info("Blinds reading no movie is running");
                    getContext().getLog().info("Blinds open");
                }
            }
        }

        // Kein Wetter? Nur isPlayingMovie berücksichtigen
        // If a movie is playing the blinds are closed.
        else if (isPlayingMovie.isPresent() && isPlayingMovie.get()) {
            isOpen = false;
            getContext().getLog().info("Blinds closed");
        }

        return Behaviors.same();
    }

    private Behavior<BlindsCommand> onControlBlindsWeather(ControlBlindsWeather message) {
        this.currentWeather = Optional.of(message.weather);
        onControlBlinds();
        return Behaviors.same();
    }

    private Behavior<BlindsCommand> onControlBlindsMovie(ControlBlindsMovie message) {
        this.isPlayingMovie = Optional.of(message.isPlayingMovie);
        onControlBlinds();
        return Behaviors.same();
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor stopped");

        return this;
    }
}
