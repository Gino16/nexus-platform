# ADR-005 Identity and Access Management

## Status: Accepted

## Decision

We will implement a centralized Identity and Access Management (IAM) system for the Nexus platform.
This system will handle authentication, authorization, and user management, ensuring secure access
to platform resources and services.

We will use Keycloak as Identity Provider (IdP) by Oath2/OICD protocol for authentication and
authorization. Keycloak will manage user identities, roles, and permissions, providing a single
sign-on (SSO) experience across the platform.

## Alternatives Considered

1. **Using a different IAM solution**: We could consider other IAM solutions such as Auth0, Okta, or
   AWS Cognito. However, Keycloak is open-source, highly customizable, and provides the necessary
   features for our platform.
2. **Decentralized IAM**: Each service could manage its own authentication and authorization. This
   approach would increase complexity, reduce security consistency, and make it harder to provide a
   seamless SSO experience across the platform.
3. **Custom-built IAM**: Developing a custom IAM solution would require significant development
   effort, ongoing maintenance, and could introduce security vulnerabilities. Leveraging an
   established solution like Keycloak reduces these risks.

## Consequences

- **Pros**:
    - Centralized IAM simplifies user management and access control across the platform.
    - Keycloak provides robust security features, including support for various authentication
      protocols, multifactor authentication, and user federation.
    - SSO improves user experience by allowing users to authenticate once and access multiple
      services without re-entering credentials.
    - Keycloak's open-source nature allows for customization and integration with other services as
      needed.
- **Cons**:
    - Introducing a centralized IAM system adds a component to the architecture, which
      may require monitoring and maintenance.
    - Keycloak may introduce a learning curve for developers and administrators unfamiliar with its
      configuration and management.
    - Potential single point of failure if the IAM system experiences downtime, which could impact
      access to platform resources and services.