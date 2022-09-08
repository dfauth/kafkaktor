package com.github.dfauth.avro;

import com.github.dfauth.avro.actor.ActorCreationRequest;

import java.util.Collections;
import java.util.Map;

public class ActorCreation {

    public static <T> ActorCreationRequest newActor(String name, Class<T> actorClass) {
        return newActor(name, actorClass, Collections.emptyMap());
    }

    public static <T> ActorCreationRequest newActor(String name, Class<T> actorClass, Map<String, Object> props) {
        return ActorCreationRequest.newBuilder().setClassName(actorClass.getName()).setName(name).setProperties(props).build();
    }
}
