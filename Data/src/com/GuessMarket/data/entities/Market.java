package com.GuessMarket.data.entities;
import java.util.List;
import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class Market {
    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
