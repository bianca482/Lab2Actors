package at.fhv.sysarch.lab2.homeautomation.devices;

import java.time.LocalDateTime;
import java.util.UUID;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Order;
import at.fhv.sysarch.lab2.homeautomation.domain.Receipt;

public class OrderProcessor extends AbstractBehavior<Void> {

    private final Order order;
    private final ActorRef<Fridge.FridgeCommand> replyTo;

    private OrderProcessor(ActorContext<Void> context, ActorRef<Fridge.FridgeCommand> replyTo, Order order) {
        super(context);
        this.replyTo = replyTo;
        this.order = order;

        getContext().getLog().info("OrderProcessor started");

        sendResponse();
    }

    public static Behavior<Void> create(ActorRef<Fridge.FridgeCommand> fridge, Order order) {
        return Behaviors.setup(context -> new OrderProcessor(context, fridge, order));
    }

    // Erstellt das Receipt und teilt dem Fridge mit, dass die Bestellung erfolgreich war
    public Behavior<Object> sendResponse() {
        Receipt receipt = new Receipt(UUID.randomUUID().toString(), LocalDateTime.now(), order);
        replyTo.tell(new Fridge.OrderCompleted(receipt));

        getContext().getLog().info("OrderProcessor send {} to fridge", receipt);

        return Behaviors.stopped();
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder()
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private OrderProcessor onPostStop() {
        getContext().getLog().info("OrderProcessor stopped");

        return this;
    }
}
