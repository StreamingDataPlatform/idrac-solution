package io.pravega.idracsolution.flinkprocessor.datatypes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class tag {
    public String id;
    public double v;
    public boolean q;
    public long t;
}
