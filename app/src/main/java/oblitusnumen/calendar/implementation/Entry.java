package oblitusnumen.calendar.implementation;

import oblitusnumen.calendar.implementation.content.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Entry {
    public final UUID uid;
    public String name = "";
    public List<Tag> tags = new ArrayList<>();
    public List<Content> contents = new ArrayList<>();

    public Entry(UUID uid) {
        this.uid = uid;
    }
}
