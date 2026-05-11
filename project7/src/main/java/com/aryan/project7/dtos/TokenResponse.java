package com.aryan.project7.dtos;

public record TokenResponse (String accessToken,
                            String refreshToken,
                            Long expiresIn,
                            String tokenType,
                            UserDto userDto){

    public static TokenResponse of(String accessToken,
                                   String refreshToken,
                                   Long expiresIn,
                                   UserDto userDto){
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer", userDto);
    }
}
