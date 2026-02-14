package com.example.demo.Auth;

import com.example.demo.Auth.DTOs.AuthResult;
import com.example.demo.Exceptions.*;
import com.example.demo.Jwt.JwtService;
import com.example.demo.User.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResult register(String username, String password, String deviceId) {
        Optional<UserEntity> existingUser = userService.findByUsername(username);

        if (existingUser.isPresent()) {
            throw new UserExistsException("User already exists");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(Role.USER);
        userService.save(newUser);

        String accessToken = jwtService.generateAccessTokenFromUsername(newUser.getUsername());
        String refreshToken = jwtService.generateRefreshTokenFromUsername(newUser.getUsername());

        RefreshTokenEntity newRefreshToken = new RefreshTokenEntity();
        newRefreshToken.setToken(hashToken(refreshToken));
        newRefreshToken.setExpiration(jwtService.getExpirarationFromToken(refreshToken));
        newRefreshToken.setDeviceId(deviceId);
        newRefreshToken.setUser(newUser);

        refreshTokenService.save(newRefreshToken);

        return new AuthResult(accessToken, refreshToken, newUser.getRole().name());
    }

    @Transactional
    public AuthResult login(String username, String password, String deviceId) {
        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean validPassword = passwordEncoder.matches(password, user.getPassword());

        if (!validPassword) {
            throw new InvalidPasswordException("Invalid password");
        }

        String accessToken = jwtService.generateAccessTokenFromUsername(user.getUsername());
        String refreshToken = jwtService.generateRefreshTokenFromUsername(user.getUsername());

        RefreshTokenEntity rt = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        rt.setToken(hashToken(refreshToken));
        rt.setExpiration(jwtService.getExpirarationFromToken(refreshToken));
        rt.setDeviceId(deviceId);
        rt.setUser(user);

        refreshTokenService.save(rt);

        return new AuthResult(accessToken, refreshToken, user.getRole().name());
    }

    @Transactional
    public AuthResult refreshToken(String refreshToken, String deviceId) {

        if (!jwtService.verifyToken(refreshToken)) {
            throw new InvalidRefreshTokenException("Invalid Refresh Token");
        }

        String username = jwtService.getUsernameFromToken(refreshToken);
        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RefreshTokenEntity storedToken = refreshTokenService.findByUserAndDeviceId(user, deviceId);

        String incomingTokenHash = hashToken(refreshToken);
        if (!incomingTokenHash.equals(storedToken.getToken())) {
            throw new InvalidRefreshTokenException("Token does not match records");
        }

        if (storedToken.getExpiration().before(new java.util.Date())) {
            refreshTokenService.delete(storedToken);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        String newAccessToken = jwtService.generateAccessTokenFromUsername(user.getUsername());
        String newRefreshToken = jwtService.generateRefreshTokenFromUsername(user.getUsername());

        storedToken.setToken(hashToken(newRefreshToken));
        storedToken.setExpiration(jwtService.getExpirarationFromToken(newRefreshToken));

        refreshTokenService.save(storedToken);

        return new AuthResult(newAccessToken, newRefreshToken, user.getRole().name());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidHashException("Error hashing token");
        }
    }

}
