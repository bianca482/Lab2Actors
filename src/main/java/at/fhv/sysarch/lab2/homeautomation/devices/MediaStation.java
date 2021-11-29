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
Media Station allows playing movies.
---------------------------------------------------------------------
Functionality:
Users can play movies at the media station.
---------------------------------------------------------------------
Rules:
If a movie is playing the blinds are closed.
A new movie cannot be started if another movie is already playing.
 */
public class MediaStation extends AbstractBehavior<MediaStation.MediaStationCommand> {

    public interface MediaStationCommand {}

    private static Optional<Boolean> playingMovie;

    public static final class ReadMediaStation implements MediaStation.MediaStationCommand {
        final Optional<Boolean> isPlayingMovie;

        public ReadMediaStation(Optional<Boolean> isPlayingMovie) {
            this.isPlayingMovie = isPlayingMovie;
            //Wenn schon ein Film läuft
            if (!(playingMovie.isEmpty() || playingMovie.get().equals(false))) {
                playingMovie = isPlayingMovie;
            }
        }
    }

    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final String groupId;
    private final String deviceId;

    public static Behavior<MediaStation.MediaStationCommand> create(ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        return Behaviors.setup(context -> new MediaStation(context, blinds, groupId, deviceId));
    }

    public MediaStation(ActorContext<MediaStationCommand> context, ActorRef<Blinds.BlindsCommand> blinds, String groupId, String deviceId) {
        super(context);
        this.blinds = blinds;
        this.groupId = groupId;
        this.deviceId = deviceId;
        playingMovie = Optional.empty();

        getContext().getLog().info("Media Station started");
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadMediaStation.class, this::onReadMediaStation)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaStationCommand> onReadMediaStation(ReadMediaStation m) {
        // A new movie cannot be started if another movie is already playing.
        if (playingMovie.isEmpty() || playingMovie.get().equals(false)) {
            getContext().getLog().info("Media Station received {}", m.isPlayingMovie);

            playingMovie = m.isPlayingMovie;

            this.blinds.tell(new Blinds.ControlBlindsMovie(m.isPlayingMovie.get()));
        } else {
            getContext().getLog().info("Cannot play new movie because another movie is already playing.");
        }
        return this;
    }

    private MediaStation onPostStop() {
        getContext().getLog().info("Media Station actor {}--{} stopped", groupId, deviceId);
        return this;
    }
}
