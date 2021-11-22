package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.ActorSelection;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.Patterns;
import akka.util.Timeout;
import at.fhv.sysarch.lab2.homeautomation.domain.Order;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;
import at.fhv.sysarch.lab2.homeautomation.domain.Receipt;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/*
Fridge manages products and allows ordering new products.
Based on the currently contained products an order might no be realizable.

The Fridge is a special kind of device in our system as it contains itself two additional sensors, measuring weight of and space taken by contained products:

The Fridge has a maximum number of storable products.
The Fridge has a maximum weight load.
Each Product has a price and a weight.
---------------------------------------------------------------
Functionality:
Users can consume products from the Fridge.
Users can order products at the Fridge.
A successful order returns a receipt.
The Fridge allows for querying the currently stored products.
The Fridge allows for querying the history of orders.
---------------------------------------------------------------
Rules:
The Fridge can only process an order if there is enough room in the fridge, i.e., the contained products and newly order products do not exceed the maximum number of storable products.
The Fridge can only process an order if the weight of the sum of the contained products and newly order products does not exceed its maximum weight capacity.
If a product runs out in the fridge it is automatically ordered again.
 */
// ToDo: User queries the fridge with Request-Response && User consumes a product from the fridge with Ignoring replies -> Wie sieht der User aus?
// ToDo: User orders products -> fridge should relay this request to a separate OrderProcessor actor (Per session child Actor)
// ToDo: Weight + Space Sensoren einbauen!
public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {
    public interface FridgeCommand {}

    //Users can consume products from the Fridge.
    public static final class ConsumeProduct implements FridgeCommand {
        Product productToConsume;
        int amount;

        public ConsumeProduct(Product productToConsume, int amount) {
            this.productToConsume = productToConsume;
            this.amount = amount;
        }
    }

    //Users can order products at the Fridge.
    public static final class OrderProduct implements FridgeCommand {
        Product productToOrder;
        int amount;
        ActorRef<FridgeCommand> respondTo;

        public OrderProduct(Product productToOrder, int amount, ActorRef<FridgeCommand> respondTo) {
            this.productToOrder = productToOrder;
            this.amount = amount;
            this.respondTo = respondTo;
        }
    }

    //Antwort von OrderProcessor, wenn die Bestellung erfolgreich abgeschlossen wurde
    public static final class OrderCompleted implements FridgeCommand {
        final Receipt receipt;

        public OrderCompleted(Receipt receipt) {
            this.receipt = receipt;
        }
    }

    //The Fridge allows for querying the currently stored products = Abfrage der aktuell im Kühlschrank enthaltenen Produkte
    public static final class QueryingStoredProducts implements FridgeCommand {}

    //The Fridge allows for querying the history of orders = Abfrage der bisherigen Bestellungen
    public static final class QueryingHistoryOfOrders implements FridgeCommand {}

    private final int maxNumberOfProducts;
    private final int maxWeightLoad;
    private final String groupId;
    private final String deviceId;
    private Map<Product, Integer> products;
    private List<Order> orders;
    private double currentWeightLoad;
    private int currentNumberOfProducts;
    private ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private volatile AnswerFromSpaceSensor answerFromSpaceSensor;

    public static final class AnswerFromSpaceSensor implements FridgeCommand {
        boolean wasSuccessful;

        public AnswerFromSpaceSensor(boolean wasSuccessful) {
            this.wasSuccessful = wasSuccessful;
        }
    }

    private Fridge(ActorContext<FridgeCommand> context, int maxNumberOfProducts, int maxWeightLoad, String groupId, String deviceId) {
        super(context);
        this.maxNumberOfProducts = maxNumberOfProducts;
        this.maxWeightLoad = maxWeightLoad;
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.products = new HashMap<>();
        this.orders = new LinkedList<>();

        this.spaceSensor = getContext().spawn(SpaceSensor.create(this.getContext().getSelf(), maxNumberOfProducts), "SpaceSensor");

        getContext().getLog().info("Fridge started");
    }

    public static Behavior<Fridge.FridgeCommand> create(int maxNumberOfProducts, int maxWeightLoad, String groupId, String deviceId) {
        return Behaviors.setup(context -> new Fridge(context, maxNumberOfProducts, maxWeightLoad, groupId, deviceId));
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConsumeProduct.class, this::onConsumeProduct)
                .onMessage(OrderProduct.class, this::onOrderProduct)
                .onMessage(OrderCompleted.class, this::onOrderCompleted)
                .onMessage(QueryingStoredProducts.class, this::onQueryingStoredProducts)
                .onMessage(QueryingHistoryOfOrders.class, this::onQueryingHistoryOfOrders)
                .onMessage(AnswerFromSpaceSensor.class, this::onAnswerFromSpaceSensor)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProduct message) {
        getContext().getLog().info("Fridge received: User wants to consume {}x {}", message.amount, message.productToConsume.getName());
        int amountOfProduct = products.get(message.productToConsume);
        //Check if product can be consumed
        if (amountOfProduct - message.amount >= 0) {
            products.put(message.productToConsume, amountOfProduct - message.amount);
            getContext().getLog().info("Successfully consumed {}", message.productToConsume.getName());
        } else {
            getContext().getLog().info("Cannot consume {}", message.productToConsume.getName());
        }
        return this;
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProduct message) throws InterruptedException, TimeoutException, ExecutionException {
        getContext().getLog().info("Fridge received: User wants to order {}x {}", message.amount, message.productToOrder.getName());

        this.spaceSensor.tell(new SpaceSensor.CanAddProduct(message.amount));

//        if (answerFromSpaceSensor == null) {
////            Timeout timeout = Timeout.create(Duration.ofSeconds(5));
////            Future<Object> future = Patterns.ask(this.spaceSensor, new SpaceSensor.CanAddProduct(message.amount), timeout);
////            String result = (String) Await.result(future, timeout.duration());
//
//            ExecutorService threadpool = Executors.newCachedThreadPool();
//            Future<SpaceSensor.CanAddProduct> futureTask = threadpool.submit(() -> new SpaceSensor.CanAddProduct(message.amount));
//
//            while (!futureTask.isDone()) {
//                System.out.println("FutureTask is not finished yet...");
//            }
//            SpaceSensor.CanAddProduct result = futureTask.get();
//
//            threadpool.shutdown();
//        }

        if ((currentWeightLoad + (message.productToOrder.getWeight() * message.amount) <= maxWeightLoad) && answerFromSpaceSensor.wasSuccessful) {
            // Add product
            if (products.containsKey(message.productToOrder)) {
                int oldAmount = products.get(message.productToOrder);
                products.put(message.productToOrder, oldAmount + message.amount);
            } else {
                products.put(message.productToOrder, message.amount);
            }
            // Add order
            Order order = new Order(message.productToOrder, message.amount);
            orders.add(order);

            //Per Session Actor
            getContext().spawn(OrderProcessor.create(message.respondTo, order), "OrderProcessor");

            // Adjust current weight + number of products
            currentWeightLoad = currentWeightLoad + (message.productToOrder.getWeight() * message.amount);
            currentNumberOfProducts = currentNumberOfProducts + message.amount;

            getContext().getLog().info("Successfully ordered {}", message.productToOrder.getName());
        } else {
            getContext().getLog().info("Cannot order {}", message.productToOrder.getName());
        }

        return this;
    }

    private Behavior<FridgeCommand> onAnswerFromSpaceSensor(AnswerFromSpaceSensor message) {
        getContext().getLog().info("Fridge received AnswerFromSpaceSensor. Was successful: {}", message.wasSuccessful);
        this.answerFromSpaceSensor = message;
        return this;
    }

    private Behavior<FridgeCommand> onOrderCompleted(OrderCompleted message) {
        getContext().getLog().info("Fridge received OrderCompleted. Receipt: {}", message.receipt);
        return this;
    }

    private Behavior<FridgeCommand> onQueryingStoredProducts(QueryingStoredProducts message) {
        getContext().getLog().info("Fridge received QueryingStoredProducts Command");
        System.out.println(products.entrySet());
        return this;
    }

    private Behavior<FridgeCommand> onQueryingHistoryOfOrders(QueryingHistoryOfOrders message) {
        getContext().getLog().info("Fridge received QueryingHistoryOfOrders Command");
        for (Order o : orders) {
            System.out.println(o);
        }
        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}--{} stopped", groupId, deviceId);
        return this;
    }
}
