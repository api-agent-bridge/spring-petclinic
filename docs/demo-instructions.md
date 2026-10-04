## 5.1 Starting the app in mock mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

## 6.1 Starting Keycloak

```bash
docker compose -f keycloak/compose.yaml up -d
```

To log out of Keycloak, open <http://localhost:8180/realms/petclinic/protocol/openid-connect/logout> and confirm.

Keycloak runs at <http://localhost:8180> with the realm `petclinic`. To connect the MCP Inspector to `http://localhost:8080/mcp`:

1. In the server's OAuth settings, set the Client ID to `mcp-inspector` and leave the Client Secret empty. Without a client ID, the Inspector tries to register itself, and Keycloak refuses.
2. Uncheck "Request refresh token", next to Client ID and Client Secret. While it is checked, the Inspector asks for `offline_access`, and Keycloak refuses the sign-in with `invalid_scope`.
3. Connect, and sign in as one of two users:

| User | Password | Gets |
| --- | --- | --- |
| `read` | `read` | `mcp:tools`: the read tools |
| `write` | `write` | `mcp:tools` and `petclinic:write`: the read and the write tools |

Keycloak reads `keycloak/petclinic-realm.json` at the first start only. After a change to that file, recreate the container with `docker compose -f keycloak/compose.yaml down` and start it again.
