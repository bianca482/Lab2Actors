package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.domain.Product;

import java.util.HashMap;

/*

// TODO kompliziert zum implementieren --> fridge muss immer abwechselnd Sensoren und Product Catalog aufrufen
// wo wird die Anzahl an Produkten gespeichert? Lassen wir es im Fridge?

public class ProductCatalog extends AbstractBehavior<ProductCatalog.ProductCatalogCommand> {

    public interface ProductCatalogCommand {
    }

    public static final class GetProductByName implements ProductCatalogCommand {
        String productName;
        String callBackMethod;
        public GetProductByName(String productName, String callBackMethod) {
            this.productName = productName;
            this.callBackMethod = callBackMethod;
        }
    }

    private HashMap<String, Product> productHashMap;
    private final ActorRef<Fridge.FridgeCommand> fridge;

    private ProductCatalog(ActorContext<ProductCatalog.ProductCatalogCommand> context, ActorRef<Fridge.FridgeCommand> fridge) {
        super(context);
        this.productHashMap = new HashMap<String, Product> ();
        initializeProductHashMap();
        getContext().getLog().info("ProductCatalog started");
        this.fridge = fridge;
    }

    private void initializeProductHashMap() {
        productHashMap.put("milk",new Product("milk",1.00,1.00));
        productHashMap.put("cheese",new Product("cheese",8.00,0.50 ));
        productHashMap.put("yogurt",new Product("yogurt",0.49,0.20));
        productHashMap.put("butter",new Product("butter",2.50,0.25));
        productHashMap.put("chicken",new Product("chicken",18.00,1.25));
        productHashMap.put("coke",new Product("coke",1.79,1.50));
        productHashMap.put("salad",new Product("salad",1.50,1.00));
    }

    public static Behavior<ProductCatalogCommand> create(ActorRef<Fridge.FridgeCommand> fridge) {
        return Behaviors.setup(context -> new ProductCatalog(context, fridge));
    }

    @Override
    public Receive<ProductCatalogCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProductCatalog.GetProductByName.class, this::onProductByName)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<ProductCatalogCommand> onProductByName(GetProductByName n) {
        getContext().getLog().info("ProductCatalog received {}", n.productName);

        if(productHashMap.containsKey(n.productName)){
            Product product = productHashMap.get(n.productName);
            fridge.tell(new Fridge.AnswerFromProductCatalog(true, product, n.productName));
        }else{
            fridge.tell(new Fridge.AnswerFromProductCatalog(false, null,n.productName));
        }

        return this;
    }



    private ProductCatalog onPostStop() {
        getContext().getLog().info("ProductCatalog actor stopped");
        return this;
    }
}
*/