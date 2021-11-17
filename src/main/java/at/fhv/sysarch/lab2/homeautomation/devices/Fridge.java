package at.fhv.sysarch.lab2.homeautomation.devices;

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
public class Fridge {
}
