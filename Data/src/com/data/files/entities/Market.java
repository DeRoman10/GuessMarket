package com.data.files.entities;

import com.data.events.entities.Event;
import com.data.users.entities.User;
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class Market implements Serializable {
    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<Event> events;

    @XmlElementWrapper(name = "GM-users")
    @XmlElement(name = "GM-user")
    private List<User> users;

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}
