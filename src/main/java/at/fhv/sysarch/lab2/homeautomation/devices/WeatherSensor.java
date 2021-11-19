package at.fhv.sysarch.lab2.homeautomation.devices;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;

/*
Weather Sensor measures the weather condition.
-------------------------------------------------
Rules:
If the weather is sunny the blinds will close.
If the weather is not sunny the blinds will open (unless a movie is playing).
 */
public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand{}

    public static final class ReadWeather implements WeatherSensor.WeatherCommand {
        final Weather weather;

        public ReadWeather(Weather weather) {
            this.weather = weather;
        }
    }

    public WeatherSensor(ActorContext<WeatherCommand> context) {
        super(context);
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return null;
    }

}
