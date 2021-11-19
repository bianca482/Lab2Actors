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
Blinds regulates the blinds depending on the measured weather condition, e.g., closes the blinds if the weather is sunny.
----------------------------------------------
Rules:
If the weather is sunny the blinds will close.
If the weather is not sunny the blinds will open (unless a movie is playing).
If a movie is playing the blinds are closed.
 */
public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {
    public interface BlindsCommand {
    }

    private static Optional<Weather> currentWeather = Optional.empty();
    private static Optional<Boolean> playingMovie = Optional.empty();

    public static final class ControlBlinds implements BlindsCommand {
        Weather weather;
        boolean isPlayingMovie;

        public ControlBlinds(Weather weather) {
            if (playingMovie.isPresent()){
                isPlayingMovie = playingMovie.get();
            }
            this.weather = weather;
            currentWeather = Optional.of(weather);
        }

        public ControlBlinds(boolean isPlayingMovie) {
            this.isPlayingMovie = isPlayingMovie;
            playingMovie = Optional.of(isPlayingMovie);
            if (currentWeather.isPresent()) {
                this.weather = currentWeather.get();
            }
        }
    }

    private boolean isOpen;

    public Blinds(ActorContext<BlindsCommand> context) {
        super(context);
        isOpen = false;
        getContext().getLog().info("Blinds Actor started");
    }

    public static Behavior<BlindsCommand> create(ActorRef<BlindsCommand> blinds) {
        return Behaviors.setup(Blinds::new);
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ControlBlinds.class, this::onControlBlinds)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onControlBlinds(ControlBlinds b) {
        getContext().getLog().info("Blinds reading the weather is {}", b.weather);

        // If the weather is sunny the blinds will close.
        if (b.weather != null) {
            if (b.weather.equals(Weather.SUNNY)) {
                isOpen = false;
                getContext().getLog().info("Blinds reading a movie is running is {}", b.isPlayingMovie);
                getContext().getLog().info("Blinds closed");
            }
            // If the weather is not sunny the blinds will open (unless a movie is playing).
            else if (b.weather.equals(Weather.CLOUDY)) {
                // Movie is running: Close blinds
                if (b.isPlayingMovie) {
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

        // If a movie is playing the blinds are closed.
        else if (b.isPlayingMovie) {
            isOpen = false;
            getContext().getLog().info("Blinds closed");
        }

        return Behaviors.same();
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor stopped");
        return this;
    }
}
