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
    public interface BlindsCommand {}

    public static final class ControlBlinds implements BlindsCommand {
        Weather weather;
        Optional<Boolean> isPlayingMedia;

        public ControlBlinds(Weather weather, Optional<Boolean> isPlayingMedia) {
            this.weather = weather;
            this.isPlayingMedia = isPlayingMedia;
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
        if (b.weather.equals(Weather.SUNNY)) {
            isOpen = false;
            getContext().getLog().info("Blinds reading a movie is running is {}", b.isPlayingMedia.get());
            getContext().getLog().info("Blinds closed");
        }
        // If the weather is not sunny the blinds will open (unless a movie is playing).
        else if (b.weather.equals(Weather.CLOUDY)) {
            // No movie is running: Open blinds
            if (b.isPlayingMedia.get().equals(false)) {
                isOpen = true;
                getContext().getLog().info("Blinds reading no movie is running");
                getContext().getLog().info("Blinds open");
            // Movie is running: Close blinds
            } else {
                isOpen = false;
                getContext().getLog().info("Blinds reading movie is running");
                getContext().getLog().info("Blinds closed");
            }
        }
        // If a movie is playing the blinds are closed.
        else if (b.isPlayingMedia.get().equals(true)) {
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
