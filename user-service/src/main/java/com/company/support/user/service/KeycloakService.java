package com.company.support.user.service;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KeycloakService {

    private static final Logger LOG = Logger.getLogger(KeycloakService.class);

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "keycloak.admin.url")
    String keycloakAdminUrl;

    @ConfigProperty(name = "keycloak.admin.realm")
    String realm;

    @ConfigProperty(name = "keycloak.admin.client-id")
    String clientId;

    @ConfigProperty(name = "keycloak.admin.client-secret", defaultValue = "")
    String clientSecret;

    private Uni<String> getAdminToken() {
        String tokenUrl = keycloakAdminUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        return webClient.postAbs(tokenUrl)
                .sendForm(io.vertx.mutiny.core.MultiMap.caseInsensitiveMultiMap()
                        .add("grant_type", "client_credentials")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret))
                .map(response -> {
                    String token = response.bodyAsJsonObject().getString("access_token");
                    if (token == null) {
                        throw new InternalServerErrorException("Failed to get admin token: " + response.bodyAsString());
                    }
                    return token;
                });
    }

    public Uni<String> createUser(CreateUserRequest request) {
        return getAdminToken().flatMap(token -> {
            var body = new JsonObject()
                    .put("username", request.getUserLogin())
                    .put("email", request.getEmail())
                    .put("firstName", request.getUserFullName().split(" ")[0])
                    .put("lastName", request.getUserFullName().contains(" ")
                            ? request.getUserFullName().substring(request.getUserFullName().indexOf(" ") + 1) : "")
                    .put("enabled", true)
                    .put("emailVerified", true)
                    .put("credentials", new JsonArray().add(new JsonObject()
                            .put("type", "password")
                            .put("value", request.getPassword())
                            .put("temporary", false)));

            LOG.infof("Creating Keycloak user: %s", request.getUserLogin());

            return webClient.postAbs(keycloakAdminUrl + "/admin/realms/" + realm + "/users")
                    .putHeader("Authorization", "Bearer " + token)
                    .putHeader("Content-Type", "application/json")
                    .sendJsonObject(body)
                    .flatMap(response -> {
                        LOG.infof("Create user response status: %d", response.statusCode());
                        if (response.statusCode() != 201) {
                            throw new InternalServerErrorException(
                                    "Failed to create Keycloak user: " + response.bodyAsString());
                        }
                        String location = response.getHeader("Location");
                        String keycloakId = location.substring(location.lastIndexOf("/") + 1);

                        // Назначаем роль отдельным запросом
                        return assignRealmRole(token, keycloakId, request.getRole().name().toLowerCase())
                                .replaceWith(keycloakId);
                    });
        });
    }

    private Uni<Void> assignRealmRole(String token, String keycloakId, String roleName) {
        // Сначала получаем объект роли по имени
        return webClient.getAbs(keycloakAdminUrl + "/admin/realms/" + realm + "/roles/" + roleName)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .flatMap(response -> {
                    if (response.statusCode() != 200) {
                        LOG.warnf("Role '%s' not found in Keycloak, skipping assignment", roleName);
                        return Uni.createFrom().voidItem();
                    }
                    var roleObj = response.bodyAsJsonObject();
                    var roleArray = new JsonArray().add(roleObj);

                    // Назначаем роль пользователю
                    return webClient.postAbs(keycloakAdminUrl + "/admin/realms/" + realm
                                    + "/users/" + keycloakId + "/role-mappings/realm")
                            .putHeader("Authorization", "Bearer " + token)
                            .putHeader("Content-Type", "application/json")
                            .sendBuffer(io.vertx.mutiny.core.buffer.Buffer.buffer(roleArray.encode()))
                            .invoke(r -> LOG.infof("Assign role '%s' response: %d", roleName, r.statusCode()))
                            .replaceWithVoid();
                });
    }

    public Uni<Void> updateUser(String keycloakId, UpdateUserRequest request) {
        return getAdminToken().flatMap(token -> {
            var body = new JsonObject();
            if (request.getEmail() != null) body.put("email", request.getEmail());
            if (request.getUserFullName() != null) {
                body.put("firstName", request.getUserFullName().split(" ")[0]);
                body.put("lastName", request.getUserFullName().contains(" ")
                        ? request.getUserFullName().substring(request.getUserFullName().indexOf(" ") + 1) : "");
            }

            return webClient.putAbs(keycloakAdminUrl + "/admin/realms/" + realm + "/users/" + keycloakId)
                    .putHeader("Authorization", "Bearer " + token)
                    .putHeader("Content-Type", "application/json")
                    .sendJsonObject(body)
                    .replaceWithVoid();
        });
    }

    public Uni<Void> deleteUser(String keycloakId) {
        return getAdminToken().flatMap(token ->
                webClient.deleteAbs(keycloakAdminUrl + "/admin/realms/" + realm + "/users/" + keycloakId)
                        .putHeader("Authorization", "Bearer " + token)
                        .send()
                        .replaceWithVoid());
    }
}