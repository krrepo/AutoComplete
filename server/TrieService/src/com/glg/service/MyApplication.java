package com.glg.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.glg.service.resources.TrieResource;

public class MyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        final Set<Class<?>> classes = new HashSet<Class<?>>();

        // register root resources
        classes.add(TrieResource.class);
        return classes;
    }
}
