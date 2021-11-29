package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WeightSensor extends AbstractBehavior<WeightSensor.WeightSensorCommand> {

    public interface WeightSensorCommand {}

    //Prüft, ob das Produkt bestellt werden kann
    public static final class CanAddProduct implements WeightSensorCommand {
        double weight;
        double currentWeight;

        public CanAddProduct(double weight, double currentWeight) {
            this.weight = weight;
            this.currentWeight = currentWeight;
        }
    }

    private final double maxWeightLoad;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    private WeightSensor(ActorContext<WeightSensorCommand> context, ActorRef<Fridge.FridgeCommand> fridge, double maxWeightLoad) {
        super(context);
        this.fridge = fridge;
        this.maxWeightLoad = maxWeightLoad;

        getContext().getLog().info("WeightSensor started");
    }

    public static Behavior<WeightSensorCommand> create(ActorRef<Fridge.FridgeCommand> fridge, double maxWeightLoad) {
        return Behaviors.setup(context -> new WeightSensor(context, fridge, maxWeightLoad));
    }

    @Override
    public Receive<WeightSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeightSensor.CanAddProduct.class, this::onWeight)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeightSensorCommand> onWeight(CanAddProduct n) {
        getContext().getLog().info("WeightSensor received {}", n.weight);

        if (n.currentWeight + n.weight <= maxWeightLoad) {
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
