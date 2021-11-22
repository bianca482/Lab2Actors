package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand> {

    public interface WeightSensorCommand {
    }

    public static final class CanAddProduct implements WeightSensor.WeightSensorCommand {
        double weight;

        public CanAddProduct(double weight) {
            this.weight = weight;
        }
    }

    private double currentWeightLoad;
    private final double maxWeightLoad;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    private WeightSensor(ActorContext<WeightSensor.WeightSensorCommand> context, ActorRef<Fridge.FridgeCommand> fridge, double maxWeightLoad) {
        super(context);
        this.fridge = fridge;
        this.maxWeightLoad = maxWeightLoad;

        getContext().getLog().info("WeightSensor started");
    }

    public static Behavior<WeightSensor.WeightSensorCommand> create(ActorRef<Fridge.FridgeCommand> fridge, double maxWeightLoad) {
        return Behaviors.setup(context -> new WeightSensor(context, fridge, maxWeightLoad));
    }

    @Override
    public Receive<WeightSensor.WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeightSensor.CanAddProduct.class, this::onWeight)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeightSensor.WeightSensorCommand> onWeight(WeightSensor.CanAddProduct n) {
        getContext().getLog().info("WeightSensor received {}", n.weight);

        if (currentWeightLoad + n.weight <= maxWeightLoad) {
            fridge.tell(new Fridge.AnswerFromWeightSensor(true));
        } else {
            fridge.tell(new Fridge.AnswerFromWeightSensor(false));
        }
        return this;
    }

    private WeightSensor onPostStop() {
        getContext().getLog().info("WeightSensor actor stopped");
        return this;
    }
}
