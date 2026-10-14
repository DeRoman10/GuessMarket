package com.data.files.entities;

import com.data.events.entities.Event;
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class Market implements Serializable {
    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }
}
