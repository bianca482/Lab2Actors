package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class SpaceSensor extends AbstractBehavior<SpaceSensor.SpaceSensorCommand> {
    public interface SpaceSensorCommand {}

    public static final class CanAddProduct implements SpaceSensorCommand {
        int amount;

        public CanAddProduct(int amount) {
            this.amount = amount;
        }
    }

    private int currentNumberOfProducts;
    private final int maxNumberOfProducts;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    private SpaceSensor(ActorContext<SpaceSensorCommand> context, ActorRef<Fridge.FridgeCommand> fridge, int maxNumberOfProducts) {
        super(context);
        this.fridge = fridge;
        this.maxNumberOfProducts = maxNumberOfProducts;

        getContext().getLog().info("SpaceSensor started");
    }

    public static Behavior<SpaceSensorCommand> create(ActorRef<Fridge.FridgeCommand> fridge, int maxNumberOfProducts) {
        return Behaviors.setup(context -> new SpaceSensor(context, fridge, maxNumberOfProducts));
    }

    @Override
    public Receive<SpaceSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CanAddProduct.class, this::onNumberOfProducts)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<SpaceSensorCommand> onNumberOfProducts (CanAddProduct n) {
        getContext().getLog().info("SpaceSensor received {}", n.amount);

        if (currentNumberOfProducts + n.amount <= maxNumberOfProducts) {
            fridge.tell(new Fridge.AnswerFromSpaceSensor(true));
        } else {
            fridge.tell(new Fridge.AnswerFromSpaceSensor(false));
        }
        return this;
    }

    private SpaceSensor onPostStop(){
        getContext().getLog().info("SpaceSensor actor stopped");
        return this;
    }
}
