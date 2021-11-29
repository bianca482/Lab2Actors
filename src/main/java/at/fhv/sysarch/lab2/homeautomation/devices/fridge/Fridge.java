package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.ProductCatalog;
import at.fhv.sysarch.lab2.homeautomation.domain.Order;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;
import at.fhv.sysarch.lab2.homeautomation.domain.Receipt;

import java.util.*;

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
public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {
    public interface FridgeCommand {}

    //Users can consume products from the Fridge.
    public static final class ConsumeProduct implements FridgeCommand {
        String productToConsume;
        int amount;

        public ConsumeProduct(String productToConsume, int amount) {
            this.productToConsume = productToConsume;
            this.amount = amount;
        }
    }

    //Users can order products at the Fridge.
    public static final class OrderProduct implements FridgeCommand {
        String productToOrder;
        int amount;
        ActorRef<FridgeCommand> respondTo;

        public OrderProduct(String productToOrder, int amount, ActorRef<FridgeCommand> respondTo) {
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

    //Produkt zum ProductCatalog hinzufügen
    public static final class AddProductToCatalog implements FridgeCommand {
        String name;
        double price;
        double weight;

        public AddProductToCatalog(String name, double price, double weight) {
            this.name = name;
            this.price = price;
            this.weight = weight;
        }
    }

    //Rückmeldung vom SpaceSensor, ob Bestellung bearbeitet werden kann
    public static final class AnswerFromSpaceSensor implements FridgeCommand {
        boolean wasSuccessful;

        public AnswerFromSpaceSensor(boolean wasSuccessful) {
            this.wasSuccessful = wasSuccessful;
        }
    }

    //Rückmeldung vom WeightSensor, ob Bestellung bearbeitet werden kann
    public static final class AnswerFromWeightSensor implements FridgeCommand {
        boolean wasSuccessful;

        public AnswerFromWeightSensor(boolean wasSuccessful) {
            this.wasSuccessful = wasSuccessful;
        }
    }

    private final String groupId;
    private final String deviceId;
    private final Map<Product, Integer> productAmountMap; // speichert sich die Anzahl der Produkte
    private final ProductCatalog productCatalog;        // mappt Produktname zu Produkt
    private final List<Order> orders; //Für Historie von Bestellungen
    private double currentWeightLoad;
    private int currentNumberOfProducts;
    private final ActorRef<SpaceSensor.SpaceSensorCommand> spaceSensor;
    private final ActorRef<WeightSensor.WeightSensorCommand> weightSensor;
    private OrderProduct lastOrderedProductMessage;
    private boolean answerFromSpaceSensor;
    private boolean answerFromWeightSensor;

    private Fridge(ActorContext<FridgeCommand> context, int maxNumberOfProducts, int maxWeightLoad, String groupId, String deviceId) {
        super(context);
        this.groupId = groupId;
        this.deviceId = deviceId;
        this.productCatalog = new ProductCatalog();
        this.productAmountMap = new HashMap<>();
        this.orders = new LinkedList<>();

        this.spaceSensor = getContext().spawn(SpaceSensor.create(this.getContext().getSelf(), maxNumberOfProducts), "SpaceSensor");
        this.weightSensor = getContext().spawn(WeightSensor.create(this.getContext().getSelf(), maxWeightLoad), "WeightSensor");

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
                .onMessage(AnswerFromWeightSensor.class, this::onAnswerFromWeightSensor)
                .onMessage(AddProductToCatalog.class, this::onAddProductToCatalog)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProduct message) {
        getContext().getLog().info("Fridge received: User wants to consume {}x {}", message.amount, message.productToConsume);

        Product product = productCatalog.getProductMap().get(message.productToConsume);

        // Prüfen, ob sich das Produkt im Kühlschrank befindet
        if (!productAmountMap.containsKey(product)) {
            getContext().getLog().info("No {} to consume available. You must order it before consuming.", message.productToConsume);
            return this;
        }

        int amountOfProduct = productAmountMap.get(product);
        //Check if product can be consumed
        if (amountOfProduct - message.amount >= 0) {
            productAmountMap.put(product, amountOfProduct - message.amount);

            currentWeightLoad = currentWeightLoad - (message.amount * productAmountMap.get(product).longValue());
            currentNumberOfProducts = currentNumberOfProducts - message.amount;

            // If a product runs out in the fridge it is automatically ordered again.
            if (productAmountMap.get(product) == 0){
                getContext().getLog().info("Ordering new product because all products are consumed: {}", product.getName());
                getContext().getSelf().tell((new Fridge.OrderProduct(product.getName(), 1, getContext().getSelf())));
            }
            getContext().getLog().info("Successfully consumed {}", product.getName());
        } else {
            getContext().getLog().info("{} {} not in the fridge.", message.amount, product.getName());
        }
        return this;
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProduct message) {
        getContext().getLog().info("Fridge received: User wants to order {}x {}", message.amount, message.productToOrder);

        if (!productCatalog.getProductMap().containsKey(message.productToOrder)) {
            getContext().getLog().info("Cannot order {} because this product is not in the product catalog.", message.productToOrder);
            return this;
        }

        Product product = productCatalog.getProductMap().get(message.productToOrder);
        //Eventuelle vorherigen Werte zurücksetzen
        answerFromSpaceSensor = false;
        answerFromWeightSensor = false;

        //Aktuell bestelltes Produkt setzen
        lastOrderedProductMessage = message;

        //Bei Sensoren abfragen, ob die Bestellung durchgeführt werden kann
        this.spaceSensor.tell(new SpaceSensor.CanAddProduct(message.amount, currentNumberOfProducts));
        this.weightSensor.tell(new WeightSensor.CanAddProduct(message.amount * product.getWeight(), currentWeightLoad));

        return this;
    }

    private Behavior<FridgeCommand> onAnswerFromSpaceSensor(AnswerFromSpaceSensor message) {
        getContext().getLog().info("Fridge received AnswerFromSpaceSensor. Was successful: {}", message.wasSuccessful);

        this.answerFromSpaceSensor = message.wasSuccessful;

        if (!message.wasSuccessful) {
            getContext().getLog().info("Cannot order {} because there is not enough room in the fridge", lastOrderedProductMessage.productToOrder);
        }

        onWeightAndSpaceResponse();
        return this;
    }

    private Behavior<FridgeCommand> onAnswerFromWeightSensor(AnswerFromWeightSensor message) {
        getContext().getLog().info("Fridge received AnswerFromWeightSensor. Was successful: {}", message.wasSuccessful);

        this.answerFromWeightSensor = message.wasSuccessful;

        if (!message.wasSuccessful) {
            getContext().getLog().info("Cannot complete order because the weight would exceed the allowed maximum weight of the fridge");
        }

        onWeightAndSpaceResponse();
        return this;
    }

    private void onWeightAndSpaceResponse() {
        //Warten, bis beide Sensoren die Rückmeldung gegeben haben
        if (!answerFromSpaceSensor || !answerFromWeightSensor || lastOrderedProductMessage == null) {
            return;
        }

        Product product = productCatalog.getProductMap().get(lastOrderedProductMessage.productToOrder);

        if (answerFromWeightSensor && answerFromSpaceSensor) {
            // Add product
            if (productAmountMap.containsKey(product)) {
                //Anzahl des Produktes anpassen
                int oldAmount = productAmountMap.get(product);
                productAmountMap.put(product, oldAmount + lastOrderedProductMessage.amount);
            } else {
                productAmountMap.put(product, lastOrderedProductMessage.amount);
            }
            // Add order
            Order order = new Order(product, lastOrderedProductMessage.amount);
            orders.add(order);

            // Per Session Actor -> OrderProcessor erstellen
            getContext().spawn(OrderProcessor.create(lastOrderedProductMessage.respondTo, order), "OrderProcessor" + UUID.randomUUID());

            // Adjust current weight + number of products
            currentWeightLoad = currentWeightLoad + (product.getWeight() * lastOrderedProductMessage.amount);
            currentNumberOfProducts = currentNumberOfProducts + lastOrderedProductMessage.amount;

            getContext().getLog().info("Successfully ordered {}", product.getName());
        } else {
            getContext().getLog().info("Cannot order {}", product.getName());
        }
    }

    private Behavior<FridgeCommand> onOrderCompleted(OrderCompleted message) {
        getContext().getLog().info("Fridge received OrderCompleted for product {}", message.receipt.getOrder().getProduct().getName());

        return this;
    }

    private Behavior<FridgeCommand> onQueryingStoredProducts(QueryingStoredProducts message) {
        getContext().getLog().info("Fridge received QueryingStoredProducts Command");

        System.out.println(productAmountMap.entrySet());
        return this;
    }

    private Behavior<FridgeCommand> onQueryingHistoryOfOrders(QueryingHistoryOfOrders message) {
        getContext().getLog().info("Fridge received QueryingHistoryOfOrders Command");

        for (Order o : orders) {
            System.out.println(o);
        }
        return this;
    }

    private Behavior<FridgeCommand> onAddProductToCatalog(AddProductToCatalog message) {
        getContext().getLog().info("Fridge received AddProductToCatalog Command");

        if (productCatalog.getProductMap().containsKey(message.name)) {
            getContext().getLog().info("Could not add product because it already is in catalog");
        } else {
            productCatalog.addProduct(new Product(message.name, message.price, message.weight));
            getContext().getLog().info("Added {} to product catalog", message.name);
        }
        return this;
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge actor {}--{} stopped", groupId, deviceId);
        return this;
    }
}
