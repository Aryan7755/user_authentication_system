package com.aryan.project7.helper;

import java.util.UUID;

// A handy helper to deal with UUID conversions without cluttering the main logic
public class UserHelper {

    // Converts a standard String ID into a UUID object that JPA and Postgres love
    public static UUID parseUUID(String uId){
        // Tip: You might want to wrap this in a try-catch later to throw a
        // ResourceNotFound or BadRequest if the string isn't a valid UUID!
        return UUID.fromString(uId);
    }
}