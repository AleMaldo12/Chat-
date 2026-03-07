package com.chat.message.service

import com.chat.message.dto.ConfirmRequest
import com.chat.message.dto.LoginRequest
import com.chat.message.dto.RegisterRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.nio.charset.StandardCharsets

@Service
class AuthService(
    private val cognitoClient: CognitoIdentityProviderClient,
    @Value("\${aws.cognito.clientId}") private val clientId: String,
    @Value("\${aws.cognito.clientSecret}") private val clientSecret: String
) {

    fun register(request: RegisterRequest) {
        val secretHash = calculateSecretHash(clientId, clientSecret, request.nickname)
        val signUpRequest = SignUpRequest.builder()
            .clientId(clientId)
            .secretHash(secretHash)
            .username(request.nickname)
            .password(request.password)
            .userAttributes(
                AttributeType.builder().name("email").value(request.email).build(),
                AttributeType.builder().name("nickname").value(request.nickname).build()
            )
            .build()
        cognitoClient.signUp(signUpRequest)
    }

    fun confirmRegistration(request: ConfirmRequest) {
        val secretHash = calculateSecretHash(clientId, clientSecret, request.email)
        val confirmRequest = ConfirmSignUpRequest.builder()
            .clientId(clientId)
            .secretHash(secretHash)
            .username(request.email)
            .confirmationCode(request.confirmationCode)
            .build()
        cognitoClient.confirmSignUp(confirmRequest)
    }

    // --- ESTA FUNCIÓN ES LA QUE EL CONTROLADOR NO ENCONTRABA ---
    fun resendCode(email: String) { // Asegúrate que recibe un String
        val secretHash = calculateSecretHash(clientId, clientSecret, email)
        val resendRequest = ResendConfirmationCodeRequest.builder()
            .clientId(clientId)
            .secretHash(secretHash)
            .username(email)
            .build()
        cognitoClient.resendConfirmationCode(resendRequest)
    }

    fun login(request: LoginRequest): String {
        val secretHash = calculateSecretHash(clientId, clientSecret, request.email)
        val authRequest = InitiateAuthRequest.builder()
            .clientId(clientId)
            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .authParameters(mapOf(
                "USERNAME" to request.email,
                "PASSWORD" to request.password,
                "SECRET_HASH" to secretHash
            ))
            .build()

        val response = cognitoClient.initiateAuth(authRequest)
        return response.authenticationResult().idToken()
    }

    private fun calculateSecretHash(userPoolClientId: String, userPoolClientSecret: String, userName: String): String {
        val hmacSha256Algorithm = "HmacSHA256"
        val signingKey = SecretKeySpec(userPoolClientSecret.toByteArray(StandardCharsets.UTF_8), hmacSha256Algorithm)
        val mac = Mac.getInstance(hmacSha256Algorithm)
        mac.init(signingKey)
        mac.update(userName.toByteArray(StandardCharsets.UTF_8))
        val rawHmac = mac.doFinal(userPoolClientId.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(rawHmac)
    }
}