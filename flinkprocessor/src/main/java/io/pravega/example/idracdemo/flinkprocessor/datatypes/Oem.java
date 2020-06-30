package io.pravega.example.idracdemo.flinkprocessor.datatypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Oem implements Serializable {
    public Dell Dell;

    @Override
    public String toString() {
        return "oem{" +
                "Dell=" + Dell +
                '}';
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dell implements Serializable {
        public String ContextID;
        public String Label;

        @Override
        public String toString() {
            return "Dell{" +
                    "ContextID='" + ContextID + '\'' +
                    ", Label='" + Label + '\'' +
                    '}';
        }
    }
}
