# Lab 2 (Actor-based Home Automation)

#### Bianca

Architekturentscheidungen begründen (inkl. wieso kein Blackboard Pattern aber auch wieso es gut gewesen wäre)

Domain Model einfügen (wie Aktoren miteinander kommunizieren + Java Classes)

Erklärung, wie Kommunikation zwischen Aktoren funktioniert + wer mit wem kommuniziert/ eigene Aktoren erstellt
- beinhaltet Interaction Patterns (wo wir welche verwendet haben)

-------------------------------------------------------------------------------------

## 1. Architektur

## 2. Verwendete Actors und deren Kommunikation
Im Folgenden werden die verschiedenen Actors des Systems mitsamt
ihrer Kommunikation beschrieben. Folgende Abbildung bietet
einen Überblick darüber, welche Actors es gibt und wer mit wem kommuniziert.

<img src="src/main/resources/Lab2_Actors_Communication_v4.jpg" alt="Kommunikation Actors"/>

In der Abbildung ist ersichtlich, dass das System über die Klasse "HomeAutomationSystem"
gestartet wird. Diese erstellt einen "HomeAutomationController", welcher
alle Actors des Systems erstellt. Der "HomeAutomationController" schickt
außerdem eine Nachricht an den "UI"-Actor, damit dieser einen neuen Thread mit
startet und der User somit Befehle eingeben kann.

### 2.1 UI
Das "UI" nimmt die Anfragen des Users entgegen und sendet diese an die
entsprechenden Actors weiter.

Über das "UI" ist es somit z.B. möglich, den Kühlschrank anzusprechen und somit
Produkte zu bestellen, zu konsumieren, sowie auch die Produkte, die sich aktuell
im Kühlschrank befinden, als auch eine Historie der Bestellungen ausgeben zu lassen.
Zudem kann der "AC" manuell ein- und ausgeschaltet werden. Außerdem kann der User Filme
abspielen als auch die Temperatur und das Wetter manuell setzen.
#### Kommunikation und eingesetzte Interaktions-Pattern
Das "UI" kennt alle Aktoren, die vom User direkt angesprochen werden können. Dies sind 
der "Fridge", "AC", "MediaStation", "TemperatureSensor" und "WeatherSensor". Die Nachrichten werden
jeweils über das "Fire and Forget"-Pattern verschickt.


### 2.2 MediaStation
Über die "MediaStation" kann ein Film abgespielt werden. 
#### Kommunikation und eingesetzte Interaktions-Pattern
Diese schickt nach dem "Fire and Forget"-Ansatz den "Blinds" Nachrichten. 
Dabei wird den "Blinds" mitgeteilt, ob aktuell gerade ein Film läuft oder nicht.


### 2.3 TemperatureSimulator
Der "TemperatureSimulator" schickt sich prinzipiell nach einem selbstgewählten
Timeout immer selbst Nachrichten. Wenn das Timeout erreicht wurde, wird
die Temperatur um einen zufälligen Wert im Bereich von -1 bis +1 Grad erhöht/verringert
und die neue Temperatur dem "TemperatureSensor" übermittelt.
#### Kommunikation und eingesetzte Interaktions-Pattern
Um diesen "TemperatureSimulator" implementieren zu können, wurde das Interaktions-Pattern 
"Scheduling messages to self" verwendet. Die Kommunikation zwischen diesem Simulator 
und dem Sensor erfolgt dabei über das Pattern "Fire and Forget". 
Der Simulator kennt daher den Sensor und pushed die neue Temperatur einfach auf den Sensor.


### 2.4 TemperatureSensor
Bekommt vom "TemperaturSimulator" oder vom "UI" eine Temperatur.
#### Kommunikation und eingesetzte Interaktions-Pattern
Der "TemperatureSensor" kennt nur den "AC" und schickt diesem nach dem
Interaktions-Pattern "Fire and Forget" immer die neue aktuelle Temperatur.


### 2.5 AC
Der "AC" erhält Nachrichten vom "TemperatureSensor" und schaltet sich je nach Temperatur
ein- bzw. aus. Zusätzlich kann dieser Actor auch von außen komplett ausgeschalten werden.
Per Default läuft der "AC" zwar, ist aber auf Standby geschalten (kühlt nicht).
#### Kommunikation und eingesetzte Interaktions-Pattern
Kennt keinen anderen Actor. 


### 2.6 WeatherSimulator
Bei dem "WeatherSimulator" verhält es sich genau gleich wie beim "TemperatureSimulator", nur 
mit dem Unterschied, dass dieser Simulator zufällig ein Wetter generiert.
#### Kommunikation und eingesetzte Interaktions-Pattern
Auch hier wurde das Pattern "Scheduling messages to self" implementiert,
damit in einem Intervall immer wieder ein neues Wetter generiert wird. Das Wetter
wird wieder nach dem "Fire and Forget"-Ansatz dem "WeatherSensor" weitergeschickt.


### 2.7 WeatherSensor
Bekommt vom "WeatherSimulator" ein Wetter.
#### Kommunikation und eingesetzte Interaktions-Pattern
Der "WeatherSensor" kennt die "Blinds" und pushed das aktuelle Wetter
auf die "Blinds" (= "Fire and Forget").


### 2.8 Blinds
Die "Blinds" bekommen von der "MediaStation" und dem "WeatherSensor" Nachrichten 
zugeschickt. Je nachdem, welche Werte übermittelt wurden, ändert sich der Öffnungszustand 
der "Blinds".
#### Kommunikation und eingesetzte Interaktions-Pattern
Kennt keinen anderen Aktor.


### 2.9 Fridge
Der "Fridge" hat selber zwei eigene Sensoren, den "WeightSensor" und den "SpaceSensor".
Diese werden beim Erstellen des Kühlschranks initialisiert und erhalten somit auch
die maximale Anzahl an Produkten bzw. das mögliche Maximalgewicht. Im Falle der 
Bearbeitung einer "OrderProduct"-Anfrage vom "UI" wird dabei zunächst bei
den beiden Sensoren angefragt, ob vom Gewicht bzw. von der Anzahl der Produkten
her die Bestellung durchgeführt werden kann. Der "Fridge" wartet, bis beide Sensoren
geprüft haben, ob die Bestellung von ihrer Seite aus durchgeführt werden kann.
Ist die Bestellung möglich, wird ein "Per session child Actor" namens "OrderProcessor" 
erstellt, welcher die Bestellung abschließt. 

- #### 2.9.1 WeightSensor und SpaceSensor
Beide Sensoren bekommen jeweils einen Request vom "Fridge", bearbeiten die Anfrage und 
schicken dem "Fridge" eine Antwort zurück. Hierbei wurde auf das "Request and Response"-Interaktionspattern zurückgegriffen.
- #### 2.9.2 OrderProcessor
Wird als "Per session child Actor" vom "Fridge" erstellt und erhält auf diesen entsprechend
auch eine Referenz. Er erstellt eine passende Rechnung, welche er dem "Fridge" zurückschickt.

---------------------------------------------------------------------------
## 3. Domain Model
Neben den soeben beschriebenen Actors, kamen auch normale Java Klassen zum Einsatz.

<img src="src/main/resources/domain_model.png" alt="Domänenmodell"/>

Wie in der obigen Abbildung ersichtlich ist, wurde eine eigene Klasse für die "Temperature" erstellt
Dies hat den Sinn, zusätzlich zur jewiligen Grad-Anzahl auch die entsprechende Einheit
speichern zu können. Hierfür hat die "Temperature" eine interne Enum-Klasse, welche aktuell nur
Celsius enthält. Diese könnte auch durch andere Maßeinheiten, wie beispielsweise Kelvin oder
Fahrenheit, erweitert werden.

Ein "Product" kann bestellt oder konsumiert werden. Jedes "Product" hat einen Namen, einen
Preis und ein Gewicht. Der "ProductCatalog" speichert alle möglichen Produkte, die in einem
"Fridge" gespeichert werden können. Jeder Fridge erstellt sich einen "ProductCatalog".
Dies Klasse wurde eingefügt, um nicht extra bei jedem Produkt, das im "UI" bestellt oder
konsumiert wird, einen Preis und ein Gewicht bereitstellen zu müssen. Die Produkte werden zu Beginn
initialisiert und können aber im Nachhinein über das "AddProductToCatalog"-Command des "Fridge"
erweitert werden.

Eine "Order"-Klasse wurde erstellt, um sich einen Verlauf der Bestellungen anzeigen lassen zu können.
Beim Erstellen einer Bestellung wird ein neues "Order"-Objekt angelegt und in einer Liste im "Fridge" gespeichert.

Wurde eine Bestellung erfolgreich abgeschlossen, wird ein "Receipt"-Objekt erstellt. In dieser ist nicht nur
erstichtlich, was bestellt wurde, sondern auch zu welchem Zeitpunkt.

Das "Weather" ist ein einfaches Enum, welches aktuell nur aus "sunny" und "cloudy" besteht. Dies wurde
als Enum implementiert, da keine weiteren Attribute notwendig sind und auf diese Art und Weise
auch nachträglich leicht weitere Wetterzustände gespeichert werden könnten.


---------------------------------------------------------------------------
#### Ilona

Rules + Functionality erklären (welche Testfälle wir ausprobiert haben, welches Device wann wie reagiert)

Erklärung, wie Befehle in UI eingegeben werden müssen

---------------------------------------------------------------------------
## 3. Verwendung Applikation

### 3.1 Bedienung UI
Um die Applikation zu bedienen, muss zuerst das HomeAutomationSystem gestartet werden.
Im Anschluss können über die Kommandozeile Commands eingegeben werden, welche vom UI entgegengenommen und an die zuständigen Actors weitergeleitet werden.
Folgend werden die bereitgestellten Commands aufgelistet und beschrieben, wie diese verwendet werden können um die Applikation zu bedienen.

#### 3.1.1 Temperatur verändern:
- Command: t [temperature]

Mit "t" wird angedeutet, dass die Umgebungstemperatur verändert werden soll.
Die Temperatur muss als Zahl angegeben werden und entspricht der Einheit "Grad Celsius".
Die Einheit wird automatisch angehängt.
Da die Temperatur über einen TemperatureSensor gesteuert wird, ist des nicht nötig, dieses Command händisch auszuführen, es wurde aber für Testzwecke implementiert.

#### 3.1.2 AC ein- oder ausschalten:
- Command: a [true/false]

Mit "a" wird angedeutet, dass die AC bedient werden soll.
Mit [true] kann die AC eingeschaltet, mit [false] kann sie ausgeschalten werden.

#### 3.1.3 Mediastation bedienen:
- Command: m [true/false]

Mit "m" wird angedeutet, dass die Media Station bedient werden soll.
Mit [true] kann ein Film gestartet werden, mit [false] kann ein Film gestoppt werden.

#### 3.1.4 Wetter verändern:
- Command: w [sunny/cloudy]

Mit "w" wird angedeutet, dass das Wetter verändert werden soll.
Die Wettersituation kann mit [sunny] auf sonnig und mit [cloudy] auf wolking geändert werden.
Da das Wetter über einen WeatherSensor gesteuert wird, ist des nicht nötig, dieses Command händisch auszuführen, es wurde aber für Testzwecke implementiert.

#### 3.1.5 Kühlschrank bedienen:

Um den Kühlschrank zu bedienen, wurde bei der Initialisierung des Kühlschranks ein Default-Produktkatalog erstellt der folgende Artikel enthält:
milk, cheese, yogurt, butter, chicken, coke, salad.
Diese Produkte können bestellt und konsumiert werden. Sollten andere Produkte bestellt oder konsumiert werden wollen, können diese manuell hinzugefügt werden.

**Produkt zum Produktkatalog hinzufügen:**
- Command: f catalog

Mit "f" wird angedeutet, dass der Kühlschrank angesprochen werden soll und mit "catalog", dass ein Produkt zum Produktkatalog hinzugefügt werden soll.
Daraufhin erscheint die Meldung, dass [name] [price] [weight] des Produktes, welches hinzugefügt werden will, eingegeben werden soll.
Dem Produkt kann ein beliebiger Name, ein beliebiger Preis als Zahl und ein beliebiges Gewicht als Zahl mitgegeben werden.

**Produkt bestellen:**
- Command: f order [name] [Optional amount]
    
Mit "order" wird angedeutet, dass eine Produkt-Bestellung aufgegeben werden soll.
Dazu muss der Produktname angegeben werden. Zusätzlich kann die zu bestellende Menge angegeben werden, wird diese nicht angegeben, wird das Produkt automatisch 1x bestellt.

**Produkt konsumieren:**
- Command: f consume [name] [Optional amount]

Mit "consume" wird angedeutet, dass ein Produkt konsumiert werden soll.
Dazu muss der Produktname angegeben werden. Zusätzlich kann die zu konusmierende Menge angegeben werden, wird diese nicht angegeben, wird das Produkt automatisch 1x konsumiert.
  
**Im Kühlschrank befindliche Produkte ausgeben:**
- Command: f products 
  
Mit "products" wird eine Liste an Produkten ausgegeben, welche sich aktuell im Kühlschrank befinden.

**Bestellhistorie ausgeben:**
- Command: f orders

Mit "orders" wird eine Historie der erfolgreich durchgeführten Bestellungen ausgegeben.



