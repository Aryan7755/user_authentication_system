package com.aryan.project7.helper;

import java.util.UUID;

public class UserHelper {
    public static UUID parseUUID(String uId){
        return UUID.fromString(uId);
    }
}
