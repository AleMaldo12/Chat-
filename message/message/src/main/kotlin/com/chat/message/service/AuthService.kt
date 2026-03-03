package com.chat.message.service

import com.chat.message.dto.ConfirmRequest
import com.chat.message.dto.LoginRequest
import com.chat.message.dto.RegisterRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.nio.charset.StandardCharsets
import java.util.UUID
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest

@Service
class AuthService(
    private val cognitoClient: CognitoIdentityProviderClient,
    @Value("\${aws.cognito.clientId}") private val clientId: String,
    @Value("\${aws.cognito.clientSecret}") private val clientSecret: String
) {

    fun register(request: RegisterRequest) {
        try {
            val internalUsername = UUID.randomUUID().toString()
            val secretHash = calculateSecretHash(clientId, clientSecret, internalUsername)

            val signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash)
                .username(internalUsername)
                .password(request.password)
                .userAttributes(
                    AttributeType.builder().name("email").value(request.email).build(),
                    // Agregamos el nickName aquí para cumplir con el esquema de AWS
                    AttributeType.builder().name("nickname").value(request.nickName).build()
                )
                .build()

            cognitoClient.signUp(signUpRequest)
        } catch (e: Exception) {
            println("ERROR DETECTADO EN COGNITO: ${e.message}")
            throw e
        }
    }

    fun confirmRegistration(request: ConfirmRequest) {
        // 1. IMPORTANTE: Aquí hay un detalle. Cognito espera el USERNAME interno.
        // Si usaste el correo como nombre de usuario, usa el email.
        // Si usaste el UUID, tendríamos que buscarlo.
        // Pero si configuraste "Email Alias", normalmente acepta el email aquí:
        val secretHash = calculateSecretHash(clientId, clientSecret, request.email)

        val confirmRequest = ConfirmSignUpRequest.builder()
            .clientId(clientId)
            .secretHash(secretHash)
            .username(request.email)
            .confirmationCode(request.confirmationCode)
            .build()

        cognitoClient.confirmSignUp(confirmRequest)
    }

    fun resendCode(email: String) {
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

        // Caso 1: Todo bien, tenemos token
        if (response.authenticationResult() != null) {
            return response.authenticationResult().accessToken()
        }

        // Caso 2: El desafío que te está saliendo ahora
        // Caso 2: El desafío de contraseña nueva Y falta de atributos
        if ("NEW_PASSWORD_REQUIRED" == response.challengeNameAsString()) {
            val challengeResponse = RespondToAuthChallengeRequest.builder()
                .challengeName("NEW_PASSWORD_REQUIRED")
                .clientId(clientId)
                .challengeResponses(mapOf(
                    "USERNAME" to request.email,
                    "NEW_PASSWORD" to request.password,
                    "SECRET_HASH" to secretHash,
                    // AWS exige que envíes los atributos obligatorios faltantes aquí
                    "userAttributes.nickname" to "UsuarioPelicula" // Puedes usar request.email si prefieres
                ))
                .session(response.session())
                .build()

            val challengeResult = cognitoClient.respondToAuthChallenge(challengeResponse)
            return challengeResult.authenticationResult().accessToken()
        }

        throw Exception("Desafío de AWS no soportado: ${response.challengeNameAsString()}")
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