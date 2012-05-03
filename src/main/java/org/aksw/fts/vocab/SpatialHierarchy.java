package org.aksw.fts.vocab;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class SpatialHierarchy {
    public static final String ns = "http://ns.aksw.org/spatialHierarchy/";

    public static final Resource isLocatedIn = ResourceFactory.createProperty(ns + "isLocatedIn");
    public static final Resource contains = ResourceFactory.createProperty(ns + "contains");

    public static final Resource City = ResourceFactory.createResource(ns + "City");
    public static final Resource Country = ResourceFactory.createResource(ns + "Country");
    //public static final Resource Address = ResourceFactory.createResource(ns + "Address");

}