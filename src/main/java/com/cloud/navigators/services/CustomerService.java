package com.cloud.navigators.services;

import com.cloud.navigators.client.ProductCartClient;
import com.cloud.navigators.model.AuthResponse;
import com.cloud.navigators.model.ResetPassword;
import com.cloud.navigators.model.UserLogin;
import com.cloud.navigators.model.UserSignup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.utils.ImmutableMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.client-secret}")
    private String clientSecret;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.region}")
    private String region ;

    public void signupUser(UserSignup user){

        try (CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build()) {

            List<AttributeType> attributes = Arrays.asList(
                    AttributeType.builder().name("family_name").value(user.getName()).build(),
                    AttributeType.builder().name("picture").value(user.getPhotoUrl()).build()
            );


            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .userAttributes(attributes)
                    .secretHash(calculateSecretHash(user.getEmail(),clientId, clientSecret))
                    .build();
            SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequest);
            System.out.println("User signed up successfully. Confirmation code: " + signUpResponse.codeDeliveryDetails().destination());

        } catch (CognitoIdentityProviderException e) {
            System.err.println("Error signing up user: " + e.awsErrorDetails().errorMessage());
        }
    }

    public ConfirmSignUpResponse confirmUser(String code, UserSignup user){

        try (CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build()) {
            ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(user.getEmail())
                    .confirmationCode(code) // Replace with the actual code received by the user
                    .secretHash(calculateSecretHash(user.getEmail(), clientId, clientSecret))
                    .build();

            ConfirmSignUpResponse confirmSignUpResponse = cognitoClient.confirmSignUp(confirmSignUpRequest);

            System.out.println("Verified");
            return confirmSignUpResponse ;
        }
    }


    public Object authenticateUser(UserLogin user) {

        // Use the provided username and password for authentication
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .build();

        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", user.getEmail());
        authParameters.put("PASSWORD", user.getPassword());
        authParameters.put("SECRET_HASH", calculateSecretHash(user.getEmail(),clientId,
                clientSecret));

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();

        try {
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            if (authResponse.challengeName() != null && authResponse.challengeName().toString().equals("NEW_PASSWORD_REQUIRED")) {
                return "resetpassword" ;
            }

            GetUserResponse getUserResponse = cognitoClient.getUser(GetUserRequest.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .build());


            Map<String, String> userAttributes = getUserResponse.userAttributes()
                    .stream()
                    .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

            return AuthResponse.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .refreshToken(authResponse.authenticationResult().refreshToken())
                    .idToken(authResponse.authenticationResult().idToken())
                    .photolink(userAttributes.get("picture"))
                    .username(userAttributes.get("family_name"))
                    .build() ;

        } catch (CognitoIdentityProviderException e) {
            // Authentication failed, handle the exception
            cognitoClient.close();
            throw new RuntimeException("Failed to Authenticate") ;
        }finally {
            cognitoClient.close();
        }
    }

    public Object handleResetPassword(UserLogin user, ResetPassword resetPassword) {

        // Use the provided username and password for authentication
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .build();

        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", user.getEmail());
        authParameters.put("PASSWORD", user.getPassword());
        authParameters.put("SECRET_HASH", calculateSecretHash(user.getEmail(),clientId,
                clientSecret));

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();

        try {
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            if (authResponse.challengeName() != null && authResponse.challengeName().toString().equals("NEW_PASSWORD_REQUIRED")) {

                // The user is required to change their password
                String sessionId = authResponse.session();
                String newPassword = resetPassword.getNewpassword();

                // Call a method to respond to the new password challenge
                RespondToAuthChallengeResponse respondToAuthChallengeResponse =
                        respondToNewPasswordChallenge(cognitoClient, sessionId, newPassword, user);

                return AuthResponse.builder()
                        .accessToken(respondToAuthChallengeResponse.authenticationResult().accessToken())
                        .refreshToken(respondToAuthChallengeResponse.authenticationResult().refreshToken())
                        .idToken(respondToAuthChallengeResponse.authenticationResult().idToken())
                        .username(user.getEmail())
                        .photolink(user.getProfile_pic() == null ? "Default" : user.getProfile_pic())
                        .build() ;
            }

            return AuthResponse.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .refreshToken(authResponse.authenticationResult().refreshToken())
                    .idToken(authResponse.authenticationResult().idToken())
                    .username(user.getEmail())
                    .photolink(user.getProfile_pic() == null ? "Default" : user.getProfile_pic())
                    .build() ;

        } catch (CognitoIdentityProviderException e) {
            // Authentication failed, handle the exception
            cognitoClient.close();
            throw new RuntimeException("Failed to Authenticate") ;
        }finally {
            cognitoClient.close();
        }
    }

    public RespondToAuthChallengeResponse respondToNewPasswordChallenge(CognitoIdentityProviderClient cognitoClient, String sessionId, String newPassword, UserLogin user) {
        RespondToAuthChallengeRequest challengeRequest =RespondToAuthChallengeRequest.builder()
                .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .clientId(clientId)
                .session(sessionId)
                .challengeResponses(ImmutableMap.of("NEW_PASSWORD", newPassword,
                        "USERNAME", user.getEmail(),
                        "SECRET_HASH", calculateSecretHash(user.getEmail(),clientId, clientSecret),
                        "family_name", (user.getFamily_name() == null ? "NOTGIVEN" : user.getFamily_name())))
                .build();

        RespondToAuthChallengeResponse challengeResponse = cognitoClient.respondToAuthChallenge(challengeRequest);

        // Handle the response accordingly
        if (challengeResponse.challengeName() != null && "SUCCESS".equals(challengeResponse.challengeName().toString())) {
            // Password change was successful
            System.out.println("Updated Password");
        } else {
            // Handle the failure
            System.out.println("Failure");
        }

        return challengeResponse ;
    }



    private static String calculateSecretHash(String username, String clientId, String clientSecret) {
        try {
            String message = username + clientId;
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] hash = sha256HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean validateToken(HttpSession session){
        AuthResponse response = (AuthResponse)session.getAttribute("accessToken") ;
        return response == null || response.getAccessToken() == null;
    }


}
