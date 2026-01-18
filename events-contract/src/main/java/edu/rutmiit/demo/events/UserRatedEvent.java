package edu.rutmiit.demo.events;

import java.io.Serializable;

public record UserRatedEvent(
        Long userId,
        Double rating,
        String verdict
) implements Serializable {}