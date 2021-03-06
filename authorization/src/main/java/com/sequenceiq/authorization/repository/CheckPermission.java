package com.sequenceiq.authorization.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.authorization.resource.ResourceAction;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermission {
    ResourceAction action() default ResourceAction.READ;
}
